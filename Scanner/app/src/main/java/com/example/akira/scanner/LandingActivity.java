package com.example.akira.scanner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

public class LandingActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_CODE = 1;
    private static int mPermission = 0;
    private static float mLow = 50;
    private static float mHigh = 200;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        List<String> optionsList = new ArrayList<>();
        optionsList.add("Default");
        optionsList.add("Edges");

        Spinner optionsSpinner = findViewById(R.id.options);
        ArrayAdapter<String> optionsAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, optionsList);
        optionsSpinner.setAdapter(optionsAdapter);

        if (ContextCompat.checkSelfPermission(this,
            Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_CODE);
        } else {
            mPermission = 1;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mPermission = 1;
                } else {
                    finish();
                }
            }
        }
    }

    public void submitLowHy(View view) {
        EditText text = findViewById(R.id.lowHy);
        if (!text.getText().toString().isEmpty()) {
            mLow = Float.parseFloat(text.getText().toString());
        }
    }

    public void submitHighHy(View view) {
        EditText text = findViewById(R.id.highHy);
        if (!text.getText().toString().isEmpty()) {
            mHigh = Float.parseFloat(text.getText().toString());
        }
    }

    public void submitScanner(View view) {
        if (mPermission != 0) {
            Spinner options = findViewById(R.id.options);
            String option = options.getSelectedItem().toString();
            Intent scannerIntent = new Intent(LandingActivity.this, ScannerActivity.class);
            scannerIntent.putExtra("option", option);
            scannerIntent.putExtra("low", mLow);
            scannerIntent.putExtra("high", mHigh);
            startActivity(scannerIntent);
        }
    }
}
