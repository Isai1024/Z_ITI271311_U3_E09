package com.z_iti_271311_u3_e09;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OverlayView extends View {

    private List<PoseLandmark> landmarks;
    private int imageWidth;
    private int imageHeight;
    private boolean isFrontCamera;
    private int rotationDegrees;
    private Paint landmarkPaint;
    private Paint linePaint;
    private Paint linePaintArm;
    private ArrayList<Integer> points;

    public OverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        points = new ArrayList<>(Arrays.asList(12, 14, 16, 18, 20, 22));

        landmarkPaint = new Paint();
        landmarkPaint.setColor(Color.RED);
        landmarkPaint.setStyle(Paint.Style.FILL);
        landmarkPaint.setStrokeWidth(8f);

        linePaint = new Paint();
        linePaint.setColor(Color.TRANSPARENT);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(5f);

        linePaintArm = new Paint();
        linePaintArm.setColor(Color.BLACK);
        linePaintArm.setStyle(Paint.Style.STROKE);
        linePaintArm.setStrokeWidth(5f);
    }

    public void setLandmarks(List<PoseLandmark> landmarks, int imageWidth, int imageHeight, boolean isFrontCamera, int rotationDegrees) {
        this.landmarks = landmarks;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.isFrontCamera = isFrontCamera;
        this.rotationDegrees = rotationDegrees;
        invalidate(); // Llamar a onDraw() para redibujar
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (landmarks == null || landmarks.isEmpty()) {
            Log.d("OverlayView", "No landmarks to draw");
            return;
        }

        Log.d("OverlayView", "Drawing landmarks");

        // Escalado para igualar el tamaño de OverlayView y la imagen de entrada
        float scaleX = getWidth() / (float) imageWidth;
        float scaleY = getHeight() / (float) imageHeight;
        float scale = Math.min(scaleX, scaleY); // Usar el menor de los dos para mantener la proporción

        // Offset para centrar los landmarks en OverlayView
        float offsetX = (getWidth() - (imageWidth * scale)) / 2;
        float offsetY = (getHeight() - (imageHeight * scale)) / 2;

        // Dibujar conexiones entre landmarks para formar la silueta
        drawBodyConnections(canvas, scale, offsetX, offsetY);

        // Dibujar puntos individuales de los landmarks
        for (PoseLandmark landmark : landmarks) {
            PointF point = landmark.getPosition();
            float x = point.x * scale + offsetX;
            float y = point.y * scale + offsetY;

            // Reflejar para cámara frontal
            if (isFrontCamera) {
                x = getWidth() - x;
            }

            // Dibujar puntos individuales con un tamaño grande para visibilidad
            if(points.contains(landmark.getLandmarkType())){
                canvas.drawCircle(x, y, 10f, landmarkPaint);
                canvas.drawText(String.valueOf(landmark.getLandmarkType()), x + 10, y - 10, landmarkPaint);
            }

        }
    }

    private void drawBodyConnections(Canvas canvas, float scale, float offsetX, float offsetY) {
        PoseLandmark rightShoulder = getLandmark(PoseLandmark.RIGHT_SHOULDER);
        PoseLandmark rightElbow = getLandmark(PoseLandmark.RIGHT_ELBOW);
        PoseLandmark rightWrist = getLandmark(PoseLandmark.RIGHT_WRIST);
        PoseLandmark rightthumb = getLandmark(PoseLandmark.RIGHT_THUMB);
        PoseLandmark rightindex = getLandmark(PoseLandmark.RIGHT_INDEX);
        PoseLandmark rightmiddle = getLandmark(PoseLandmark.RIGHT_PINKY);

        connect(canvas, rightShoulder, rightElbow, scale, offsetX, offsetY);
        connect(canvas, rightElbow, rightWrist, scale, offsetX, offsetY);
        connect(canvas, rightWrist, rightthumb, scale, offsetX, offsetY);
        connect(canvas, rightWrist, rightindex, scale, offsetX, offsetY);
        connect(canvas, rightWrist, rightmiddle, scale, offsetX, offsetY);
    }

    private PoseLandmark getLandmark(int type) {
        if (landmarks == null) return null;
        for (PoseLandmark landmark : landmarks) {
            if (landmark.getLandmarkType() == type) return landmark;
        }
        return null;
    }

    private void connect(Canvas canvas, PoseLandmark start, PoseLandmark end, float scale, float offsetX, float offsetY) {
        if (start == null || end == null) return;

        float startX = start.getPosition().x * scale + offsetX;
        float startY = start.getPosition().y * scale + offsetY;
        float endX = end.getPosition().x * scale + offsetX;
        float endY = end.getPosition().y * scale + offsetY;

        if (isFrontCamera) {
            startX = getWidth() - startX;
            endX = getWidth() - endX;
        }

        canvas.drawLine(startX, startY, endX, endY, linePaintArm);
    }
}
