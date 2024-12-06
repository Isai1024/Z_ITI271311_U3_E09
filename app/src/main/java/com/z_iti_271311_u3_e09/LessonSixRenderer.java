package com.z_iti_271311_u3_e09;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Stack;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


/**
 *Esta clase implementa nuestro renderizador personalizado. Tenga en cuenta que el parámetro GL10 pasado no se utiliza para OpenGL ES 2.0
   * procesadores: en su lugar se utiliza la clase estática GLES20.
 */
public class LessonSixRenderer implements GLSurfaceView.Renderer 
{	

	private final Context mActivityContext;
	
	/**
	 * Almacenar la matriz del modelo. Esta matriz se utiliza para mover modelos desde el espacio de objetos (donde se puede pensar cada modelo
	 * de estar ubicado en el centro del universo) al espacio mundial.
	 */
	private float[] mModelMatrix = new float[16];

	/**
	 * Almacenar la matriz de vista. Esto se puede considerar como nuestra cámara. Esta matriz transforma el espacio del mundo al espacio ocular;
	 * Posiciona cosas relativas a nuestro ojo.
	 */
	private float[] mViewMatrix = new float[16];

	/* Almacenar la matriz de proyección. Esto se utiliza para proyectar la escena en una ventana 2D. */
	private float[] mProjectionMatrix = new float[16];
	

	/** Asignar almacenamiento para la matriz final combinada. Esto se pasará al programa de sombreado. */
	private float[] mMVPMatrix = new float[16];
	
	/** Almacenar la rotación acumulada. */
	private final float[] mAccumulatedRotation = new float[16];
	
	/** Almacena la rotación actual. */
	private final float[] mCurrentRotation = new float[16];
	
	/** Una matriz temporal. */
	private float[] mTemporaryMatrix = new float[16];
	
	/** 
	 * Almacena una copia de la matriz del modelo específicamente para la posición de luz.
	 */
	private float[] mLightModelMatrix = new float[16];	
	
	/** Almacena nuestros datos modelo en un búfer flotante */
	private final FloatBuffer mCubePositions;	
	private final FloatBuffer mCubeNormals;
	private final FloatBuffer mCubeTextureCoordinates;

	/** Esto se utilizará para pasar en la matriz de transformación. */
	private int mMVPMatrixHandle;
	
	/** Esto se utilizará para pasar en la matriz de modelview. */
	private int mMVMatrixHandle;
	
	/** Esto se utilizará para pasar en la posición de luz. */
	private int mLightPosHandle;
	
	/** Esto se utilizará para pasar en la textura. */
	private int mTextureUniformHandle;
	
	/** Esto se usará para pasar la información de posición del modelo. */
	private int mPositionHandle;
	
	/** Esto se utilizará para pasar en la información normal del modelo. */
	private int mNormalHandle;
	
	/** Esto se utilizará para pasar información de coordenadas de la textura del modelo. */
	private int mTextureCoordinateHandle;

	/** Cuántos bytes por flotador. */
	private final int mBytesPerFloat = 4;	
	
	/** Tamaño de los datos de posición en elementos. */
	private final int mPositionDataSize = 3;	
	
	/** Tamaño de los datos normales en elementos. */
	private final int mNormalDataSize = 3;
	
	/** Tamaño de la textura de datos de coordenadas en elementos.*/
	private final int mTextureCoordinateDataSize = 2;
	
	/** Se utiliza para mantener una luz centrada en el origen en el espacio modelo. Necesitamos una cuarta coordenada para poder hacer que las traducciones funcionen cuando
	 * Multiplicamos esto por nuestras matrices de transformación. */
	private final float[] mLightPosInModelSpace = new float[] {0.0f, 0.0f, 0.0f, 1.0f};
	
	/** Se utiliza para mantener la posición actual de la luz en el espacio mundial (después de la transformación a través de la matriz del modelo). */
	private final float[] mLightPosInWorldSpace = new float[4];
	
	/** Se utiliza para mantener la posición transformada de la luz en el espacio ocular (después de la transformación a través de la matriz Modelview) */
	private final float[] mLightPosInEyeSpace = new float[4];
	
	/** Este es un identificador de nuestro programa de sombreado de cubos. */
	private int mProgramHandle;
		
	/** Este es un identificador de nuestro programa de puntos de luz. */
	private int mPointProgramHandle;
	
	/** Estos son los identificadores de nuestros datos de textura. */
	private int texturaPiedra;
	
	// Estos aún funcionan sin volatilidad, pero no se garantiza que las actualizaciones se realicen.
	public volatile float mDeltaX;
	public volatile float mDeltaY;

	public float ANGLE_STEP = 3.0f;     // Los incrementos del ángulo de rotación (grados).
	public float g_joint1Angle = 0.0f; // El ángulo de rotación de la articulación1 (grados)
	public float g_joint2Angle = 90.0f;  // El ángulo de rotación de la articulación 2 (grados)
	public float g_joint3Angle = 0.0f;  // El ángulo de rotación de la articulación3 (grados)

	public float g_joint1Z = 0.0f;
	public float g_joint2Z = 0.0f;
	public float g_joint3Z = 0.0f;
	/**
	 * Inicializar los datos del modelo.
	 */
	public LessonSixRenderer(final Context activityContext) {
		mActivityContext = activityContext;
		// Define los puntos de todas las partes del brazo en el siguiente orden:
		// Cubo base, Brazo, Muñeca, Dedo, Otro Dedo
		// X, Y, Z
		final float[] cubePositionData =
		{
				// En OpenGL el bobinado en sentido antihorario está predeterminado. Esto significa que cuando miramos un triángulo,
				// si los puntos son en sentido contrario a las agujas del reloj estamos mirando el "frente". Si no estamos mirando a
				// la parte de atrás. OpenGL tiene una optimización donde todos los triángulos orientados hacia atrás se seleccionan, ya que
				// normalmente representa la parte trasera de un objeto y no es visible de todos modos.

				// Front face
				-1.0f, 1.0f, 1.0f,
				-1.0f, -1.0f, 1.0f,
				1.0f, 1.0f, 1.0f,
				-1.0f, -1.0f, 1.0f,
				1.0f, -1.0f, 1.0f,
				1.0f, 1.0f, 1.0f,
				// Right face
				1.0f, 1.0f, 1.0f,
				1.0f, -1.0f, 1.0f,
				1.0f, 1.0f, -1.0f,
				1.0f, -1.0f, 1.0f,
				1.0f, -1.0f, -1.0f,
				1.0f, 1.0f, -1.0f,
				// Back face
				1.0f, 1.0f, -1.0f,
				1.0f, -1.0f, -1.0f,
				-1.0f, 1.0f, -1.0f,
				1.0f, -1.0f, -1.0f,
				-1.0f, -1.0f, -1.0f,
				-1.0f, 1.0f, -1.0f,
				// Left face
				-1.0f, 1.0f, -1.0f,
				-1.0f, -1.0f, -1.0f,
				-1.0f, 1.0f, 1.0f,
				-1.0f, -1.0f, -1.0f,
				-1.0f, -1.0f, 1.0f,
				-1.0f, 1.0f, 1.0f,
				// Top face
				-1.0f, 1.0f, -1.0f,
				-1.0f, 1.0f, 1.0f,
				1.0f, 1.0f, -1.0f,
				-1.0f, 1.0f, 1.0f,
				1.0f, 1.0f, 1.0f,
				1.0f, 1.0f, -1.0f,
				// Bottom face
				1.0f, -1.0f, -1.0f,
				1.0f, -1.0f, 1.0f,
				-1.0f, -1.0f, -1.0f,
				1.0f, -1.0f, 1.0f,
				-1.0f, -1.0f, 1.0f,
				-1.0f, -1.0f, -1.0f,
				// Front face
				-1.0f, 4.0f, 1.0f,
				-1.0f, 0.0f, 1.0f,
				1.0f, 4.0f, 1.0f,
				-1.0f, 0.0f, 1.0f,
				1.0f, 0.0f, 1.0f,
				1.0f, 4.0f, 1.0f,
				// Right face
				1.0f, 4.0f, 1.0f,
				1.0f, 0.0f, 1.0f,
				1.0f, 4.0f, -1.0f,
				1.0f, 0.0f, 1.0f,
				1.0f, 0.0f, -1.0f,
				1.0f, 4.0f, -1.0f,
				// Back face
				1.0f, 4.0f, -1.0f,
				1.0f, 0.0f, -1.0f,
				-1.0f, 4.0f, -1.0f,
				1.0f, 0.0f, -1.0f,
				-1.0f, 0.0f, -1.0f,
				-1.0f, 4.0f, -1.0f,
				// Left face
				-1.0f, 4.0f, -1.0f,
				-1.0f, 0.0f, -1.0f,
				-1.0f, 4.0f, 1.0f,
				-1.0f, 0.0f, -1.0f,
				-1.0f, 0.0f, 1.0f,
				-1.0f, 4.0f, 1.0f,
				// Top face
				-1.0f, 4.0f, -1.0f,
				-1.0f, 4.0f, 1.0f,
				1.0f, 4.0f, -1.0f,
				-1.0f, 4.0f, 1.0f,
				1.0f, 4.0f, 1.0f,
				1.0f, 4.0f, -1.0f,
				// Bottom face
				1.0f, 0.0f, -1.0f,
				1.0f, 0.0f, 1.0f,
				-1.0f, 0.0f, -1.0f,
				1.0f, 0.0f, 1.0f,
				-1.0f, 0.0f, 1.0f,
				-1.0f, 0.0f, -1.0f,
				// Front face
				-1.0f, 10.0f, 1.0f,
				-1.0f, 8.0f, 1.0f,
				1.0f, 10.0f, 1.0f,
				-1.0f, 8.0f, 1.0f,
				1.0f, 8.0f, 1.0f,
				1.0f, 10.0f, 1.0f,
				// Right face
				1.0f, 10.0f, 1.0f,
				1.0f, 8.0f, 1.0f,
				1.0f, 10.0f, -1.0f,
				1.0f, 8.0f, 1.0f,
				1.0f, 8.0f, -1.0f,
				1.0f, 10.0f, -1.0f,
				// Back face
				1.0f, 10.0f, -1.0f,
				1.0f, 8.0f, -1.0f,
				-1.0f, 10.0f, -1.0f,
				1.0f, 8.0f, -1.0f,
				-1.0f, 8.0f, -1.0f,
				-1.0f, 10.0f, -1.0f,
				// Left face
				-1.0f, 10.0f, -1.0f,
				-1.0f, 8.0f, -1.0f,
				-1.0f, 10.0f, 1.0f,
				-1.0f, 8.0f, -1.0f,
				-1.0f, 8.0f, 1.0f,
				-1.0f, 10.0f, 1.0f,
				// Top face
				-1.0f, 10.0f, -1.0f,
				-1.0f, 10.0f, 1.0f,
				1.0f, 10.0f, -1.0f,
				-1.0f, 10.0f, 1.0f,
				1.0f, 10.0f, 1.0f,
				1.0f, 10.0f, -1.0f,
				// Bottom face
				1.0f, 8.0f, -1.0f,
				1.0f, 8.0f, 1.0f,
				-1.0f, 8.0f, -1.0f,
				1.0f, 8.0f, 1.0f,
				-1.0f, 8.0f, 1.0f,
				-1.0f, 8.0f, -1.0f,
                // Front face
                2.0f, 8.0f, 1.0f,
                2.0f, 6.0f, 1.0f,
                4.0f, 8.0f, 1.0f,
                2.0f, 6.0f, 1.0f,
                4.0f, 6.0f, 1.0f,
                4.0f, 8.0f, 1.0f,
                // Right face
                4.0f, 8.0f, 1.0f,
                4.0f, 6.0f, 1.0f,
                4.0f, 8.0f, -1.0f,
                4.0f, 6.0f, 1.0f,
                4.0f, 6.0f, -1.0f,
                4.0f, 8.0f, -1.0f,
                // Back face
                4.0f, 8.0f, -1.0f,
                4.0f, 6.0f, -1.0f,
                2.0f, 8.0f, -1.0f,
                4.0f, 6.0f, -1.0f,
                2.0f, 6.0f, -1.0f,
                2.0f, 8.0f, -1.0f,
                // Left face
                2.0f, 8.0f, -1.0f,
                2.0f, 6.0f, -1.0f,
                2.0f, 8.0f, 1.0f,
                2.0f, 6.0f, -1.0f,
                2.0f, 6.0f, 1.0f,
                2.0f, 8.0f, 1.0f,
                // Top face
                2.0f, 8.0f, -1.0f,
                2.0f, 8.0f, 1.0f,
                4.0f, 8.0f, -1.0f,
                2.0f, 8.0f, 1.0f,
                4.0f, 8.0f, 1.0f,
                4.0f, 8.0f, -1.0f,
                // Bottom face
                4.0f, 6.0f, -1.0f,
                4.0f, 6.0f, 1.0f,
                2.0f, 6.0f, -1.0f,
                4.0f, 6.0f, 1.0f,
                2.0f, 6.0f, 1.0f,
                2.0f, 6.0f, -1.0f,
				// Front face
				-4.0f, 8.0f, 1.0f,
				-4.0f, 6.0f, 1.0f,
				-2.0f, 8.0f, 1.0f,
				-4.0f, 6.0f, 1.0f,
				-2.0f, 6.0f, 1.0f,
				-2.0f, 8.0f, 1.0f,
				// Right face
				-2.0f, 8.0f, 1.0f,
				-2.0f, 6.0f, 1.0f,
				-2.0f, 8.0f, -1.0f,
				-2.0f, 6.0f, 1.0f,
				-2.0f, 6.0f, -1.0f,
				-2.0f, 8.0f, -1.0f,
				// Back face
				-2.0f, 8.0f, -1.0f,
				-2.0f, 6.0f, -1.0f,
				-4.0f, 8.0f, -1.0f,
				-2.0f, 6.0f, -1.0f,
				-4.0f, 6.0f, -1.0f,
				-4.0f, 8.0f, -1.0f,
				// Left face
				-4.0f, 8.0f, -1.0f,
				-4.0f, 6.0f, -1.0f,
				-4.0f, 8.0f, 1.0f,
				-4.0f, 6.0f, -1.0f,
				-4.0f, 6.0f, 1.0f,
				-4.0f, 8.0f, 1.0f,
				// Top face
				-4.0f, 8.0f, -1.0f,
				-4.0f, 8.0f, 1.0f,
				-2.0f, 8.0f, -1.0f,
				-4.0f, 8.0f, 1.0f,
				-2.0f, 8.0f, 1.0f,
				-2.0f, 8.0f, -1.0f,
				// Bottom face
				-2.0f, 6.0f, -1.0f,
				-2.0f, 6.0f, 1.0f,
				-4.0f, 6.0f, -1.0f,
				-2.0f, 6.0f, 1.0f,
				-4.0f, 6.0f, 1.0f,
				-4.0f, 6.0f, -1.0f,
		};

		// X, Y, Z
		// Lo normal se usa en cálculos de luz y es un vector que señala
		// ortogonal al plano de la superficie. Para un modelo de cubo, los normales.
		// Debe ser ortogonal a los puntos de cada cara.
		final float[] cubeNormalData =
		{
				// Front face
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,
				// Right face
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				// Back face
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,
				// Left face
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,
				// Top face
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				// Bottom face
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f,
				// Front face
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,
				// Right face
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				// Back face
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,
				// Left face
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,
				// Top face
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				// Bottom face
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f,
				// Front face
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,
				// Right face
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				// Back face
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,
				// Left face
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,
				// Top face
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				// Bottom face
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f,
                // Front face
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                // Right face
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                // Back face
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,
                0.0f, 0.0f, -1.0f,
                // Left face
                -1.0f, 0.0f, 0.0f,
                -1.0f, 0.0f, 0.0f,
                -1.0f, 0.0f, 0.0f,
                -1.0f, 0.0f, 0.0f,
                -1.0f, 0.0f, 0.0f,
                -1.0f, 0.0f, 0.0f,
                // Top face
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                // Bottom face
                0.0f, -1.0f, 0.0f,
                0.0f, -1.0f, 0.0f,
                0.0f, -1.0f, 0.0f,
                0.0f, -1.0f, 0.0f,
                0.0f, -1.0f, 0.0f,
                0.0f, -1.0f, 0.0f,
				// Front face
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,
				// Right face
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				// Back face
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,
				// Left face
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,
				// Top face
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				// Bottom face
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f,
		};

		// S, T (or X, Y)
		// Datos de coordenadas de la textura.
		// Porque las imágenes tienen un eje Y que apunta hacia abajo (los valores aumentan a medida que se mueve hacia abajo en la imagen) mientras
		// OpenGL tiene un eje Y que apunta hacia arriba, lo ajustamos aquí al voltear el eje Y.
		// Lo que es más es que las coordenadas de la textura son las mismas para cada cara.
		final float[] cubeTextureCoordinateData =
		{
				// Front face
				0.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f,
				// Right face
				0.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f,
				// Back face
				0.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f,
				// Left face
				0.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f,
				// Top face
				0.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f,
				// Bottom face
				0.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f,
				// Front face
				0.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f,
				// Right face
				0.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f,
				// Back face
				0.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f,
				// Left face
				0.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f,
				// Top face
				0.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f,
				// Bottom face
				0.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f,
				// Front face
				0.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f,
				// Right face
				0.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f,
				// Back face
				0.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f,
				// Left face
				0.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f,
				// Top face
				0.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f,
				// Bottom face
				0.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f,
                // Front face
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,
                1.0f, 0.0f,
                // Right face
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,
                1.0f, 0.0f,
                // Back face
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,
                1.0f, 0.0f,
                // Left face
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,
                1.0f, 0.0f,
                // Top face
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,
                1.0f, 0.0f,

                // Bottom face
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,
                1.0f, 0.0f,
				// Front face
				0.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f,
				// Right face
				0.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f,
				// Back face
				0.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f,
				// Left face
				0.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f,
				// Top face
				0.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f,
				// Bottom face
				0.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f
		};


		// Inicializa los vertices de la figura
		mCubePositions = ByteBuffer.allocateDirect(cubePositionData.length * mBytesPerFloat)
        .order(ByteOrder.nativeOrder()).asFloatBuffer();							
		mCubePositions.put(cubePositionData).position(0);				
		
		mCubeNormals = ByteBuffer.allocateDirect(cubeNormalData.length * mBytesPerFloat)
        .order(ByteOrder.nativeOrder()).asFloatBuffer();							
		mCubeNormals.put(cubeNormalData).position(0);
		
		mCubeTextureCoordinates = ByteBuffer.allocateDirect(cubeTextureCoordinateData.length * mBytesPerFloat)
		.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mCubeTextureCoordinates.put(cubeTextureCoordinateData).position(0);

	}
	
	@Override
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
		// Establecer el color de fondo claro a negro.
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		
		// Utilice cullin para eliminar las caras posteriores.
		GLES20.glEnable(GLES20.GL_CULL_FACE);
		
		// Habilitar pruebas de profundidad
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
			
		// Posiciona el ojo frente al origen.
		final float eyeX = 0.0f;
		final float eyeY = 0.0f;
		final float eyeZ = -15.5f;

		// Estamos mirando hacia la distancia.
		final float lookX = 0.0f;
		final float lookY = 0.0f;
		final float lookZ = -5.0f;

		// Configurar nuestro vector. Aquí es donde nuestra cabeza estaría apuntando si sostuviéramos la cámara.
		final float upX = 0.0f;
		final float upY = 1.0f;
		final float upZ = 0.0f;

		// Establecer la matriz de vista. Se puede decir que esta matriz representa la posición de la cámara.
		// NOTA: En OpenGL 1, se usa una matriz de ModelView, que es una combinación de un modelo y
		// ver matriz. En OpenGL 2, podemos realizar un seguimiento de estas matrices por separado si lo elegimos.
		Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);		

		final String vertexShader = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.per_pixel_vertex_shader_tex_and_light);
 		final String fragmentShader = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.per_pixel_fragment_shader_tex_and_light);
		
		final int vertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);		
		final int fragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);		
		
		mProgramHandle = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle, 
				new String[] {"a_Position",  "a_Normal", "a_TexCoordinate"});								                                							       
        
        // Definir un programa de sombreado simple para nuestro punto.
        final String pointVertexShader = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.point_vertex_shader);
        final String pointFragmentShader = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.point_fragment_shader);
        
        final int pointVertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, pointVertexShader);
        final int pointFragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, pointFragmentShader);
        mPointProgramHandle = ShaderHelper.createAndLinkProgram(pointVertexShaderHandle, pointFragmentShaderHandle, 
        		new String[] {"a_Position"}); 

        // Cargar la textura
		texturaPiedra = TextureHelper.loadTexture(mActivityContext, R.drawable.texture_metal);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);

        // Inicializar la matriz de rotación acumulada.
        Matrix.setIdentityM(mAccumulatedRotation, 0);
	}	
		
	@Override
	public void onSurfaceChanged(GL10 glUnused, int width, int height) {
		// Establezca la ventana OpenGL al mismo tamaño que la superficie.
		GLES20.glViewport(0, 0, width, height);

		// Crear una nueva matriz de proyección de perspectivas. La altura se mantendrá igual.
		// mientras que el ancho variará según la relación de aspecto.
		final float ratio = (float) width / height;
		final float left = -ratio;
		final float right = ratio;
		final float bottom = -1.0f;
		final float top = 1.0f;
		final float near = 1.0f;
		final float far = 1000.0f;
		
		Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
	}	

	@Override
	public void onDrawFrame(GL10 glUnused) {
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Establece nuestro programa de iluminación por vértice.
        GLES20.glUseProgram(mProgramHandle);

        // Establecer los controladores de programa para el dibujo del cubo.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVMatrix");
        mLightPosHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_LightPos");
        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
        mNormalHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Normal");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");

        // Calcula la posición de la luz. Gire y luego empuje en la distancia.
        Matrix.setIdentityM(mLightModelMatrix, 0);
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, -2.0f);
        Matrix.rotateM(mLightModelMatrix, 0, 180, 0.0f, 1.0f, 0.0f);
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, 10.0f);
        Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0);
        Matrix.multiplyMV(mLightPosInEyeSpace, 0, mViewMatrix, 0, mLightPosInWorldSpace, 0);

        // Traducir el cubo a la pantalla.
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, -5.8f, -3.5f);

        /**Base estatica**/
		pushMatrix(mModelMatrix);
		Matrix.scaleM(mModelMatrix, 0, 3.0f, 0.25f, 3.0f);
		dibujarBase();
		mModelMatrix = popMatrix();


		// Set a matrix that contains the current rotation.
        Matrix.setIdentityM(mCurrentRotation, 0);
    	Matrix.rotateM(mCurrentRotation, 0, mDeltaX, 0.0f, 1.0f, 0.0f);
		mDeltaX = 0.0f;
    	// Multiply the current rotation by the accumulated rotation, and then set the accumulated rotation to the result.
    	Matrix.multiplyMM(mTemporaryMatrix, 0, mCurrentRotation, 0, mAccumulatedRotation, 0);
    	System.arraycopy(mTemporaryMatrix, 0, mAccumulatedRotation, 0, 16);
        // Rotate the cube taking the overall rotation into account.     	
    	Matrix.multiplyMM(mTemporaryMatrix, 0, mModelMatrix, 0, mAccumulatedRotation, 0);
    	System.arraycopy(mTemporaryMatrix, 0, mModelMatrix, 0, 16);


    	/**PARTE BASE**/
		pushMatrix(mModelMatrix);
		Matrix.translateM(mModelMatrix, 0, 0.0f, 2.0f, 0.0f);
		Matrix.scaleM(mModelMatrix, 0, 0.7f, 2.0f, 0.7f);
        dibujarBase();
		mModelMatrix = popMatrix();

        /**BRAZO**/
        pushMatrix(mModelMatrix);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 4.0f, 0.0f);
		Matrix.rotateM(mModelMatrix, 0, g_joint1Angle, 1.0f, 0.0f, 0.0f);
		dibujarBrazo();
        mModelMatrix = popMatrix();

		//MUÑECA
        pushMatrix(mModelMatrix);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 4.0f, 0.0f);
		Matrix.rotateM(mModelMatrix, 0, g_joint1Angle, 1.0f, 0.0f, 0.0f);
		Matrix.rotateM(mModelMatrix, 0, g_joint2Angle, 0.0f, 1.0f, 0.0f);
        Matrix.scaleM(mModelMatrix, 0, 1.5f, 0.5f, 0.7f);
        dibujarMuneca();
        mModelMatrix = popMatrix();

        //**Dedos
        //positivo
        pushMatrix(mModelMatrix);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 4.0f, 0.0f);
        Matrix.rotateM(mModelMatrix, 0, g_joint1Angle, 1.0f, 0.0f, 0.0f);
        Matrix.rotateM(mModelMatrix, 0, g_joint2Angle, 0.0f, 1.0f, 0.0f);
		Matrix.rotateM(mModelMatrix, 0, g_joint3Angle, 0.0f, 0.0f, 1.0f);  // Rotate around the x-axis
        Matrix.scaleM(mModelMatrix, 0, 0.3f, 0.8f, 0.3f);
        dibujarDedo();
        mModelMatrix = popMatrix();

        //Nagativo
        pushMatrix(mModelMatrix);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 4.0f, 0.0f);
		Matrix.rotateM(mModelMatrix, 0, g_joint1Angle, 1.0f, 0.0f, 0.0f);
		Matrix.rotateM(mModelMatrix, 0, g_joint2Angle, 0.0f, 1.0f, 0.0f);
		Matrix.rotateM(mModelMatrix, 0, -1* g_joint3Angle, 0.0f, 0.0f, 1.0f);
        Matrix.scaleM(mModelMatrix, 0, 0.3f, 0.8f, 0.3f);
        dibujarOtroDedo();
        mModelMatrix = popMatrix();

        // Dibuja un punto para indicar la luz.
        GLES20.glUseProgram(mPointProgramHandle);
	}	
	

	/**
	 * Dibuja la base de la figura, tanto la figura alargada que esta en el piso
	 * Como la parte inferior del brazo
	 */			
	private void dibujarBase() {
		// Pase en la información de posición
		mCubePositions.position(0);		
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
        		0, mCubePositions);        
                
        GLES20.glEnableVertexAttribArray(mPositionHandle);                       
        
        // Pase de la información normal
        mCubeNormals.position(0);
        GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false, 
        		0, mCubeNormals);

        GLES20.glEnableVertexAttribArray(mNormalHandle);

		// Esto multiplica la matriz de vistas por la matriz del modelo y almacena el resultado en la matriz de MVP
		// (que actualmente contiene el modelo * vista).
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);   
        
        // Pasar en la matriz modelview.
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);

		// Esto multiplica la matriz de vista de modelo por la matriz de proyección y almacena el resultado en la matriz de MVP
		// (que ahora contiene modelo * vista * proyección).
        Matrix.multiplyMM(mTemporaryMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        System.arraycopy(mTemporaryMatrix, 0, mMVPMatrix, 0, 16);

        // Pasar en la matriz combinada.
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        
        // Pasar en posición de luz en el espacio ocular.
        GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);
        
        // Dibuja los trangulos que constituyen la base de la figura
        //
		bindTexture(texturaPiedra,mCubeTextureCoordinates);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);

	}

	private void dibujarBrazo() {
		// Pase en la información de posición
		mCubePositions.position(0);
		GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
				0, mCubePositions);

		GLES20.glEnableVertexAttribArray(mPositionHandle);

		// Pase de la información normal
		mCubeNormals.position(0);
		GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false,
				0, mCubeNormals);

		GLES20.glEnableVertexAttribArray(mNormalHandle);

		// Esto multiplica la matriz de vistas por la matriz del modelo y almacena el resultado en la matriz de MVP
		// (que actualmente contiene el modelo * vista).
		Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

		// Pasar en la matriz modelview.
		GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);

		// Esto multiplica la matriz de vista de modelo por la matriz de proyección y almacena el resultado en la matriz de MVP
		// (que ahora contiene modelo * vista * proyección).
		Matrix.multiplyMM(mTemporaryMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
		System.arraycopy(mTemporaryMatrix, 0, mMVPMatrix, 0, 16);

		// Pasar en la matriz combinada.
		GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

		// Pasar en posición de luz en el espacio ocular.
		GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);

		// Dibuja los triangulos que conforman a la parte del cubo del centro
		//Que representa el brazo
		bindTexture(texturaPiedra,mCubeTextureCoordinates);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 36, 36);

	}

	private void dibujarMuneca() {
		// Pase en la información de posición
		mCubePositions.position(0);
		GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
				0, mCubePositions);

		GLES20.glEnableVertexAttribArray(mPositionHandle);

		// Pase de la información normal
		mCubeNormals.position(0);
		GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false,
				0, mCubeNormals);

		GLES20.glEnableVertexAttribArray(mNormalHandle);

		// Esto multiplica la matriz de vistas por la matriz del modelo y almacena el resultado en la matriz de MVP
		// (que actualmente contiene el modelo * vista).
		Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

		// Pasar en la matriz modelview.
		GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);

		// Esto multiplica la matriz de vista de modelo por la matriz de proyección y almacena el resultado en la matriz de MVP
		// (que ahora contiene modelo * vista * proyección).
		Matrix.multiplyMM(mTemporaryMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
		System.arraycopy(mTemporaryMatrix, 0, mMVPMatrix, 0, 16);

		// Pasar en la matriz combinada.
		GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

		// Pasar en posición de luz en el espacio ocular.
		GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);

		// Se dibujan los traignulos correspondientes para formar la parte superior
		// el cubo que puede girar en si mismo que representa la muñeca
		bindTexture(texturaPiedra,mCubeTextureCoordinates);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 72, 36);
	}

    private void dibujarDedo() {
		// Pase en la información de posición
		mCubePositions.position(0);
		GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
				0, mCubePositions);

		GLES20.glEnableVertexAttribArray(mPositionHandle);

		// Pase de la información normal
		mCubeNormals.position(0);
		GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false,
				0, mCubeNormals);

		GLES20.glEnableVertexAttribArray(mNormalHandle);

		// Esto multiplica la matriz de vistas por la matriz del modelo y almacena el resultado en la matriz de MVP
		// (que actualmente contiene el modelo * vista).
		Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

		// Pasar en la matriz modelview.
		GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);

		// Esto multiplica la matriz de vista de modelo por la matriz de proyección y almacena el resultado en la matriz de MVP
		// (que ahora contiene modelo * vista * proyección).
		Matrix.multiplyMM(mTemporaryMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
		System.arraycopy(mTemporaryMatrix, 0, mMVPMatrix, 0, 16);

		// Pasar en la matriz combinada.
		GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

		// Pasar en posición de luz en el espacio ocular.
		GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);

		//Dibuja los triangulos correspondientes elegidos de la matriz en
		// donde estan definidos los vertices para formar la figura del cubo
		// pequeño que forma el dedo del brazo
		bindTexture(texturaPiedra,mCubeTextureCoordinates);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 108, 36);

    }

	private void dibujarOtroDedo() {
		// Pase en la información de posición
		mCubePositions.position(0);
		GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
				0, mCubePositions);

		GLES20.glEnableVertexAttribArray(mPositionHandle);

		// Pase de la información normal
		mCubeNormals.position(0);
		GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false,
				0, mCubeNormals);

		GLES20.glEnableVertexAttribArray(mNormalHandle);

		// Esto multiplica la matriz de vistas por la matriz del modelo y almacena el resultado en la matriz de MVP
		// (que actualmente contiene el modelo * vista).
		Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

		// Pasar en la matriz modelview.
		GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);

		// Esto multiplica la matriz de vista de modelo por la matriz de proyección y almacena el resultado en la matriz de MVP
		// (que ahora contiene modelo * vista * proyección).
		Matrix.multiplyMM(mTemporaryMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
		System.arraycopy(mTemporaryMatrix, 0, mMVPMatrix, 0, 16);

		// Pasar en la matriz combinada.
		GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

		// Pasar en posición de luz en el espacio ocular.
		GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);

		//Dibuja los triangulos correspondientes elegidos de la matriz en
		// donde estan definidos los vertices para formar la figura del cubo
		// pequeño que forma el dedo del brazo
		bindTexture(texturaPiedra,mCubeTextureCoordinates);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 144, 36);

	}

	private void bindTexture(int foto, FloatBuffer arreglo) {
		// Establezca la unidad de textura activa en la unidad de textura 0.
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

		// Enlazar la textura a esta unidad.
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, foto);

		// Indique al muestreador de textura uniforme que use esta textura en el sombreador enlazando a la unidad de textura 0.
		GLES20.glUniform1i(mTextureUniformHandle, 0);

		// Pasar en la información de coordenadas de la textura.
		//mCubeTextureCoordinatesForPlane.position(0);
		GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false,
				0, arreglo);

		GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
	}

	//Estos metodos se usan para agregar la funcionalidad de
	// PUSH y POP matriz que se utilizan en OPEN GL, funcionalidad con la que no
	// cuenta OPEN GL ES

    Stack<float[]> pila = new Stack<>();
    public void pushMatrix(float[] mod){
        float[] mod2 = new float[16];
        System.arraycopy(mod, 0, mod2, 0, 16);
        //mod2 = mod;
        pila.push(mod2);
    }

    public float[] popMatrix(){
        return pila.pop();
    }

}
