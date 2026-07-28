package com.example.damaiassistant.domain

import com.example.damaiassistant.model.TaskConfig
import com.example.damaiassistant.model.TicketPreference
import com.example.damaiassistant.model.UiNodeSnapshot
import com.example.damaiassistant.model.VisiblePage
import com.example.damaiassistant.model.VisiblePageKind
import com.example.damaiassistant.model.VisibleTicketTier
import com.example.damaiassistant.model.ViewerRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchaseStateMachineTest {
    @Test
    fun movesFromWaitingToTicketSelectionAndHandsOffForHumanChecks() {
        val machine = PurchaseStateMachine(task())

        assertEquals(PurchaseState.WaitingForRelease, machine.start(nowEpochMs = 1_000L).state)
        assertEquals(
            PurchaseState.WaitingForRelease,
            machine.onPage(VisiblePage(VisiblePageKind.Appointment, listOf("预约中"))).state
        )
        assertEquals(PurchaseState.Preparing, machine.onRelease().state)

        val ticketDecision = machine.onPage(
            VisiblePage(
                kind = VisiblePageKind.TicketSelection,
                texts = listOf("票档"),
                ticketTiers = listOf(VisibleTicketTier("内场", 128000, true, "tier-inner"))
            )
        )
        assertEquals(PurchaseState.SelectingQuantity, ticketDecision.state)
        assertEquals(PageAction.SelectTicketTier("tier-inner"), ticketDecision.action)

        val handoff = machine.onPage(VisiblePage(VisiblePageKind.HumanVerification, listOf("验证码")))
        assertEquals(PurchaseState.HumanHandoff, handoff.state)
        assertEquals(HandoffReason.HumanVerification, handoff.handoffReason)
        assertEquals(PurchaseState.Preparing, machine.resume().state)
    }

    @Test
    fun stopsAtPaymentAndNeverProducesActionsAfterStop() {
        val machine = PurchaseStateMachine(task())
        machine.start(nowEpochMs = 2_000L)

        val payment = machine.onPage(VisiblePage(VisiblePageKind.Payment, listOf("付款收银台")))
        assertEquals(PurchaseState.StoppedAtPayment, payment.state)
        assertNull(payment.action)

        val afterPayment = machine.onPage(
            VisiblePage(
                VisiblePageKind.TicketSelection,
                listOf("票档"),
                ticketTiers = listOf(VisibleTicketTier("内场", 128000, true, "tier-inner"))
            )
        )
        assertEquals(PurchaseState.StoppedAtPayment, afterPayment.state)
        assertNull(afterPayment.action)

        val stopped = machine.stop()
        assertTrue(stopped.state == PurchaseState.StoppedAtPayment)
    }

    private fun task() = TaskConfig(
        eventName = "目标演出",
        performanceName = "周六 19:30",
        releaseAtEpochMs = 2_000L,
        ticketCount = 1,
        viewers = listOf(ViewerRef("观演人 A")),
        ticketPreferences = listOf(TicketPreference("内场", 128000, 0))
    )
}

