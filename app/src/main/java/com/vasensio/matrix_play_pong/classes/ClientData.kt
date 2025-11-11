package com.alejandrolopez.connecta4game.classes

import org.json.JSONObject


class ClientData {
    var name: String?
    var color: String?
    var mouseX: Int
    var mouseY: Int
    var row: Int
    var col: Int
    var isPlaying: Boolean

    constructor(name: String?, color: String?) {
        this.name = name
        this.color = color
        this.mouseX = -1
        this.mouseY = -1
        this.row = -1
        this.col = -1
        this.isPlaying = false
    }

    constructor(name: String?, color: String?, mouseX: Int, mouseY: Int, row: Int, col: Int) {
        this.name = name
        this.color = color
        this.mouseX = mouseX
        this.mouseY = mouseY
        this.row = row
        this.col = col
        this.isPlaying = false
    }

    fun SetColor(color: String?) {
        this.color = color
    }

    fun SetIsPlaying(playing: Boolean) {
        this.isPlaying = playing
    }

    override fun toString(): String {
        return this.toJSON().toString()
    }

    // Converteix l'objecte a JSON
    fun toJSON(): JSONObject {
        val obj = JSONObject()
        obj.put("name", name)
        obj.put("color", color)
        obj.put("mouseX", mouseX)
        obj.put("mouseY", mouseY)
        obj.put("row", row)
        obj.put("col", col)
        obj.put("isPlaying", isPlaying)
        return obj
    }

    companion object {
        // Crea un ClientData a partir de JSON
        fun fromJSON(obj: JSONObject): ClientData {
            val name = obj.optString("name", null)
            val color = obj.optString("color", null)

            val cd = ClientData(name, color)
            // cd.color = obj.optString("color", color);
            cd.mouseX = obj.optInt("mouseX", -1)
            cd.mouseY = obj.optInt("mouseY", -1)
            cd.row = obj.optInt("row", -1)
            cd.col = obj.optInt("col", -1)
            cd.isPlaying = obj.optBoolean("play", false)
            return cd
        }
    }
}