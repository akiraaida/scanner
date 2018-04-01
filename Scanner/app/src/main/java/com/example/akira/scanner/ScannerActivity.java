package com.example.akira.scanner;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class ScannerActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, Menu.OnFragmentInteractionListener {

    private CameraBridgeViewBase mOpenCvCameraView;
    public static int mSameFrameCounter = 0;
    public static Rect mSavedRect = null;

    // Low Hysteresis (eliminates non meaningful edges)
    private static final int LOW_HYSTERESIS = 0;
    // High Hysteresis (determines definitive edges)
    private static final int HIGH_HYSTERESIS = 80;
    // First tier is a "red" box to show what is in focus
    private static final int FOCUS_COUNTER_LEVEL_1 = 3;
    // Second tier is a "green" box to show what is focus and that it has been in focus for x30 frames, once this number is exceeded a picture will be taken
    private static final int FOCUS_COUNTER_LEVEL_2 = 5;
    // Toggle the detection on or off
    public static int mToggle = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_scanner);
        mToggle = 0;

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

    public boolean checkRect(Rect rect) {
        // Get the 4 coords out the current rectangle bounding box
        Point p1 = new Point(rect.x, rect.y);
        Point p2 = new Point(rect.x + rect.width, rect.y);
        Point p3 = new Point(rect.x, rect.y + rect.height);
        Point p4 = new Point(rect.x + rect.width, rect.y + rect.height);
        Point currPoints[] = {p1, p2, p3, p4};

        // If the saved frame is null then return it as true, otherwise check that the current rectangle
        // is within the tolerance of the previous rectangle (need the rectangle to be somewhat consistent
        // so don't capture garbage or the wrong thing).
        if (mSavedRect == null) {
            mSavedRect = rect;
            return true;
        } else {
            // Get the previous points for the last saved rectangle
            Point prevP1 = new Point(mSavedRect.x, mSavedRect.y);
            Point prevP2 = new Point(mSavedRect.x + mSavedRect.width, mSavedRect.y);
            Point prevP3 = new Point(mSavedRect.x, mSavedRect.y + mSavedRect.height);
            Point prevP4 = new Point(mSavedRect.x + mSavedRect.width, mSavedRect.y + mSavedRect.height);
            Point prevPoints[] = {prevP1, prevP2, prevP3, prevP4};

            for(int i = 0; i < currPoints.length; ++i) {
                if(!checkTol(currPoints[i], prevPoints[i])) {
                    // If the tolerance check fails, reset the saved frame and return false
                    mSavedRect = null;
                    return false;
                }
            }
            // Update the previous frame to the current frame since the tolerance check passed
            mSavedRect = rect;
        }
        return true;
    }

    // Assumption is p1 is the current rectangle point and p2 is the previous rectangle point
    public boolean checkTol(Point p1, Point p2) {
        double xDiff = Math.abs(p1.x - p2.x);
        double yDiff = Math.abs(p1.y - p2.y);

        // 5% tolerance on the shift of x/y from previous frames
        double xTol = p2.x * 0.1;
        double yTol = p2.y * 0.1;

        if (xDiff <= xTol && yDiff <= yTol) {
            return true;
        }
        return false;
    }

    private boolean falsePos(Rect rect, Mat dispFrame) {
        if(rect.height < (dispFrame.height() / 2) && rect.width < (dispFrame.width() / 2)) {
            return true;
        } else {
            return false;
        }
    }

    public Mat findFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat dispFrame = inputFrame.rgba();
        // Convert the image to grey scale
        Mat greyFrame = inputFrame.gray();

        // Use a bilateral filter to keep edges and remove noise
        Mat blurFrame = new Mat();
        // GaussianBlur is faster but reduces accuracy
        //Imgproc.GaussianBlur(greyFrame, blurFrame, new Size(5, 5), 5);
        Imgproc.bilateralFilter(greyFrame, blurFrame, 5, 200, 200);

        // Use canny edge detection to detect the edges
        Mat edgeFrame = new Mat();
        Imgproc.Canny(blurFrame, edgeFrame, LOW_HYSTERESIS, HIGH_HYSTERESIS);

        // Find the contours within the image using the edges found by canny
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(edgeFrame, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);

        // Sort the contours based on area (largest area is likely to be our expected frame)
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

        // Figure out which contour is the frame we want
        for (MatOfPoint contour : contours) {
            MatOfPoint2f contour2f = new MatOfPoint2f();
            MatOfPoint2f shape = new MatOfPoint2f();
            contour.convertTo(contour2f, CvType.CV_32F);
            double contourPerimeter = Imgproc.arcLength(contour2f, true);
            Imgproc.approxPolyDP(contour2f, shape, 0.02 * contourPerimeter, true);
            Point[] shapeArr = shape.toArray();
            MatOfPoint points = new MatOfPoint(shapeArr);

            if (shapeArr.length >= 4) {
                Rect rect = Imgproc.boundingRect(points);
                if (falsePos(rect, dispFrame)) {
                    break;
                }
                boolean sameRect = checkRect(rect);
                if (sameRect) {
                    mSameFrameCounter += 1;
                } else {
                    mSameFrameCounter = 0;
                }
                if (mSameFrameCounter < FOCUS_COUNTER_LEVEL_1) {
                    Imgproc.rectangle(dispFrame, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 0, 0), 3);
                } else if (mSameFrameCounter < FOCUS_COUNTER_LEVEL_2) {
                    Imgproc.rectangle(dispFrame, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0), 3);
                } else {
                    // Get the region of interest
                    Point tl = new Point(rect.x, rect.y);
                    Point br = new Point(rect.x + rect.width, rect.y + rect.height);
                    Rect roi = new Rect(tl, br);
                    // Crop the frame to just the region of interest
                    dispFrame = new Mat(dispFrame, roi);
                    // Rotate the frame since the camera is in landscape mode
                    Core.flip(dispFrame.t(), dispFrame, 1);
                    // Convert the frame to an image
                    Bitmap img = Bitmap.createBitmap(dispFrame.cols(), dispFrame.rows(),Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(dispFrame, img);
                    // Save the image locally
                    String path = storeImage(img);
                    // Send the path to the display activity
                    Intent intent = new Intent(ScannerActivity.this, DisplayActivity.class);
                    intent.putExtra("imgPath", path);
                    startActivity(intent);
                    finish();
                }
                break;
            }
        }
        return dispFrame;
    }

    private String storeImage(Bitmap image) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
        String millisInString  = dateFormat.format(new Date());
        File file = new File(this.getFilesDir() + "/pics/", millisInString + ".jpeg");
        try {
            FileOutputStream fos = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.e("ERROR", e.getStackTrace().toString());
        } catch (IOException e) {
            Log.e("ERROR", e.getStackTrace().toString());
        }
        return file.getAbsolutePath();
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        if (mToggle == 0) {
            return inputFrame.rgba();
        } else {
            return findFrame(inputFrame);
        }
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

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
