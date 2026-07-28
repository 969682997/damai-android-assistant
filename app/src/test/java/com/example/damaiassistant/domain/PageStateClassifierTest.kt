package com.example.damaiassistant.domain

import com.example.damaiassistant.model.VisiblePage
import com.example.damaiassistant.model.VisiblePageKind
import org.junit.Assert.assertEquals
import org.junit.Test

class PageStateClassifierTest {
    @Test
    fun givesPaymentTheHighestPriorityAndRoutesHumanChecksToHandoff() {
        assertEquals(
            PageClassification.PaymentPage,
            PageStateClassifier.classify(VisiblePage(VisiblePageKind.Payment, listOf("付款")))
        )
        assertEquals(
            PageClassification.HumanHandoff(HandoffReason.HumanVerification),
            PageStateClassifier.classify(
                VisiblePage(VisiblePageKind.HumanVerification, listOf("验证码"))
            )
        )
        assertEquals(
            PageClassification.HumanHandoff(HandoffReason.SmsVerification),
            PageStateClassifier.classify(
                VisiblePage(VisiblePageKind.SmsVerification, listOf("短信验证码"))
            )
        )
    }

    @Test
    fun keepsUnknownPagesExplicit() {
        assertEquals(
            PageClassification.UnknownPage,
            PageStateClassifier.classify(VisiblePage(VisiblePageKind.Unknown, listOf("未知")))
        )
    }
}

