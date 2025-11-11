package com.server

import org.json.JSONObject

class GameObject(var id: String?) {
    override fun toString(): String {
        return this.toJSON().toString()
    }

    // Converteix l'objecte a JSON
    fun toJSON(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        return obj
    }

    companion object {
        // Crea un GameObjects a partir de JSON
        fun fromJSON(obj: JSONObject): GameObject {
            val go = GameObject(
                obj.optString("id", null)
            )
            return go
        }
    }
}