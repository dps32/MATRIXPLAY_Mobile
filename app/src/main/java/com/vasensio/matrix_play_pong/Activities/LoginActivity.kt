package com.vasensio.matrix_play_pong.Activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import com.vasensio.matrix_play_pong.R

import java.net.URI

class LoginActivity : AppCompatActivity() {

    private lateinit var btnConnect: Button
    private lateinit var imgAvatar: ImageView
    private lateinit var playerNameInput: TextInputEditText
    private lateinit var urlInput: TextInputEditText
    private val connectionTimeout = 5000L // 5 segundos timeout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_activity)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializar vistas
        btnConnect = findViewById(R.id.buttonConnectionConnect)
        imgAvatar = findViewById(R.id.imageView)
        playerNameInput = findViewById(R.id.playerNameInputText)
        urlInput = findViewById(R.id.urlInputText)

        // Cargar el GIF animado de gatos en el ImageView usando Glide
        Glide.with(this)
            .asGif()
            .load(R.drawable.img_cats)
            .into(imgAvatar)

        // Cargar configuración guardada o usar por defecto
        loadSavedConfiguration()

        // Verificar si venimos de un "Connection Lost"
        val connectionLost = intent.getBooleanExtra("connection_lost", false)
        if (connectionLost) {
            Toast.makeText(this, "Connection Lost", Toast.LENGTH_LONG).show()
        }

        // Configurar listeners
        btnConnect.setOnClickListener {
            connectToServer()
        }

        imgAvatar.setOnClickListener {
            Toast.makeText(this, "Avatar selection coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSavedConfiguration() {
        try {
            val prefs = getSharedPreferences("PongGamePrefs", MODE_PRIVATE)

            // Cargar URL guardada o usar por defecto
            val savedUrl = prefs.getString("server_url", "wss://matrixplay1.ieti.site:443")
            urlInput.setText(savedUrl)

            // NO cargar el nombre del jugador - siempre empieza vacío
            playerNameInput.setText("")

            Log.d("LoginActivity", "[*] Loaded saved configuration (name cleared)")
        } catch (e: Exception) {
            Log.e("LoginActivity", "[*] Error loading saved configuration: ${e.message}")
            // Usar configuración por defecto
            urlInput.setText("wss://matrixplay1.ieti.site:443")
            playerNameInput.setText("")
        }
    }

    private fun saveConfiguration(url: String) {
        try {
            val prefs = getSharedPreferences("PongGamePrefs", MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString("server_url", url)
            // NO guardar el nombre del jugador
            editor.remove("player_name")
            editor.apply()

            Log.d("LoginActivity", "[*] Configuration saved (without player name)")
        } catch (e: Exception) {
            Log.e("LoginActivity", "[*] Error saving configuration: ${e.message}")
        }
    }

    private fun connectToServer() {
        val playerName = playerNameInput.text.toString().trim()
        val url = urlInput.text.toString().trim()

        // Validar nombre del jugador
        if (playerName.isEmpty()) {
            Toast.makeText(this, "Please enter a player name!", Toast.LENGTH_SHORT).show()
            playerNameInput.requestFocus()
            return
        }

        // Validar longitud del nombre
        if (playerName.length < 2) {
            Toast.makeText(this, "Player name must be at least 2 characters", Toast.LENGTH_SHORT).show()
            playerNameInput.requestFocus()
            return
        }

        // Validar URL
        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter a server URL!", Toast.LENGTH_SHORT).show()
            urlInput.requestFocus()
            return
        }

        // Validar formato de URL
        if (!url.startsWith("ws://") && !url.startsWith("wss://")) {
            Toast.makeText(this, "URL must start with ws:// or wss://", Toast.LENGTH_SHORT).show()
            urlInput.requestFocus()
            return
        }

        // Deshabilitar botón para evitar múltiples conexiones
        btnConnect.isEnabled = false
        btnConnect.text = "Connecting..."

        try {
            // Validar que la URI es válida
            val uri = URI(url)
            Log.d("LoginActivity", "[*] Attempting connection to: $uri")

            // Guardar configuración (solo URL)
            saveConfiguration(url)

            // Guardar nombre del jugador
            MainActivity.playerName = playerName
            Log.d("LoginActivity", "[*] Player name set to: $playerName")

            // Conectar al WebSocket usando la URI completa
            val connectionResult = MainActivity.connectWS(uri)

            if (connectionResult) {
                Toast.makeText(this, "Connecting to server...", Toast.LENGTH_SHORT).show()
                Log.d("LoginActivity", "[*] Connection initiated successfully")

                // Crear un Handler para el timeout
                val timeoutHandler = Handler(Looper.getMainLooper())
                var hasNavigated = false

                // Timeout de conexión
                val timeoutRunnable = Runnable {
                    if (!hasNavigated) {
                        hasNavigated = true
                        Log.e("LoginActivity", "[*] Connection timeout - server not responding")

                        // Desconectar el WebSocket
                        MainActivity.disconnectWS()

                        runOnUiThread {
                            Toast.makeText(
                                this,
                                "Connection timeout. Please check the server URL and try again.",
                                Toast.LENGTH_LONG
                            ).show()
                            btnConnect.isEnabled = true
                            btnConnect.text = "Connect"
                        }
                    }
                }

                // Programar el timeout
                timeoutHandler.postDelayed(timeoutRunnable, connectionTimeout)

                // Verificar la conexión periódicamente
                val checkConnectionRunnable = object : Runnable {
                    var attempts = 0
                    val maxAttempts = 10 // Verificar 10 veces en 5 segundos (cada 500ms)

                    override fun run() {
                        if (hasNavigated) return

                        attempts++

                        if (MainActivity.isConnectedToServer()) {
                            hasNavigated = true
                            timeoutHandler.removeCallbacks(timeoutRunnable)

                            Log.d("LoginActivity", "[*] Connection confirmed, moving to WaitActivity")
                            runOnUiThread {
                                val intent = Intent(this@LoginActivity, WaitActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                        } else if (attempts < maxAttempts) {
                            // Seguir verificando
                            timeoutHandler.postDelayed(this, 500)
                        }
                        // Si llegamos a maxAttempts, el timeout manejará el error
                    }
                }

                // Iniciar verificación después de 500ms
                timeoutHandler.postDelayed(checkConnectionRunnable, 500)

            } else {
                Log.e("LoginActivity", "[*] Failed to initiate connection")
                Toast.makeText(this, "Failed to connect to server", Toast.LENGTH_LONG).show()
                btnConnect.isEnabled = true
                btnConnect.text = "Connect"
            }

        } catch (e: Exception) {
            Log.e("LoginActivity", "[*] Connection error: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Invalid URL format: ${e.message}", Toast.LENGTH_LONG).show()
            btnConnect.isEnabled = true
            btnConnect.text = "Connect"
        }
    }

    override fun onResume() {
        super.onResume()
        // Limpiar el nombre del jugador cada vez que se regresa a esta pantalla
        playerNameInput.setText("")
        btnConnect.isEnabled = true
        btnConnect.text = "Connect"
    }

    override fun onDestroy() {
        super.onDestroy()
        // No desconectar aquí, dejamos que MainActivity lo maneje
    }
}