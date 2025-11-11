package com.vasensio.matrix_play_pong.Activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.vasensio.matrix_play_pong.R
import java.net.URI

class LoginActivity : AppCompatActivity() {

    private lateinit var btnConnect: Button
    private lateinit var btnAvatar: Button
    private lateinit var playerNameInput: TextInputEditText
    private lateinit var urlInput: TextInputEditText

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
        btnAvatar = findViewById(R.id.button2)
        playerNameInput = findViewById(R.id.playerNameInputText)
        urlInput = findViewById(R.id.urlInputText)

        // Configurar URL por defecto
        setDefaultConfiguration()

        // Configurar listeners
        btnConnect.setOnClickListener {
            connectToServer()
        }

        btnAvatar.setOnClickListener {
            // TODO: Implementar selección de avatar
            Toast.makeText(this, "Avatar selection coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setDefaultConfiguration() {
        // Configuración por defecto: servidor indicado por el usuario
        urlInput.setText("wss://matrixplay1.ieti.site:443")
    }

    private fun connectToServer() {
        val playerName = playerNameInput.text.toString().trim()
        val url = urlInput.text.toString().trim()

        // Validar nombre del jugador
        if (playerName.isEmpty()) {
            Toast.makeText(this, "Please enter a player name!", Toast.LENGTH_SHORT).show()
            return
        }

        // Validar URL
        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter a server URL!", Toast.LENGTH_SHORT).show()
            return
        }

        // Validar formato de URL
        if (!url.startsWith("ws://") && !url.startsWith("wss://")) {
            Toast.makeText(this, "URL must start with ws:// or wss://", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Validar que la URI es válida
            val uri = URI(url)

            // Guardar nombre del jugador
            MainActivity.playerName = playerName

            // Conectar al WebSocket usando la URI completa
            val connectionResult = MainActivity.connectWS(uri)

            if (connectionResult) {
                Toast.makeText(this, "Connecting to server...", Toast.LENGTH_SHORT).show()

                // Pasar a la siguiente actividad
                val intent = Intent(this, WaitActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Failed to connect to server", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Invalid URL format: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}