package com.example.damaiassistant.data

import com.example.damaiassistant.model.DamaiTaskDraft
import com.example.damaiassistant.model.TicketPreference
import com.example.damaiassistant.model.ViewerRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LatestDamaiPageStoreTest {
    @Test
    fun savesAndLoadsLatestDraft() {
        val store = LatestDamaiPageStore(MemoryPreferencesStore())
        val draft = DamaiTaskDraft(
            capturedAtEpochMs = 1234L,
            eventName = "活动",
            performanceName = "场次",
            releaseAtEpochMs = 5678L,
            ticketPreferences = listOf(TicketPreference("内场", 128000, 0)),
            viewers = listOf(ViewerRef("张三"))
        )

        store.save(draft)

        val loaded = store.load()
        assertNotNull(loaded)
        assertEquals(draft, loaded)
    }
}
