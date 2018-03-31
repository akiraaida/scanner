package com.example.akira.scanner;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {

    List<File> galleryFiles = null;
    int mCurrFile = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        galleryFiles = new ArrayList<>();

        File picDir = new File(this.getFilesDir() + "/pics/");
        File[] files = picDir.listFiles();
        if (files.length > 0) {
            for (File file : files) {
                galleryFiles.add(file);
            }
            mCurrFile = 0;
            dispFile();
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

    private void dispFile() {
        ImageView galleryView = findViewById(R.id.galleryView);
        try {
            File file = galleryFiles.get(mCurrFile);
            galleryView.setImageBitmap(BitmapFactory.decodeStream(new FileInputStream(file)));
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

    public void onHome(View view) {
        Intent intent = new Intent(GalleryActivity.this, ScannerActivity.class);
        startActivity(intent);
        finish();
    }
}
