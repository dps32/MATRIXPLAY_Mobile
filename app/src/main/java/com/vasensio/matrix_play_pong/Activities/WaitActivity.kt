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
    private var hasNavigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wait)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_wait)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        textViewStatus = findViewById(R.id.textView4)
        textViewStatus.text = "WAITING FOR PLAYERS..."

        // Actualizar referencia de actividad actual
        MainActivity.currentActivityRef = this

        // Registrar listener en WSClient
        MainActivity.wsClient.wsListener = object : WSClient.WSListener {
            override fun onConnectionEstablished() {
                Log.d("WaitActivity", "[*] Connection established")
                runOnUiThread {
                    textViewStatus.text = "CONNECTED - WAITING FOR PLAYERS..."
                }
            }

            override fun onTwoPlayersReady() {
                // Solo navegamos una vez
                if (!hasNavigated) {
                    hasNavigated = true
                    runOnUiThread {
                        Log.d("WaitActivity", "[*] Players ready, moving to CountdownActivity")
                        textViewStatus.text = "PLAYERS READY!"

                        // Pequeño delay para que el usuario vea el mensaje
                        textViewStatus.postDelayed({
                            val intent = Intent(this@WaitActivity, CountdownActivity::class.java)
                            startActivity(intent)
                            finish()
                        }, 500)
                    }
                }
            }

            override fun onCountdownStart(startNumber: Int) {
                // WaitActivity no maneja el countdown directamente
                // Esto será manejado por CountdownActivity
                Log.d("WaitActivity", "[*] Countdown signal received: $startNumber")
            }
        }

        // Opcional: Solicitar info del servidor
        try {
            MainActivity.wsClient.requestGroupName()
        } catch (e: Exception) {
            Log.e("WaitActivity", "[*] Error requesting group name: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        MainActivity.currentActivityRef = this
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpiar listener si la actividad se destruye
        if (MainActivity.currentActivityRef == this) {
            MainActivity.currentActivityRef = null
        }
    }
}