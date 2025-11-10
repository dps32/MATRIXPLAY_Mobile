package com.vasensio.matrix_play_pong.Activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.vasensio.matrix_play_pong.R

class LoginActivity : AppCompatActivity(){

    private lateinit var btnConnect : Button
    private lateinit var playerNameInput : TextInputEditText
    private lateinit var urlInput : TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_activity)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnConnect = findViewById<Button>(R.id.buttonConnectionConnect)
        playerNameInput = findViewById<TextInputEditText>(R.id.playerNameInputText)
        urlInput = findViewById<TextInputEditText>(R.id.urlInputText)

        btnConnect.setOnClickListener {
            connectToServer()
        }
    }
    private fun connectToServer() {
        //var name : String = playerNameInput.text.toString()
        //var url : String = urlInput.text.toString()

        val intent = Intent(this, WaitActivity::class.java)
        startActivity(intent)
        //finish()
    }
}