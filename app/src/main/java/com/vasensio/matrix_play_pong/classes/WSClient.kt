package com.vasensio.matrix_play_pong.classes

import android.util.Log
import com.vasensio.matrix_play_pong.PongApplication
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.lang.Exception
import java.net.URI

class WSClient(serverUri: URI) : WebSocketClient(serverUri) {

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

                    // Notificar que la conexión fue exitosa
                    PongApplication.onConnectionEstablished()
                }

                KeyValues.K_URL.value -> {
                    val serverUrl = msgObj.optString(KeyValues.K_MESSAGE.value, "")
                    Log.d("WSConnection", "[*] Server URL: $serverUrl")
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