package com.vasensio.matrix_play_pong.Activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.vasensio.matrix_play_pong.R
import com.vasensio.matrix_play_pong.classes.WSClient
import org.json.JSONObject
import java.io.Serializable

class FinalResultActivity : AppCompatActivity() {

    private lateinit var resultText1: TextView
    private lateinit var resultText2: TextView
    private lateinit var imageResult: ImageView
    private lateinit var btnExit: Button
    private lateinit var btnPlayAgain: Button

    private var gameResult: GameResult = GameResult.DISCONNECTED
    private var finalScore1: Int = 0
    private var finalScore2: Int = 0

    enum class GameResult : Serializable {
        WINNER,
        LOSER,
        DISCONNECTED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_final_result)
            Log.d("FinalResultActivity", "[*] Layout inflated successfully")

            // Inicializar vistas con verificación de null
            if (!initializeViews()) {
                Log.e("FinalResultActivity", "[*] Failed to initialize views")
                finish()
                return
            }

            // Obtener datos del intent de forma segura
            gameResult = try {
                intent.getSerializableExtra("gameResult") as? GameResult ?: GameResult.DISCONNECTED
            } catch (e: Exception) {
                Log.e("FinalResultActivity", "[*] Error getting gameResult: ${e.message}")
                GameResult.DISCONNECTED
            }

            finalScore1 = intent.getIntExtra("score1", 0)
            finalScore2 = intent.getIntExtra("score2", 0)

            Log.d("FinalResultActivity", "[*] Game data - Result: $gameResult, Scores: $finalScore1-$finalScore2")

            // Actualizar referencia de actividad actual
            MainActivity.currentActivityRef = this

            // Registrar listener WS solo si está conectado
            if (MainActivity.isConnectedToServer()) {
                setupWebSocketListener()
            } else {
                Log.w("FinalResultActivity", "[*] Not connected to server, skipping listener setup")
            }

            // Mostrar resultado
            displayResult()

            // Configurar botones
            setupButtons()

            Log.d("FinalResultActivity", "[*] FinalResultActivity created successfully")

        } catch (e: Exception) {
            Log.e("FinalResultActivity", "[*] Critical error in onCreate: ${e.message}")
            Log.e("FinalResultActivity", "[*] Stack trace: ${e.stackTraceToString()}")
            e.printStackTrace()
            finish()
        }
    }

    private fun initializeViews(): Boolean {
        return try {
            resultText1 = findViewById(R.id.textResult1)
            resultText2 = findViewById(R.id.textResult2)
            imageResult = findViewById(R.id.imageResult)
            btnExit = findViewById(R.id.btnExit)
            btnPlayAgain = findViewById(R.id.btnPlayAgain)

            Log.d("FinalResultActivity", "[*] All views initialized successfully")
            true
        } catch (e: Exception) {
            Log.e("FinalResultActivity", "[*] Error initializing views: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private fun displayResult() {
        try {
            when (gameResult) {
                GameResult.WINNER -> {
                    resultText1.text = "WINNER"
                    resultText1.setTextColor(Color.parseColor("#00FF00"))
                    resultText1.setShadowLayer(20f, 0f, 0f, Color.parseColor("#00FF00"))

                    val myScore = if (MainActivity.myPlayerId == 1) finalScore1 else finalScore2
                    resultText2.text = "You won with $myScore points!"
                    resultText2.setTextColor(Color.WHITE)

                    try {
                        imageResult.setImageResource(R.drawable.winner)
                    } catch (e: Exception) {
                        Log.w("FinalResultActivity", "[*] winner drawable not found")
                        imageResult.setImageDrawable(null)
                    }
                }

                GameResult.LOSER -> {
                    resultText1.text = "LOSER"
                    resultText1.setTextColor(Color.parseColor("#FF0000"))
                    resultText1.setShadowLayer(20f, 0f, 0f, Color.parseColor("#FF0000"))

                    resultText2.text = "Next Time :("
                    resultText2.setTextColor(Color.WHITE)

                    try {
                        imageResult.setImageResource(R.drawable.loser)
                    } catch (e: Exception) {
                        Log.w("FinalResultActivity", "[*] loser drawable not found")
                        imageResult.setImageDrawable(null)
                    }
                }

                GameResult.DISCONNECTED -> {
                    resultText1.text = "DISCONNECTED"
                    resultText1.setTextColor(Color.parseColor("#FFA500"))
                    resultText1.setShadowLayer(20f, 0f, 0f, Color.parseColor("#FFA500"))

                    resultText2.text = "A player disconnected"
                    resultText2.setTextColor(Color.WHITE)

                    try {
                        imageResult.setImageResource(R.drawable.loser)
                    } catch (e: Exception) {
                        Log.w("FinalResultActivity", "[*] Image not found")
                        imageResult.setImageDrawable(null)
                    }
                }
            }
            Log.d("FinalResultActivity", "[*] Result displayed successfully")
        } catch (e: Exception) {
            Log.e("FinalResultActivity", "[*] Error displaying result: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun setupButtons() {
        try {
            btnExit.setOnClickListener {
                Log.d("FinalResultActivity", "[*] Exit button pressed")
                navigateToLogin()
            }

            btnPlayAgain.setOnClickListener {
                Log.d("FinalResultActivity", "[*] Play Again button pressed")
                playAgain()
            }
            Log.d("FinalResultActivity", "[*] Buttons configured successfully")
        } catch (e: Exception) {
            Log.e("FinalResultActivity", "[*] Error setting up buttons: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun setupWebSocketListener() {
        try {
            MainActivity.wsClient.wsListener = object : WSClient.WSListener {
                override fun onConnectionEstablished() {
                    Log.d("FinalResultActivity", "[*] WS Connection confirmed")
                    runOnUiThread {
                        resultText2.text = "Connected! Waiting for match..."
                    }
                }

                override fun onTwoPlayersReady() {
                    Log.d("FinalResultActivity", "[*] Players ready signal received")
                }

                override fun onCountdownStart(startNumber: Int) {
                    Log.d("FinalResultActivity", "[*] Countdown received, navigating to CountdownActivity")
                    runOnUiThread {
                        navigateToCountdown()
                    }
                }

                override fun onGameStateUpdate(gameState: JSONObject) {
                    // No necesitamos manejar game state aquí
                }

                override fun onGameEnd(winner: Int, score1: Int, score2: Int) {
                    // No deberíamos recibir esto aquí
                    Log.d("FinalResultActivity", "[*] Unexpected game end message")
                }

                override fun onPlayerDisconnected() {
                    // No deberíamos recibir esto aquí
                    Log.d("FinalResultActivity", "[*] Unexpected player disconnected")
                }

                override fun onMessageReceived(message: String) {
                    Log.d("FinalResultActivity", "[*] WS Message: $message")
                }
            }
            Log.d("FinalResultActivity", "[*] WebSocket listener configured")
        } catch (e: Exception) {
            Log.e("FinalResultActivity", "[*] Error setting up WS listener: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun navigateToLogin() {
        try {
            MainActivity.disconnectWS()
            MainActivity.playerName = ""
            MainActivity.opponentName = ""
            MainActivity.myPlayerId = 0

            val intent = Intent(this@FinalResultActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()

            Log.d("FinalResultActivity", "[*] Navigating to LoginActivity")
        } catch (e: Exception) {
            Log.e("FinalResultActivity", "[*] Error navigating to LoginActivity: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun playAgain() {
        try {
            if (MainActivity.playerName.isEmpty()) {
                Log.e("FinalResultActivity", "[*] No player name saved")
                resultText2.text = "Error: Please login again"
                return
            }

            btnPlayAgain.isEnabled = false
            btnPlayAgain.text = "Connecting..."
            resultText2.text = "Reconnecting..."

            if (MainActivity.isConnectedToServer()) {
                Log.d("FinalResultActivity", "[*] Already connected, navigating to WaitActivity")
                navigateToWait()
            } else {
                Log.d("FinalResultActivity", "[*] Not connected, reconnecting...")
                val savedUrl = getSavedServerUrl()
                if (savedUrl != null) {
                    val connected = MainActivity.connectWS(savedUrl)
                    if (connected) {
                        resultText2.text = "Connected! Waiting for match..."
                        resultText2.postDelayed({
                            navigateToWait()
                        }, 500)
                    } else {
                        resultText2.text = "Connection failed"
                        btnPlayAgain.isEnabled = true
                        btnPlayAgain.text = "PLAY AGAIN"
                    }
                } else {
                    Log.e("FinalResultActivity", "[*] No server URL saved")
                    resultText2.text = "Error: Please login again"
                    btnPlayAgain.isEnabled = true
                    btnPlayAgain.text = "PLAY AGAIN"
                }
            }
        } catch (e: Exception) {
            Log.e("FinalResultActivity", "[*] Error in playAgain: ${e.message}")
            e.printStackTrace()
            resultText2.text = "Error: ${e.message}"
            btnPlayAgain.isEnabled = true
            btnPlayAgain.text = "PLAY AGAIN"
        }
    }

    private fun getSavedServerUrl(): java.net.URI? {
        return try {
            val prefs = getSharedPreferences("PongGamePrefs", MODE_PRIVATE)
            val url = prefs.getString("server_url", "wss://matrixplay1.ieti.site:443")
            if (url != null) java.net.URI(url) else null
        } catch (e: Exception) {
            Log.e("FinalResultActivity", "[*] Error getting saved URL: ${e.message}")
            null
        }
    }

    private fun navigateToWait() {
        try {
            val intent = Intent(this@FinalResultActivity, WaitActivity::class.java)
            startActivity(intent)
            finish()
            Log.d("FinalResultActivity", "[*] Navigating to WaitActivity")
        } catch (e: Exception) {
            Log.e("FinalResultActivity", "[*] Error navigating to WaitActivity: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun navigateToCountdown() {
        try {
            val intent = Intent(this@FinalResultActivity, CountdownActivity::class.java)
            startActivity(intent)
            finish()
            Log.d("FinalResultActivity", "[*] Navigating to CountdownActivity")
        } catch (e: Exception) {
            Log.e("FinalResultActivity", "[*] Error navigating to CountdownActivity: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        MainActivity.currentActivityRef = this
        Log.d("FinalResultActivity", "[*] FinalResultActivity resumed")
    }

    override fun onDestroy() {
        super.onDestroy()

        if (MainActivity.currentActivityRef == this) {
            MainActivity.currentActivityRef = null
        }

        Log.d("FinalResultActivity", "[*] FinalResultActivity destroyed")
    }
}