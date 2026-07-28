package com.example.damaiassistant.data

import android.content.SharedPreferences
import com.example.damaiassistant.model.TaskConfig
import com.example.damaiassistant.model.TicketPreference
import com.example.damaiassistant.model.ViewerRef
import org.json.JSONArray
import org.json.JSONObject

interface PreferencesStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
}

class SharedPreferencesStore(
    private val preferences: SharedPreferences
) : PreferencesStore {
    override fun getString(key: String): String? = preferences.getString(key, null)

    override fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }
}

sealed class TaskRepositoryException(message: String) : IllegalStateException(message) {
    class UnknownVersion(val version: String) :
        TaskRepositoryException("Unknown task storage version: $version")

    class InvalidTask(message: String, cause: Throwable? = null) : TaskRepositoryException(message) {
        init {
            if (cause != null) initCause(cause)
        }
    }
}

class TaskRepository(
    private val store: PreferencesStore
) {
    fun save(config: TaskConfig) {
        val json = JSONObject()
            .put("eventName", config.eventName)
            .put("performanceName", config.performanceName)
            .put("releaseAtEpochMs", config.releaseAtEpochMs)
            .put("ticketCount", config.ticketCount)
            .put("enabled", config.enabled)
            .put("viewers", JSONArray().apply {
                config.viewers.forEach { put(JSONObject().put("displayName", it.displayName)) }
            })
            .put("ticketPreferences", JSONArray().apply {
                config.ticketPreferences.forEach {
                    put(
                        JSONObject()
                            .put("name", it.name)
                            .put("priceCents", it.priceCents)
                            .put("priority", it.priority)
                    )
                }
            })

        store.putString(VERSION_KEY, CURRENT_VERSION.toString())
        store.putString(TASK_KEY, json.toString())
    }

    fun load(): TaskConfig? {
        val jsonText = store.getString(TASK_KEY) ?: return null
        val version = store.getString(VERSION_KEY) ?: ""
        if (version != CURRENT_VERSION.toString()) {
            throw TaskRepositoryException.UnknownVersion(version)
        }

        return try {
            val json = JSONObject(jsonText)
            TaskConfig(
                eventName = json.getString("eventName"),
                performanceName = json.getString("performanceName"),
                releaseAtEpochMs = json.getLong("releaseAtEpochMs"),
                ticketCount = json.getInt("ticketCount"),
                viewers = json.getJSONArray("viewers").toViewerRefs(),
                ticketPreferences = json.getJSONArray("ticketPreferences").toTicketPreferences(),
                enabled = json.getBoolean("enabled")
            )
        } catch (error: Exception) {
            if (error is TaskRepositoryException) throw error
            throw TaskRepositoryException.InvalidTask("Saved task cannot be read", error)
        }
    }

    fun delete() {
        store.remove(TASK_KEY)
        store.remove(VERSION_KEY)
    }

    private fun JSONArray.toViewerRefs(): List<ViewerRef> =
        (0 until length()).map { index ->
            ViewerRef(getJSONObject(index).getString("displayName"))
        }

    private fun JSONArray.toTicketPreferences(): List<TicketPreference> =
        (0 until length()).map { index ->
            getJSONObject(index).let {
                TicketPreference(
                    name = it.getString("name"),
                    priceCents = it.getInt("priceCents"),
                    priority = it.getInt("priority")
                )
            }
        }

    private companion object {
        const val CURRENT_VERSION = 1
        const val VERSION_KEY = "task_version"
        const val TASK_KEY = "task_json"
    }
}
