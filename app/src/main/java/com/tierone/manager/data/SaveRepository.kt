package com.tierone.manager.data

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.File

class SaveRepository(context: Context) {

    private val file = File(context.filesDir, "tierone_save.json")
    private val temp = File(context.filesDir, "tierone_save.tmp")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        allowStructuredMapKeys = true
    }

    fun load(): GameState? = try {
        if (!file.exists()) null
        else json.decodeFromString<GameState>(file.readText())
    } catch (t: Throwable) {
        null
    }

    fun save(state: GameState) {
        try {
            temp.writeText(json.encodeToString(GameState.serializer(), state))
            if (!temp.renameTo(file)) {
                file.writeText(temp.readText())
                temp.delete()
            }
        } catch (_: Throwable) {
            // Autosave must never crash the game.
        }
    }

    fun clear() {
        try {
            file.delete()
            temp.delete()
        } catch (_: Throwable) {
        }
    }
}
