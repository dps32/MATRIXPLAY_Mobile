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
import org.json.JSONObject

class WaitActivity : AppCompatActivity() {

    private lateinit var textViewStatus: TextView
    private var hasNavigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
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
            setupWebSocketListener()

            // Opcional: Solicitar info del servidor
            try {
                if (MainActivity.isConnectedToServer()) {
                    MainActivity.wsClient.requestGroupName()
                }
            } catch (e: Exception) {
                Log.e("WaitActivity", "[*] Error requesting group name: ${e.message}")
            }

            Log.d("WaitActivity", "[*] WaitActivity created successfully")

        } catch (e: Exception) {
            Log.e("WaitActivity", "[*] Error creating WaitActivity: ${e.message}")
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
                Log.d("WaitActivity", "[*] Connection established")
                runOnUiThread {
                    textViewStatus.text = "CONNECTED - WAITING FOR PLAYERS..."
                }
            }

            override fun onTwoPlayersReady() {
                // Solo navegamos una vez
                if (!hasNavigated) {
                    hasNavigated = true
                    Log.d("WaitActivity", "[*] Two players ready, moving to CountdownActivity")

                    runOnUiThread {
                        textViewStatus.text = "PLAYERS READY!"

                        // Pequeño delay para que el usuario vea el mensaje
                        textViewStatus.postDelayed({
                            navigateToCountdown()
                        }, 500)
                    }
                }
            }

            override fun onCountdownStart(startNumber: Int) {
                // WaitActivity no maneja el countdown directamente
                // Esto será manejado por CountdownActivity
                Log.d("WaitActivity", "[*] Countdown signal received: $startNumber")

                // Si recibimos countdown y no hemos navegado, hacerlo ahora
                if (!hasNavigated) {
                    hasNavigated = true
                    runOnUiThread {
                        navigateToCountdown()
                    }
                }
            }

            override fun onMessageReceived(message: String) {
                // Procesar mensajes adicionales si es necesario
                try {
                    val json = JSONObject(message)
                    val type = json.optString("type", "")

                    Log.d("WaitActivity", "[*] Message received - Type: $type")

                    when (type) {
                        "player_joined" -> {
                            val playerName = json.optString("player_name", "Unknown")
                            runOnUiThread {
                                textViewStatus.text = "PLAYER JOINED: $playerName"
                            }
                        }

                        "player_count" -> {
                            val count = json.optInt("count", 0)
                            runOnUiThread {
                                textViewStatus.text = "PLAYERS: $count/2"
                            }
                        }

                        "error" -> {
                            val errorMsg = json.optString("message", "Unknown error")
                            Log.e("WaitActivity", "[*] Server error: $errorMsg")
                            runOnUiThread {
                                textViewStatus.text = "ERROR: $errorMsg"
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WaitActivity", "[*] Error parsing message: ${e.message}")
                }
            }
        }
    }

    /**
     * Navegar a CountdownActivity
     */
    private fun navigateToCountdown() {
        try {
            val intent = Intent(this@WaitActivity, CountdownActivity::class.java)
            startActivity(intent)
            finish()
            Log.d("WaitActivity", "[*] Navigating to CountdownActivity")
        } catch (e: Exception) {
            Log.e("WaitActivity", "[*] Error navigating to CountdownActivity: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        MainActivity.currentActivityRef = this

        // Verificar conexión
        if (!MainActivity.isConnectedToServer()) {
            Log.w("WaitActivity", "[*] Not connected to server in onResume")
            textViewStatus.text = "CONNECTION LOST"
        }

        Log.d("WaitActivity", "[*] WaitActivity resumed")
    }

    override fun onPause() {
        super.onPause()
        Log.d("WaitActivity", "[*] WaitActivity paused")
    }

    override fun onDestroy() {
        super.onDestroy()

        // Limpiar listener si la actividad se destruye
        if (MainActivity.currentActivityRef == this) {
            MainActivity.currentActivityRef = null
        }

        Log.d("WaitActivity", "[*] WaitActivity destroyed")
    }
}