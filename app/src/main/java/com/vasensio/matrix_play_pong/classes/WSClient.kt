package com.vasensio.matrix_play_pong.classes

import android.util.Log
import com.vasensio.matrix_play_pong.Activities.LoginActivity
import com.vasensio.matrix_play_pong.Activities.MainActivity
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.lang.Exception
import java.net.URI

class WSClient(serverUri: URI) : WebSocketClient(serverUri) {

    // Listener para comunicar eventos a las actividades
    interface WSListener {
        fun onConnectionEstablished()
        fun onTwoPlayersReady()
        fun onCountdownStart(startNumber: Int)
        fun onMessageReceived(message: String)
        fun onGameStateUpdate(gameState: JSONObject) // Nuevo callback para estado del juego
    }

    var wsListener: WSListener? = null
    private var welcomeCount = 0
    private var savedCountdownNumber: Int? = null
    private var hasReceivedCountdown = false // Flag para saber si ya llegó el countdown

    override fun onOpen(handshakedata: ServerHandshake?) {
        Log.d("WSConnection", "[*] Opened Connection!")

        // Enviar confirmación del cliente inmediatamente después de conectar
        sendClientConfirmation()
    }

    override fun onMessage(message: String?) {
        if (message != null) {
            // Notificar el mensaje completo al listener
            wsListener?.onMessageReceived(message)
            wsMessage(message)
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        Log.d("WSConnection", "[*] Closed Connection! Code: $code, Reason: $reason")
    }

    override fun onError(ex: Exception?) {
        Log.e("WSConnection", "[*] An error occurred: ${ex?.toString()}")
    }

    private fun wsMessage(response: String) {
        Log.d("WSConnection", "[*] Message received: $response")

        try {
            val msgObj = JSONObject(response)
            val type = msgObj.optString("type", "")

            Log.d("WSConnection", "[*] Processing message type: $type")

            when (type) {
                "welcome" -> {
                    val message = msgObj.optString("message", "")
                    Log.d("WSConnection", "[*] Welcome message: $message")

                    welcomeCount++

                    // La primera vez que recibimos welcome, notificamos la conexión
                    if (welcomeCount == 1) {
                        wsListener?.onConnectionEstablished()
                        MainActivity.onConnectionEstablished()
                    }
                }

                "playerAssigned" -> {
                    // El servidor asigna un playerId (1 o 2)
                    val playerId = msgObj.optInt("playerId", 0)
                    MainActivity.myPlayerId = playerId
                    Log.d("WSConnection", "[*] Player ID assigned: $playerId")
                }

                "playerNames" -> {
                    // El servidor envía los nombres de ambos jugadores
                    val player1Name = msgObj.optString("player1", "Player 1")
                    val player2Name = msgObj.optString("player2", "Player 2")
                    
                    Log.d("WSConnection", "[*] Players names - P1: $player1Name, P2: $player2Name")
                    Log.d("WSConnection", "[*] My player ID: ${MainActivity.myPlayerId}")
                    
                    // Asignar nombres según nuestro playerId
                    if (MainActivity.myPlayerId == 1) {
                        MainActivity.opponentName = player2Name
                        Log.d("WSConnection", "[*] I am Player 1, opponent is $player2Name")
                    } else if (MainActivity.myPlayerId == 2) {
                        MainActivity.opponentName = player1Name
                        Log.d("WSConnection", "[*] I am Player 2, opponent is $player1Name")
                    }
                }

                "gameState" -> {
                    Log.d("WSConnection", "[*] Game state update received")

                    // Notificar el estado completo del juego
                    wsListener?.onGameStateUpdate(msgObj)
                }

                "countdown" -> {
                    val startNumber = msgObj.optInt("number", 3)
                    Log.d("WSConnection", "[*] Countdown start number: $startNumber")

                    // Guardar el número para CountdownActivity
                    savedCountdownNumber = startNumber
                    hasReceivedCountdown = true

                    // Cuando recibimos countdown, significa que hay 2 jugadores listos
                    Log.d("WSConnection", "[*] Countdown received - triggering twoPlayersReady and onCountdownStart")
                    
                    if (wsListener != null) {
                        wsListener?.onTwoPlayersReady()
                        wsListener?.onCountdownStart(startNumber)
                    } else {
                        Log.w("WSConnection", "[*] WARNING: wsListener is NULL! Countdown will be processed when listener is set")
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("WSConnection", "[*] Error processing message: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Enviar confirmación del cliente al servidor
     */
    private fun sendClientConfirmation() {
        try {
            val msgObject = JSONObject()
            msgObject.put("type", "clientConfirmation")
            msgObject.put("name", MainActivity.playerName)
            send(msgObject.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Enviar movimiento de pala al servidor (nuevo formato)
     */
    fun sendPaddleMove(yPosition: Float) {
        try {
            val msgObject = JSONObject()
            msgObject.put("type", "paddleMove")
            msgObject.put("y", yPosition)
            send(msgObject.toString())
            Log.d("WSConnection paddle move", "[*] Sent paddle move: y=$yPosition")
        } catch (e: Exception) {
            Log.e("WSConnection paddle move", "[*] Error sending paddle move: ${e.message}")
        }
    }

    /**
     * Obtener el número guardado del countdown
     */
    fun getSavedCountdownNumber(): Int? {
        return savedCountdownNumber
    }
}