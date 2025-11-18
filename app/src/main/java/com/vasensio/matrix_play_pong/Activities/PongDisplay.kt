package com.vasensio.matrix_play_pong.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
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

    // Paint para zona de gol (opcional)
    private val goalZonePaint = Paint().apply {
        color = Color.parseColor("#FF8000")
        alpha = 60
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Dimensiones de las palas
    private val paddleWidth = 20f
    private val paddleHeight = 150f
    private val paddleMargin = 15f

    // Dimensiones de la pelota
    private val ballRadius = 15f

    // Posiciones de las palas (0-100)
    private var leftPaddlePosition = 50f
    private var rightPaddlePosition = 50f

    // Posición de la pelota (porcentaje 0-100)
    private var ballXPercent = 50f
    private var ballYPercent = 50f

    // Estado del juego
    private var isGameRunning = false
    private var viewInitialized = false

    // Callback para cuando la vista está lista
    private var onViewReadyCallback: (() -> Unit)? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (w > 0 && h > 0 && !viewInitialized) {
            viewInitialized = true
            // Inicializar pelota en el centro
            ballXPercent = 50f
            ballYPercent = 50f

            onViewReadyCallback?.invoke()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Fondo negro
        canvas.drawColor(Color.BLACK)

        // Verificar que la vista tiene tamaño
        if (width == 0 || height == 0) {
            return
        }

        // --- BORDES ---
        canvas.drawRect(0f, 0f, 10f, height.toFloat(), goalZonePaint)
        canvas.drawRect(width - 10f, 0f, width.toFloat(), height.toFloat(), goalZonePaint)

        // Dibujar línea central punteada
        drawCenterLine(canvas)

        // Dibujar palas (left && right)
        drawLeftPaddle(canvas)
        drawRightPaddle(canvas)

        // Dibujar pelota
        drawBall(canvas)
    }

    /**
     * Dibuja la pelota
     */
    private fun drawBall(canvas: Canvas) {
        val ballX = calculatePixelX(ballXPercent)
        val ballY = calculatePixelY(ballYPercent)
        canvas.drawCircle(ballX, ballY, ballRadius, ballPaint)
    }

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
     * Convierte porcentaje X a píxeles
     */
    private fun calculatePixelX(percent: Float): Float {
        return (percent / 100f) * width
    }

    /**
     * Convierte porcentaje Y a píxeles
     */
    private fun calculatePixelY(percent: Float): Float {
        return (percent / 100f) * height
    }

    /**
     * Convierte la posición del SeekBar (0-100) a coordenadas Y
     */
    private fun calculatePaddleY(position: Float): Float {
        val minY = paddleHeight / 2 + paddleMargin
        val maxY = height - (paddleHeight / 2) - paddleMargin

        // Convertir 0–100 → posición dentro del canvas
        val normalizedY = (position / 100f) * height

        // Limitar rango
        return normalizedY.coerceIn(minY, maxY)
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
     * Actualiza la posición de la pelota (0-100 para ambos ejes)
     */
    fun setBallPosition(xPercent: Float, yPercent: Float) {
        ballXPercent = xPercent.coerceIn(0f, 100f)
        ballYPercent = yPercent.coerceIn(0f, 100f)
        invalidate()
    }

    /**
     * Inicia el juego
     */
    fun startGame() {
        isGameRunning = true
        Log.d("PongDisplay", "[*] Game started")
        invalidate()
    }

    /**
     * Pausa el juego
     */
    fun pauseGame() {
        isGameRunning = false
        Log.d("PongDisplay", "[*] Game paused")
    }

    /**
     * Reinicia el juego
     */
    fun resetGame() {
        leftPaddlePosition = 50f
        rightPaddlePosition = 50f
        ballXPercent = 50f
        ballYPercent = 50f
        Log.d("PongDisplay", "[*] Game reset")
        invalidate()
    }

    /**
     * Establece un callback para cuando la vista esté lista
     */
    fun setOnViewReadyCallback(callback: () -> Unit) {
        onViewReadyCallback = callback
        if (viewInitialized) {
            callback()
        }
    }
}