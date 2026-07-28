package com.example.damaiassistant.domain

import com.example.damaiassistant.model.TicketPreference
import com.example.damaiassistant.model.VisibleTicketTier
import org.junit.Assert.assertEquals
import org.junit.Test

class TicketMatcherTest {
    @Test
    fun selectsTheHighestPriorityAvailableTier() {
        val result = TicketMatcher.select(
            preferences = listOf(
                TicketPreference("内场", 128000, 0),
                TicketPreference("看台", 88000, 1)
            ),
            visibleTiers = listOf(
                VisibleTicketTier("看台", 88000, available = true),
                VisibleTicketTier("内场", 128000, available = true)
            )
        )

        assertEquals(TicketMatchResult.Selected(VisibleTicketTier("内场", 128000, true)), result)
    }

    @Test
    fun skipsSoldOutPrimaryAndSelectsTheFirstAvailableBackup() {
        val result = TicketMatcher.select(
            preferences = listOf(
                TicketPreference("内场", 128000, 0),
                TicketPreference("看台", 88000, 1)
            ),
            visibleTiers = listOf(
                VisibleTicketTier("内场", 128000, available = false),
                VisibleTicketTier("看台", 88000, available = true)
            )
        )

        assertEquals(TicketMatchResult.Selected(VisibleTicketTier("看台", 88000, true)), result)
    }

    @Test
    fun refusesWhenNoConfiguredTierIsAvailableOrCandidateIsAmbiguous() {
        assertEquals(
            TicketMatchResult.NoAvailableTier,
            TicketMatcher.select(
                preferences = listOf(TicketPreference("内场", 128000, 0)),
                visibleTiers = listOf(VisibleTicketTier("内场", 128000, available = false))
            )
        )

        assertEquals(
            TicketMatchResult.AmbiguousTier,
            TicketMatcher.select(
                preferences = listOf(TicketPreference("内场", 128000, 0)),
                visibleTiers = listOf(
                    VisibleTicketTier("内场", 128000, available = true),
                    VisibleTicketTier("内场", 128000, available = true)
                )
            )
        )
    }
}

