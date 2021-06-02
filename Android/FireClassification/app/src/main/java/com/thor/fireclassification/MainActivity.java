package com.thor.fireclassification;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.location.LocationManager;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.JsonObject;
import com.thor.fireclassification.model.DataItem;
import com.thor.fireclassification.model.DataResult;
import com.thor.fireclassification.service.ApiClient;
import com.thor.fireclassification.service.SendData;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();
    private CameraCaptureSession mCaptureSession;
    private CameraDevice mCameraDevice;

    private HandlerThread mCaptureThread;
    private Handler mCaptureHandler;
    private HandlerThread mInferThread;
    private Handler mInferHandler;


    private ImageReader mImageReader;
    private boolean isFont = false;
    private Size mPreviewSize;
    private boolean mCapturing;

    private AutoFitTextureView mTextureView;

    private final Object lock = new Object();
    private boolean runClassifier = false;
    private ArrayList<String> classNames;
    private TFLiteHandler tfLiteHandler;
    private TextView resultText;
    private TextView classText;
    private TextView probText;
    private TextView timeText;


    private String uid;
    private String latitudeUser;
    private String longitudeUser;
    private DatabaseReference databaseReference;
    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private static final long START_TIME_IN_MILLIS = 60000;
    private boolean permissionToSend = true;
    private long mTimeLeftInMillis = START_TIME_IN_MILLIS;

    private static int DATA_COUNT = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadTiming();
        if (!hasPermission()) {
            requestPermission();
        }


        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
        getCurrentLocation();

        classNames = Utils.ReadListFromFile(getAssets(), "labels.txt");
        String classificationModelPath = getCacheDir().getAbsolutePath() + File.separator + "model_transfer_update_2.tflite";
        Utils.copyFileFromAsset(MainActivity.this, "model_transfer_update_2.tflite", classificationModelPath);
        try {
            tfLiteHandler = new TFLiteHandler(classificationModelPath);
            Toast.makeText(MainActivity.this, "Model Loaded！", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Cannot Load Model！", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            finish();
        }


        mTextureView = findViewById(R.id.texture_view);
        resultText = findViewById(R.id.result_text);
        classText = findViewById(R.id.class_name);
        probText = findViewById(R.id.prob);
        timeText = findViewById(R.id.timeText);
    }

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
            startCapture();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
            //configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

        }
    };

    private final CameraDevice.StateCallback mCameraCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            createCaptureSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
            mCapturing = false;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, final int error) {
            camera.close();
            mCameraDevice = null;
            mCapturing = false;
        }
    };


    private final Runnable periodicClassify =
            new Runnable() {
                @Override
                public void run() {
                    synchronized (lock) {
                        if (runClassifier) {
                            if (getApplicationContext() != null && mCameraDevice != null && tfLiteHandler != null) {
                                predict();
                            }
                        }
                    }
                    if (mInferThread != null && mInferHandler != null && mCaptureHandler != null && mCaptureThread != null) {
                        mInferHandler.post(periodicClassify);
                    }
                }
            };




    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){

            fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    Location location = task.getResult();

                    if (location != null) {
                        latitudeUser = String.valueOf(location.getLatitude());
                        longitudeUser = String.valueOf(location.getLongitude());
                    }else {
                        LocationRequest locationRequest = new LocationRequest()
                                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                                .setInterval(10000)
                                .setFastestInterval(1000)
                                .setNumUpdates(1);

                        LocationCallback locationCallback = new LocationCallback() {
                            @Override
                            public void onLocationResult(@NonNull LocationResult locationResult) {
                                super.onLocationResult(locationResult);

                                Location location1 = locationResult.getLastLocation();
                                latitudeUser = String.valueOf(location1.getLatitude());
                                longitudeUser = String.valueOf(location1.getLongitude());

                            }
                        };


                        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                    }
                }
            });

        }else {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }



    private void sendData(final String class_name, final float probability,
                          final Bitmap bitmap, final String longitude, final String latitude){
        uid = Utils.generateRandomUid();
        Log.d(TAG, "sendData: "+uid);


        String currentDate = Utils.getCurrentDate();

        databaseReference = FirebaseDatabase.getInstance().getReference("Data").child(uid);

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference().child(uid+".JPEG");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] dataImage = baos.toByteArray();

        UploadTask uploadTask = storageReference.putBytes(dataImage);

        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        String imgUrl = uri.toString();
                        Log.d(TAG, "onSuccess: IMG_URL: "+imgUrl);
                        DataItem data = new DataItem(currentDate, imgUrl, probability, latitude, longitude, class_name);
                        timingSendData();
                        databaseReference.setValue(data).addOnCompleteListener(task -> {
                            Toast.makeText(MainActivity.this, "Data Send!", Toast.LENGTH_SHORT).show();

                        });
                    }
                });

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });



    }

    private void sendDataUsingRetrofit(final String class_name, final float probability,
                          final Bitmap bitmap, final String longitude, final String latitude){

        uid = Utils.generateRandomUid();
        Log.d(TAG, "sendData: "+uid);


        String currentDate = Utils.getCurrentDate();
        Log.d(TAG, "currentDate: "+currentDate);


        if (DATA_COUNT != 0 ){
            DATA_COUNT = DATA_COUNT-1;
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageReference = storage.getReference().child(uid+".JPEG");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] dataImage = baos.toByteArray();

            UploadTask uploadTask = storageReference.putBytes(dataImage);
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String imgUrl = uri.toString();
                            Log.d(TAG, "onSuccess: IMG_URL: "+imgUrl);
                       /* Map<String, String> requestBody = new HashMap<>();
                        requestBody.put("class_name", class_name);
                        requestBody.put("probability", String.valueOf(probability));
                        requestBody.put("date", currentDate);
                        requestBody.put("longitude", longitude);
                        requestBody.put("latitude", latitude);
                        requestBody.put("imgUrl", imgUrl);*/

                            DataItem data = new DataItem(currentDate, imgUrl, probability, latitude, longitude, class_name);

                            SendData service = ApiClient.getRetrofitInstance().create(SendData.class);
                            Call<String> call = service.sendData(data);
                            call.enqueue(new Callback<String>() {
                                @Override
                                public void onResponse(Call<String> call, Response<String> response) {
                                    String result = String.valueOf(response.body());
                                    Log.d(TAG, "status: "+response.code());
                                    Log.d("MainActivity", "response = " + response.message());
                                    Log.d("MainActivity", "result = " + result);
                                    if (response.code() == 200) {
                                        timingSendData();
                                        Toast.makeText(MainActivity.this, "Data Send!", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(MainActivity.this, "Data Not Send!", Toast.LENGTH_SHORT).show();
                                    }


                                }

                                @Override
                                public void onFailure(Call<String> call, Throwable t) {
                                    Log.d(TAG, "onFailure: "+t.getMessage());
                                    Log.d(TAG, "onFailure: "+t.getLocalizedMessage());
                                    Toast.makeText(MainActivity.this, "Cannot Send Data!", Toast.LENGTH_SHORT).show();
                                }
                            });


                        }
                    });

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                }
            });
        }

    }


    private void sendDataUsingBoth(final String class_name, final float probability,
                                       final Bitmap bitmap, final String longitude, final String latitude){

        uid = Utils.generateRandomUid();
        Log.d(TAG, "sendData: "+uid);


        String currentDate = Utils.getCurrentDate();
        Log.d(TAG, "currentDate: "+currentDate);


        if (DATA_COUNT != 0 ){
            DATA_COUNT = DATA_COUNT-1;

            databaseReference = FirebaseDatabase.getInstance().getReference("Data").child(uid);

            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageReference = storage.getReference().child(uid+".JPEG");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] dataImage = baos.toByteArray();

            UploadTask uploadTask = storageReference.putBytes(dataImage);
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String imgUrl = uri.toString();
                            Log.d(TAG, "onSuccess: IMG_URL: "+imgUrl);

                            DataItem data = new DataItem(currentDate, imgUrl, probability, latitude, longitude, class_name);

                            databaseReference.setValue(data).addOnCompleteListener(task -> {

                            });
                            SendData service = ApiClient.getRetrofitInstance().create(SendData.class);
                            Call<String> call = service.sendData(data);
                            call.enqueue(new Callback<String>() {
                                @Override
                                public void onResponse(Call<String> call, Response<String> response) {
                                    String result = String.valueOf(response.body());
                                    Log.d(TAG, "status: "+response.code());
                                    Log.d("MainActivity", "response = " + response.message());
                                    Log.d("MainActivity", "result = " + result);
                                    if (response.code() == 200) {
                                        timingSendData();
                                        Toast.makeText(MainActivity.this, "Data Send!", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(MainActivity.this, "Data Not Send!", Toast.LENGTH_SHORT).show();
                                    }


                                }

                                @Override
                                public void onFailure(Call<String> call, Throwable t) {
                                    Log.d(TAG, "onFailure: "+t.getMessage());
                                    Log.d(TAG, "onFailure: "+t.getLocalizedMessage());
                                    Toast.makeText(MainActivity.this, "Send Data!", Toast.LENGTH_SHORT).show();
                                }
                            });


                        }
                    });

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                }
            });
        }

    }


    private void timingSendData(){
        CountDownTimer mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
            @Override
            public void onTick(long l) {
                mTimeLeftInMillis = l;
            }

            @Override
            public void onFinish() {
                permissionToSend = true;
                DATA_COUNT = 1;
            }
        }.start();

        permissionToSend = false;
    }

    private void loadTiming(){
        CountDownTimer mCountDownTimer = new CountDownTimer(6000, 1000) {
            @Override
            public void onTick(long l) {
                mTimeLeftInMillis = l;
            }

            @Override
            public void onFinish() {
                permissionToSend = true;
                DATA_COUNT = 3;
            }
        }.start();

        permissionToSend = false;
    }


    @SuppressLint("SetTextI18n")
    private void predict() {
        Bitmap bitmap = mTextureView.getBitmap();
        try {
            long start = System.currentTimeMillis();
            float[] result = tfLiteHandler.predictImage(bitmap);
            long end = System.currentTimeMillis();

            int class_arr = (int) result[0];
            String class_name = classNames.get((int) result[0]);
            float probability = result[1];
            long time = (end - start);



            boolean statement = checkData(class_name,probability,permissionToSend);
            Log.d(TAG, "predict: Statement :"+statement);

            if (statement){
                //sendData(class_name, probability, bitmap, longitudeUser, latitudeUser);
                sendDataUsingBoth(  class_name, probability, bitmap, longitudeUser, latitudeUser);
            }

            resultText.setText("Result :"+class_arr);
            classText.setText("Class :"+class_name);
            probText.setText("Probability :"+probability);
            timeText.setText("Time :"+time+" ms");



        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean checkData(String class_name, Float prob, boolean state) {
        if (class_name.equals("fire") && prob>0.98 && state){
            return true;
        }else{
            return  false;
        }
    }




    private void startCapture() {
        if (mCapturing) return;
        mCapturing = true;

        final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        String cameraIdAvailable = null;
        try {
            assert manager != null;
            for (final String cameraId : manager.getCameraIdList()) {
                final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (isFont) {
                    if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        cameraIdAvailable = cameraId;
                        break;
                    }
                } else {
                    if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        cameraIdAvailable = cameraId;
                        break;
                    }
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        try {
            assert cameraIdAvailable != null;
            final CameraCharacteristics characteristics =
                    manager.getCameraCharacteristics(cameraIdAvailable);

            final StreamConfigurationMap map =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            mPreviewSize = Utils.chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    mTextureView.getWidth(),
                    mTextureView.getHeight());
            Log.d("mPreviewSize", String.valueOf(mPreviewSize));
            mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            manager.openCamera(cameraIdAvailable, mCameraCallback, mCaptureHandler);
        } catch (CameraAccessException | SecurityException e) {
            mCapturing = false;
            e.printStackTrace();
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == mTextureView || null == mPreviewSize) {
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    private void createCaptureSession() {
        try {
            final SurfaceTexture texture = mTextureView.getSurfaceTexture();
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            final Surface surface = new Surface(texture);
            final CaptureRequest.Builder captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);

            mImageReader = ImageReader.newInstance(
                    mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.JPEG, 10);

            mCameraDevice.createCaptureSession(
                    Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            if (null == mCameraDevice) {
                                return;
                            }

                            mCaptureSession = cameraCaptureSession;
                            try {
                                captureRequestBuilder.set(
                                        CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                captureRequestBuilder.set(
                                        CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                                CaptureRequest previewRequest = captureRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(
                                        previewRequest, new CameraCaptureSession.CaptureCallback() {
                                            @Override
                                            public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
                                                super.onCaptureProgressed(session, request, partialResult);
                                            }

                                            @Override
                                            public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                                                super.onCaptureFailed(session, request, failure);
                                                Log.d(TAG, "onCaptureFailed = " + failure.getReason());
                                            }

                                            @Override
                                            public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
                                                super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
                                                Log.d(TAG, "onCaptureSequenceCompleted");
                                            }
                                        }, mCaptureHandler);
                            } catch (final CameraAccessException e) {
                                Log.e(TAG, "onConfigured exception ", e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull final CameraCaptureSession cameraCaptureSession) {
                            Log.e(TAG, "onConfigureFailed ");
                        }
                    },
                    null);
        } catch (final CameraAccessException e) {
           e.printStackTrace();
        }
    }


    private void closeCamera() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
        mCapturing = false;
    }


    private void stopBackgroundThread() {
        try {
            if (mCaptureThread != null) {
                mCaptureThread.quitSafely();
                mCaptureThread.join();
            }
            mCaptureThread = null;
            mCaptureHandler = null;
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private void stopInferThread() {
        try {
            if (mInferThread != null) {
                mInferThread.quitSafely();
                mInferThread.join();
            }
            mInferThread = null;
            mInferHandler = null;
            synchronized (lock) {
                runClassifier = false;
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();
        stopBackgroundThread();
        stopInferThread();
    }



    private void startBackgroundThread() {
        mCaptureThread = new HandlerThread("capture");
        mCaptureThread.start();
        mCaptureHandler = new Handler(mCaptureThread.getLooper());
    }

    private void startInferThread() {
        mInferThread = new HandlerThread("inference");
        mInferThread.start();
        mInferHandler = new Handler(mInferThread.getLooper());
        synchronized (lock) {
            runClassifier = true;
        }
        mInferHandler.post(periodicClassify);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        startInferThread();

        if (mTextureView.isAvailable()) {
            startCapture();
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }


    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    // request permission
    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
    }
}