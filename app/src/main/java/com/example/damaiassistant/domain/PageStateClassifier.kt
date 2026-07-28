package com.example.damaiassistant.domain

import com.example.damaiassistant.model.VisiblePage
import com.example.damaiassistant.model.VisiblePageKind

enum class HandoffReason {
    HumanVerification,
    SmsVerification,
    Queue,
    LoginRequired,
    UnknownPage,
    TicketUnavailable,
    TicketAmbiguous,
    ViewerMissing,
    ViewerAmbiguous,
    ViewerCountMismatch
}

sealed interface PageClassification {
    data object AppointmentPage : PageClassification
    data object TicketPage : PageClassification
    data object ViewerPage : PageClassification
    data object PaymentPage : PageClassification
    data class HumanHandoff(val reason: HandoffReason) : PageClassification
    data object UnknownPage : PageClassification
}

object PageStateClassifier {
    fun classify(page: VisiblePage): PageClassification = when (page.kind) {
        VisiblePageKind.Payment -> PageClassification.PaymentPage
        VisiblePageKind.HumanVerification ->
            PageClassification.HumanHandoff(HandoffReason.HumanVerification)
        VisiblePageKind.SmsVerification ->
            PageClassification.HumanHandoff(HandoffReason.SmsVerification)
        VisiblePageKind.Queue -> PageClassification.HumanHandoff(HandoffReason.Queue)
        VisiblePageKind.LoginRequired -> PageClassification.HumanHandoff(HandoffReason.LoginRequired)
        VisiblePageKind.Appointment -> PageClassification.AppointmentPage
        VisiblePageKind.TicketSelection -> PageClassification.TicketPage
        VisiblePageKind.ViewerSelection -> PageClassification.ViewerPage
        VisiblePageKind.Unknown -> PageClassification.UnknownPage
    }
}

