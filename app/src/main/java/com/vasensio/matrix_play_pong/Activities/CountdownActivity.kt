// CountdownActivity.kt
package com.vasensio.matrix_play_pong.Activities

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.vasensio.matrix_play_pong.R
import com.vasensio.matrix_play_pong.classes.WSClient

class CountdownActivity : AppCompatActivity() {

    private lateinit var counterText: TextView
    private lateinit var textPlayer1: TextView
    private lateinit var textPlayer2: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_countdown)

        counterText = findViewById(R.id.counterText)
        textPlayer1 = findViewById(R.id.textPlayer1)
        textPlayer2 = findViewById(R.id.textPlayer2)

        textPlayer1.text = MainActivity.playerName
        textPlayer2.text = MainActivity.opponentName

        // Registrar listener WS para recibir el número inicial
        MainActivity.wsClient.wsListener = object : WSClient.WSListener {
            override fun onConnectionEstablished() {}
            override fun onTwoPlayersReady() {
                runOnUiThread {
                textPlayer2.text = MainActivity.opponentName
                }
            }
            override fun onCountdownStart(startNumber: Int) {
                runOnUiThread { // Asegura actualizar UI en hilo principal
                    startCountdown(startNumber)
                }
            }
        }
    }

    private fun startCountdown(number: Int) {
        val totalMillis = (number * 1000).toLong()
        val interval = 1000L

        object : CountDownTimer(totalMillis, interval) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt() + 1
                counterText.text = secondsLeft.toString()
            }

            override fun onFinish() {
                // Iniciar PlayActivity al finalizar el conteo
                val intent = Intent(this@CountdownActivity, PlayActivity::class.java)
                startActivity(intent)
                finish()
            }
        }.start()
    }
}
