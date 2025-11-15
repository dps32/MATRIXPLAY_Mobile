package com.vasensio.matrix_play_pong.Activities

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.vasensio.matrix_play_pong.R
import com.vasensio.matrix_play_pong.classes.WSClient

class CountdownActivity : AppCompatActivity() {

    private lateinit var counterText: TextView
    private lateinit var textPlayer1: TextView
    private lateinit var textPlayer2: TextView
    private var countdownStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        }

        // IMPORTANTE: Verificar si ya hay un número de countdown guardado
        val savedNumber = MainActivity.wsClient.getSavedCountdownNumber()
        if (savedNumber != null && !countdownStarted) {
            Log.d("CountdownActivity", "[*] Using saved countdown number: $savedNumber")
            countdownStarted = true
            startCountdown(savedNumber)
        }
    }

    override fun onResume() {
        super.onResume()
        MainActivity.currentActivityRef = this
    }

    private fun startCountdown(startNumber: Int) {
        counterText.text = startNumber.toString()

        // Countdown timer: cada segundo actualiza el texto
        object : CountDownTimer((startNumber * 1000L) + 500, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                if (secondsLeft > 0) {
                    counterText.text = secondsLeft.toString()
                    Log.d("CountdownActivity", "[*] Countdown: $secondsLeft")
                }
            }

            override fun onFinish() {
                counterText.text = "GO!"
                Log.d("CountdownActivity", "[*] Countdown finished - Starting game")

                // Esperar medio segundo antes de iniciar el juego
                counterText.postDelayed({
                    val intent = Intent(this@CountdownActivity, PlayActivity::class.java)
                    startActivity(intent)
                    finish()
                }, 500)
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (MainActivity.currentActivityRef == this) {
            MainActivity.currentActivityRef = null
        }
    }
}