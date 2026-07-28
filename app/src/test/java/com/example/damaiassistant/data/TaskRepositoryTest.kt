package com.example.damaiassistant.data

import com.example.damaiassistant.model.TaskConfig
import com.example.damaiassistant.model.TicketPreference
import com.example.damaiassistant.model.ViewerRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskRepositoryTest {
    @Test
    fun savesAndLoadsTheSingleTaskWithoutSensitiveFields() {
        val store = MemoryPreferencesStore()
        val repository = TaskRepository(store)
        val task = TaskConfig(
            eventName = "目标演出",
            performanceName = "周六 19:30",
            releaseAtEpochMs = 2_000L,
            ticketCount = 2,
            viewers = listOf(ViewerRef("观演人 A"), ViewerRef("观演人 B")),
            ticketPreferences = listOf(
                TicketPreference("内场", 128000, 0),
                TicketPreference("看台", 88000, 1),
                TicketPreference("二层", 58000, 2)
            ),
            enabled = true
        )

        repository.save(task)

        assertEquals(task, repository.load())
        assertTrue(store.values.values.none { value ->
            value.contains("身份证") || value.contains("密码") || value.contains("验证码")
        })
    }

    @Test
    fun returnsNullWhenThereIsNoSavedTaskAndDeletesTheTask() {
        val repository = TaskRepository(MemoryPreferencesStore())

        assertNull(repository.load())

        repository.save(sampleTask())
        repository.delete()

        assertNull(repository.load())
    }

    @Test(expected = TaskRepositoryException.UnknownVersion::class)
    fun rejectsAnUnknownStorageVersion() {
        val store = MemoryPreferencesStore()
        store.putString("task_version", "999")
        store.putString("task_json", "{}")

        TaskRepository(store).load()
    }

    private fun sampleTask() = TaskConfig(
        eventName = "演出",
        performanceName = "场次",
        releaseAtEpochMs = 2_000L,
        ticketCount = 1,
        viewers = listOf(ViewerRef("观演人")),
        ticketPreferences = listOf(TicketPreference("票档", 10000, 0))
    )
}

