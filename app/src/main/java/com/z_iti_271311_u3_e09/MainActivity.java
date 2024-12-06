package com.z_iti_271311_u3_e09;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private LessonSixGLSurfaceView mGLSurfaceView;
    private LessonSixRenderer mRenderer;
    private static final int REQUEST_CAMERA_PERMISSION = 10;
    private PreviewView previewView;
    private OverlayView overlayView;
    private PoseDetector poseDetector;
    private ExecutorService cameraExecutor;

    // Selector de cámara y botones
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
    private boolean isFrontCamera = (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA);

    private ProcessCameraProvider cameraProvider; // Añadir esta variable para manejar la cámara

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        overlayView = findViewById(R.id.overlayView);
        mGLSurfaceView = (LessonSixGLSurfaceView) findViewById(R.id.gl_surface_view);

        // Configurar el detector de poses
        PoseDetectorOptions options = new PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build();
        poseDetector = PoseDetection.getClient(options);

        // Solicitar permisos de cámara
        requestCameraPermission();

        cameraExecutor = Executors.newSingleThreadExecutor();

        //Boton que rota positivamente la parte de la primera articulacion del brazo
        /*Button B2 = findViewById(R.id.brazoArriba);
        B2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRenderer.g_joint1Angle < 135.0) mRenderer.g_joint1Angle += mRenderer.ANGLE_STEP;
            }
        });

        //Boton que rota negativamente la parte de la primera articulacion del brazo
        Button B1 = findViewById(R.id.brazoAbajo);
        B1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRenderer.g_joint1Angle > -135.0) mRenderer.g_joint1Angle -= mRenderer.ANGLE_STEP;
            }
        });

        //Boton que rota positivamente la articulacion de la muñeca
        //La cual se mueve en su propio eje ya sea a la izqueirda o hacia la derecha
        Button B3 = findViewById(R.id.munecaDerecha);
        B3.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mRenderer.g_joint2Angle = (mRenderer.g_joint2Angle + mRenderer.ANGLE_STEP) % 360;
            }
        });

        //Boton que rota negativamente la articulacion de la muñeca
        //La cual se mueve en su propio eje ya sea a la izqueirda o hacia la derecha
        Button B4 = findViewById(R.id.munecaIzquierda);
        B4.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mRenderer.g_joint2Angle = (mRenderer.g_joint2Angle - mRenderer.ANGLE_STEP) % 360;
            }
        });

        //Boton que abre los dedos del brazo
        Button B5 = findViewById(R.id.dedosAbrir);
        B5.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRenderer.g_joint3Angle < 8.0)  mRenderer.g_joint3Angle = (mRenderer.g_joint3Angle + mRenderer.ANGLE_STEP) % 360;
            }
        });


        //Boton que cierra los dedos del brazo
        Button B6 = findViewById(R.id.dedosCerrar);
        B6.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRenderer.g_joint3Angle > -8.0) mRenderer.g_joint3Angle = (mRenderer.g_joint3Angle - mRenderer.ANGLE_STEP) % 360;
            }
        });*/

        // Solicite un contexto compatible con OpenGL ES 2.0.
        mGLSurfaceView.setEGLContextClientVersion(2);

        final DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        // Establezca el renderizador en nuestro renderizador de demostración, que se define a continuación.
        mRenderer = new LessonSixRenderer(this);
        mGLSurfaceView.setRenderer(mRenderer, displayMetrics.density);
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        } else {
            startCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Permiso de cámara necesario", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9) // Ajusta esto según la relación que mejor funcione
                        .build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720)) // Usa una resolución adecuada para tu cámara
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::processImageProxy);

                // Desvincula y vuelve a vincular la cámara con el selector actual
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraX", "Error al inicializar la cámara", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void processImageProxy(ImageProxy imageProxy) {
        if (imageProxy == null || imageProxy.getImage() == null) {
            return;
        }

        InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

        poseDetector.process(image)
                .addOnSuccessListener(pose -> {
                    processPose(pose);

                    List< PoseLandmark > landmarks = pose.getAllPoseLandmarks();

                    if (landmarks.isEmpty()) {
                        // No se detectó ninguna pose, limpiar el OverlayView
                        runOnUiThread(() -> overlayView.setLandmarks(null, 0, 0, isFrontCamera, 0));
                    } else {
                        // Pasar la información correcta a OverlayView
                        int imageWidth = image.getWidth();
                        int imageHeight = image.getHeight();
                        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();

                        runOnUiThread(() -> {
                            Log.d("OverlayView", "Landmarks count: " + landmarks.size());
                            overlayView.setLandmarks(landmarks, imageWidth, imageHeight, isFrontCamera, rotationDegrees);
                        });

                    }

                    imageProxy.close();
                })
                .addOnFailureListener(e -> {
                    Log.e("PoseDetection", "Error al procesar la imagen", e);
                    imageProxy.close();
                });
    }

    private void processPose(Pose pose) {
        // Obtener las posiciones de las articulaciones relevantes (x, y, z)
        PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
        PoseLandmark rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW);
        PoseLandmark rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
        PoseLandmark thumbWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_THUMB);

        // Asegurarse de que las articulaciones estén presentes
        if (rightShoulder != null && rightElbow != null && rightWrist != null && thumbWrist != null) {

            double angle = calculateArmAngle(rightShoulder, rightElbow, rightWrist);
            double angleWrist = calculateWristAngle(thumbWrist, rightElbow, rightWrist);

            Log.i("OverlayView", "Angulo de la muñeca: " + angle + " grados");
            mRenderer.g_joint1Angle = (float) angle;
            mRenderer.g_joint2Angle = (float) angleWrist;

        }
    }

    public double calculateWristAngle(PoseLandmark handRight, PoseLandmark rightElbow, PoseLandmark rightWrist){
        float[] vectorAB = {
                rightWrist.getPosition().x - rightElbow.getPosition().x,
                rightWrist.getPosition().y - rightElbow.getPosition().y
        };

        float[] vectorBC = {
                handRight.getPosition().x - rightWrist.getPosition().x,
                handRight.getPosition().y - rightWrist.getPosition().y
        };

        // Magnitudes de los vectores
        double magnitudeAB = Math.sqrt(
                vectorAB[0] * vectorAB[0] +
                        vectorAB[1] * vectorAB[1]
        );

        double magnitudeBC = Math.sqrt(
                vectorBC[0] * vectorBC[0] +
                        vectorBC[1] * vectorBC[1]
        );

        // Producto escalar
        double dotProduct =
                vectorAB[0] * vectorBC[0] +
                        vectorAB[1] * vectorBC[1];

        // Calcular el ángulo en radianes
        double angle = Math.acos(dotProduct / (magnitudeAB * magnitudeBC));

        return Math.toDegrees(angle);
    }

    public double calculateArmAngle(PoseLandmark rightShoulder, PoseLandmark rightElbow, PoseLandmark rightWrist){

        float[] vectorAB = {
                rightElbow.getPosition().x - rightShoulder.getPosition().x,
                rightElbow.getPosition().y - rightShoulder.getPosition().y
        };

        float[] vectorBC = {
                rightWrist.getPosition().x - rightElbow.getPosition().x,
                rightWrist.getPosition().y - rightElbow.getPosition().y
        };

        double magnitudeAB = Math.sqrt(vectorAB[0] * vectorAB[0] + vectorAB[1] * vectorAB[1]);
        double magnitudeBC = Math.sqrt(vectorBC[0] * vectorBC[0] + vectorBC[1] * vectorBC[1]);

        double dotProduct = vectorAB[0] * vectorBC[0] + vectorAB[1] * vectorBC[1];

        double angle = Math.acos(dotProduct / (magnitudeAB * magnitudeBC));

        return Math.toDegrees(angle);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        poseDetector.close();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }

}
