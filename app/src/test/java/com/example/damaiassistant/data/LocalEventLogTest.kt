package com.example.damaiassistant.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalEventLogTest {
    @Test
    fun recordsOnlyEventMetadataAndKeepsTheNewestEntries() {
        val store = MemoryPreferencesStore()
        val log = LocalEventLog(store, maxRecords = 2)

        log.append("PAGE_READ", "SelectingTicket", 1L)
        log.append("HANDOFF", "HumanHandoff", 2L)
        log.append("STOPPED", "StoppedAtPayment", 3L)

        assertEquals(
            listOf("HANDOFF", "STOPPED"),
            log.records().map { it.type }
        )
        assertEquals(listOf(2L, 3L), log.records().map { it.timestampEpochMs })
        assertEquals(null, store.values["task_json"])
    }
}

