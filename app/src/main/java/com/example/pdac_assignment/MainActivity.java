package com.example.pdac_assignment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.pdac_assignment.Utils.Histogram;
import com.example.pdac_assignment.Utils.Utils;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity implements Camera.PreviewCallback{

    private CameraPreview mCameraPreview = null;
    private Camera mCamera = null;
    private FrameLayout mCameraContainer = null;
    private ImageView mChekcImage = null;
    private TextView mCheckTextView = null;
    private final static int REQUEST_PERMISSION = 100;

    private ArrayBlockingQueue<ExecutionContent> mImageDataBlockingArray = new ArrayBlockingQueue<ExecutionContent>(1);
    private ExecutorService mExecutor = null;
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
        mChekcImage = findViewById(R.id.main_image_check);
        mCheckTextView = findViewById(R.id.main_text_view);

        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            showCriticalDialogMessage("Camera hardware features is not present");
            return;
        }


    }
    private void prepareCameraPreview(){
        try {
            mCamera = Camera.open();
            if(mCamera == null) {
                showCriticalDialogMessage("Camera is unavailable");
                return;
            }
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
                    ExecutionContent content = null;

                    while ((content = mImageDataBlockingArray.take()) != null) {
                        byte[] bytes = Utils.convertYuvToJpeg(content.bytes, content.previewFormat, content.width, content.height);
                        final Histogram histogram = Histogram.instantiateHistogram(bytes, 0, bytes.length, new Histogram.Config(200, 512));
                        histogram.getSortedColors();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Histogram.Color[] colors = histogram.getSortedColors();
                                StringBuilder stringBuilder = new StringBuilder();
                                for(int i = 0; i < 5; i++){
                                    stringBuilder.append(String.format("%.2f",(colors[i].getCount()/(float) histogram.getItemCount())*100))
                                            .append("% ")
                                            .append(colors[i].toString())
                                            .append("\n");
                                }
                                mCheckTextView.setText(stringBuilder.toString());

                            }
                        });
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

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
        mCameraContainer.removeAllViews();
        if(mCamera != null)
            mCamera.release();
        mExecutor.shutdown();

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