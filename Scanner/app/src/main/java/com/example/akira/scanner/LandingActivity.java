package com.example.akira.scanner;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.icu.util.Output;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class LandingActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_CODE = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!assetsExist()) {
            copyAssets();
        }

        if (ContextCompat.checkSelfPermission(this,
            Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_CODE);
        } else {
            Intent scannerIntent = new Intent(LandingActivity.this, DisplayActivity.class);
            startActivity(scannerIntent);
            finish();
        }
    }

    private boolean assetsExist() {
        File file = new File(this.getFilesDir().getAbsolutePath() + "/tessdata/eng.traineddata");
        return file.exists();
    }

    private void copyAssets() {
        AssetManager assetManager = getAssets();
        String[] files = null;
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            Log.e("AssetsError", "Did not get asset file list", e);
        }
        if (files != null) {
            for (String fileName : files) {
                if (fileName.compareTo("eng.traineddata") == 0) {
                    InputStream in = null;
                    OutputStream out = null;
                    try {
                        in = assetManager.open(fileName);
                        File tessDir = new File(this.getFilesDir().getAbsolutePath() + "/tessdata/");
                        if (!tessDir.exists()) {
                            tessDir.mkdirs();
                        }
                        File outfile = new File(this.getFilesDir().getAbsolutePath() + "/tessdata/", fileName);
                        out = new FileOutputStream(outfile);
                        copyFile(in, out);
                    } catch (IOException e) {
                        Log.e("AssetsError", "Failed to copy asset file: " + fileName, e);
                    } finally {
                        if (in != null) {
                            try {
                                in.close();
                            } catch (IOException e) {
                                Log.e("AssetsError", "Failed to close inputstream", e);
                            }
                        }
                        if (out != null) {
                            try {
                                out.close();
                            } catch (IOException e) {
                                Log.e("AssetsError", "Failed to close outputstream", e);
                            }
                        }
                    }
                }
            }
        }
    }

    private void copyFile(InputStream in, OutputStream out) {
        try {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch (IOException e) {
            Log.e("AssetsError", "Failed to copy file", e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent scannerIntent = new Intent(LandingActivity.this, DisplayActivity.class);
                    startActivity(scannerIntent);
                    finish();
                } else {
                    finish();
                }
            }
        }
    }
}
