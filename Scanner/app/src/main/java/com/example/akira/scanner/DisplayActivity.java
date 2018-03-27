package com.example.akira.scanner;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class DisplayActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);
        Intent intent = getIntent();
        String imgPath = intent.getStringExtra("imgPath");
        loadImageFromStorage(imgPath);
    }

    // TODO: Move this into a callback function
    private void loadImageFromStorage(String path) {
        try {
            File f = new File(path);
            Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
            ImageView img = findViewById(R.id.displayImage);
            img.setImageBitmap(b);
            TessBaseAPI tess = new TessBaseAPI();
            tess.init(this.getFilesDir().getAbsolutePath(), "eng");
            tess.setImage(b);
            String text = tess.getUTF8Text();
            Log.i("AKIRA_TEST", text);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

    }
}
