package com.example.damaiassistant.permission

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DamaiPackageMatcherTest {
    @Test
    fun prefersKnownDamaiPackageWhenItCanBeLaunched() {
        val candidates = listOf(
            DamaiPackageCandidate("com.example.fake", "大麦测试工具", true),
            DamaiPackageCandidate("cn.damai", "大麦", true)
        )

        assertEquals("cn.damai", DamaiPackageMatcher.find(candidates))
    }

    @Test
    fun fallsBackToLaunchableApplicationLabeledDamai() {
        val candidates = listOf(
            DamaiPackageCandidate("cn.damai", "大麦", false),
            DamaiPackageCandidate("com.vendor.damai", "大麦", true)
        )

        assertEquals("com.vendor.damai", DamaiPackageMatcher.find(candidates))
    }

    @Test
    fun ignoresUnlaunchableOrUnrelatedApplications() {
        val candidates = listOf(
            DamaiPackageCandidate("com.vendor.damai", "大麦", false),
            DamaiPackageCandidate("com.example.other", "其他应用", true)
        )

        assertNull(DamaiPackageMatcher.find(candidates))
    }
}
