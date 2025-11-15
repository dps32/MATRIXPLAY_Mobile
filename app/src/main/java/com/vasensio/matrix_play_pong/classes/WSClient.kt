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
    }

    var wsListener: WSListener? = null
    private var welcomeCount = 0
    private var savedCountdownNumber: Int? = null

    override fun onOpen(handshakedata: ServerHandshake?) {
        Log.d("WSConnection", "[*] Opened Connection!")

        // No enviamos nada inicialmente, esperamos el welcome del servidor
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

                    welcomeCount++

                    // La primera vez que recibimos welcome, notificamos la conexión
                    if (welcomeCount == 1) {
                        wsListener?.onConnectionEstablished()
                        MainActivity.onConnectionEstablished()
                    }

                    // Si recibimos 2 welcomes seguidos, significa que hay 2 jugadores
                    // (el servidor envía welcome a todos cuando alguien se conecta)
                    if (welcomeCount >= 2) {
                        Log.d("WSConnection", "[*] Two players detected via welcome messages")
                        // No hacemos nada aquí, esperamos el countdown
                    }
                }

                KeyValues.K_URL.value -> {
                    val serverUrl = msgObj.optString(KeyValues.K_MESSAGE.value, "")
                    Log.d("WSConnection", "[*] Server URL: $serverUrl")
                }

                KeyValues.K_GROUPNAME.value -> {
                    val groupName = msgObj.optString(KeyValues.K_MESSAGE.value, "")
                    Log.d("WSConnection", "[*] Group name: $groupName")
                }

                KeyValues.K_COUNTDOWN.value -> {
                    val startNumber = msgObj.optInt(KeyValues.K_NUMBER.value, 3)
                    Log.d("WSConnection", "[*] Countdown start number: $startNumber")

                    // Guardar el número para CountdownActivity
                    savedCountdownNumber = startNumber

                    // Cuando recibimos countdown, significa que hay 3 jugadores listos
                    wsListener?.onTwoPlayersReady()

                    // Iniciar el countdown
                    wsListener?.onCountdownStart(startNumber)
                }

                "error" -> {
                    val errorMsg = msgObj.optString(KeyValues.K_MESSAGE.value, "Unknown error")
                    Log.e("WSConnection", "[*] Server error: $errorMsg")
                }

                else -> {
                    Log.d("WSConnection", "[*] Unknown message type: $type")
                }
            }

        } catch (e: Exception) {
            Log.e("WSConnection", "[*] Error parsing message: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Obtener el número guardado del countdown
     */
    fun getSavedCountdownNumber(): Int? {
        return savedCountdownNumber
    }

    /**
     * Solicitar la URL del servidor
     */
    fun requestServerUrl() {
        try {
            val msgObject = JSONObject()
            msgObject.put(KeyValues.K_TYPE.value, KeyValues.K_URL.value)
            send(msgObject.toString())
            Log.d("WSConnection", "[*] Requested server URL")
        } catch (e: Exception) {
            Log.e("WSConnection", "[*] Error requesting server URL: ${e.message}")
        }
    }

    /**
     * Solicitar el nombre del grupo
     */
    fun requestGroupName() {
        try {
            val msgObject = JSONObject()
            msgObject.put(KeyValues.K_TYPE.value, KeyValues.K_GROUPNAME.value)
            send(msgObject.toString())
            Log.d("WSConnection", "[*] Requested group name")
        } catch (e: Exception) {
            Log.e("WSConnection", "[*] Error requesting group name: ${e.message}")
        }
    }
}