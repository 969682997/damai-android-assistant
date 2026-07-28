package com.example.damaiassistant.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.example.damaiassistant.model.UiBounds
import com.example.damaiassistant.model.UiNodeSnapshot

class NodeSnapshotter(
    private val maxNodes: Int = 2_000,
    private val maxDepth: Int = 40
) {
    fun snapshot(root: AccessibilityNodeInfo?): UiNodeSnapshot? {
        var copiedNodes = 0

        fun copy(node: AccessibilityNodeInfo?, depth: Int): UiNodeSnapshot? {
            if (node == null || depth > maxDepth || copiedNodes >= maxNodes) return null
            copiedNodes++
            return try {
                val rect = Rect()
                node.getBoundsInScreen(rect)
                val children = (0 until node.childCount).mapNotNull { index ->
                    copy(node.getChild(index), depth + 1)
                }
                UiNodeSnapshot(
                    text = node.text?.toString(),
                    contentDescription = node.contentDescription?.toString(),
                    className = node.className?.toString(),
                    clickable = node.isClickable,
                    enabled = node.isEnabled,
                    selected = node.isSelected,
                    checked = node.isChecked,
                    bounds = UiBounds(rect.left, rect.top, rect.right, rect.bottom),
                    children = children
                )
            } finally {
                node.recycle()
            }
        }

        return copy(root, 0)
    }
}

