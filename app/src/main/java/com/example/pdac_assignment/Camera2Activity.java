package com.example.pdac_assignment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.MutableLiveData;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.example.pdac_assignment.Utils.Histogram;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Camera2Activity extends AppCompatActivity implements SurfaceHolder.Callback{
    private static final String TAG = "Camera2Activity";

    private final  int REQUEST_PERMISSION = 100;
    //Capture camera session reference
    CameraCaptureSession mCaptureSession = null;

    //Check image used to test the received data from ImageReader
    private ImageView mCheckImage = null;

    private ColorBoxViewHolder [] mColorHolders = new ColorBoxViewHolder[5];
    final MutableLiveData<Histogram> mExecutionData = new MutableLiveData<>();

    private final static int INITIAL_SCALING_BY = 4;
    private ArrayBlockingQueue<ExecutionContent> mImageDataBlockingArray = new ArrayBlockingQueue<ExecutionContent>(1);
    private ExecutorService mExecutor = null;


    private FrameLayout mSurfaceFrame = null;
    private SurfaceView mSurfaceView = null;
    //region SurfaceView
    private SurfaceHolder mSurfaceHolder = null;

    private HandlerThread mCameraSessionHandlerThread = null;
    private Handler mCameraSessionHandler = null;

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {

       Log.i(TAG, "Surface created");
       createPreviewSession(surfaceHolder.getSurface(),mImageReader.getSurface());

    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int format, int w, int h) {
        Log.i(TAG, "Surface changed");
        Size size = findClosestRatio(supportedSizes,w,h);
        surfaceHolder.setFixedSize(size.getWidth(), h /*to take full screen*/);

    }
    private Size findClosestRatio(Size[] sizes, int width, int height){

        if(sizes == null)
            return new Size(width,height);
        Size targetSize = sizes[0];
        float targetRatio = Float.MAX_VALUE;
        float ratio = width > height ? width / (float) height : height / (float) width;

        for(Size size : sizes) {
            if(size.getWidth() * size.getHeight() > width * height)
                continue;
            float tmpRatio = size.getWidth() / (float) size.getHeight();
            if (Math.abs(tmpRatio - ratio) < Math.abs(targetRatio - ratio)){
                targetRatio = tmpRatio;
                targetSize = size;
            }
        }
        return targetSize;
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        Log.i(TAG, "Surface destroyed");
    }

    //endregion


    private String mCameraId;
    private Size[] supportedSizes = null;

    private CameraDevice mCameraDevice;

    private final static int MAX_IMAGE_SIZE_BOUNDARY = 1920/Histogram.DEFAULT_SCALING_FACTOR;
    private ImageReader mImageReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSurfaceFrame = findViewById(R.id.main_camera_preview);
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

    @Override
    protected void onStart() {
        super.onStart();

        (mCameraSessionHandlerThread = initializeCameraSessionHandlerThread()).start();

        mExecutor = Executors.newSingleThreadExecutor();
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    int scaleBy = INITIAL_SCALING_BY;
                    while (true) {
                        ExecutionContent content = mImageDataBlockingArray.take();
                        final Histogram histogram = Histogram.instantiateHistogram(content.bytes, 0, content.bytes.length,
                                new Histogram.ConfigBuilder()
                                        .setScaleBy(scaleBy)
                                        .build());
                        if(scaleBy > 1)
                            scaleBy /=2;
                        mExecutionData.postValue(histogram);
                    }

                } catch (InterruptedException e) {
                    Log.d(TAG,"Executing interrupted",e);
                    e.printStackTrace();
                }
            }
        });
        openCamera();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCameraSessionHandlerThread.quitSafely();
        mExecutor.shutdown();
        closeCamera();
    }
    private HandlerThread initializeCameraSessionHandlerThread(){
        return new HandlerThread("SessionHandlerThread") {
            @Override
            protected void onLooperPrepared() {
                super.onLooperPrepared();
                mCameraSessionHandler = new Handler(getLooper());
            }
        };
    }
    private void acquireCameraCharacteristics(@NonNull CameraManager cm){
        try {
            mCameraId = cm.getCameraIdList()[0];
            CameraCharacteristics characteristics = cm.getCameraCharacteristics(mCameraId);
            StreamConfigurationMap configMap = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            supportedSizes = configMap.getOutputSizes(SurfaceHolder.class);

        } catch (CameraAccessException e) {
            Log.e(TAG, "Unable to access camera", e);
        }
    }
    private void createImageReader() {

        try {
            CameraManager cm = (CameraManager) getBaseContext().getSystemService(CAMERA_SERVICE);
            CameraCharacteristics characteristics = cm.getCameraCharacteristics(mCameraId);
            Size[] sizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);

            Size size = new Size(MAX_IMAGE_SIZE_BOUNDARY, MAX_IMAGE_SIZE_BOUNDARY);
            for (Size s : sizes) {
                if (s.getHeight() > MAX_IMAGE_SIZE_BOUNDARY || s.getWidth() > MAX_IMAGE_SIZE_BOUNDARY)
                    continue;
                size = s;
                break;
            }
            mImageReader = ImageReader.newInstance(size.getWidth(), size.getHeight(), ImageFormat.JPEG, 2);
            mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader imageReader) {
                    try {
                        Image image = imageReader.acquireLatestImage();
                        if (image != null) {
                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();

                            byte[] bytes = new byte[buffer.capacity()];
                            buffer.get(bytes);
                            mImageDataBlockingArray.clear();
                            mImageDataBlockingArray.put(new ExecutionContent(image.getFormat(), image.getWidth(), image.getHeight(), bytes));
//                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);

//                mCheckImage.setImageBitmap(bitmap);
                            image.close();

                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Image reading exception", e);
                    }

                }
            }, null);
        }catch (CameraAccessException e){
            Log.e(TAG,"Unable to create Image reader ",e);
        }
    }
    private void openCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION);
            Log.w(TAG,"No camera permission granted");
            return;
        }
        try {

            CameraManager cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            acquireCameraCharacteristics(cm);

                cm.openCamera(mCameraId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice cameraDevice) {
                        mCameraDevice = cameraDevice;
                        createImageReader();

                        mSurfaceFrame.removeAllViews();
                        mSurfaceFrame.addView(mSurfaceView = new SurfaceView(Camera2Activity.this));
                        mSurfaceHolder = mSurfaceView.getHolder();
                        mSurfaceHolder.addCallback(Camera2Activity.this);

                        Log.i(TAG,"Camera opened " + cameraDevice);
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                        Log.i(TAG,"Camera disconnected " + cameraDevice);
                        mCameraDevice = null;
                    }

                    @Override
                    public void onError(@NonNull CameraDevice cameraDevice, int i) {
                        Log.w(TAG,"Unable to open camera device id: " + i + " " + cameraDevice);
                    }
                }, null);
            } catch(CameraAccessException e){
                e.printStackTrace();
            }
    }
    private void closeCamera(){
        if(mCameraDevice != null)
            mCameraDevice.close();

    }
    private void createPreviewSession(Surface ... surfaces){
        try {
            mCameraDevice.createCaptureSession(Arrays.asList(surfaces), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mCaptureSession = cameraCaptureSession;
                    try {
                        CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        for(Surface s : surfaces)
                            builder.addTarget(s);
                        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);


                        cameraCaptureSession.setRepeatingRequest(builder.build(), new CameraCaptureSession.CaptureCallback() {
                            @Override
                            public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                                super.onCaptureStarted(session, request, timestamp, frameNumber);
                            }

                            @Override
                            public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
                                super.onCaptureProgressed(session, request, partialResult);
                            }

                            @Override
                            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                                super.onCaptureCompleted(session, request, result);
                            }

                            @Override
                            public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                                super.onCaptureFailed(session, request, failure);
                            }
                        }, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.e(TAG,"Unable to create capture session "+ cameraCaptureSession);
                }
            },mCameraSessionHandler);

        } catch (Exception e) {
           Log.e(TAG,"Unable to access camera device",e);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                showCriticalDialogMessage("Camera permission is denied");
            }else {
                openCamera();
            }
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


    private void populateColorBoxes(Histogram histogram){
        Histogram.Color[] colors = histogram.getSortedColors();
        for(int i = 0; i < mColorHolders.length && i < colors.length; i++)
            mColorHolders[i].populateWith(colors[i],histogram.getColorShare(colors[i]));
    }
}