package com.example.damaiassistant.domain

import com.example.damaiassistant.model.VisibleViewer

sealed interface ViewerSelectionResult {
    data class Selected(val viewers: List<VisibleViewer>) : ViewerSelectionResult
    data object CountMismatch : ViewerSelectionResult
    data class MissingViewer(val name: String) : ViewerSelectionResult
    data class AmbiguousViewer(val name: String) : ViewerSelectionResult
    data class DuplicateSelection(val name: String) : ViewerSelectionResult
}

object ViewerSelector {
    fun select(
        ticketCount: Int,
        selectedViewerNames: List<String>,
        visibleViewers: List<VisibleViewer>
    ): ViewerSelectionResult {
        if (ticketCount <= 0 || selectedViewerNames.size != ticketCount) {
            return ViewerSelectionResult.CountMismatch
        }

        val normalizedNames = selectedViewerNames.map(TextNormalizer::normalize)
        val duplicate = normalizedNames.groupingBy { it }.eachCount().entries.firstOrNull { it.value > 1 }
        if (duplicate != null) {
            return ViewerSelectionResult.DuplicateSelection(duplicate.key)
        }

        val selected = selectedViewerNames.mapIndexed { index, requestedName ->
            val normalized = normalizedNames[index]
            val matches = visibleViewers.filter {
                TextNormalizer.normalize(it.displayName) == normalized
            }
            when {
                matches.isEmpty() -> return ViewerSelectionResult.MissingViewer(requestedName)
                matches.size > 1 -> return ViewerSelectionResult.AmbiguousViewer(requestedName)
                else -> matches.single()
            }
        }

        return ViewerSelectionResult.Selected(selected)
    }
}

