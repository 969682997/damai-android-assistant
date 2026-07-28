package com.example.damaiassistant.domain

import com.example.damaiassistant.model.TaskConfig
import com.example.damaiassistant.model.TicketPreference
import com.example.damaiassistant.model.ViewerRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskValidatorTest {
    @Test
    fun acceptsTwoTicketsWithExactlyTwoViewersAndOneTier() {
        val errors = TaskValidator.validate(validTask(), nowEpochMs = 1_000L)

        assertTrue(errors.isEmpty())
    }

    @Test
    fun rejectsViewerCountThatDoesNotMatchTicketCount() {
        val task = validTask().copy(viewers = listOf(ViewerRef("观演人 A")))

        val errors = TaskValidator.validate(task, nowEpochMs = 1_000L)

        assertEquals(listOf(ValidationCode.VIEWER_COUNT_MISMATCH), errors.map { it.code })
    }

    @Test
    fun rejectsMissingRequiredFieldsAndPastReleaseTime() {
        val task = validTask().copy(
            eventName = " ",
            performanceName = "",
            releaseAtEpochMs = 999L,
            ticketCount = 0,
            ticketPreferences = emptyList()
        )

        val codes = TaskValidator.validate(task, nowEpochMs = 1_000L).map { it.code }.toSet()

        assertTrue(ValidationCode.EVENT_REQUIRED in codes)
        assertTrue(ValidationCode.PERFORMANCE_REQUIRED in codes)
        assertTrue(ValidationCode.RELEASE_IN_PAST in codes)
        assertTrue(ValidationCode.TICKET_COUNT_INVALID in codes)
        assertTrue(ValidationCode.TIER_REQUIRED in codes)
    }

    private fun validTask() = TaskConfig(
        eventName = "目标演出",
        performanceName = "周六 19:30",
        releaseAtEpochMs = 2_000L,
        ticketCount = 2,
        viewers = listOf(ViewerRef("观演人 A"), ViewerRef("观演人 B")),
        ticketPreferences = listOf(TicketPreference("看台 A 区", 58000, 0))
    )
}
