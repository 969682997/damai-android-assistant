package com.example.damaiassistant.domain

import com.example.damaiassistant.model.TicketPreference
import com.example.damaiassistant.model.VisibleTicketTier

sealed interface TicketMatchResult {
    data class Selected(val tier: VisibleTicketTier) : TicketMatchResult
    data object NoAvailableTier : TicketMatchResult
    data object AmbiguousTier : TicketMatchResult
}

object TicketMatcher {
    fun select(
        preferences: List<TicketPreference>,
        visibleTiers: List<VisibleTicketTier>
    ): TicketMatchResult {
        preferences
            .withIndex()
            .sortedWith(compareBy<IndexedValue<TicketPreference>> { it.value.priority }.thenBy { it.index })
            .forEach { preference ->
                val matches = visibleTiers.filter { tier ->
                    tier.available &&
                        TextNormalizer.normalize(tier.name) == TextNormalizer.normalize(preference.value.name) &&
                        tier.priceCents == preference.value.priceCents
                }
                when {
                    matches.size > 1 -> return TicketMatchResult.AmbiguousTier
                    matches.size == 1 -> return TicketMatchResult.Selected(matches.single())
                }
            }

        return TicketMatchResult.NoAvailableTier
    }
}

object TextNormalizer {
    fun normalize(value: String): String = value.trim().replace(Regex("\\s+"), " ")
}

