package com.example.damaiassistant.model

data class DamaiTaskDraft(
    val capturedAtEpochMs: Long,
    val eventName: String? = null,
    val performanceName: String? = null,
    val releaseAtEpochMs: Long? = null,
    val ticketPreferences: List<TicketPreference> = emptyList(),
    val viewers: List<ViewerRef> = emptyList()
)
