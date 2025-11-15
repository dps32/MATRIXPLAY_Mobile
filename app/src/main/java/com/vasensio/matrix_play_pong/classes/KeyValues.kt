package com.vasensio.matrix_play_pong.classes

/**
 * Constantes para las claves JSON del protocolo WebSocket.
 * Sincronizadas con el servidor Java.
 */
enum class KeyValues(val value: String) {
    // Claves generales
    K_TYPE("type"),
    K_MESSAGE("message"),
    K_NUMBER("number"),
    K_COUNTDOWN("countdown"),

    // Tipos de mensaje Cliente -> Servidor
    K_URL("url"),
    K_GROUPNAME("groupname"),

    // Tipos de mensaje Servidor -> Cliente
    K_WELCOME("welcome"),

    // Claves adicionales para el cliente
    K_PLAYERS_READY("players_ready"),  // Uso interno del cliente
    K_OPPONENT_NAME("opponentName")
}