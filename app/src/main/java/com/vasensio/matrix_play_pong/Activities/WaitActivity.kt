package com.vasensio.matrix_play_pong.Activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private val connectionCheckHandler = Handler(Looper.getMainLooper())
    private var connectionCheckRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_wait)

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_wait)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }

            // Inicializar el TextView (usar el existente textView4)
            textViewStatus = findViewById(R.id.textView4)
            textViewStatus.text = "CONNECTING..."

            // Actualizar referencia de actividad actual
            MainActivity.currentActivityRef = this

            // Registrar listener en WSClient
            setupWebSocketListener()

            // Iniciar verificación periódica de conexión
            startConnectionCheck()

        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }

    /**
     * Verificar periódicamente el estado de la conexión
     */
    private fun startConnectionCheck() {
        connectionCheckRunnable = object : Runnable {
            override fun run() {
                if (!hasNavigated && !MainActivity.isConnectedToServer()) {
                    Log.e("WaitActivity", "[*] Connection lost detected!")
                    handleConnectionLost()
                } else if (!hasNavigated) {
                    // Seguir verificando cada 2 segundos
                    connectionCheckHandler.postDelayed(this, 2000)
                }
            }
        }

        // Iniciar la primera verificación después de 2 segundos
        connectionCheckHandler.postDelayed(connectionCheckRunnable!!, 2000)
    }

    /**
     * Detener la verificación de conexión
     */
    private fun stopConnectionCheck() {
        connectionCheckRunnable?.let {
            connectionCheckHandler.removeCallbacks(it)
        }
    }

    /**
     * Manejar pérdida de conexión
     */
    private fun handleConnectionLost() {
        if (!hasNavigated) {
            hasNavigated = true
            Log.e("WaitActivity", "[*] Handling connection lost - returning to LoginActivity")

            runOnUiThread {
                textViewStatus.text = "CONNECTION LOST"

                // Desconectar el WebSocket
                MainActivity.disconnectWS()

                // Volver a LoginActivity con flag de connection_lost
                val intent = Intent(this@WaitActivity, LoginActivity::class.java)
                intent.putExtra("connection_lost", true)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    /**
     * Configurar el listener de WebSocket
     */
    private fun setupWebSocketListener() {
        Log.d("WaitActivity", "[*] Setting up WebSocket listener")

        MainActivity.wsClient.wsListener = object : WSClient.WSListener {
            override fun onConnectionEstablished() {
                Log.d("WaitActivity", "[*] Connection established")
                runOnUiThread {
                    textViewStatus.text = "CONNECTED - WAITING FOR PLAYERS..."
                }
            }

            override fun onTwoPlayersReady() {
                Log.d("WaitActivity", "[*] onTwoPlayersReady called! hasNavigated=$hasNavigated")

                // Solo navegamos una vez
                if (!hasNavigated) {
                    hasNavigated = true
                    stopConnectionCheck() // Detener verificación de conexión
                    Log.d("WaitActivity", "[*] Two players ready, moving to CountdownActivity")

                    runOnUiThread {
                        textViewStatus.text = "PLAYERS READY!"

                        // Pequeño delay para que el usuario vea el mensaje
                        textViewStatus.postDelayed({
                            navigateToCountdown()
                        }, 500)
                    }
                } else {
                    Log.w("WaitActivity", "[*] Already navigated, ignoring duplicate onTwoPlayersReady")
                }
            }

            override fun onCountdownStart(startNumber: Int) {
                // WaitActivity no maneja el countdown directamente
                // Esto será manejado por CountdownActivity
                Log.d("WaitActivity", "[*] Countdown signal received: $startNumber")

                // Si recibimos countdown y no hemos navegado, hacerlo ahora
                if (!hasNavigated) {
                    hasNavigated = true
                    stopConnectionCheck() // Detener verificación de conexión
                    Log.d("WaitActivity", "[*] Countdown received, navigating now")
                    runOnUiThread {
                        navigateToCountdown()
                    }
                } else {
                    Log.w("WaitActivity", "[*] Already navigated, ignoring countdown signal")
                }
            }

            override fun onGameStateUpdate(gameState: JSONObject) {
                // WaitActivity no maneja estados de juego
            }

            override fun onMessageReceived(message: String) {
                // Procesar mensajes adicionales si es necesario
                try {
                    val json = JSONObject(message)
                    val type = json.optString("type", "")

                    Log.d("WaitActivity", "[*] Message received - Type: $type")

                    when (type) {
                        "playerAssigned" -> {
                            val playerId = json.optInt("playerId", 0)
                            runOnUiThread {
                                textViewStatus.text = "YOU ARE PLAYER $playerId"
                            }
                        }

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

                            // Si es un error crítico, volver a login
                            if (errorMsg.contains("disconnect", ignoreCase = true) ||
                                errorMsg.contains("connection", ignoreCase = true)) {
                                handleConnectionLost()
                            }
                        }

                        "disconnect" -> {
                            Log.e("WaitActivity", "[*] Server sent disconnect message")
                            handleConnectionLost()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WaitActivity", "[*] Error parsing message: ${e.message}")
                }
            }
        }

        Log.d("WaitActivity", "[*] WebSocket listener configured successfully")

        // IMPORTANTE: Verificar si el countdown ya fue recibido ANTES de configurar el listener
        val savedNumber = MainActivity.wsClient.getSavedCountdownNumber()
        if (savedNumber != null && !hasNavigated) {
            Log.d("WaitActivity", "[*] Countdown was already received before listener setup! Navigating now...")
            hasNavigated = true
            stopConnectionCheck() // Detener verificación de conexión
            runOnUiThread {
                textViewStatus.text = "JOINING GAME..."
                navigateToCountdown()
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

        // Verificar conexión inmediatamente
        if (!MainActivity.isConnectedToServer()) {
            Log.w("WaitActivity", "[*] Not connected to server in onResume")
            handleConnectionLost()
        }

        Log.d("WaitActivity", "[*] WaitActivity resumed")
    }

    override fun onPause() {
        super.onPause()
        Log.d("WaitActivity", "[*] WaitActivity paused")
    }

    override fun onDestroy() {
        super.onDestroy()

        // Detener verificación de conexión
        stopConnectionCheck()

        // Limpiar listener si la actividad se destruye
        if (MainActivity.currentActivityRef == this) {
            MainActivity.currentActivityRef = null
        }

        Log.d("WaitActivity", "[*] WaitActivity destroyed")
    }
}