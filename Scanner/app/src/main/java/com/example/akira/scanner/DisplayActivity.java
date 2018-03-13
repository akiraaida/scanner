package com.example.akira.scanner;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

public class DisplayActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        Intent intent = getIntent();
        long imageAddress = intent.getLongExtra("ImageAddress", -1);
        Mat tmp = new Mat(imageAddress);
        Log.i("AKIRA-DIM-INIT", tmp.height() + ", " + tmp.width());
//        if (imageAddress != -1) {
//            // Get the information from the address
//            Mat tmp = new Mat(imageAddress);
//            Log.i("AKIRA-DIM-INIT", tmp.height() + ", " + tmp.width());
//            // Clone the image since the address may be deleted from the previous activity
//            Mat img = tmp.clone();
//            Bitmap imgBitmap = Bitmap.createBitmap(img.cols(), img.rows(),Bitmap.Config.ARGB_8888);
//            Utils.matToBitmap(img, imgBitmap);
//
//            ImageView imageView = findViewById(R.id.displayImage);
//            imageView.setImageBitmap(imgBitmap);
//        } else {
//            // Image capture failed
//            finish();
//        }
    }
}
