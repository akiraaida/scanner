package com.example.akira.scanner;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ScannerActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private CameraBridgeViewBase mOpenCvCameraView;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_scanner);
        mOpenCvCameraView = findViewById(R.id.cvView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
    public void onCameraViewStarted(int width, int height) {
    }
    public void onCameraViewStopped() {
    }
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        // Convert hte input frame to grey scale
        Mat colorFrame = inputFrame.rgba();
        Mat grayFrame = inputFrame.gray();
        Mat blurFrame = new Mat();
        Mat edgeFrame = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        // Blur the frame for pre-processing of canny
        Imgproc.GaussianBlur(grayFrame, blurFrame, new Size(3,3), 1);
        // Detect the edges in the current frame
        Imgproc.Canny(blurFrame, edgeFrame, 75, 150);
        // Find the contours within the image using the edges found by canny
        Imgproc.findContours(edgeFrame, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);

        contours.sort(new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint contour1, MatOfPoint contour2) {
                double val = Imgproc.contourArea(contour1) - Imgproc.contourArea(contour2);
                if (val > 0) {
                    return -1;
                } else if (val < 0) {
                    return 1;
                }
                return 0;
            }
        });

        for (MatOfPoint contour : contours) {
            MatOfPoint2f contour2f = new MatOfPoint2f();
            MatOfPoint2f shape = new MatOfPoint2f();
            contour.convertTo(contour2f, CvType.CV_32F);
            double contourPerimeter = Imgproc.arcLength(contour2f, true);
            Imgproc.approxPolyDP(contour2f, shape, 0.1 * contourPerimeter, true);
            MatOfPoint points = new MatOfPoint( shape.toArray() );

            if (shape.toArray().length == 4) {
                Rect rect = Imgproc.boundingRect(points);
                Imgproc.rectangle(colorFrame, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 0, 0), 3);
                break;
            }
        }
        return colorFrame;
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("onManager", "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
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
}
