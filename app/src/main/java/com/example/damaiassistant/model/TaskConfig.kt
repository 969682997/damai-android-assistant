package com.example.damaiassistant.model

data class TaskConfig(
    val eventName: String,
    val performanceName: String,
    val releaseAtEpochMs: Long,
    val ticketCount: Int,
    val viewers: List<ViewerRef>,
    val ticketPreferences: List<TicketPreference>,
    val enabled: Boolean = false
)

data class ViewerRef(
    val displayName: String
)

data class TicketPreference(
    val name: String,
    val priceCents: Int,
    val priority: Int
)
