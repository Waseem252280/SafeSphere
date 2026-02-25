//package com.example.safesphere.media;
//
//import android.Manifest;
//import android.annotation.SuppressLint;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Looper;
//import android.view.MotionEvent;
//import android.view.View;
//import android.widget.ImageView;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.camera.core.*;
//import androidx.camera.lifecycle.ProcessCameraProvider;
//import androidx.camera.video.*;
//import androidx.camera.view.PreviewView;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//
//import com.example.safesphere.R;
//
//import java.io.File;
//import java.util.concurrent.Executor;
//import android.os.Build;
//import android.os.VibrationEffect;
//import android.os.Vibrator;
//
//public class CameraActivity extends AppCompatActivity {
//
//    private View recordDot;
//    private boolean blink = true;
//
//    private TextView txtTimer;
//    private Handler timerHandler = new Handler(Looper.getMainLooper());
//    private int seconds = 0;
//
//
//    private static final int PERMISSION_REQ = 10;
//
//    private PreviewView previewView;
//    private ImageCapture imageCapture;
//    private VideoCapture<Recorder> videoCapture;
//    private Recording activeRecording;
//    private File lastVideoFile;
//
//    private Camera camera;
//    private boolean flashOn = false;
//    private CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
//    private ImageView btnFlash;
//
//    private boolean longPressTriggered = false;
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_camera);
//
//        previewView = findViewById(R.id.previewView);
//
//        if (hasCameraPermission()) startCamera();
//        else requestPermissions();
//
//        setupUiActions();
//    }
//
//    // ================= UI =================
//
//    private void setupUiActions() {
//
//        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
//
//        findViewById(R.id.btnFlash).setOnClickListener(v -> toggleFlash());
//
//        findViewById(R.id.btnSwitch).setOnClickListener(v -> {
//                    if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA){
//                        btnFlash.setVisibility(View.VISIBLE);
//                        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
//                    }else{
//                        btnFlash.setVisibility(View.GONE);
//                        cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
//                    }
//            startCamera();
//        });
//
//        View capture = findViewById(R.id.btnCapture);
//        btnFlash = findViewById(R.id.btnFlash);
//        txtTimer = findViewById(R.id.txtTimer);
//        recordDot = findViewById(R.id.recordDot);
//
//        capture.setOnTouchListener((v, event) -> {
//
//            switch (event.getAction()) {
//
//                case MotionEvent.ACTION_DOWN:
//                    longPressTriggered = false;
//                    v.setPressed(true);
//                    animatePress(v);
//
//                    v.postDelayed(() -> {
//                        if (v.isPressed()) {
//                            longPressTriggered = true;
//                            animateRecordingStart(v);
//                            vibrateOnRecordStart();
//                            startVideo();
//                        }
//                    }, 400);
//                    return true;
//
//                case MotionEvent.ACTION_UP:
//                case MotionEvent.ACTION_CANCEL:
//
//                    v.setPressed(false);
//
//                    if (longPressTriggered) {
//                        stopVideo();
//                        animateRecordingStop(v);
//                    } else {
//                        takePhoto();
//                        animateRelease(v);
//                    }
//                    return true;
//            }
//            return false;
//        });
//    }
//
//    private final Runnable blinkRunnable = new Runnable() {
//        @Override
//        public void run() {
//            if (recordDot.getVisibility() == View.VISIBLE) {
//                recordDot.setAlpha(blink ? 0f : 1f);
//                blink = !blink;
//                recordDot.postDelayed(this, 500); // blink speed
//            }
//        }
//    };
//
//
//    private void startTimer() {
//        seconds = 0;
//        txtTimer.setText("00:00");
//        txtTimer.setVisibility(View.VISIBLE);
//
//        timerHandler.postDelayed(timerRunnable, 1000);
//    }
//
//    private final Runnable timerRunnable = new Runnable() {
//        @Override
//        public void run() {
//            seconds++;
//
//            int min = seconds / 60;
//            int sec = seconds % 60;
//
//            txtTimer.setText(
//                    String.format("%02d:%02d", min, sec)
//            );
//
//            timerHandler.postDelayed(this, 1000);
//        }
//    };
//
//    private void stopTimer() {
//        timerHandler.removeCallbacks(timerRunnable);
//        txtTimer.setVisibility(View.GONE);
//        seconds = 0;
//    }
//
//
//    // ================= CAMERA =================
//
//    @SuppressLint("MissingPermission")
//    private void startCamera() {
//        ProcessCameraProvider.getInstance(this).addListener(() -> {
//            try {
//                ProcessCameraProvider provider =
//                        ProcessCameraProvider.getInstance(this).get();
//
//                provider.unbindAll();
//
//                Preview preview = new Preview.Builder().build();
//                preview.setSurfaceProvider(previewView.getSurfaceProvider());
//
//                imageCapture = new ImageCapture.Builder().build();
//
//                Recorder recorder = new Recorder.Builder()
//                        .setQualitySelector(QualitySelector.from(Quality.HD))
//                        .build();
//
//                videoCapture = VideoCapture.withOutput(recorder);
//
//                camera = provider.bindToLifecycle(
//                        this,
//                        cameraSelector,
//                        preview,
//                        imageCapture,
//                        videoCapture
//                );
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }, getExecutor());
//    }
//
//    private void toggleFlash() {
//        if (camera == null) return;
//
//        flashOn = !flashOn;
//        if (flashOn) {
//            btnFlash.setImageResource(R.drawable.ic_flash_on);
//        } else {
//            btnFlash.setImageResource(R.drawable.ic_flash_off);
//        }
//
//        // Enable torch on back camera only
//        camera.getCameraControl().enableTorch(flashOn);
//    }
//
//
//    // ================= PHOTO =================
//
//    private void takePhoto() {
//        if (imageCapture == null) return;
//
//        File file = new File(getCacheDir(),
//                System.currentTimeMillis() + ".jpg");
//
//        ImageCapture.OutputFileOptions options =
//                new ImageCapture.OutputFileOptions.Builder(file).build();
//
//        imageCapture.takePicture(
//                options,
//                getExecutor(),
//                new ImageCapture.OnImageSavedCallback() {
//@Override
//public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
//
//    Intent intent = new Intent(CameraActivity.this, PreviewActivity.class);
//    intent.putExtra("localPath", file.getAbsolutePath());
//    intent.putExtra("type", "image");
//    startActivity(intent);
//    finish();
//}
//                    @Override
//                    public void onError(
//                            @NonNull ImageCaptureException exception) {
//                        exception.printStackTrace();
//                    }
//                }
//        );
//    }
//
//    // ================= VIDEO =================
//
//    private void vibrateOnRecordStart() {
//        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
//        if (vibrator == null) return;
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            vibrator.vibrate(
//                    VibrationEffect.createOneShot(
//                            40, // milliseconds (WhatsApp jaisa light)
//                            VibrationEffect.DEFAULT_AMPLITUDE
//                    )
//            );
//        } else {
//            vibrator.vibrate(40);
//        }
//    }
//
//    @SuppressLint("MissingPermission")
//    private void startVideo() {
//        if (videoCapture == null || activeRecording != null) return;
//
//        File file = new File(getCacheDir(),
//                System.currentTimeMillis() + ".mp4");
//
//        lastVideoFile = file;
//
//        PendingRecording pending =
//                videoCapture.getOutput()
//                        .prepareRecording(
//                                this,
//                                new FileOutputOptions.Builder(file).build()
//                        );
//
//        if (hasAudioPermission())
//            pending.withAudioEnabled();
//
//        activeRecording =
//                pending.start(getExecutor(), event -> {
//
//                    if (event instanceof VideoRecordEvent.Finalize) {
//
//                        VideoRecordEvent.Finalize finalize =
//                                (VideoRecordEvent.Finalize) event;
//
//                        if (!finalize.hasError()) {
//
//                            runOnUiThread(() -> {
//                                Intent intent =
//                                        new Intent(CameraActivity.this,
//                                                PreviewActivity.class);
//
//                                intent.putExtra("localPath",
//                                        lastVideoFile.getAbsolutePath());
//                                intent.putExtra("type", "video");
//                                startActivity(intent);
//                                finish();
//                            });
//
//                        } else {
//                            finalize.getError();
//                        }
//                    }
//                });
//
//        startTimer();
//        recordDot.setVisibility(View.VISIBLE);
//        recordDot.post(blinkRunnable);
//    }
//
//
//    private void stopVideo() {
//        if (activeRecording != null) {
//            activeRecording.stop(); // ✅ preview yahan se nahi kholna
//            activeRecording = null;
//
//            stopTimer();
//            recordDot.removeCallbacks(blinkRunnable);
//            recordDot.setVisibility(View.GONE);
//            blink = true;
//        }
//    }
//
//    private void animatePress(View v) {
//        v.animate()
//                .scaleX(0.9f)
//                .scaleY(0.9f)
//                .setDuration(100)
//                .start();
//    }
//
//    private void animateRelease(View v) {
//        v.animate()
//                .scaleX(1f)
//                .scaleY(1f)
//                .setDuration(100)
//                .start();
//    }
//    private void animateRecordingStart(View v) {
//        v.animate()
//                .scaleX(1.15f)
//                .scaleY(1.15f)
//                .setDuration(200)
//                .start();
//    }
//
//    private void animateRecordingStop(View v) {
//        v.animate()
//                .scaleX(1f)
//                .scaleY(1f)
//                .setDuration(200)
//                .start();
//    }
//
//
//
//    // ================= PERMISSIONS =================
//
//    private boolean hasCameraPermission() {
//        return ContextCompat.checkSelfPermission(
//                this, Manifest.permission.CAMERA)
//                == PackageManager.PERMISSION_GRANTED;
//    }
//
//    private boolean hasAudioPermission() {
//        return ContextCompat.checkSelfPermission(
//                this, Manifest.permission.RECORD_AUDIO)
//                == PackageManager.PERMISSION_GRANTED;
//    }
//
//    private void requestPermissions() {
//        ActivityCompat.requestPermissions(
//                this,
//                new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
//                PERMISSION_REQ
//        );
//    }
//
//    private Executor getExecutor() {
//        return ContextCompat.getMainExecutor(this);
//    }
//}








package com.example.safesphere.media;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.*;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.safesphere.R;

import java.io.File;
import java.util.concurrent.Executor;

public class CameraActivity extends AppCompatActivity {

    private View recordDot;
    private boolean blink = true;

    private TextView txtTimer;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private int seconds = 0;

    private static final int PERMISSION_REQ = 10;

    private PreviewView previewView;
    private ImageCapture imageCapture;
    private VideoCapture<Recorder> videoCapture;
    private Recording activeRecording;
    private File lastVideoFile;

    private Camera camera;
    private boolean flashOn = false;
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
    private ImageView btnFlash;

    private boolean longPressTriggered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.previewView);

        if (hasCameraPermission()) startCamera();
        else requestPermissions();

        setupUiActions();
    }

    // ================= UI =================

    private void setupUiActions() {
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        findViewById(R.id.btnFlash).setOnClickListener(v -> toggleFlash());

        findViewById(R.id.btnSwitch).setOnClickListener(v -> {
            if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                btnFlash.setVisibility(View.VISIBLE);
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
            } else {
                btnFlash.setVisibility(View.GONE);
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
            }
            startCamera();
        });

        View capture = findViewById(R.id.btnCapture);
        btnFlash = findViewById(R.id.btnFlash);
        txtTimer = findViewById(R.id.txtTimer);
        recordDot = findViewById(R.id.recordDot);

        capture.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    longPressTriggered = false;
                    v.setPressed(true);
                    animatePress(v);

                    v.postDelayed(() -> {
                        if (v.isPressed()) {
                            longPressTriggered = true;
                            animateRecordingStart(v);
                            vibrateOnRecordStart();
                            startVideo();
                        }
                    }, 400);
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false);

                    if (longPressTriggered) {
                        stopVideo();
                        animateRecordingStop(v);
                    } else {
                        takePhoto();
                        animateRelease(v);
                    }
                    return true;
            }
            return false;
        });
    }

    private final Runnable blinkRunnable = new Runnable() {
        @Override
        public void run() {
            if (recordDot.getVisibility() == View.VISIBLE) {
                recordDot.setAlpha(blink ? 0f : 1f);
                blink = !blink;
                recordDot.postDelayed(this, 500);
            }
        }
    };

    private void startTimer() {
        seconds = 0;
        txtTimer.setText("00:00");
        txtTimer.setVisibility(View.VISIBLE);
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            seconds++;
            int min = seconds / 60;
            int sec = seconds % 60;
            txtTimer.setText(String.format("%02d:%02d", min, sec));
            timerHandler.postDelayed(this, 1000);
        }
    };

    private void stopTimer() {
        timerHandler.removeCallbacks(timerRunnable);
        txtTimer.setVisibility(View.GONE);
        seconds = 0;
    }

    // ================= CAMERA =================

    @SuppressLint("MissingPermission")
    private void startCamera() {
        ProcessCameraProvider.getInstance(this).addListener(() -> {
            try {
                ProcessCameraProvider provider = ProcessCameraProvider.getInstance(this).get();
                provider.unbindAll();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();

                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HD))
                        .build();
                videoCapture = VideoCapture.withOutput(recorder);

                camera = provider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageCapture,
                        videoCapture
                );

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, getExecutor());
    }

    private void toggleFlash() {
        if (camera == null) return;

        flashOn = !flashOn;
        btnFlash.setImageResource(flashOn ? R.drawable.ic_flash_on : R.drawable.ic_flash_off);

        if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
            camera.getCameraControl().enableTorch(flashOn);
    }

    // ================= PHOTO =================

    private void takePhoto() {
        if (imageCapture == null) return;

        File file = new File(getCacheDir(), System.currentTimeMillis() + ".jpg");

        ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(file).build();

        imageCapture.takePicture(
                options,
                getExecutor(),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        returnResult(file.getAbsolutePath(), "image");
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        exception.printStackTrace();
                    }
                }
        );
    }

    // ================= VIDEO =================

    private void vibrateOnRecordStart() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(40);
        }
    }

    @SuppressLint("MissingPermission")
    private void startVideo() {
        if (videoCapture == null || activeRecording != null) return;

        File file = new File(getCacheDir(), System.currentTimeMillis() + ".mp4");
        lastVideoFile = file;

        PendingRecording pending = videoCapture.getOutput()
                .prepareRecording(this, new FileOutputOptions.Builder(file).build());

        if (hasAudioPermission()) pending.withAudioEnabled();

        activeRecording = pending.start(getExecutor(), event -> {
            if (event instanceof VideoRecordEvent.Finalize) {
                VideoRecordEvent.Finalize finalize = (VideoRecordEvent.Finalize) event;
                if (!finalize.hasError()) {
                    returnResult(lastVideoFile.getAbsolutePath(), "video");
                } else {
                    finalize.getError();
                }
            }
        });

        startTimer();
        recordDot.setVisibility(View.VISIBLE);
        recordDot.post(blinkRunnable);
    }

    private void stopVideo() {
        if (activeRecording != null) {
            activeRecording.stop();
            activeRecording = null;

            stopTimer();
            recordDot.removeCallbacks(blinkRunnable);
            recordDot.setVisibility(View.GONE);
            blink = true;
        }
    }

    private void animatePress(View v) {
        v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).start();
    }

    private void animateRelease(View v) {
        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
    }

    private void animateRecordingStart(View v) {
        v.animate().scaleX(1.15f).scaleY(1.15f).setDuration(200).start();
    }

    private void animateRecordingStop(View v) {
        v.animate().scaleX(1f).scaleY(1f).setDuration(200).start();
    }

    // ================= PERMISSIONS =================

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasAudioPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                PERMISSION_REQ);
    }

    private Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    // ================= RESULT HANDLER =================

    private void returnResult(String path, String type) {
        stopVideo();
        Intent result = new Intent();
        result.putExtra("localPath", path);
        result.putExtra("type", type);
        setResult(RESULT_OK, result);
        finish();
    }
}
