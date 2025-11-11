package com.vasensio.matrix_play_pong.Activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.TextView
import com.vasensio.matrix_play_pong.R
import com.vasensio.matrix_play_pong.classes.WSClient

class WaitActivity : AppCompatActivity() {

    private lateinit var textViewStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wait)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_wait)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        textViewStatus = findViewById(R.id.textView4)
        textViewStatus.text = "WAITING PLAYERS"

        // Registrar listener en WSClient
        MainActivity.wsClient.wsListener = object : WSClient.WSListener {
            override fun onConnectionEstablished() {
                Log.d("WaitActivity", "[*] Connection established")
            }

            override fun onTwoPlayersReady() {
                runOnUiThread {
                    Log.d("WaitActivity", "[*] Two players ready, moving to CountdownActivity")
                    val intent = Intent(this@WaitActivity, CountdownActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }

            override fun onCountdownStart(startNumber: Int) {
                // WaitActivity no hace nada con el countdown
            }
        }
    }
}
