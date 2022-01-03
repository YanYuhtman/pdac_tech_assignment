package com.example.pdac_assignment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.MutableLiveData;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.example.pdac_assignment.Utils.Histogram;
import com.example.pdac_assignment.Utils.Utils;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@SuppressWarnings("deprecation")
public class CameraActivity extends AppCompatActivity implements Camera.PreviewCallback{

    private static final String TAG = "MainActivity";
    private CameraPreview mCameraPreview = null;
    private Camera mCamera = null;
    private FrameLayout mCameraContainer = null;
    private ImageView mCheckImage = null;
    private ColorBoxViewHolder [] mColorHolders = new ColorBoxViewHolder[5];
    private final static int REQUEST_PERMISSION = 100;

    final MutableLiveData<Histogram> mExecutionData = new MutableLiveData<>();


    private ArrayBlockingQueue<ExecutionContent> mImageDataBlockingArray = new ArrayBlockingQueue<ExecutionContent>(1);
    private ExecutorService mExecutor = null;
    private final int INITIAL_SCALING_BY = 64;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCameraContainer =  findViewById(R.id.main_camera_preview);
        mCheckImage = findViewById(R.id.main_image_check);
        mColorHolders[0] = new ColorBoxViewHolder(findViewById(R.id.main_camera_colorbox_0));
        mColorHolders[1] = new ColorBoxViewHolder(findViewById(R.id.main_camera_colorbox_1));
        mColorHolders[2] = new ColorBoxViewHolder(findViewById(R.id.main_camera_colorbox_2));
        mColorHolders[3] = new ColorBoxViewHolder(findViewById(R.id.main_camera_colorbox_3));
        mColorHolders[4] = new ColorBoxViewHolder(findViewById(R.id.main_camera_colorbox_4));

        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            showCriticalDialogMessage("Camera hardware features is not present");
            return;
        }
        mExecutionData.observe(this, this::populateColorBoxes);

    }
    private void prepareCameraPreview(){
        try {
            mCamera = Camera.open();
            if(mCamera == null) {
                showCriticalDialogMessage("Camera is unavailable");
                return;
            }

            Camera.Size size = mCamera.getParameters().getSupportedPreviewSizes().get(0);
            Camera.Parameters params = mCamera.getParameters();
            if(params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            mCamera.setParameters(params);

            mCamera.setPreviewCallback(this);
            testPixelsBitmap = null;
            amountsOfRGBs = null;

        }catch (Exception e){
            showCriticalDialogMessage(e.getMessage());
            return;
        }
        mCameraPreview = new CameraPreview(this,mCamera);
        mCameraContainer.addView(mCameraPreview);
        mExecutor = Executors.newSingleThreadExecutor();
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    int scaleBy = INITIAL_SCALING_BY;
                    while (true) {
                        ExecutionContent content = mImageDataBlockingArray.take();
                        byte[] bytes = Utils.convertYuvToJpeg(content.bytes, content.previewFormat, content.width, content.height);
                        final Histogram histogram = Histogram.instantiateHistogram(bytes, 0, bytes.length,
                                new Histogram.ConfigBuilder()
                                        .setScaleBy(scaleBy)
                                        .build());
                        if(scaleBy > Histogram.DEFAULT_SCALING_FACTOR)
                            scaleBy /=2;
                        mExecutionData.postValue(histogram);
                    }

                } catch (InterruptedException e) {
                    Log.d(TAG,"Executing interrupted",e);
                    e.printStackTrace();
                }
            }
        });

    }
    private void populateColorBoxes(Histogram histogram){
        Histogram.Color[] colors = histogram.getSortedColors();
        for(int i = 0; i < mColorHolders.length && i < colors.length; i++)
            mColorHolders[i].populateWith(colors[i],histogram.getColorShare(colors[i]));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CameraActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION);
        }else{
            prepareCameraPreview();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        releaseCamera();
        mExecutor.shutdown();
        mCameraContainer.removeAllViews();


    }
    private void releaseCamera(){
        if(mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }


    private void showCriticalDialogMessage(String message){
        new AlertDialog.Builder(this)
                .setTitle("Critical error")
                .setMessage(message)
                .setCancelable(true)
                .setOnCancelListener(dialogInterface -> finish())
                .show();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
               showCriticalDialogMessage("Camera permission is denied");
            }else {
                prepareCameraPreview();
            }
        }
    }


//    Test image conversion by YuvImage class
    int []testPixelsBitmap = null;
    float[] amountsOfRGBs = null;
    private long executionTimeAvg = -1;
    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {

        try {
            mImageDataBlockingArray.clear();
            mImageDataBlockingArray.put(new ExecutionContent(camera.getParameters().getPreviewFormat()
                    ,camera.getParameters().getPreviewSize().width
                    ,camera.getParameters().getPreviewSize().height
                    ,bytes));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }





}