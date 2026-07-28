package com.example.damaiassistant.permission

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

data class DamaiPackageCandidate(
    val packageName: String,
    val label: String,
    val canLaunch: Boolean
)

object DamaiPackageMatcher {
    const val KNOWN_PACKAGE = "cn.damai"

    fun find(candidates: List<DamaiPackageCandidate>): String? {
        return candidates.firstOrNull {
            it.packageName.equals(KNOWN_PACKAGE, ignoreCase = true) && it.canLaunch
        }?.packageName ?: candidates.firstOrNull {
            it.canLaunch && it.label.contains("大麦")
        }?.packageName
    }
}

object DamaiPackageResolver {
    fun resolve(context: Context): String? {
        val packageManager = context.packageManager
        val candidates = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            .filterNot { it.packageName.equals(context.packageName, ignoreCase = true) }
            .map { it.toCandidate(packageManager) }
            .toList()
        return DamaiPackageMatcher.find(candidates)
    }

    fun launchIntent(context: Context): Intent? {
        val packageName = resolve(context) ?: return null
        return context.packageManager.getLaunchIntentForPackage(packageName)
    }

    fun isDamaiPackage(eventPackageName: String?, resolvedPackageName: String?): Boolean {
        val packageName = eventPackageName?.trim().orEmpty()
        if (packageName.isEmpty()) return false
        return packageName.equals(DamaiPackageMatcher.KNOWN_PACKAGE, ignoreCase = true) ||
            packageName.equals(resolvedPackageName, ignoreCase = true)
    }

    private fun ApplicationInfo.toCandidate(packageManager: PackageManager): DamaiPackageCandidate {
        return DamaiPackageCandidate(
            packageName = packageName,
            label = loadLabel(packageManager)?.toString().orEmpty(),
            canLaunch = packageManager.getLaunchIntentForPackage(packageName) != null
        )
    }
}
