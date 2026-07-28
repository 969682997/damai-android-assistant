package com.example.damaiassistant.domain

import com.example.damaiassistant.model.VisibleViewer
import org.junit.Assert.assertEquals
import org.junit.Test

class ViewerSelectorTest {
    @Test
    fun selectsExactlyTheConfiguredViewers() {
        val result = ViewerSelector.select(
            ticketCount = 2,
            selectedViewerNames = listOf("观演人 B", "观演人 A"),
            visibleViewers = listOf(
                VisibleViewer("观演人 A", "viewer-a"),
                VisibleViewer("观演人 B", "viewer-b"),
                VisibleViewer("观演人 C", "viewer-c")
            )
        )

        assertEquals(
            ViewerSelectionResult.Selected(
                listOf(
                    VisibleViewer("观演人 B", "viewer-b"),
                    VisibleViewer("观演人 A", "viewer-a")
                )
            ),
            result
        )
    }

    @Test
    fun refusesCountMismatchMissingViewerAndDuplicateName() {
        assertEquals(
            ViewerSelectionResult.CountMismatch,
            ViewerSelector.select(2, listOf("观演人 A"), listOf(VisibleViewer("观演人 A", "a")))
        )
        assertEquals(
            ViewerSelectionResult.MissingViewer("观演人 B"),
            ViewerSelector.select(
                2,
                listOf("观演人 A", "观演人 B"),
                listOf(VisibleViewer("观演人 A", "a"))
            )
        )
        assertEquals(
            ViewerSelectionResult.AmbiguousViewer("观演人 A"),
            ViewerSelector.select(
                1,
                listOf("观演人 A"),
                listOf(VisibleViewer("观演人 A", "a"), VisibleViewer("观演人 A", "a2"))
            )
        )
    }
}

