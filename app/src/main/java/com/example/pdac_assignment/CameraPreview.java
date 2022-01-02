package com.example.pdac_assignment;
/** class is taken from android official documentation: https://developer.android.com/guide/topics/media/camera#java **/

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

/** A basic Camera preview class */
@SuppressWarnings( "deprecation" )
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "CameraPreview";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private int orientation = 0;


    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;
        orientation = ((Activity)context).getWindowManager().getDefaultDisplay()
                .getRotation();
        //fix camera orientation
        setCameraDisplayOrientation((Activity) context, 0, camera);

//        mCamera.getParameters()

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    /** Fixes camera orientation
     * code taken from official documentation https://developer.android.com/reference/android/hardware/Camera#setDisplayOrientation(int)
     */
    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            Camera.Parameters params = setClosestRatio(mCamera.getParameters(),w,h) ;
            mCamera.setParameters(params);
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
            if(getContext() instanceof Camera.PreviewCallback)
                mCamera.setPreviewCallback((Camera.PreviewCallback)getContext());

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }
    private Camera.Parameters setClosestRatio(Camera.Parameters params, int width,int height){
        if(orientation == Surface.ROTATION_0 || orientation == Surface.ROTATION_180) {
            int tmpHeight = height;
            height = width;
            width = tmpHeight;
        }

        List<Camera.Size> sizes = params.getSupportedPreviewSizes();
        float closestRatio = Float.MAX_VALUE;
        for(Camera.Size size : sizes) {
            if(size.width * size.height > width*height) {
                continue;
            }
            float tmpRatio = Math.abs(size.width / (float) size.height - width / (float) height);
            if (closestRatio > tmpRatio) {
                closestRatio = tmpRatio;
                params.setPreviewSize(size.width,size.height);
            }
        }
        return params;

    }
}