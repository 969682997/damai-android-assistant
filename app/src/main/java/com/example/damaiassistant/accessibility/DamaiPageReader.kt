package com.example.damaiassistant.accessibility

import com.example.damaiassistant.domain.TextNormalizer
import com.example.damaiassistant.model.UiNodeSnapshot
import com.example.damaiassistant.model.VisiblePage
import com.example.damaiassistant.model.VisiblePageKind
import com.example.damaiassistant.model.VisibleTicketTier
import com.example.damaiassistant.model.VisibleViewer
import java.math.BigDecimal
import java.math.RoundingMode

object DamaiPageReader {
    fun read(root: UiNodeSnapshot): VisiblePage {
        val indexedNodes = flatten(root)
        val texts = indexedNodes.flatMap { listOfNotNull(it.node.text, it.node.contentDescription) }
            .map(String::trim)
            .filter(String::isNotEmpty)
        val ticketTiers = readTicketTiers(indexedNodes)
        val viewers = readViewers(indexedNodes)
        val allText = texts.joinToString(" ")

        return VisiblePage(
            kind = classify(allText, ticketTiers, viewers),
            texts = texts,
            ticketTiers = ticketTiers,
            viewers = viewers,
            quantityControlKey = readActionKey(indexedNodes, QUANTITY_KEYWORDS),
            submitControlKey = readActionKey(indexedNodes, SUBMIT_KEYWORDS)
        )
    }

    private fun readTicketTiers(nodes: List<IndexedNode>): List<VisibleTicketTier> =
        nodes.asSequence()
            .filter { it.node.children.size >= 2 }
            .filter { node ->
                !node.node.children.any { child -> child.children.isNotEmpty() && containsPrice(child) }
            }
            .mapNotNull { indexed ->
                val texts = indexed.node.children.flatMap { child ->
                    flatten(child).flatMap { listOfNotNull(it.node.text, it.node.contentDescription) }
                }
                val price = texts.asSequence().mapNotNull(::parsePriceCents).firstOrNull() ?: return@mapNotNull null
                val name = texts.firstOrNull { isTierName(it) } ?: return@mapNotNull null
                val combined = texts.joinToString(" ")
                VisibleTicketTier(
                    name = name.trim(),
                    priceCents = price,
                    available = indexed.node.enabled && !containsSoldOut(combined),
                    stableKey = indexed.node.contentDescription ?: name.trim(),
                    bounds = indexed.node.bounds
                )
            }
            .distinctBy { it.stableKey }
            .toList()

    private fun readViewers(nodes: List<IndexedNode>): List<VisibleViewer> =
        nodes.asSequence()
            .filter { isViewerContainer(it.node) }
            .flatMap { container ->
                container.node.children.asSequence().mapIndexedNotNull { index, child ->
                    if (child.children.isNotEmpty()) return@mapIndexedNotNull null
                    val displayName = child.text?.trim()?.takeIf(String::isNotEmpty) ?: return@mapIndexedNotNull null
                    if (displayName in VIEWER_LABELS) return@mapIndexedNotNull null
                    VisibleViewer(
                        displayName = displayName,
                        stableKey = child.contentDescription ?: displayName,
                        selected = child.selected || child.checked,
                        bounds = child.bounds
                    )
                }
            }
            .distinctBy { it.stableKey }
            .toList()

    private fun classify(
        allText: String,
        ticketTiers: List<VisibleTicketTier>,
        viewers: List<VisibleViewer>
    ): VisiblePageKind {
        val text = TextNormalizer.normalize(allText).lowercase()
        return when {
            containsAny(text, PAYMENT_KEYWORDS) -> VisiblePageKind.Payment
            containsAny(text, SMS_KEYWORDS) -> VisiblePageKind.SmsVerification
            containsAny(text, HUMAN_CHECK_KEYWORDS) -> VisiblePageKind.HumanVerification
            containsAny(text, QUEUE_KEYWORDS) -> VisiblePageKind.Queue
            containsAny(text, LOGIN_KEYWORDS) -> VisiblePageKind.LoginRequired
            containsAny(text, APPOINTMENT_KEYWORDS) -> VisiblePageKind.Appointment
            ticketTiers.isNotEmpty() -> VisiblePageKind.TicketSelection
            viewers.isNotEmpty() -> VisiblePageKind.ViewerSelection
            else -> VisiblePageKind.Unknown
        }
    }

    private fun flatten(root: UiNodeSnapshot): List<IndexedNode> {
        val result = mutableListOf<IndexedNode>()
        fun visit(node: UiNodeSnapshot, path: String) {
            result += IndexedNode(node, path)
            node.children.forEachIndexed { index, child -> visit(child, "$path-$index") }
        }
        visit(root, "0")
        return result
    }

    private fun containsPrice(node: UiNodeSnapshot): Boolean =
        flatten(node).any { indexed ->
            listOfNotNull(indexed.node.text, indexed.node.contentDescription).any { parsePriceCents(it) != null }
        }

    private fun parsePriceCents(value: String): Int? {
        val match = PRICE_PATTERN.find(value) ?: return null
        return try {
            BigDecimal(match.groupValues[1].replace(",", ""))
                .setScale(2, RoundingMode.UNNECESSARY)
                .movePointRight(2)
                .intValueExact()
        } catch (_: ArithmeticException) {
            null
        }
    }

    private fun isTierName(value: String): Boolean =
        value.isNotBlank() &&
            parsePriceCents(value) == null &&
            value !in TIER_STATUS_LABELS &&
            !value.contains("票档")

    private fun containsSoldOut(value: String): Boolean =
        TIER_SOLD_OUT_KEYWORDS.any(value::contains)

    private fun isViewerContainer(node: UiNodeSnapshot): Boolean {
        val text = listOfNotNull(node.text, node.contentDescription).joinToString(" ").lowercase()
        return text.contains("观演人") || node.className.orEmpty().lowercase().contains("viewer")
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean = keywords.any(text::contains)

    private fun readActionKey(nodes: List<IndexedNode>, keywords: List<String>): String? =
        nodes.asSequence()
            .map { it.node }
            .filter { it.clickable && it.enabled }
            .mapNotNull { node ->
                val visibleText = listOfNotNull(node.text, node.contentDescription).firstOrNull { text ->
                    keywords.any(text::contains)
                }
                visibleText?.let { node.contentDescription ?: it }
            }
            .firstOrNull()

    private data class IndexedNode(val node: UiNodeSnapshot, val path: String)

    private val PRICE_PATTERN = Regex("(?:¥|￥|rmb|人民币)?\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE)
    private val TIER_STATUS_LABELS = setOf("可选", "有票", "售罄", "已售罄", "缺货", "不可选")
    private val TIER_SOLD_OUT_KEYWORDS = listOf("售罄", "已售罄", "缺货", "不可选", "无票")
    private val VIEWER_LABELS = setOf("全选", "取消全选", "确定", "下一步")
    private val PAYMENT_KEYWORDS = listOf("付款", "收银台", "确认支付", "支付订单", "待支付")
    private val HUMAN_CHECK_KEYWORDS = listOf("验证码", "滑块", "人机验证", "请完成验证")
    private val SMS_KEYWORDS = listOf("短信验证码", "短信验证", "短信校验")
    private val QUEUE_KEYWORDS = listOf("排队中", "排队", "队列")
    private val LOGIN_KEYWORDS = listOf("请登录", "登录失效", "重新登录")
    private val APPOINTMENT_KEYWORDS = listOf("预约", "预约成功")
    private val QUANTITY_KEYWORDS = listOf("数量", "加一张", "减一张")
    private val SUBMIT_KEYWORDS = listOf("提交订单", "确认订单", "立即购买")
}
