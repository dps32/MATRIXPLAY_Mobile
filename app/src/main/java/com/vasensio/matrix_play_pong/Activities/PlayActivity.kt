package com.vasensio.matrix_play_pong.Activities

import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vasensio.matrix_play_pong.R
import com.vasensio.matrix_play_pong.classes.WSClient
import com.vasensio.matrix_play_pong.views.PongDisplay
import org.json.JSONObject

class PlayActivity : AppCompatActivity() {

    // Vistas del juego
    private lateinit var scorePlayer1: TextView
    private lateinit var scorePlayer2: TextView
    private lateinit var paddleLeft: SeekBar
    private lateinit var paddleRight: SeekBar
    private lateinit var pongDisplay: PongDisplay

    // Variables del juego
    private var score1 = 0
    private var score2 = 0

    // ID del jugador (0 = no asignado, 1 = jugador izquierda, 2 = jugador derecha)
    private var myPlayerId = 0

    // Flag para evitar configurar el listener múltiples veces
    private var isListenerConfigured = false

    // Variables para determinar qué jugador somos
    private var lastPaddle1Y: Float = 0.5f
    private var lastPaddle2Y: Float = 0.5f
    private var playerIdDetermined = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_play)

            // Configurar edge-to-edge si es necesario
            try {
                ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_play)) { v, insets ->
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                    insets
                }
            } catch (e: Exception) {
                Log.w("PlayActivity", "[*] Could not set window insets: ${e.message}")
            }

            // Usar el playerId de MainActivity si ya está asignado
            if (MainActivity.myPlayerId != 0) {
                myPlayerId = MainActivity.myPlayerId
                playerIdDetermined = true
                Log.d("PlayActivity", "[*] Using playerId from MainActivity: $myPlayerId")
            }

            // Inicializar vistas
            initViews()

            // Configurar controles
            setupControls()
            
            // Configurar controles del jugador según su ID
            if (myPlayerId != 0) {
                configurePlayerControls()
                Log.d("PlayActivity", "[*] Player controls configured for player $myPlayerId")
            }

            // Iniciar el juego
            pongDisplay.startGame()

            Log.d("PlayActivity", "[*] PlayActivity created successfully")

            // Actualizar referencia de actividad actual
            MainActivity.currentActivityRef = this

            // Configurar listener de WebSocket solo una vez
            if (!isListenerConfigured) {
                setupWebSocketListener()
                isListenerConfigured = true
            }

            Log.d("PlayActivity", "[*] Game started!")
            Log.d("PlayActivity", "[*] Player: ${MainActivity.playerName}")
            Log.d("PlayActivity", "[*] Opponent: ${MainActivity.opponentName}")
            Log.d("PlayActivity", "[*] My Player ID: $myPlayerId")

        } catch (e: Exception) {
            Log.e("PlayActivity", "[*] Error creating PlayActivity: ${e.message}")
            e.printStackTrace()
            finish()
        }
    }

    private fun initViews() {
        try {
            scorePlayer1 = findViewById(R.id.textView6)
            scorePlayer2 = findViewById(R.id.textView7)
            paddleLeft = findViewById(R.id.seekBar)
            paddleRight = findViewById(R.id.seekBar2)
            pongDisplay = findViewById(R.id.pongDisplay)

            scorePlayer1.text = "0"
            scorePlayer2.text = "0"

            Log.d("PlayActivity", "[*] Views initialized successfully")
        } catch (e: Exception) {
            Log.e("PlayActivity", "[*] Error initializing views: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    private fun setupControls() {
        try {
            // Configurar SeekBar izquierdo (Jugador 1)
            paddleLeft.max = 100
            paddleLeft.progress = 50

            paddleLeft.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    // Actualizar visualmente siempre
                    pongDisplay.setLeftPaddlePosition(progress)

                    // Solo enviar al servidor si:
                    // 1. El cambio fue hecho por el usuario (fromUser = true)
                    // 2. Este jugador controla la pala izquierda (myPlayerId == 1 o aún no se determinó)
                    if (fromUser && (myPlayerId == 1 || myPlayerId == 0)) {
                        val yPosition = progress / 100f
                        sendPaddlePosition(yPosition)
                        
                        // Si aún no determinamos el playerId, probablemente seamos jugador 1
                        if (myPlayerId == 0) {
                            setPlayerId(1)
                        }
                        Log.d("PlayActivity", "[*] P1 paddle moved by user: $progress (y=$yPosition)")
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            // Configurar SeekBar derecho (Jugador 2)
            paddleRight.max = 100
            paddleRight.progress = 50

            paddleRight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    // Actualizar visualmente siempre
                    pongDisplay.setRightPaddlePosition(progress)

                    // Solo enviar al servidor si:
                    // 1. El cambio fue hecho por el usuario (fromUser = true)
                    // 2. Este jugador controla la pala derecha (myPlayerId == 2 o aún no se determinó)
                    if (fromUser && (myPlayerId == 2 || myPlayerId == 0)) {
                        val yPosition = progress / 100f
                        sendPaddlePosition(yPosition)
                        
                        // Si aún no determinamos el playerId, probablemente seamos jugador 2
                        if (myPlayerId == 0) {
                            setPlayerId(2)
                        }
                        Log.d("PlayActivity", "[*] P2 paddle moved by user: $progress (y=$yPosition)")
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            Log.d("PlayActivity", "[*] Controls configured successfully")
        } catch (e: Exception) {
            Log.e("PlayActivity", "[*] Error setting up controls: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    private fun setupWebSocketListener() {
        try {
            MainActivity.wsClient.wsListener = object : WSClient.WSListener {
                override fun onConnectionEstablished() {
                }

                override fun onTwoPlayersReady() {
                }

                override fun onCountdownStart(startNumber: Int) {
                }

                override fun onGameStateUpdate(gameState: JSONObject) {
                    runOnUiThread {
                        handleGameState(gameState)
                    }
                }

                override fun onMessageReceived(message: String) {
                    // Procesar mensajes para determinar playerId si es necesario
                    try {
                        val json = JSONObject(message)
                        val type = json.optString("type", "")
                        if (type == "playerAssigned") {
                            val playerId = json.optInt("playerId", 0)
                            runOnUiThread {
                                setPlayerId(playerId)
                            }
                        }
                    } catch (e: Exception) {
                        // No es JSON o no es playerAssigned
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleGameState(gameState: JSONObject) {
        try {
            // Obtener posición de la pelota
            val ball = gameState.optJSONObject("ball")
            if (ball != null) {
                val ballX = (ball.optDouble("x", 0.5) * 100).toFloat()
                val ballY = (ball.optDouble("y", 0.5) * 100).toFloat()
                pongDisplay.setBallPosition(ballX, ballY)
            }

            // Obtener posición de la pala 1
            val paddle1 = gameState.optJSONObject("paddle1")
            if (paddle1 != null) {
                val paddle1Y = paddle1.optDouble("y", 0.5).toFloat()

                // Si aún no sabemos qué jugador somos, intentar determinar
                if (!playerIdDetermined) {
                    determinePlayerId(paddle1Y, lastPaddle1Y, 1)
                    lastPaddle1Y = paddle1Y
                }

                // Actualizar visualmente SIEMPRE (el servidor es la fuente de verdad)
                val progress = (paddle1Y * 100).toInt().coerceIn(0, 100)
                
                // Actualizar el SeekBar sin disparar el listener (usando post)
                if (paddleLeft.progress != progress) {
                    paddleLeft.progress = progress
                }
                
                // Actualizar el PongDisplay directamente
                pongDisplay.setLeftPaddlePosition(progress)
            }

            // Obtener posición de la pala 2
            val paddle2 = gameState.optJSONObject("paddle2")
            if (paddle2 != null) {
                val paddle2Y = paddle2.optDouble("y", 0.5).toFloat()

                // Si aún no sabemos qué jugador somos, intentar determinar
                if (!playerIdDetermined) {
                    determinePlayerId(paddle2Y, lastPaddle2Y, 2)
                    lastPaddle2Y = paddle2Y
                }

                // Actualizar visualmente SIEMPRE (el servidor es la fuente de verdad)
                val progress = (paddle2Y * 100).toInt().coerceIn(0, 100)
                
                // Actualizar el SeekBar sin disparar el listener (usando post)
                if (paddleRight.progress != progress) {
                    paddleRight.progress = progress
                }
                
                // Actualizar el PongDisplay directamente
                pongDisplay.setRightPaddlePosition(progress)
            }

            // Obtener puntuaciones
            val score = gameState.optJSONObject("score")
            if (score != null) {
                score1 = score.optInt("player1", 0)
                score2 = score.optInt("player2", 0)
                scorePlayer1.text = score1.toString()
                scorePlayer2.text = score2.toString()
            }

            // Estado del juego
            val running = gameState.optBoolean("running", true)
            if (!running) {
                pongDisplay.pauseGame()
            } else {
                pongDisplay.startGame()
            }

            Log.d("PlayActivity", "[*] Game state updated - Score: $score1-$score2")

        } catch (e: Exception) {
            Log.e("PlayActivity", "[*] Error handling game state: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Determina el playerId basándose en qué paleta se mueve cuando el usuario interactúa
     */
    private fun determinePlayerId(currentY: Float, lastY: Float, paddleId: Int) {
        // Si hay un cambio significativo en la posición y el usuario está interactuando,
        // probablemente esa sea nuestra paleta
        val threshold = 0.01f // Umbral para detectar movimiento

        if (Math.abs(currentY - lastY) > threshold) {
            // Hay movimiento en esta paleta, verificar si coincide con la interacción del usuario
            val leftProgress = paddleLeft.progress / 100f
            val rightProgress = paddleRight.progress / 100f

            // Comprobar qué seekbar está siendo movido por el usuario
            if (paddleId == 1 && Math.abs(currentY - leftProgress) < threshold) {
                setPlayerId(1)
                Log.d("PlayActivity", "[*] Auto-detected as Player 1 (left paddle)")
            } else if (paddleId == 2 && Math.abs(currentY - rightProgress) < threshold) {
                setPlayerId(2)
                Log.d("PlayActivity", "[*] Auto-detected as Player 2 (right paddle)")
            }
        }
    }

    /**
     * Establece el ID del jugador y configura los controles
     */
    private fun setPlayerId(playerId: Int) {
        if (playerId != myPlayerId && playerId in 1..2) {
            myPlayerId = playerId
            playerIdDetermined = true
            configurePlayerControls()

            Log.d("PlayActivity", "[*] Player ID set to: $playerId")

            // Mostrar en UI qué jugador somos
            val playerText = if (playerId == 1) "PLAYER 1" else "PLAYER 2"
            // Puedes mostrar esto en un TextView si quieres
        }
    }

    private fun configurePlayerControls() {
        when (myPlayerId) {
            1 -> {
                // Jugador 1: SOLO controla pala izquierda
                paddleLeft.isEnabled = true
                paddleRight.isEnabled = false
                paddleRight.alpha = 0.3f // Más transparente para indicar deshabilitado
                Log.d("PlayActivity", "[*] Player 1 - Left paddle ENABLED, right DISABLED")
            }
            2 -> {
                // Jugador 2: SOLO controla pala derecha
                paddleLeft.isEnabled = false
                paddleLeft.alpha = 0.3f // Más transparente para indicar deshabilitado
                paddleRight.isEnabled = true
                Log.d("PlayActivity", "[*] Player 2 - Right paddle ENABLED, left DISABLED")
            }
            else -> {
                // Espectador: ambas deshabilitadas
                paddleLeft.isEnabled = false
                paddleRight.isEnabled = false
                paddleLeft.alpha = 0.3f
                paddleRight.alpha = 0.3f
                Log.d("PlayActivity", "[*] Spectator mode - All paddles DISABLED")
            }
        }
    }

    private fun sendPaddlePosition(yPosition: Float) {
        try {
            if (MainActivity.isConnectedToServer()) {
                MainActivity.wsClient.sendPaddleMove(yPosition)
                Log.d("PlayActivity", "[*] Sent paddle position: y=$yPosition")
            } else {
                Log.w("PlayActivity", "[*] Cannot send paddle position - not connected")
            }
        } catch (e: Exception) {
            Log.e("PlayActivity", "[*] Error sending paddle position: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        MainActivity.currentActivityRef = this
        pongDisplay.startGame()

        if (!isListenerConfigured) {
            setupWebSocketListener()
            isListenerConfigured = true
        }

        Log.d("PlayActivity", "[*] PlayActivity resumed")
    }

    override fun onPause() {
        super.onPause()
        pongDisplay.pauseGame()
        Log.d("PlayActivity", "[*] PlayActivity paused")
    }

    override fun onDestroy() {
        super.onDestroy()
        pongDisplay.pauseGame()

        if (MainActivity.currentActivityRef == this) {
            MainActivity.currentActivityRef = null
        }

        isListenerConfigured = false
        Log.d("PlayActivity", "[*] PlayActivity destroyed")
    }
}