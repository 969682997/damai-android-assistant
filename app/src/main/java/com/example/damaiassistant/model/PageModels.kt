package com.example.damaiassistant.model

data class UiBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

data class UiNodeSnapshot(
    val text: String? = null,
    val contentDescription: String? = null,
    val className: String? = null,
    val clickable: Boolean = false,
    val enabled: Boolean = true,
    val selected: Boolean = false,
    val checked: Boolean = false,
    val bounds: UiBounds? = null,
    val children: List<UiNodeSnapshot> = emptyList()
)

data class VisibleTicketTier(
    val name: String,
    val priceCents: Int,
    val available: Boolean,
    val stableKey: String? = null,
    val bounds: UiBounds? = null
)

data class VisibleViewer(
    val displayName: String,
    val stableKey: String,
    val selected: Boolean = false,
    val bounds: UiBounds? = null
)

enum class VisiblePageKind {
    Appointment,
    TicketSelection,
    ViewerSelection,
    HumanVerification,
    SmsVerification,
    Payment,
    Queue,
    LoginRequired,
    Unknown
}

data class VisiblePage(
    val kind: VisiblePageKind,
    val texts: List<String>,
    val ticketTiers: List<VisibleTicketTier> = emptyList(),
    val viewers: List<VisibleViewer> = emptyList(),
    val quantityControlKey: String? = null,
    val submitControlKey: String? = null
)
