package com.example.damaiassistant.data

import org.json.JSONArray
import org.json.JSONObject

data class EventLogRecord(
    val type: String,
    val state: String,
    val timestampEpochMs: Long
)

class LocalEventLog(
    private val store: PreferencesStore,
    private val maxRecords: Int = 100
) {
    fun append(type: String, state: String, timestampEpochMs: Long) {
        val records = readJson()
        records.put(
            JSONObject()
                .put("type", type)
                .put("state", state)
                .put("timestampEpochMs", timestampEpochMs)
        )

        while (records.length() > maxRecords) {
            records.remove(0)
        }
        store.putString(KEY, records.toString())
    }

    fun records(): List<EventLogRecord> = (0 until readJson().length()).map { index ->
        readJson().getJSONObject(index).let {
            EventLogRecord(
                type = it.getString("type"),
                state = it.getString("state"),
                timestampEpochMs = it.getLong("timestampEpochMs")
            )
        }
    }

    private fun readJson(): JSONArray =
        store.getString(KEY)?.let { JSONArray(it) } ?: JSONArray()

    private companion object {
        const val KEY = "event_log"
    }
}

