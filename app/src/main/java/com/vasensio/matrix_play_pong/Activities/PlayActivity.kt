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

    // ID del jugador (1 o 2)
    private var myPlayerId = 0

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

            // Configurar listener de WebSocket
            setupWebSocketListener()

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
            paddleLeft.progress = 50

            paddleLeft.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    // Solo enviar si este jugador controla la pala izquierda
                    if (fromUser && myPlayerId == 1) {
                        pongDisplay.setLeftPaddlePosition(progress)
                        sendPaddlePosition(1, progress)
                        Log.d("PlayActivity", "[*] Left paddle position: $progress")
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
                    // Solo enviar si este jugador controla la pala derecha
                    if (fromUser && myPlayerId == 2) {
                        pongDisplay.setRightPaddlePosition(progress)
                        sendPaddlePosition(2, progress)
                        Log.d("PlayActivity", "[*] Right paddle position: $progress")
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            Log.d("PlayActivity", "[*] Controls configured successfully")
        } catch (e: Exception) {
            Log.e("PlayActivity", "[*] Error setting up controls: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Configurar el listener de WebSocket para recibir mensajes
     */
    private fun setupWebSocketListener() {
        MainActivity.wsClient.wsListener = object : WSClient.WSListener {
            override fun onConnectionEstablished() {
                Log.d("PlayActivity", "[*] Connection confirmed in game")
            }

            override fun onTwoPlayersReady() {
                // Ya estamos jugando
            }

            override fun onCountdownStart(startNumber: Int) {
                // Ya estamos jugando
            }

            override fun onMessageReceived(message: String) {
                runOnUiThread {
                    handleWebSocketMessage(message)
                }
            }
        }
    }

    /**
     * Manejar mensajes recibidos del servidor
     */
    private fun handleWebSocketMessage(message: String) {
        try {
            val json = JSONObject(message)
            val type = json.optString("type", "")

            when (type) {
                "player_assignment" -> {
                    // Asignación de jugador
                    myPlayerId = json.optInt("player_id", 0)
                    Log.d("PlayActivity", "[*] Assigned as Player $myPlayerId")

                    runOnUiThread {
                        configurePlayerControls()
                    }
                }

                "paddle_update" -> {
                    // Actualización de posición de pala
                    val playerId = json.optInt("player_id", -1)
                    val position = json.optInt("position", 50)

                    Log.d("PlayActivity", "[*] Received paddle update - Player: $playerId, Position: $position")

                    runOnUiThread {
                        updatePaddleFromServer(playerId, position)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PlayActivity", "[*] Error handling WebSocket message: ${e.message}")
        }
    }

    /**
     * Configurar controles según el jugador asignado
     */
    private fun configurePlayerControls() {
        when (myPlayerId) {
            1 -> {
                // Jugador 1: solo controla pala izquierda
                paddleLeft.isEnabled = true
                paddleRight.isEnabled = false
                paddleRight.alpha = 0.5f
                Log.d("PlayActivity", "[*] Player 1 - Left paddle enabled")
            }
            2 -> {
                // Jugador 2: solo controla pala derecha
                paddleLeft.isEnabled = false
                paddleLeft.alpha = 0.5f
                paddleRight.isEnabled = true
                Log.d("PlayActivity", "[*] Player 2 - Right paddle enabled")
            }
            else -> {
                // Espectador: ambas deshabilitadas
                paddleLeft.isEnabled = false
                paddleRight.isEnabled = false
                paddleLeft.alpha = 0.5f
                paddleRight.alpha = 0.5f
                Log.d("PlayActivity", "[*] Spectator mode - All paddles disabled")
            }
        }
    }

    /**
     * Actualizar posición de pala según mensaje del servidor
     */
    private fun updatePaddleFromServer(playerId: Int, position: Int) {
        when (playerId) {
            1 -> {
                // Actualizar pala izquierda
                paddleLeft.progress = position
                pongDisplay.setLeftPaddlePosition(position)
            }
            2 -> {
                // Actualizar pala derecha
                paddleRight.progress = position
                pongDisplay.setRightPaddlePosition(position)
            }
        }
    }

    /**
     * Enviar posición de la paleta al servidor
     */
    private fun sendPaddlePosition(playerId: Int, position: Int) {
        try {
            if (MainActivity.isConnectedToServer()) {
                val message = JSONObject().apply {
                    put("type", "paddle_move")
                    put("player_id", playerId)
                    put("position", position)
                }

                MainActivity.wsClient.send(message.toString())
                Log.d("PlayActivity", "[*] Sent paddle position: Player $playerId = $position")
            }
        } catch (e: Exception) {
            Log.e("PlayActivity", "[*] Error sending paddle position: ${e.message}")
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

    override fun onResume() {
        super.onResume()
        MainActivity.currentActivityRef = this
        pongDisplay.startGame()
        setupWebSocketListener()
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
}