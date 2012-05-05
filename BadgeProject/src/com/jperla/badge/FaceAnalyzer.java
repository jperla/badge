package com.jperla.badge;

import android.hardware.Camera;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.util.Log;


public class FaceAnalyzer implements FaceDetectionListener {
    private static final String LOG_TAG = "----- FaceAnalyzer -----";

    @Override
    public void onFaceDetection(Face[] faces, Camera camera) {
        Log.d(LOG_TAG, "Found " + faces.length + " faces.");
    }
}
