package com.example.akira.scanner;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class DisplayActivity extends AppCompatActivity {

    private TessBaseAPI mTess = null;
    private int mHeightLen = -1;
    private int mFalsePos = -1;
    private int mFalseCaptureHeight = -1;
    private int mFalseCaptureWidth = -1;
    private Bitmap mImg = null;
    private String mImgPath = "";
    private String mText = "";
    private int mPossibleText = -1;
    private int mCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        TextView textView = findViewById(R.id.dispText);
        textView.setMovementMethod(new ScrollingMovementMethod());

        mTess = new TessBaseAPI();
        mTess.init(this.getFilesDir().getAbsolutePath(), "eng");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        File file = new File(mImgPath);
        file.delete();
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("onManager", "OpenCV loaded successfully");
                    // Testing code
//                    mImgPath = DisplayActivity.this.getFilesDir().getAbsolutePath() + "/receipt.jpg";
                    Intent intent = getIntent();
                    mImgPath = intent.getStringExtra("imgPath");
                    loadImageFromStorage(mImgPath);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
    }

    private void loadImageFromStorage(String path) {
        try {
            File f = new File(path);
            mImg = BitmapFactory.decodeStream(new FileInputStream(f));
            mHeightLen = (mImg.getHeight() / 100)*2;
            mFalsePos = (mImg.getHeight() / 100);
            mFalseCaptureHeight = (mImg.getHeight() / 100) * 50;
            mFalseCaptureWidth = (mImg.getWidth() / 100) * 50;
            dispImage(mImg);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    private void dispImage(Bitmap bmp) {
        ImageView imgView = findViewById(R.id.displayImage);
        imgView.setImageBitmap(bmp);
    }

    private Bitmap convertMat(Mat mat) {
        Bitmap bmp = Bitmap.createBitmap(mat.cols(), mat.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bmp);
        return bmp;
    }

    private Mat convertBitmap(Bitmap bmp) {
        Mat mat = new Mat (bmp.getHeight(), bmp.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(bmp, mat);
        return mat;
    }

    private void processImg(Bitmap bmp) {
        // Convert the Bitmap object to a Mat object to use for CV.
        Mat mat = convertBitmap(bmp);
        Mat origMat = new Mat();
        mat.copyTo(origMat);
        // Convert the Mat object from BGR color code to gray scale.
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
        // Create an inverse color threshold from 180-255 on the original image to create a mask.
        // Since it's inverse, the colors kept will be the blacks from 0-75 (the text color).
        Mat mask = new Mat();
        Imgproc.threshold(mat, mask, 100, 255, Imgproc.THRESH_BINARY_INV);
        // Grow the mask's response by dilating it to determine where the text is later on.
        // Use the cross kernel which will increase the response in both the x and y direction.
//        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(3, 3));
//        Mat dilated = new Mat();
//        Imgproc.dilate(mask, dilated, kernel, new Point(-1, -1), 5);
//        Mat temp = new Mat();
//        dilated.copyTo(temp);
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(mask, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);
        List<Rect> boundingBoxes = filterContours(contours);
        mPossibleText = boundingBoxes.size();
        for (Rect rect : boundingBoxes) {
//            Imgproc.rectangle(origMat, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 0, 0, 255), 2);
            Mat text = new Mat(origMat, rect);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    detectText(text);
                }
            }).start();
        }
    }

    private List<Rect> filterContours(List<MatOfPoint> contours) {
        // Get the bounding box for each contour
        List<Rect> rects = contours.stream().map(contour -> Imgproc.boundingRect(contour)).collect(Collectors.toList());
        // Remove all small boxes that are false positives
        rects.removeIf(rect -> rect.height < mFalsePos && rect.width < mFalsePos);
        // Remove the large false positives
        rects.removeIf(rect -> (rect.height > mFalseCaptureHeight && rect.width > mFalseCaptureWidth));
        List<List<Rect>> sim = new ArrayList<>();
        while (rects.size() > 0) {
            Rect rect = rects.get(0);
            int bot = rect.y;
            int top = rect.y + rect.height;
            List<Rect> similar = rects.stream().filter(r -> ((Math.abs(r.y - bot) <= mHeightLen) && (Math.abs(r.y + r.height) - top) <= mHeightLen)).collect(Collectors.toList());
            rects = rects.stream().filter(r -> ((Math.abs(r.y - bot) > mHeightLen) || (Math.abs(r.y + r.height) - top) > mHeightLen)).collect(Collectors.toList());
            sim.add(similar);
        }
        List<Rect> boundingBoxes = new ArrayList<>();
        for (List<Rect> horizRects : sim) {
            Point tl = new Point(horizRects.get(0).x, horizRects.get(0).y);
            Point br = new Point(horizRects.get(0).x + horizRects.get(0).width, horizRects.get(0).y + horizRects.get(0).height);
            for (Rect rect : horizRects) {
                if (rect.x < tl.x) {
                    tl = new Point(rect.x, tl.y);
                }
                if (rect.y < tl.y) {
                    tl = new Point(tl.x, rect.y);
                }
                if (rect.x + rect.width > br.x) {
                    br = new Point(rect.x + rect.width, br.y);
                }
                if (rect.y + rect.height > br.y) {
                    br = new Point(br.x, rect.y + rect.height);
                }
            }
            boundingBoxes.add(new Rect(tl, br));
        }
        return boundingBoxes;
    }

    private synchronized void detectText(Mat mat) {
        mCounter += 1;
        Bitmap bmp = convertMat(mat);
        mTess.setImage(bmp);
        String text = mTess.getUTF8Text();
        if (!text.isEmpty()) {
            if (text.contains("$")) {
                if (text.matches(".*\\d+.*") && text.contains(".")) {
                    if (mText.isEmpty()) {
                        mText = text;
                    } else {
                        mText += "\n" + text;
                    }
                }
            }
        }
        Log.i("TEST", mCounter + ", " + mPossibleText);
        if (mCounter == mPossibleText) {
            TextView dispText = findViewById(R.id.dispText);
            dispText.setText(mText);
        }
    }

    public void onSave(View view) {
        Intent intent = new Intent(DisplayActivity.this, ScannerActivity.class);
        startActivity(intent);
        finish();
    }

    public void onDelete(View view) {
        File file = new File(mImgPath);
        file.delete();
        Intent intent = new Intent(DisplayActivity.this, ScannerActivity.class);
        startActivity(intent);
        finish();
    }

    public void onDetect(View view) {
        Button detectBtn = findViewById(R.id.detectText);
        detectBtn.setEnabled(false);
        processImg(mImg);
    }
}
