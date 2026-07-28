package com.example.damaiassistant.data

import com.example.damaiassistant.model.DamaiTaskDraft
import com.example.damaiassistant.model.TicketPreference
import com.example.damaiassistant.model.ViewerRef
import org.json.JSONArray
import org.json.JSONObject

class LatestDamaiPageStore(
    private val store: PreferencesStore
) {
    fun save(draft: DamaiTaskDraft) {
        val json = JSONObject()
            .put("capturedAtEpochMs", draft.capturedAtEpochMs)
            .putNullable("eventName", draft.eventName)
            .putNullable("performanceName", draft.performanceName)
            .putNullable("releaseAtEpochMs", draft.releaseAtEpochMs)
            .put("ticketPreferences", JSONArray().apply {
                draft.ticketPreferences.forEach {
                    put(
                        JSONObject()
                            .put("name", it.name)
                            .put("priceCents", it.priceCents)
                            .put("priority", it.priority)
                    )
                }
            })
            .put("viewers", JSONArray().apply {
                draft.viewers.forEach { put(JSONObject().put("displayName", it.displayName)) }
            })
        store.putString(KEY, json.toString())
    }

    fun load(): DamaiTaskDraft? {
        val jsonText = store.getString(KEY) ?: return null
        return try {
            val json = JSONObject(jsonText)
            DamaiTaskDraft(
                capturedAtEpochMs = json.getLong("capturedAtEpochMs"),
                eventName = json.optNullableString("eventName"),
                performanceName = json.optNullableString("performanceName"),
                releaseAtEpochMs = json.optNullableLong("releaseAtEpochMs"),
                ticketPreferences = json.getJSONArray("ticketPreferences").toTicketPreferences(),
                viewers = json.getJSONArray("viewers").toViewerRefs()
            )
        } catch (_: Exception) {
            null
        }
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

    private fun JSONArray.toViewerRefs(): List<ViewerRef> =
        (0 until length()).map { index -> ViewerRef(getJSONObject(index).getString("displayName")) }

    private fun JSONObject.putNullable(key: String, value: Any?): JSONObject =
        put(key, value ?: JSONObject.NULL)

    private fun JSONObject.optNullableString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)

    private fun JSONObject.optNullableLong(key: String): Long? =
        if (isNull(key)) null else optLong(key).takeIf { it > 0L }

    private companion object {
        const val KEY = "latest_damai_page_draft"
    }
}
