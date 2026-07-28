package com.example.damaiassistant.accessibility

import com.example.damaiassistant.model.UiBounds
import com.example.damaiassistant.model.VisiblePage
import com.example.damaiassistant.model.VisiblePageKind
import com.example.damaiassistant.model.VisibleTicketTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DamaiTaskDraftParserTest {
    @Test
    fun extractsEventPerformanceReleaseTimeAndTicketPreferences() {
        val page = VisiblePage(
            kind = VisiblePageKind.Appointment,
            texts = listOf(
                "周杰伦上海演唱会",
                "2026-08-01 周六 19:30",
                "开售时间：2026-07-20 12:00",
                "预约购票"
            ),
            ticketTiers = listOf(
                VisibleTicketTier("内场", 128000, true, "inner", UiBounds(0, 0, 1, 1)),
                VisibleTicketTier("看台", 68000, true, "stand", UiBounds(0, 0, 1, 1))
            )
        )

        val draft = DamaiTaskDraftParser.parse(page)

        assertEquals("周杰伦上海演唱会", draft.eventName)
        assertEquals("2026-08-01 周六 19:30", draft.performanceName)
        assertEquals(1784520000000L, draft.releaseAtEpochMs)
        assertEquals(listOf("内场", "看台"), draft.ticketPreferences.map { it.name })
        assertEquals(listOf(128000, 68000), draft.ticketPreferences.map { it.priceCents })
    }

    @Test
    fun leavesUnavailableFieldsEmptyForManualTemplateEntry() {
        val draft = DamaiTaskDraftParser.parse(
            VisiblePage(
                kind = VisiblePageKind.Unknown,
                texts = listOf("预约购票", "立即购买")
            )
        )

        assertNull(draft.eventName)
        assertNull(draft.performanceName)
        assertNull(draft.releaseAtEpochMs)
        assertEquals(emptyList<Any>(), draft.ticketPreferences)
    }
}
