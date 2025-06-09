package com.example.frameprocessor;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.media.Image;
import android.media.ImageReader;
import android.hardware.camera2.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.opengl.GLSurfaceView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private TextureView textureView;
    private GLSurfaceView glSurfaceView;
    private GLRenderer glRenderer;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private LinearLayout permissionLayout;
    private Button requestPermissionButton;
    private Button captureButton;
    private Mat inputMat;
    private Mat outputMat;
    private boolean isProcessing = false;
    private ImageReader imageReader;
    private Size previewSize;
    private TextView fpsTextView;

    private long frameCount = 0;
    private long lastLogTime = System.currentTimeMillis();

    static {
        System.loadLibrary("edge_detector");
    }

    private native void processFrame(long inputMatAddr, long outputMatAddr);

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.d(TAG, "OpenCV loaded successfully");
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        glSurfaceView = findViewById(R.id.gl_surface_view);
        glRenderer = new GLRenderer();
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(glRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        glSurfaceView.setVisibility(View.GONE);

        glRenderer.setFrameProcessedCallback(() -> {
            Log.d(TAG, "frameProcessedCallback: OpenGL texture updated. Resetting isProcessing to false.");
            isProcessing = false;
            frameCount++;

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastLogTime >= 1000) {
                final long currentFPS = frameCount;
                Log.d(TAG, "FPS: " + currentFPS);
                runOnUiThread(() -> {
                    fpsTextView.setText(String.format("FPS: %d", currentFPS));
                });
                frameCount = 0;
                lastLogTime = currentTime;
            }

            if (backgroundHandler != null) {
                backgroundHandler.postDelayed(processFrameRunnable, 1000 / 15);
            }
        });

        textureView = findViewById(R.id.texture_view);
        permissionLayout = findViewById(R.id.permission_layout);
        requestPermissionButton = findViewById(R.id.request_permission_button);
        captureButton = findViewById(R.id.capture_button);
        fpsTextView = findViewById(R.id.fps_text_view);

        requestPermissionButton.setOnClickListener(v -> requestCameraPermission());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            permissionLayout.setVisibility(View.VISIBLE);
            textureView.setVisibility(View.GONE);
            captureButton.setVisibility(View.GONE);
            fpsTextView.setVisibility(View.GONE);
        } else {
            permissionLayout.setVisibility(View.GONE);
            textureView.setAlpha(1.0f);
            textureView.setVisibility(View.VISIBLE);
            captureButton.setVisibility(View.GONE);
            fpsTextView.setVisibility(View.VISIBLE);
        }

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.d(TAG, "Surface texture available: " + width + "x" + height);
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                    if (backgroundHandler != null) {
                        Log.d(TAG, "Scheduling initial processFrameRunnable post.");
                        backgroundHandler.post(processFrameRunnable);
                    }
                } else {
                    Log.d(TAG, "Showing permission request");
                    permissionLayout.setVisibility(View.VISIBLE);
                    textureView.setVisibility(View.GONE);
                    captureButton.setVisibility(View.GONE);
                    fpsTextView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                Log.d(TAG, "Surface texture size changed: " + width + "x" + height);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                Log.d(TAG, "Surface texture destroyed");
                if (backgroundHandler != null) {
                    backgroundHandler.removeCallbacks(processFrameRunnable);
                    Log.d(TAG, "Removed processFrameRunnable callbacks on surface destroyed.");
                }
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                // Frame processing handled by timed loop
            }
        });
    }

    private final Runnable processFrameRunnable = this::processCurrentFrame;

    private void processCurrentFrame() {
        Log.d(TAG, "processCurrentFrame: Attempting to process a frame. isProcessing = " + isProcessing);
        if (isProcessing) {
            Log.d(TAG, "processCurrentFrame: Skipping frame because isProcessing is true.");
            if (backgroundHandler != null) {
                backgroundHandler.postDelayed(processFrameRunnable, 1000 / 15);
            }
            return;
        }
        isProcessing = true;

        try {
            Bitmap bitmap = textureView.getBitmap();
            if (bitmap != null) {
                Log.d(TAG, "Processing frame: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                if (inputMat == null || inputMat.cols() != bitmap.getWidth() || inputMat.rows() != bitmap.getHeight()) {
                    inputMat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);
                    outputMat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);
                }
                
                Utils.bitmapToMat(bitmap, inputMat);
                processFrame(inputMat.getNativeObjAddr(), outputMat.getNativeObjAddr());
                
                Log.d(TAG, "Output Mat properties: type=" + outputMat.type() +
                        ", channels=" + outputMat.channels() +
                        ", cols=" + outputMat.cols() +
                        ", rows=" + outputMat.rows());

                int total = (int)outputMat.total();
                int channels = outputMat.channels();
                byte[] data = new byte[total * channels];
                outputMat.get(0, 0, data);
                
                final int cols = outputMat.cols();
                final int rows = outputMat.rows();
                glSurfaceView.queueEvent(() -> {
                    Log.d(TAG, "Queuing OpenGL updateTexture call.");
                    glRenderer.updateTexture(data, cols, rows);
                    glSurfaceView.requestRender();
                });

                runOnUiThread(() -> {
                    if (glSurfaceView.getVisibility() != View.VISIBLE) {
                        Log.d(TAG, "Switching to GLSurfaceView visibility.");
                        glSurfaceView.setVisibility(View.VISIBLE);
                        textureView.setVisibility(View.GONE);
                    }
                });
            } else {
                Log.e(TAG, "Failed to get bitmap from TextureView. Bitmap is null.");
                Toast.makeText(this, "Failed to get bitmap from TextureView. Is camera preview working?", Toast.LENGTH_LONG).show();
                isProcessing = false;
                if (backgroundHandler != null) {
                    Log.d(TAG, "Scheduling next processFrameRunnable post on bitmap null/error.");
                    backgroundHandler.postDelayed(processFrameRunnable, 1000 / 15);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing frame", e);
            Toast.makeText(this, "Error processing frame: " + e.getMessage(), Toast.LENGTH_LONG).show();
            isProcessing = false;
            if (backgroundHandler != null) {
                Log.d(TAG, "Scheduling next processFrameRunnable post on exception.");
                backgroundHandler.postDelayed(processFrameRunnable, 1000 / 15);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        startBackgroundThread();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        glSurfaceView.onResume();
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            permissionLayout.setVisibility(View.VISIBLE);
            textureView.setVisibility(View.GONE);
            captureButton.setVisibility(View.GONE);
            fpsTextView.setVisibility(View.GONE);
        } else {
            permissionLayout.setVisibility(View.GONE);
            textureView.setAlpha(1.0f);
            textureView.setVisibility(View.VISIBLE);
            captureButton.setVisibility(View.GONE);
            fpsTextView.setVisibility(View.VISIBLE);
            if (textureView.isAvailable() && backgroundHandler != null) {
                Log.d(TAG, "Scheduling initial processFrameRunnable post on resume.");
                backgroundHandler.post(processFrameRunnable);
            }
        }
        glSurfaceView.setVisibility(View.GONE);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        closeCamera();
        stopBackgroundThread();
        glSurfaceView.onPause();
        if (backgroundHandler != null) {
            backgroundHandler.removeCallbacks(processFrameRunnable);
            Log.d(TAG, "Removed processFrameRunnable callbacks on pause.");
        }
        super.onPause();
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionLayout.setVisibility(View.GONE);
                textureView.setAlpha(1.0f);
                textureView.setVisibility(View.VISIBLE);
                captureButton.setVisibility(View.GONE);
                fpsTextView.setVisibility(View.VISIBLE);
                glSurfaceView.setVisibility(View.GONE);
                openCamera();
                if (backgroundHandler != null) {
                    Log.d(TAG, "Scheduling initial processFrameRunnable post after permission granted and camera opened.");
                    backgroundHandler.post(processFrameRunnable);
                }
            } else {
                Log.d(TAG, "Camera permission denied");
                permissionLayout.setVisibility(View.VISIBLE);
                textureView.setVisibility(View.GONE);
                captureButton.setVisibility(View.GONE);
                fpsTextView.setVisibility(View.GONE);
            }
        }
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), 1280, 720);

            textureView.getSurfaceTexture().setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface textureSurface = new Surface(textureView.getSurfaceTexture());

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(CameraDevice camera) {
                        cameraDevice = camera;
                        createCameraPreviewSession(textureSurface);
                    }

                    @Override
                    public void onDisconnected(CameraDevice camera) {
                        camera.close();
                        cameraDevice = null;
                    }

                    @Override
                    public void onError(CameraDevice camera, int error) {
                        camera.close();
                        cameraDevice = null;
                        Log.e(TAG, "CameraDevice.StateCallback onError: " + error);
                    }
                }, backgroundHandler);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error opening camera", e);
        }
    }

    private void createCameraPreviewSession(Surface textureSurface) {
        try {
            final CaptureRequest.Builder previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(textureSurface);

            cameraDevice.createCaptureSession(Collections.singletonList(textureSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            if (cameraDevice == null) return;
                            captureSession = session;
                            try {
                                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler);
                            } catch (CameraAccessException e) {
                                Log.e(TAG, "Error starting camera preview", e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            Log.e(TAG, "Failed to configure camera session");
                        }
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error creating camera preview session", e);
        }
    }

    private void closeCamera() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundHandler != null) {
            backgroundHandler.removeCallbacks(processFrameRunnable);
            Log.d(TAG, "Removed processFrameRunnable callbacks in stopBackgroundThread.");
        }
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            Log.e(TAG, "Error stopping background thread", e);
        }
    }

    private Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight) {
        List<Size> bigEnough = new ArrayList<>();
        List<Size> tooSmall = new ArrayList<>();
        for (Size option : choices) {
            if (option.getWidth() >= textureViewWidth && option.getHeight() >= textureViewHeight) {
                bigEnough.add(option);
            } else {
                tooSmall.add(option);
            }
        }
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (tooSmall.size() > 0) {
            return Collections.max(tooSmall, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn\'t find any suitable preview size");
            return choices[0];
        }
    }

    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                                (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
} 