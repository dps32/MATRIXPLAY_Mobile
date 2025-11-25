package com.vasensio.matrix_play_pong.Activities

import android.content.Intent
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

    private lateinit var scorePlayer1: TextView
    private lateinit var scorePlayer2: TextView
    private lateinit var paddleLeft: SeekBar
    private lateinit var paddleRight: SeekBar
    private lateinit var pongDisplay: PongDisplay

    private var score1 = 0
    private var score2 = 0
    private var myPlayerId = 0
    private var isListenerConfigured = false
    private var lastPaddle1Y: Float = 0.5f
    private var lastPaddle2Y: Float = 0.5f
    private var playerIdDetermined = false
    private var gameEnded = false // Flag para evitar múltiples navegaciones

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_play)

            try {
                ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_play)) { v, insets ->
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                    insets
                }
            } catch (e: Exception) {
                Log.w("PlayActivity", "[*] Could not set window insets: ${e.message}")
            }

            if (MainActivity.myPlayerId != 0) {
                myPlayerId = MainActivity.myPlayerId
                playerIdDetermined = true
                Log.d("PlayActivity", "[*] Using playerId from MainActivity: $myPlayerId")
            }

            initViews()
            setupControls()

            if (myPlayerId != 0) {
                configurePlayerControls()
                Log.d("PlayActivity", "[*] Player controls configured for player $myPlayerId")
            }

            pongDisplay.startGame()
            MainActivity.currentActivityRef = this

            if (!isListenerConfigured) {
                setupWebSocketListener()
                isListenerConfigured = true
            }

            Log.d("PlayActivity", "[*] Game started!")
            Log.d("PlayActivity", "[*] Player: ${MainActivity.playerName}, ID: $myPlayerId")
            Log.d("PlayActivity", "[*] Opponent: ${MainActivity.opponentName}")

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
            paddleLeft.max = 100
            paddleLeft.progress = 50

            paddleLeft.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    pongDisplay.setLeftPaddlePosition(progress)

                    if (fromUser && (myPlayerId == 1 || myPlayerId == 0)) {
                        val yPosition = progress / 100f
                        sendPaddlePosition(yPosition)

                        if (myPlayerId == 0) {
                            setPlayerId(1)
                        }
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            paddleRight.max = 100
            paddleRight.progress = 50

            paddleRight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    pongDisplay.setRightPaddlePosition(progress)

                    if (fromUser && (myPlayerId == 2 || myPlayerId == 0)) {
                        val yPosition = progress / 100f
                        sendPaddlePosition(yPosition)

                        if (myPlayerId == 0) {
                            setPlayerId(2)
                        }
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
                    Log.d("PlayActivity", "[*] Connection confirmed")
                }

                override fun onTwoPlayersReady() {
                    Log.d("PlayActivity", "[*] Two players ready")
                }

                override fun onCountdownStart(startNumber: Int) {
                    Log.d("PlayActivity", "[*] Countdown started (ignored in PlayActivity)")
                }

                override fun onGameStateUpdate(gameState: JSONObject) {
                    runOnUiThread {
                        handleGameState(gameState)
                    }
                }

                override fun onGameEnd(winner: Int, score1: Int, score2: Int) {
                    Log.d("PlayActivity", "[*] GAME END! Winner: Player $winner, Score: $score1-$score2")
                    runOnUiThread {
                        handleGameEnd(winner, score1, score2)
                    }
                }

                override fun onPlayerDisconnected() {
                    Log.d("PlayActivity", "[*] Player disconnected!")
                    runOnUiThread {
                        handlePlayerDisconnected()
                    }
                }

                override fun onMessageReceived(message: String) {
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
                        // No es JSON relevante
                    }
                }
            }
            Log.d("PlayActivity", "[*] WebSocket listener configured with game end detection")
        } catch (e: Exception) {
            Log.e("PlayActivity", "[*] Error setting up WS listener: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Maneja el fin del juego
     */
    private fun handleGameEnd(winner: Int, finalScore1: Int, finalScore2: Int) {
        if (gameEnded) {
            Log.d("PlayActivity", "[*] Game end already handled, ignoring")
            return
        }

        gameEnded = true
        pongDisplay.pauseGame()

        // Determinar si ganamos o perdimos
        val gameResult = when {
            myPlayerId == winner -> FinalResultActivity.GameResult.WINNER
            else -> FinalResultActivity.GameResult.LOSER
        }

        Log.d("PlayActivity", "[*] Navigating to FinalResultActivity - Result: $gameResult")

        try {
            val intent = Intent(this@PlayActivity, FinalResultActivity::class.java)
            intent.putExtra("gameResult", gameResult)
            intent.putExtra("score1", finalScore1)
            intent.putExtra("score2", finalScore2)
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e("PlayActivity", "[*] Error navigating to FinalResultActivity: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Maneja la desconexión de un jugador
     */
    private fun handlePlayerDisconnected() {
        if (gameEnded) {
            Log.d("PlayActivity", "[*] Game already ended, ignoring disconnection")
            return
        }

        gameEnded = true
        pongDisplay.pauseGame()

        Log.d("PlayActivity", "[*] Player disconnected, navigating to FinalResultActivity")

        try {
            val intent = Intent(this@PlayActivity, FinalResultActivity::class.java)
            intent.putExtra("gameResult", FinalResultActivity.GameResult.DISCONNECTED)
            intent.putExtra("score1", score1)
            intent.putExtra("score2", score2)
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e("PlayActivity", "[*] Error navigating to FinalResultActivity: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun handleGameState(gameState: JSONObject) {
        try {
            // Actualizar la pelota siempre (viene del servidor)
            val ball = gameState.optJSONObject("ball")
            if (ball != null) {
                val ballX = (ball.optDouble("x", 0.5) * 100).toFloat()
                val ballY = (ball.optDouble("y", 0.5) * 100).toFloat()
                pongDisplay.setBallPosition(ballX, ballY)
            }

            // Actualizar paddle 1 SOLO si NO soy el jugador 1 (solo actualizo la pala del oponente)
            val paddle1 = gameState.optJSONObject("paddle1")
            if (paddle1 != null) {
                val paddle1Y = paddle1.optDouble("y", 0.5).toFloat()

                if (!playerIdDetermined) {
                    determinePlayerId(paddle1Y, lastPaddle1Y, 1)
                    lastPaddle1Y = paddle1Y
                }

                // Solo actualizar si NO soy el jugador 1 (es decir, es la pala del oponente)
                if (myPlayerId != 1) {
                    val progress = (paddle1Y * 100).toInt().coerceIn(0, 100)

                    if (paddleLeft.progress != progress) {
                        paddleLeft.progress = progress
                    }

                    pongDisplay.setLeftPaddlePosition(progress)
                }
            }

            // Actualizar paddle 2 SOLO si NO soy el jugador 2 (solo actualizo la pala del oponente)
            val paddle2 = gameState.optJSONObject("paddle2")
            if (paddle2 != null) {
                val paddle2Y = paddle2.optDouble("y", 0.5).toFloat()

                if (!playerIdDetermined) {
                    determinePlayerId(paddle2Y, lastPaddle2Y, 2)
                    lastPaddle2Y = paddle2Y
                }

                // Solo actualizar si NO soy el jugador 2 (es decir, es la pala del oponente)
                if (myPlayerId != 2) {
                    val progress = (paddle2Y * 100).toInt().coerceIn(0, 100)

                    if (paddleRight.progress != progress) {
                        paddleRight.progress = progress
                    }

                    pongDisplay.setRightPaddlePosition(progress)
                }
            }

            // Actualizar puntuaciones (siempre)
            val score = gameState.optJSONObject("score")
            if (score != null) {
                score1 = score.optInt("player1", 0)
                score2 = score.optInt("player2", 0)
                scorePlayer1.text = score1.toString()
                scorePlayer2.text = score2.toString()
            }

            // Actualizar estado del juego (siempre)
            val running = gameState.optBoolean("running", true)
            if (!running) {
                pongDisplay.pauseGame()
            } else {
                pongDisplay.startGame()
            }

        } catch (e: Exception) {
            Log.e("PlayActivity", "[*] Error handling game state: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun determinePlayerId(currentY: Float, lastY: Float, paddleId: Int) {
        val threshold = 0.01f

        if (Math.abs(currentY - lastY) > threshold) {
            val leftProgress = paddleLeft.progress / 100f
            val rightProgress = paddleRight.progress / 100f

            if (paddleId == 1 && Math.abs(currentY - leftProgress) < threshold) {
                setPlayerId(1)
            } else if (paddleId == 2 && Math.abs(currentY - rightProgress) < threshold) {
                setPlayerId(2)
            }
        }
    }

    private fun setPlayerId(playerId: Int) {
        if (playerId != myPlayerId && playerId in 1..2) {
            myPlayerId = playerId
            playerIdDetermined = true
            configurePlayerControls()
            Log.d("PlayActivity", "[*] Player ID set to: $playerId")
        }
    }

    private fun configurePlayerControls() {
        when (myPlayerId) {
            1 -> {
                paddleLeft.isEnabled = true
                paddleRight.isEnabled = false
                paddleRight.alpha = 0.3f
                Log.d("PlayActivity", "[*] Player 1 controls configured")
            }
            2 -> {
                paddleLeft.isEnabled = false
                paddleLeft.alpha = 0.3f
                paddleRight.isEnabled = true
                Log.d("PlayActivity", "[*] Player 2 controls configured")
            }
            else -> {
                paddleLeft.isEnabled = false
                paddleRight.isEnabled = false
                paddleLeft.alpha = 0.3f
                paddleRight.alpha = 0.3f
                Log.d("PlayActivity", "[*] Spectator mode")
            }
        }
    }

    private fun sendPaddlePosition(yPosition: Float) {
        try {
            if (MainActivity.isConnectedToServer()) {
                MainActivity.wsClient.sendPaddleMove(yPosition)
            }
        } catch (e: Exception) {
            Log.e("PlayActivity", "[*] Error sending paddle position: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        MainActivity.currentActivityRef = this

        if (!gameEnded) {
            pongDisplay.startGame()
        }

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