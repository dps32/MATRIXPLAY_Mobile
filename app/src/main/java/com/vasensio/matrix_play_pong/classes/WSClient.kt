package com.vasensio.matrix_play_pong.classes

import android.content.Intent
import android.util.Log
import com.vasensio.matrix_play_pong.Activities.CountdownActivity
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
    }

    var wsListener: WSListener? = null

    override fun onOpen(handshakedata: ServerHandshake?) {
        Log.d("WSConnection", "[*] Opened Connection!")

        // Enviar mensaje inicial si es necesario
        val msgObject = JSONObject()
        msgObject.put(KeyValues.K_TYPE.value, KeyValues.K_URL.value)
        send(msgObject.toString())
        Log.d("WSConnection", "[*] Message to server: $msgObject")
    }

    override fun onMessage(message: String?) {
        if (message != null) {
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
            val type = msgObj.optString(KeyValues.K_TYPE.value, "")

            when (type) {
                KeyValues.K_WELCOME.value -> {
                    val message = msgObj.optString(KeyValues.K_MESSAGE.value, "")
                    Log.d("WSConnection", "[*] Welcome message: $message")
                    wsListener?.onConnectionEstablished()
                    MainActivity.onConnectionEstablished()
                }

                KeyValues.K_URL.value -> {
                    val serverUrl = msgObj.optString(KeyValues.K_MESSAGE.value, "")
                    Log.d("WSConnection", "[*] Server URL: $serverUrl")
                }

                KeyValues.K_PLAYERS_READY.value -> {
                    val opponent = msgObj.optString("opponentName", "PLAYER2")
                    MainActivity.opponentName = opponent
                    Log.d("WSConnection", "[*] Opponent ready: $opponent")

                    // Abrir CountdownActivity solo si aún estamos en WaitActivity
                    MainActivity.currentActivityRef?.runOnUiThread {
                        val intent = Intent(MainActivity.currentActivityRef, CountdownActivity::class.java)
                        MainActivity.currentActivityRef?.startActivity(intent)
                    }

                    wsListener?.onTwoPlayersReady() // Opcional si quieres actualizar UI dinámica
                }


                KeyValues.K_COUNTDOWN.value -> {
                    val startNumber = msgObj.optInt("number", 3)
                    Log.d("WSConnection", "[*] Countdown start number: $startNumber")
                    wsListener?.onCountdownStart(startNumber)
                }

                else -> {
                    Log.d("WSConnection", "[*] Unknown message type: $type")
                }
            }

        } catch (e: Exception) {
            Log.e("WSConnection", "[*] Error parsing message: ${e.message}")
        }
    }
}
