package com.example.damaiassistant.permission

import android.app.AlarmManager
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.damaiassistant.service.DamaiAccessibilityService

data class PermissionStatus(
    val accessibilityEnabled: Boolean,
    val overlayGranted: Boolean,
    val notificationsGranted: Boolean,
    val exactAlarmGranted: Boolean
) {
    val allGranted: Boolean
        get() = accessibilityEnabled && overlayGranted && notificationsGranted && exactAlarmGranted
}

object PermissionCoordinator {
    fun check(context: Context): PermissionStatus {
        val notificationsGranted = Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        val exactAlarmGranted = Build.VERSION.SDK_INT < 31 ||
            context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
        return PermissionStatus(
            accessibilityEnabled = isAccessibilityEnabled(context),
            overlayGranted = Settings.canDrawOverlays(context),
            notificationsGranted = notificationsGranted,
            exactAlarmGranted = exactAlarmGranted
        )
    }

    fun openAccessibilitySettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    fun openOverlaySettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
        )
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= 31) {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
            )
        }
    }

    private fun isAccessibilityEnabled(context: Context): Boolean {
        val expected = ComponentName(context, DamaiAccessibilityService::class.java).flattenToString()
        val expectedPackageName = expected.substringBefore('/')
        val expectedClassName = expected.substringAfter('/')
        val managerMatch = context.getSystemService(AccessibilityManager::class.java)
            ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            ?.any { serviceInfo ->
                AccessibilityServiceMatcher.isEnabled(
                    enabledServices = serviceInfo.id,
                    expectedPackageName = expectedPackageName,
                    expectedClassName = expectedClassName
                )
            } == true
        if (managerMatch) return true

        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return AccessibilityServiceMatcher.isEnabled(
            enabledServices = enabled,
            expectedPackageName = expectedPackageName,
            expectedClassName = expectedClassName
        )
    }
}
