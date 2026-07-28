package com.example.damaiassistant.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.example.damaiassistant.accessibility.DamaiPageReader
import com.example.damaiassistant.accessibility.NodeSnapshotter
import com.example.damaiassistant.accessibility.UiActionExecutor
import com.example.damaiassistant.data.LocalEventLog
import com.example.damaiassistant.data.SharedPreferencesStore
import com.example.damaiassistant.data.TaskRepository
import com.example.damaiassistant.domain.PurchaseStateMachine
import com.example.damaiassistant.domain.PurchaseState
import com.example.damaiassistant.notify.HumanHandoffNotifier

class DamaiAccessibilityService : AccessibilityService() {
    private val snapshotter = NodeSnapshotter()
    private val actionExecutor = UiActionExecutor()
    private var stateMachine: PurchaseStateMachine? = null
    private lateinit var repository: TaskRepository
    private lateinit var eventLog: LocalEventLog
    private lateinit var notifier: HumanHandoffNotifier
    private var lastNoticeState: PurchaseState? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        val store = SharedPreferencesStore(getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE))
        repository = TaskRepository(store)
        eventLog = LocalEventLog(store)
        notifier = HumanHandoffNotifier(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName?.toString() != DAMAI_PACKAGE) return
        val task = repository.load()?.takeIf { it.enabled } ?: return
        val root = rootInActiveWindow ?: return
        val snapshot = snapshotter.snapshot(root) ?: return
        val page = DamaiPageReader.read(snapshot)
        val machine = stateMachine ?: PurchaseStateMachine(task).also {
            it.start(System.currentTimeMillis())
            stateMachine = it
        }
        if (machine.state == com.example.damaiassistant.domain.PurchaseState.WaitingForRelease &&
            System.currentTimeMillis() >= task.releaseAtEpochMs
        ) {
            machine.onRelease()
        }
        val decision = machine.onPage(page)
        eventLog.append("PAGE_READ", page.kind.name, System.currentTimeMillis())
        if (decision.action != null && decision.handoffReason == null) {
            actionExecutor.execute(rootInActiveWindow, decision.action)
        }
        if (decision.state.name.startsWith("Stopped") || decision.state.name == "HumanHandoff") {
            eventLog.append("STATE_CHANGE", decision.state.name, System.currentTimeMillis())
        }
        if (decision.state != lastNoticeState) {
            when (decision.state) {
                PurchaseState.HumanHandoff -> notifier.notifyHandoff()
                PurchaseState.StoppedAtPayment -> notifier.notifyPaymentStop()
                else -> Unit
            }
            lastNoticeState = decision.state
        }
    }

    override fun onInterrupt() = Unit

    companion object {
        const val DAMAI_PACKAGE = "cn.damai"
        private const val PREFERENCES_NAME = "damai_assistant"
    }
}
