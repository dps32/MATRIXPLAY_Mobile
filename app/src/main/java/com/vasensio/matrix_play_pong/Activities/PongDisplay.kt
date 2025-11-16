package com.vasensio.matrix_play_pong.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class PongDisplay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Paints para dibujar
    private val paddlePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val ballPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val centerLinePaint = Paint().apply {
        color = Color.GRAY
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    // Dimensiones de las palas
    private val paddleWidth = 20f
    private val paddleHeight = 150f

    // Dimensiones de la pelota
    private val ballRadius = 20f

    // Posiciones de las palas (0-100)
    private var leftPaddlePosition = 50f
    private var rightPaddlePosition = 50f

    // Posición de la pelota
    private var ballX = 0f
    private var ballY = 0f

    // Estado del juego
    private var isGameRunning = false

    init {
        // Inicializar posición de la pelota en el centro
        ballX = width / 2f
        ballY = height / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Fondo negro
        canvas.drawColor(Color.BLACK)

        // Dibujar línea central punteada
        drawCenterLine(canvas)

        // Dibujar pala izquierda
        drawLeftPaddle(canvas)

        // Dibujar pala derecha
        drawRightPaddle(canvas)

        // Dibujar pelota
        drawBall(canvas)

        // Si el juego está corriendo, actualizar posición de la pelota
        if (isGameRunning) {
            invalidate() // Redibujar continuamente
        }
    }

    /**
     * Dibuja la línea central punteada
     */
    private fun drawCenterLine(canvas: Canvas) {
        val centerX = width / 2f
        val dashHeight = 20f
        val dashGap = 20f
        var y = 0f

        while (y < height) {
            canvas.drawLine(centerX, y, centerX, y + dashHeight, centerLinePaint)
            y += dashHeight + dashGap
        }
    }

    /**
     * Dibuja la pala izquierda
     */
    private fun drawLeftPaddle(canvas: Canvas) {
        // Convertir posición de 0-100 a coordenadas Y
        val paddleY = calculatePaddleY(leftPaddlePosition)

        val left = 30f
        val top = paddleY - (paddleHeight / 2)
        val right = left + paddleWidth
        val bottom = paddleY + (paddleHeight / 2)

        canvas.drawRect(left, top, right, bottom, paddlePaint)
    }

    /**
     * Dibuja la pala derecha
     */
    private fun drawRightPaddle(canvas: Canvas) {
        // Convertir posición de 0-100 a coordenadas Y
        val paddleY = calculatePaddleY(rightPaddlePosition)

        val right = width - 30f
        val left = right - paddleWidth
        val top = paddleY - (paddleHeight / 2)
        val bottom = paddleY + (paddleHeight / 2)

        canvas.drawRect(left, top, right, bottom, paddlePaint)
    }

    /**
     * Dibuja la pelota
     */
    private fun drawBall(canvas: Canvas) {
        canvas.drawCircle(ballX, ballY, ballRadius, ballPaint)
    }

    /**
     * Convierte la posición del SeekBar (0-100) a coordenadas Y
     */
    private fun calculatePaddleY(position: Float): Float {
        // 0 = arriba, 100 = abajo
        // Invertir para que 0 sea arriba
        val normalizedPosition = 100f - position
        return (normalizedPosition / 100f) * height
    }

    /**
     * Actualiza la posición de la pala izquierda (0-100)
     */
    fun setLeftPaddlePosition(position: Int) {
        leftPaddlePosition = position.toFloat()
        invalidate()
    }

    /**
     * Actualiza la posición de la pala derecha (0-100)
     */
    fun setRightPaddlePosition(position: Int) {
        rightPaddlePosition = position.toFloat()
        invalidate()
    }

    /**
     * Inicia el juego (animación de la pelota)
     */
    fun startGame() {
        isGameRunning = true
        invalidate()
    }

    /**
     * Pausa el juego
     */
    fun pauseGame() {
        isGameRunning = false
    }

    /**
     * Reinicia el juego
     */
    fun resetGame() {
        leftPaddlePosition = 50f
        rightPaddlePosition = 50f
        invalidate()
    }
}