package com.vasensio.matrix_play_pong.Activities

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.vasensio.matrix_play_pong.classes.WSClient
import java.net.URI

class MainActivity : AppCompatActivity() {

    companion object {
        // Referencia al cliente WebSocket
        lateinit var wsClient: WSClient

        // Datos del jugador
        var playerName: String = ""

        // Nombre del oponente
        var opponentName: String = ""

        // Referencia a la actividad actual
        var currentActivityRef: Activity? = null

        // Estado de conexión
        var isConnected: Boolean = false

        /**
         * Conectar al servidor WebSocket usando URI completa
         * @param uri URI completa del servidor (ej: ws://10.0.2.2:3000)
         */
        fun connectWS(uri: URI): Boolean {
            return try {
                Log.d("WSConnection", "[*] Connecting to: $uri")

                wsClient = WSClient(uri)
                wsClient.connect()

                true
            } catch (e: Exception) {
                Log.e("WSConnection", "[*] Connection error: ${e.message}")
                e.printStackTrace()
                false
            }
        }

        /**
         * Conectar al servidor WebSocket (método alternativo con parámetros separados)
         * @param protocol Protocolo (ws o wss)
         * @param serverIP IP del servidor
         * @param port Puerto del servidor
         */
        fun connectWS(protocol: String, serverIP: String, port: String): Boolean {
            return try {
                val serverUri = URI("$protocol://$serverIP:$port")
                connectWS(serverUri)
            } catch (e: Exception) {
                Log.e("WSConnection", "[*] Connection error: ${e.message}")
                e.printStackTrace()
                false
            }
        }

        /**
         * Callback cuando se establece la conexión
         */
        fun onConnectionEstablished() {
            isConnected = true
            Log.d("WSConnection", "[*] Connection established successfully!")
        }

        /**
         * Desconectar del servidor
         */
        fun disconnectWS() {
            try {
                if (::wsClient.isInitialized) {
                    wsClient.close()
                    isConnected = false
                    Log.d("WSConnection", "[*] Disconnected from server")
                }
            } catch (e: Exception) {
                Log.e("WSConnection", "[*] Error disconnecting: ${e.message}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentActivityRef = this
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectWS()
    }
}