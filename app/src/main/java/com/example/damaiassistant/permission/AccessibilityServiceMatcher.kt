package com.example.damaiassistant.permission

object AccessibilityServiceMatcher {
    fun isEnabled(
        enabledServices: String?,
        expectedPackageName: String,
        expectedClassName: String
    ): Boolean {
        if (enabledServices.isNullOrBlank()) return false

        return enabledServices.split(':').any { serializedComponent ->
            matches(serializedComponent, expectedPackageName, expectedClassName)
        }
    }

    private fun matches(
        serializedComponent: String,
        expectedPackageName: String,
        expectedClassName: String
    ): Boolean {
        val component = serializedComponent.trim()
            .removePrefix("{")
            .removeSuffix("}")
        val separatorIndex = component.indexOf('/')
        if (separatorIndex <= 0 || separatorIndex == component.lastIndex) return false

        val packageName = component.substring(0, separatorIndex).trim()
        val classNamePart = component.substring(separatorIndex + 1).trim()
        val className = when {
            classNamePart.startsWith('.') -> packageName + classNamePart
            classNamePart.startsWith("$packageName.", ignoreCase = true) -> classNamePart
            classNamePart.contains('.') -> "$packageName.$classNamePart"
            else -> "$packageName.$classNamePart"
        }

        return packageName.equals(expectedPackageName, ignoreCase = true) &&
            className.equals(expectedClassName, ignoreCase = true)
    }
}
