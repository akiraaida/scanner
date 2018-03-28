package com.example.akira.scanner;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class DisplayActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);
        // TODO: Re-add this
        //Intent intent = getIntent();
        //String imgPath = intent.getStringExtra("imgPath");
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("onManager", "OpenCV loaded successfully");
                    String imgPath = DisplayActivity.this.getFilesDir().getAbsolutePath() + "/receipt.jpg";
                    loadImageFromStorage(imgPath);
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


    // TODO: Move this into a callback function
    private void loadImageFromStorage(String path) {
        try {
            File f = new File(path);
            Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
            processImg(b);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    private void dispImage(Mat mat) {
        Bitmap img = Bitmap.createBitmap(mat.cols(), mat.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, img);
        ImageView imgView = findViewById(R.id.displayImage);
        imgView.setImageBitmap(img);
    }

    private void processImg(Bitmap bmp) {
        // Convert the Bitmap object to a Mat object to use for CV.
        Mat mat = new Mat (bmp.getHeight(), bmp.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(bmp, mat);
        Mat orig = new Mat();
        mat.copyTo(orig);
        // Convert the Mat object from BGR color code to gray scale.
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
        // Create an inverse color threshold from 180-255 on the original image to create a mask.
        // Since it's inverse, the colors kept will be the blacks from 0-75 (the text color).
        Mat mask = new Mat();
        Imgproc.threshold(mat, mask, 180, 255, Imgproc.THRESH_BINARY_INV);
        // Grow the mask's response by dilating it to determine where the text is later on.
        // Use the cross kernel which will increase the response in both the x and y direction.
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(3, 3));
        Mat dilated = new Mat();
        Imgproc.dilate(mask, dilated, kernel, new Point(-1, -1), 9);
        Mat temp = new Mat();
        dilated.copyTo(temp);
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(temp, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);
        for (MatOfPoint contour : contours) {
            Rect rect = Imgproc.boundingRect(contour);
            if (rect.width > 20 && rect.height > 20) {
                Imgproc.rectangle(orig, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 0, 0), 2);
            }
        }
        dispImage(orig);
    }

    private void detectText(Bitmap b) {
        TessBaseAPI tess = new TessBaseAPI();
        tess.init(this.getFilesDir().getAbsolutePath(), "eng");
        tess.setImage(b);
        String text = tess.getUTF8Text();
        TextView textView = findViewById(R.id.ocrText);
        textView.setText(text);
    }
}
