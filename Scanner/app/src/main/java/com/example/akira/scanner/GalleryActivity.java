package com.example.akira.scanner;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GalleryActivity extends AppCompatActivity {

    List<File> galleryFiles = null;
    Map<String, String> mNotes = null;
    int mCurrFile = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        galleryFiles = new ArrayList<>();
        mNotes = new HashMap<>();

        File notesFile = new File(this.getFilesDir().getAbsolutePath(), "notes.txt");
        try {
            BufferedReader br = new BufferedReader(new FileReader(notesFile));
            String line;
            StringBuilder data = new StringBuilder();
            String fileName = "";

            while ((line = br.readLine()) != null) {
                if (line.contains("~,~")) {
                    if (!data.toString().isEmpty()) {
                        mNotes.put(fileName, data.toString());
                        data = new StringBuilder();
                    }
                    fileName = line.replace("~,~", "");
                } else {
                    data.append(line);
                    data.append("\n");
                }
            }
            mNotes.put(fileName, data.toString());
            br.close();
        }
        catch (IOException e) {
            Log.e("onCreate", "IOException" + e.getStackTrace());
        }

        File picDir = new File(this.getFilesDir() + "/pics/");
        File[] files = picDir.listFiles();
        if (files.length > 0) {
            for (File file : files) {
                galleryFiles.add(file);
            }
            mCurrFile = 0;
            dispFile();
        }
        if (galleryFiles.size() == 0) {
            Button deleteBtn = findViewById(R.id.deleteBtn);
            deleteBtn.setEnabled(false);
            Button updateBtn = findViewById(R.id.updateBtn);
            updateBtn.setEnabled(false);
        }
    }

    private void checkBounds() {
        Button leftBtn = findViewById(R.id.leftBtn);
        Button rightBtn = findViewById(R.id.rightBtn);
        if (mCurrFile == 0) {
            leftBtn.setEnabled(false);
        } else {
            leftBtn.setEnabled(true);
        }
        if (mCurrFile == galleryFiles.size() - 1) {
            rightBtn.setEnabled(false);
        } else {
            rightBtn.setEnabled(true);
        }
    }

    private void setNotes() {
        File file = galleryFiles.get(mCurrFile);
        String key = file.getAbsolutePath();
        EditText editText = findViewById(R.id.notes);
        String notes = mNotes.get(key);
        editText.setText(notes);
    }

    private void dispFile() {
        ImageView galleryView = findViewById(R.id.galleryView);
        try {
            File file = galleryFiles.get(mCurrFile);
            galleryView.setImageBitmap(BitmapFactory.decodeStream(new FileInputStream(file)));
            setNotes();
            checkBounds();
        } catch (IOException e) {
            Log.e("dispFile", "IOException" + e.getStackTrace());
        }
    }

    public void onClickLeft(View view) {
        mCurrFile -= 1;
        dispFile();
    }

    public void onClickRight(View view) {
        mCurrFile += 1;
        dispFile();
    }

    public void onUpdate(View view) {
        File file = galleryFiles.get(mCurrFile);
        String key = file.getAbsolutePath();
        EditText editText = findViewById(R.id.notes);
        mNotes.put(key, editText.getText().toString());
        clearFile();
        for (Map.Entry<String, String> entry : mNotes.entrySet()) {
            writeFile(entry.getKey(), entry.getValue());
        }
    }

    private void clearFile() {
        try {
            File file = new File(this.getFilesDir().getAbsolutePath(), "notes.txt");
            FileWriter writer = new FileWriter(file);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e("clearFile", "IOException: " + e.getStackTrace());
        }
    }

    public void writeFile(String fileName, String text){
        try{
            File file = new File(this.getFilesDir().getAbsolutePath(), "notes.txt");
            FileWriter writer = new FileWriter(file, true);
            writer.append(fileName + "~,~\n" + text + "\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e("writeFile", "IOException: " + e.getStackTrace());
        }
    }

    private void clearDisp() {
        EditText editText = findViewById(R.id.notes);
        ImageView galleryView = findViewById(R.id.galleryView);
        Button deleteBtn = findViewById(R.id.deleteBtn);
        deleteBtn.setEnabled(false);
        Button updateBtn = findViewById(R.id.updateBtn);
        updateBtn.setEnabled(false);
        editText.setText("");
        galleryView.setImageResource(0);
    }

    public void onDelete(View view) {
        File file = galleryFiles.get(mCurrFile);
        galleryFiles.remove(mCurrFile);
        mNotes.remove(file.getAbsolutePath());
        clearFile();
        for (Map.Entry<String, String> entry : mNotes.entrySet()) {
            writeFile(entry.getKey(), entry.getValue());
        }
        file.delete();
        mCurrFile -= 1;
        if (galleryFiles.size() > 0) {
            mCurrFile = 0;
        }
        if (mCurrFile >= 0) {
            dispFile();
        } else {
            clearDisp();
        }
    }

    public void onHome(View view) {
        Intent intent = new Intent(GalleryActivity.this, ScannerActivity.class);
        startActivity(intent);
        finish();
    }
}
