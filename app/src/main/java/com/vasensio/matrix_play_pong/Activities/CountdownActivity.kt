package com.vasensio.matrix_play_pong.Activities

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.vasensio.matrix_play_pong.R
import com.vasensio.matrix_play_pong.classes.WSClient
import org.json.JSONObject

class CountdownActivity : AppCompatActivity() {

    private lateinit var counterText: TextView
    private lateinit var textPlayer1: TextView
    private lateinit var textPlayer2: TextView
    private var countdownStarted = false
    private var countdownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_countdown)

            counterText = findViewById(R.id.counterText)
            textPlayer1 = findViewById(R.id.textPlayer1)
            textPlayer2 = findViewById(R.id.textPlayer2)

            // Asignar nombres
            textPlayer1.text = MainActivity.playerName
            textPlayer2.text = if (MainActivity.opponentName.isNotEmpty()) {
                MainActivity.opponentName
            } else {
                "PLAYER 2"
            }

            // Mostrar "READY" mientras esperamos el countdown
            counterText.text = "READY"

            // Actualizar referencia de actividad actual
            MainActivity.currentActivityRef = this

            // Registrar listener WS para recibir countdown
            setupWebSocketListener()

            // IMPORTANTE: Verificar si ya hay un número de countdown guardado
            val savedNumber = MainActivity.wsClient.getSavedCountdownNumber()
            if (savedNumber != null && !countdownStarted) {
                Log.d("CountdownActivity", "[*] Using saved countdown number: $savedNumber")
                countdownStarted = true
                startCountdown(savedNumber)
            }

            Log.d("CountdownActivity", "[*] CountdownActivity created successfully")

        } catch (e: Exception) {
            Log.e("CountdownActivity", "[*] Error creating CountdownActivity: ${e.message}")
            e.printStackTrace()
            finish()
        }
    }

    /**
     * Configurar el listener de WebSocket
     */
    private fun setupWebSocketListener() {
        MainActivity.wsClient.wsListener = object : WSClient.WSListener {
            override fun onConnectionEstablished() {
                Log.d("CountdownActivity", "[*] Connection confirmed")
            }

            override fun onTwoPlayersReady() {
                Log.d("CountdownActivity", "[*] Players ready signal received")
            }

            override fun onCountdownStart(startNumber: Int) {
                if (!countdownStarted) {
                    countdownStarted = true
                    runOnUiThread {
                        Log.d("CountdownActivity", "[*] Starting countdown from $startNumber")
                        startCountdown(startNumber)
                    }
                }
            }

            override fun onMessageReceived(message: String) {
                // Procesar mensajes adicionales si es necesario
                try {
                    val json = JSONObject(message)
                    val type = json.optString("type", "")

                    Log.d("CountdownActivity", "[*] Message received - Type: $type")

                    when (type) {
                        "player_info" -> {
                            val player1 = json.optString("player1", "")
                            val player2 = json.optString("player2", "")

                            if (player1.isNotEmpty()) {
                                MainActivity.playerName = player1
                            }
                            if (player2.isNotEmpty()) {
                                MainActivity.opponentName = player2
                            }

                            runOnUiThread {
                                textPlayer1.text = MainActivity.playerName
                                textPlayer2.text = MainActivity.opponentName
                            }
                        }

                        "error" -> {
                            val errorMsg = json.optString("message", "Unknown error")
                            Log.e("CountdownActivity", "[*] Server error: $errorMsg")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CountdownActivity", "[*] Error parsing message: ${e.message}")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        MainActivity.currentActivityRef = this
        Log.d("CountdownActivity", "[*] CountdownActivity resumed")
    }

    override fun onPause() {
        super.onPause()
        countdownTimer?.cancel()
        Log.d("CountdownActivity", "[*] CountdownActivity paused")
    }

    /**
     * Iniciar el countdown visual
     */
    private fun startCountdown(startNumber: Int) {
        counterText.text = startNumber.toString()

        // Countdown timer: cada segundo actualiza el texto
        countdownTimer = object : CountDownTimer((startNumber * 1000L) + 500, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                if (secondsLeft > 0) {
                    runOnUiThread {
                        counterText.text = secondsLeft.toString()
                    }
                    Log.d("CountdownActivity", "[*] Countdown: $secondsLeft")
                }
            }

            override fun onFinish() {
                runOnUiThread {
                    counterText.text = "GO!"
                    Log.d("CountdownActivity", "[*] Countdown finished - Starting game")

                    // Esperar medio segundo antes de iniciar el juego
                    counterText.postDelayed({
                        navigateToPlay()
                    }, 500)
                }
            }
        }

        countdownTimer?.start()
    }

    /**
     * Navegar a PlayActivity
     */
    private fun navigateToPlay() {
        try {
            val intent = Intent(this@CountdownActivity, PlayActivity::class.java)
            startActivity(intent)
            finish()
            Log.d("CountdownActivity", "[*] Navigating to PlayActivity")
        } catch (e: Exception) {
            Log.e("CountdownActivity", "[*] Error navigating to PlayActivity: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimer?.cancel()

        if (MainActivity.currentActivityRef == this) {
            MainActivity.currentActivityRef = null
        }

        Log.d("CountdownActivity", "[*] CountdownActivity destroyed")
    }
}