package com.example.damaiassistant.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import com.example.damaiassistant.domain.PageAction

class UiActionExecutor {
    fun execute(root: AccessibilityNodeInfo?, action: PageAction): Boolean {
        if (root == null) return false
        return when (action) {
            is PageAction.SelectTicketTier -> clickByKey(root, action.stableKey)
            is PageAction.SelectViewers -> action.stableKeys.all { clickByKey(root, it) }
            is PageAction.SelectQuantity -> false
        }
    }

    private fun clickByKey(root: AccessibilityNodeInfo, key: String): Boolean {
        val target = find(root, key) ?: return false
        return target.isEnabled && target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun find(node: AccessibilityNodeInfo, key: String): AccessibilityNodeInfo? {
        val contentDescription = node.contentDescription?.toString()
        val text = node.text?.toString()
        if (contentDescription == key || text == key) return node
        return (0 until node.childCount).asSequence()
            .mapNotNull { node.getChild(it) }
            .mapNotNull { child -> find(child, key) }
            .firstOrNull()
    }
}

