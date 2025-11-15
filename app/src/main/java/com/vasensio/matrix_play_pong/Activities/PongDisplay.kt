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
    private val ballRadius = 15f

    // Posiciones de las palas (0-100, donde 50 es el centro)
    private var leftPaddlePosition = 50f
    private var rightPaddlePosition = 50f

    // Posición de la pelota
    private var ballX = 0f
    private var ballY = 0f

    // Velocidad de la pelota
    private var ballVelocityX = 8f
    private var ballVelocityY = 5f

    // Estado del juego
    private var isGameRunning = false

    init {
        // Inicializar posición de la pelota en el centro
        ballX = width / 2f
        ballY = height / 2f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Actualizar posición inicial de la pelota cuando cambie el tamaño
        ballX = w / 2f
        ballY = h / 2f
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
            updateBallPosition()
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
     * Actualiza la posición de la pelota y detecta colisiones
     */
    private fun updateBallPosition() {
        // Mover la pelota
        ballX += ballVelocityX
        ballY += ballVelocityY

        // Colisión con bordes superior e inferior
        if (ballY - ballRadius <= 0 || ballY + ballRadius >= height) {
            ballVelocityY = -ballVelocityY
        }

        // Colisión con pala izquierda
        val leftPaddleY = calculatePaddleY(leftPaddlePosition)
        if (ballX - ballRadius <= 30f + paddleWidth &&
            ballY >= leftPaddleY - (paddleHeight / 2) &&
            ballY <= leftPaddleY + (paddleHeight / 2)) {
            ballVelocityX = Math.abs(ballVelocityX) // Rebote hacia la derecha
        }

        // Colisión con pala derecha
        val rightPaddleY = calculatePaddleY(rightPaddlePosition)
        if (ballX + ballRadius >= width - 30f - paddleWidth &&
            ballY >= rightPaddleY - (paddleHeight / 2) &&
            ballY <= rightPaddleY + (paddleHeight / 2)) {
            ballVelocityX = -Math.abs(ballVelocityX) // Rebote hacia la izquierda
        }

        // Si la pelota sale por los lados, reiniciar
        if (ballX < 0 || ballX > width) {
            resetBall()
        }
    }

    /**
     * Reinicia la pelota al centro
     */
    private fun resetBall() {
        ballX = width / 2f
        ballY = height / 2f
        ballVelocityX = if (ballVelocityX > 0) 8f else -8f
        ballVelocityY = 5f
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
     * Actualiza la posición de la pelota desde el servidor
     */
    fun updateBallPosition(x: Float, y: Float) {
        ballX = x
        ballY = y
        invalidate()
    }

    /**
     * Inicia el juego (animación de la pelota)
     */
    fun startGame() {
        isGameRunning = true
        resetBall()
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
        resetBall()
        invalidate()
    }
}