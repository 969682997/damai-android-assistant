package com.example.damaiassistant.accessibility

import com.example.damaiassistant.model.UiNodeSnapshot
import com.example.damaiassistant.model.VisiblePageKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DamaiPageReaderTest {
    @Test
    fun readsTicketTiersAndViewersFromVisibleContainers() {
        val page = DamaiPageReader.read(
            UiNodeSnapshot(children = listOf(
                UiNodeSnapshot(
                    text = "票档",
                    children = listOf(
                        UiNodeSnapshot(
                            clickable = true,
                            contentDescription = "tier-inner",
                            children = listOf(
                                UiNodeSnapshot(text = "内场"),
                                UiNodeSnapshot(text = "¥1,280"),
                                UiNodeSnapshot(text = "可选")
                            )
                        ),
                        UiNodeSnapshot(
                            clickable = true,
                            contentDescription = "tier-stand",
                            children = listOf(
                                UiNodeSnapshot(text = "看台"),
                                UiNodeSnapshot(text = "¥880"),
                                UiNodeSnapshot(text = "售罄")
                            )
                        )
                    )
                ),
                UiNodeSnapshot(
                    text = "观演人",
                    children = listOf(
                        UiNodeSnapshot(text = "观演人 A", contentDescription = "viewer-a"),
                        UiNodeSnapshot(text = "观演人 B", contentDescription = "viewer-b")
                    )
                )
            ))
        )

        assertEquals(VisiblePageKind.TicketSelection, page.kind)
        assertEquals(2, page.ticketTiers.size)
        assertEquals("内场", page.ticketTiers[0].name)
        assertEquals(128000, page.ticketTiers[0].priceCents)
        assertTrue(page.ticketTiers[0].available)
        assertTrue(!page.ticketTiers[1].available)
        assertEquals(listOf("观演人 A", "观演人 B"), page.viewers.map { it.displayName })
    }

    @Test
    fun classifiesHumanChecksPaymentSmsAndUnknownPages() {
        assertEquals(
            VisiblePageKind.HumanVerification,
            readText("请完成验证码和滑块验证").kind
        )
        assertEquals(
            VisiblePageKind.SmsVerification,
            readText("请输入短信验证码").kind
        )
        assertEquals(
            VisiblePageKind.Payment,
            readText("确认订单并进入付款收银台").kind
        )
        assertEquals(
            VisiblePageKind.Unknown,
            readText("没有可识别内容").kind
        )
    }

    private fun readText(text: String) = DamaiPageReader.read(UiNodeSnapshot(text = text))
}

