package com.example.damaiassistant.domain

import com.example.damaiassistant.model.TaskConfig
import com.example.damaiassistant.model.VisiblePage

enum class PurchaseState {
    NotConfigured,
    WaitingForRelease,
    Preparing,
    SelectingTicket,
    SelectingQuantity,
    SelectingViewers,
    HumanHandoff,
    Submitting,
    StoppedAtPayment,
    StoppedByUser,
    Failed
}

sealed interface PageAction {
    data class SelectTicketTier(val stableKey: String) : PageAction
    data class SelectQuantity(val stableKey: String, val count: Int) : PageAction
    data class SelectViewers(val stableKeys: List<String>) : PageAction
    data class SubmitOrder(val stableKey: String) : PageAction
}

data class StateDecision(
    val state: PurchaseState,
    val action: PageAction? = null,
    val handoffReason: HandoffReason? = null
)

class PurchaseStateMachine(
    private val task: TaskConfig
) {
    var state: PurchaseState = PurchaseState.NotConfigured
        private set

    fun start(nowEpochMs: Long): StateDecision {
        state = when {
            task.releaseAtEpochMs <= 0L || task.ticketCount <= 0 -> PurchaseState.Failed
            nowEpochMs < task.releaseAtEpochMs -> PurchaseState.WaitingForRelease
            else -> PurchaseState.Preparing
        }
        return decision()
    }

    fun onRelease(): StateDecision {
        if (state == PurchaseState.WaitingForRelease) state = PurchaseState.Preparing
        return decision()
    }

    fun onPage(page: VisiblePage): StateDecision {
        if (state == PurchaseState.StoppedAtPayment || state == PurchaseState.StoppedByUser) {
            return decision()
        }
        if (state == PurchaseState.WaitingForRelease && page.kind != com.example.damaiassistant.model.VisiblePageKind.Appointment) {
            return decision()
        }

        return when (val classification = PageStateClassifier.classify(page)) {
            PageClassification.PaymentPage -> {
                state = PurchaseState.StoppedAtPayment
                decision()
            }
            is PageClassification.HumanHandoff -> {
                state = PurchaseState.HumanHandoff
                decision(handoffReason = classification.reason)
            }
            PageClassification.AppointmentPage -> {
                if (state == PurchaseState.NotConfigured) state = PurchaseState.WaitingForRelease
                decision()
            }
            PageClassification.TicketPage -> handleTicketPage(page)
            PageClassification.ViewerPage -> stateDecisionForViewerPage(page)
            PageClassification.UnknownPage -> {
                state = PurchaseState.HumanHandoff
                decision(handoffReason = HandoffReason.UnknownPage)
            }
        }
    }

    fun resume(): StateDecision {
        if (state == PurchaseState.HumanHandoff) state = PurchaseState.Preparing
        return decision()
    }

    fun stop(): StateDecision {
        if (state != PurchaseState.StoppedAtPayment) state = PurchaseState.StoppedByUser
        return decision()
    }

    private fun handleTicketPage(page: VisiblePage): StateDecision {
        if (state == PurchaseState.SelectingQuantity) {
            val key = page.quantityControlKey
            if (key == null) {
                state = PurchaseState.HumanHandoff
                return decision(handoffReason = HandoffReason.ActionUnavailable)
            }
            state = PurchaseState.SelectingViewers
            return decision(PageAction.SelectQuantity(key, task.ticketCount))
        }
        val result = TicketMatcher.select(task.ticketPreferences, page.ticketTiers)
        return when (result) {
            is TicketMatchResult.Selected -> {
                state = PurchaseState.SelectingQuantity
                decision(PageAction.SelectTicketTier(result.tier.stableKey ?: result.tier.name))
            }
            TicketMatchResult.NoAvailableTier -> {
                state = PurchaseState.HumanHandoff
                decision(handoffReason = HandoffReason.TicketUnavailable)
            }
            TicketMatchResult.AmbiguousTier -> {
                state = PurchaseState.HumanHandoff
                decision(handoffReason = HandoffReason.TicketAmbiguous)
            }
        }
    }

    private fun stateDecisionForViewerPage(page: VisiblePage): StateDecision {
        val result = ViewerSelector.select(
            ticketCount = task.ticketCount,
            selectedViewerNames = task.viewers.map { it.displayName },
            visibleViewers = page.viewers
        )
        if (state == PurchaseState.SelectingViewers &&
            result is ViewerSelectionResult.Selected &&
            result.viewers.all { it.selected } &&
            page.submitControlKey != null
        ) {
            state = PurchaseState.Submitting
            return decision(PageAction.SubmitOrder(page.submitControlKey))
        }
        return when (result) {
            is ViewerSelectionResult.Selected -> {
                state = PurchaseState.SelectingViewers
                decision(PageAction.SelectViewers(result.viewers.map { it.stableKey }))
            }
            ViewerSelectionResult.CountMismatch -> {
                state = PurchaseState.HumanHandoff
                decision(handoffReason = HandoffReason.ViewerCountMismatch)
            }
            is ViewerSelectionResult.MissingViewer -> {
                state = PurchaseState.HumanHandoff
                decision(handoffReason = HandoffReason.ViewerMissing)
            }
            is ViewerSelectionResult.AmbiguousViewer,
            is ViewerSelectionResult.DuplicateSelection -> {
                state = PurchaseState.HumanHandoff
                decision(handoffReason = HandoffReason.ViewerAmbiguous)
            }
        }
    }

    private fun decision(
        action: PageAction? = null,
        handoffReason: HandoffReason? = null
    ) = StateDecision(state, action, handoffReason)
}
