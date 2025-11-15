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

            // Inicializar vistas
            initViews()

            // Configurar controles
            setupControls()

            // Iniciar el juego
            pongDisplay.startGame()

            Log.d("PlayActivity", "[*] PlayActivity created successfully")

            // Actualizar referencia de actividad actual
            MainActivity.currentActivityRef = this

            // Limpiar el listener de WebSocket para evitar conflictos
            MainActivity.wsClient.wsListener = object : WSClient.WSListener {
                override fun onConnectionEstablished() {
                    Log.d("PlayActivity", "[*] Connection confirmed in game")
                }

                override fun onTwoPlayersReady() {
                    // Ya estamos jugando, no hacer nada
                }

                override fun onCountdownStart(startNumber: Int) {
                    // Ya estamos jugando, no hacer nada
                }
            }

            Log.d("PlayActivity", "[*] Game started!")
            Log.d("PlayActivity", "[*] Player: ${MainActivity.playerName}")
            Log.d("PlayActivity", "[*] Opponent: ${MainActivity.opponentName}")

        } catch (e: Exception) {
            Log.e("PlayActivity", "[*] Error creating PlayActivity: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun initViews() {
        try {
            // Puntuaciones
            scorePlayer1 = findViewById(R.id.textView6)
            scorePlayer2 = findViewById(R.id.textView7)

            // Paletas (SeekBars como controles)
            paddleLeft = findViewById(R.id.seekBar)
            paddleRight = findViewById(R.id.seekBar2)

            // Vista personalizada del Pong
            pongDisplay = findViewById(R.id.pongDisplay)

            // Inicializar puntuaciones
            scorePlayer1.text = "0"
            scorePlayer2.text = "0"

            Log.d("PlayActivity", "[*] Views initialized successfully")
        } catch (e: Exception) {
            Log.e("PlayActivity", "[*] Error initializing views: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun setupControls() {
        try {
            // Configurar SeekBar izquierdo (Jugador 1)
            paddleLeft.max = 100
            paddleLeft.progress = 50 // Posición inicial en el centro

            paddleLeft.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    // Actualizar posición de la pala en la vista
                    pongDisplay.setLeftPaddlePosition(progress)

                    Log.d("PlayActivity", "[*] Left paddle position: $progress")

                    // TODO: Enviar posición al servidor vía WebSocket
                    // sendPaddlePosition("left", progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    Log.d("PlayActivity", "[*] User started moving left paddle")
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    Log.d("PlayActivity", "[*] User stopped moving left paddle")
                }
            })

            // Configurar SeekBar derecho (Jugador 2)
            paddleRight.max = 100
            paddleRight.progress = 50 // Posición inicial en el centro

            paddleRight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    // Actualizar posición de la pala en la vista
                    pongDisplay.setRightPaddlePosition(progress)

                    Log.d("PlayActivity", "[*] Right paddle position: $progress")

                    // TODO: Enviar posición al servidor vía WebSocket
                    // sendPaddlePosition("right", progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    Log.d("PlayActivity", "[*] User started moving right paddle")
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    Log.d("PlayActivity", "[*] User stopped moving right paddle")
                }
            })

            Log.d("PlayActivity", "[*] Controls configured successfully")
        } catch (e: Exception) {
            Log.e("PlayActivity", "[*] Error setting up controls: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Actualizar puntuación del jugador 1
     */
    private fun updateScore1(newScore: Int) {
        score1 = newScore
        runOnUiThread {
            scorePlayer1.text = score1.toString()
        }
        Log.d("PlayActivity", "[*] Score 1 updated: $score1")
    }

    /**
     * Actualizar puntuación del jugador 2
     */
    private fun updateScore2(newScore: Int) {
        score2 = newScore
        runOnUiThread {
            scorePlayer2.text = score2.toString()
        }
        Log.d("PlayActivity", "[*] Score 2 updated: $score2")
    }

    /**
     * Enviar posición de la paleta al servidor (para implementar más adelante)
     */
    private fun sendPaddlePosition(paddle: String, position: Int) {
        try {
            if (MainActivity.isConnectedToServer()) {
                val message = """
                    {
                        "type": "paddle_move",
                        "paddle": "$paddle",
                        "position": $position
                    }
                """.trimIndent()

                MainActivity.wsClient.send(message)
                Log.d("PlayActivity", "[*] Sent paddle position: $paddle = $position")
            }
        } catch (e: Exception) {
            Log.e("PlayActivity", "[*] Error sending paddle position: ${e.message}")
        }
    }

    /**
     * Actualizar posición de la paleta del oponente (cuando llegue del servidor)
     */
    private fun updateOpponentPaddle(position: Int) {
        runOnUiThread {
            paddleRight.progress = position
            pongDisplay.setRightPaddlePosition(position)
        }
    }

    override fun onResume() {
        super.onResume()
        MainActivity.currentActivityRef = this
        pongDisplay.startGame()
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
        Log.d("PlayActivity", "[*] PlayActivity destroyed")

        if (MainActivity.currentActivityRef == this) {
            MainActivity.currentActivityRef = null
        }
    }

    override fun onBackPressed() {
        // Opcional: confirmar antes de salir del juego
        Log.d("PlayActivity", "[*] Back pressed - exiting game")
        super.onBackPressed()

        // Desconectar del servidor si sales del juego
        MainActivity.disconnectWS()
        finish()
    }
}