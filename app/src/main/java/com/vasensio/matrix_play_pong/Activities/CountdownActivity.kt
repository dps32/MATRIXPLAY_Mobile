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

        // Asignar nombres
        textPlayer1.text = MainActivity.playerName
        textPlayer2.text = MainActivity.opponentName

        // Registrar listener WS para recibir countdown
        MainActivity.wsClient.wsListener = object : WSClient.WSListener {
            override fun onConnectionEstablished() {}
            override fun onTwoPlayersReady() {}
            override fun onCountdownStart(startNumber: Int) {
                runOnUiThread {
                    startCountdown(startNumber)
                }
            }
        }
    }


    private fun startCountdown(startNumber: Int) {
        counterText.text = startNumber.toString()

        object : CountDownTimer((startNumber * 1000L) + 500, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                if (secondsLeft > 0) counterText.text = secondsLeft.toString()
            }

            override fun onFinish() {
                counterText.text = "GO!"
                counterText.postDelayed({
                    startActivity(Intent(this@CountdownActivity, PlayActivity::class.java))
                    finish()
                }, 500)
            }
        }.start()
    }

}
