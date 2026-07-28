package com.example.damaiassistant.permission

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityServiceMatcherTest {
    private val expectedPackage = "com.example.damaiassistant"
    private val expectedClass = "com.example.damaiassistant.service.DamaiAccessibilityService"

    @Test
    fun recognizesShortComponentName() {
        assertTrue(
            AccessibilityServiceMatcher.isEnabled(
                "  COM.EXAMPLE.DAMAIASSISTANT/.service.DamaiAccessibilityService ",
                expectedPackage,
                expectedClass
            )
        )
    }

    @Test
    fun recognizesPackageRelativeClassWithoutLeadingDot() {
        assertTrue(
            AccessibilityServiceMatcher.isEnabled(
                "com.example.damaiassistant/service.DamaiAccessibilityService",
                expectedPackage,
                expectedClass
            )
        )
    }

    @Test
    fun recognizesFullyQualifiedComponentName() {
        assertTrue(
            AccessibilityServiceMatcher.isEnabled(
                "other/service:com.example.damaiassistant/com.example.damaiassistant.service.DamaiAccessibilityService",
                expectedPackage,
                expectedClass
            )
        )
    }

    @Test
    fun recognizesResolvedServiceIdentity() {
        assertTrue(
            AccessibilityServiceMatcher.isExpectedService(
                servicePackageName = "COM.EXAMPLE.DAMAIASSISTANT",
                serviceClassName = "com.example.damaiassistant.service.DamaiAccessibilityService",
                expectedPackageName = expectedPackage,
                expectedClassName = expectedClass
            )
        )
    }

    @Test
    fun rejectsDifferentServiceFromSamePackage() {
        assertFalse(
            AccessibilityServiceMatcher.isEnabled(
                "com.example.damaiassistant/.service.OtherAccessibilityService",
                expectedPackage,
                expectedClass
            )
        )
    }
}
