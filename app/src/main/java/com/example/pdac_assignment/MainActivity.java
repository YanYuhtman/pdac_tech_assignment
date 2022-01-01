package com.example.pdac_assignment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.pdac_assignment.Utils.Histogram;
import com.example.pdac_assignment.Utils.Utils;

import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity implements Camera.PreviewCallback{

    private static final String TAG = "MainActivity";
    private CameraPreview mCameraPreview = null;
    private Camera mCamera = null;
    private FrameLayout mCameraContainer = null;
    private ImageView mCheckImage = null;
    private ColorBoxViewHolder [] mColorHolders = new ColorBoxViewHolder[5];
    private final static int REQUEST_PERMISSION = 100;

    final MutableLiveData<Histogram> mExecutionData = new MutableLiveData<>();

    private static class ColorBoxViewHolder{

        View colorBox = null;
        TextView tv_percent = null;
        TextView tv_colorstring = null;
        static final String DEFAULT_PERCENT = "0.0%";
        static final String DEFAULT_COLOR = "R: B: G:";

        ColorBoxViewHolder(@NonNull View view ) {
            colorBox = view.findViewById(R.id.color_component_colorbox);
            tv_percent = view.findViewById(R.id.color_component_tv_percent);
            tv_colorstring = view.findViewById(R.id.color_component_tv_colorstring);
            setDefaults();
        }
        void setDefaults(){
            ((GradientDrawable)colorBox.getBackground()).setColor(0x00000000);
            tv_percent.setText(DEFAULT_PERCENT);
            tv_percent.setTextColor(Color.WHITE);
            tv_colorstring.setText(DEFAULT_COLOR);
        }
        int calculateTextColor(int bgColor){
            return ((bgColor & 0xff0000) >> 24 < 0x7f
                            && (bgColor & 0x00ff00) >> 16 < 0x7f
                                 && (bgColor & 0x0000ff)  < 0x7f) ? Color.WHITE
                    : Color.BLACK;
        }
        void populateWith(Histogram.Color color, float rate){
            if(color != null){
                ((GradientDrawable)colorBox.getBackground()).setColor(color.color);
                tv_percent.setText(String.format(Locale.getDefault(),"%.2f%%",(rate)));
                tv_percent.setTextColor(calculateTextColor(color.color));
                tv_colorstring.setText(color.toString());
            }else
                setDefaults();
        }
    }


    private ArrayBlockingQueue<ExecutionContent> mImageDataBlockingArray = new ArrayBlockingQueue<ExecutionContent>(1);
    private ExecutorService mExecutor = null;
    private final int INITIAL_SCALING_BY = 64;
    private class ExecutionContent{
        final int previewFormat;
        final int width;
        final int height;
        final byte[] bytes;

        public ExecutionContent(int previewFormat, int width, int height, byte[] bytes) {
            this.previewFormat = previewFormat;
            this.width = width;
            this.height = height;
            this.bytes = bytes;
        }
    }


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
//            params.setPreviewSize(size.width,size.height);
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
                    Log.d(TAG,"Executing interuppted",e);
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
    protected void onPostResume() {
        super.onPostResume();

    }


    @Override
    protected void onStart() {
        super.onStart();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION);
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
        //#region Test image conversion by YuvImage class
//        int previewFormat = camera.getParameters().getPreviewFormat();
//        int width = camera.getParameters().getPreviewSize().width;
//        int height = camera.getParameters().getPreviewSize().height;
//        long procStart = SystemClock.elapsedRealtime();
//        bytes = Utils.convertYuvToJpeg(bytes,previewFormat,width,height);
//        Palette.instantiatePalette(bytes,0,bytes.length,new Palette.Config(64,64))
//            .getSortedColors();
//
//        long procEnd = SystemClock.elapsedRealtime() ;
//        if(executionTimeAvg == -1)
//            executionTimeAvg = procEnd - procStart;
//        else executionTimeAvg = (executionTimeAvg + (procEnd - procStart))/2;
//        Log.d("[PALETTE]", "Execution avg: " + executionTimeAvg);
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inSampleSize = 1;
//        mChekcImage.setImageBitmap(BitmapFactory.decodeByteArray(bytes,0,bytes.length,options));
        //#endregion

        //#region Test image conversion by custom conversoin function
//        if(testPixelsBitmap == null)
//            testPixelsBitmap = new int[width*height];
//        if(amountsOfRGBs == null)
//            amountsOfRGBs = new float[3];
//
//        int[] bitmap = Utils.convertYUV420_NV21toARGB8888(bytes,width,height,testPixelsBitmap,amountsOfRGBs);
//        mChekcImage.setImageBitmap(Bitmap.createBitmap(bitmap,width,height, Bitmap.Config.ARGB_8888));
        //#endregion
    }





}