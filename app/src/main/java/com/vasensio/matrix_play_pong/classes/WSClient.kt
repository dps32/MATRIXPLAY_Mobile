package com.vasensio.matrix_play_pong.classes

import android.util.Log
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
        fun onGameStateUpdate(gameState: JSONObject)
        fun onGameEnd(winner: Int, score1: Int, score2: Int) {} // Nuevo callback
        fun onPlayerDisconnected() {} // Nuevo callback
    }

    var wsListener: WSListener? = null
    private var welcomeCount = 0
    private var savedCountdownNumber: Int? = null
    private var hasReceivedCountdown = false

    override fun onOpen(handshakedata: ServerHandshake?) {
        Log.d("WSConnection", "[*] Opened Connection!")
        sendClientConfirmation()
    }

    override fun onMessage(message: String?) {
        if (message != null) {
            wsListener?.onMessageReceived(message)
            wsMessage(message)
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        Log.d("WSConnection", "[*] Closed Connection! Code: $code, Reason: $reason")

        // Notificar desconexión si es remota (servidor cerró conexión)
        if (remote) {
            wsListener?.onPlayerDisconnected()
        }
    }

    override fun onError(ex: Exception?) {
        Log.e("WSConnection", "[*] An error occurred: ${ex?.toString()}")
        wsListener?.onPlayerDisconnected()
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

                    if (welcomeCount == 1) {
                        wsListener?.onConnectionEstablished()
                        MainActivity.onConnectionEstablished()
                    }
                }

                "playerAssigned" -> {
                    val playerId = msgObj.optInt("playerId", 0)
                    MainActivity.myPlayerId = playerId
                    Log.d("WSConnection", "[*] Player ID assigned: $playerId")
                }

                "playerNames" -> {
                    val player1Name = msgObj.optString("player1", "Player 1")
                    val player2Name = msgObj.optString("player2", "Player 2")

                    Log.d("WSConnection", "[*] Players names - P1: $player1Name, P2: $player2Name")
                    Log.d("WSConnection", "[*] My player ID: ${MainActivity.myPlayerId}")

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
                    wsListener?.onGameStateUpdate(msgObj)

                    // Verificar si el juego ha terminado (alguien llegó a 10 puntos)
                    checkGameEnd(msgObj)
                }

                "countdown" -> {
                    val startNumber = msgObj.optInt("number", 3)
                    Log.d("WSConnection", "[*] Countdown start number: $startNumber")

                    savedCountdownNumber = startNumber
                    hasReceivedCountdown = true

                    Log.d("WSConnection", "[*] Countdown received - triggering twoPlayersReady and onCountdownStart")

                    if (wsListener != null) {
                        wsListener?.onTwoPlayersReady()
                        wsListener?.onCountdownStart(startNumber)
                    } else {
                        Log.w("WSConnection", "[*] WARNING: wsListener is NULL!")
                    }
                }

                "gameEnd" -> {
                    // Mensaje explícito del servidor de fin de juego
                    val winner = msgObj.optInt("winner", 0)
                    val score1 = msgObj.optInt("score1", 0)
                    val score2 = msgObj.optInt("score2", 0)

                    Log.d("WSConnection", "[*] Game ended - Winner: Player $winner, Score: $score1-$score2")
                    wsListener?.onGameEnd(winner, score1, score2)
                }

                "playerDisconnected" -> {
                    // Un jugador se desconectó
                    val disconnectedPlayer = msgObj.optInt("playerId", 0)
                    Log.d("WSConnection", "[*] Player $disconnectedPlayer disconnected")
                    wsListener?.onPlayerDisconnected()
                }
            }

        } catch (e: Exception) {
            Log.e("WSConnection", "[*] Error processing message: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Verificar si el juego ha terminado basándose en el estado del juego
     */
    private fun checkGameEnd(gameState: JSONObject) {
        try {
            val score = gameState.optJSONObject("score")
            if (score != null) {
                val score1 = score.optInt("player1", 0)
                val score2 = score.optInt("player2", 0)

                // Si alguien llegó a 10 puntos, el juego terminó
                if (score1 >= 10 || score2 >= 10) {
                    val winner = if (score1 >= 10) 1 else 2
                    Log.d("WSConnection", "[*] Game ended detected! Winner: Player $winner")
                    wsListener?.onGameEnd(winner, score1, score2)
                }
            }
        } catch (e: Exception) {
            Log.e("WSConnection", "[*] Error checking game end: ${e.message}")
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
            Log.d("WSConnection", "[*] Sent clientConfirmation with name: ${MainActivity.playerName}")
        } catch (e: Exception) {
            Log.e("WSConnection", "[*] Error sending clientConfirmation: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Enviar reconexión del cliente (para play again)
     */
    fun sendClientReconnection() {
        try {
            val msgObject = JSONObject()
            msgObject.put("type", "clientConfirmation")
            msgObject.put("name", MainActivity.playerName)
            send(msgObject.toString())
            Log.d("WSConnection", "[*] Sent reconnection clientConfirmation")
        } catch (e: Exception) {
            Log.e("WSConnection", "[*] Error sending reconnection: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Enviar movimiento de pala al servidor
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