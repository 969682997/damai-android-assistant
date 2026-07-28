package com.example.damaiassistant.accessibility

import com.example.damaiassistant.model.DamaiTaskDraft
import com.example.damaiassistant.model.VisiblePage
import com.example.damaiassistant.model.TicketPreference
import com.example.damaiassistant.model.ViewerRef
import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.ZoneId

object DamaiTaskDraftParser {
    fun parse(page: VisiblePage, capturedAtEpochMs: Long = System.currentTimeMillis()): DamaiTaskDraft {
        val texts = page.texts.map(String::trim).filter(String::isNotEmpty)
        val releaseText = texts.firstOrNull { isReleaseText(it) && parseDateTime(it) != null }
        val performanceName = texts.firstOrNull(::isPerformanceText)
        val eventName = texts.firstOrNull { isEventName(it, performanceName, releaseText) }
        val releaseAt = releaseText?.let(::parseDateTime)

        return DamaiTaskDraft(
            capturedAtEpochMs = capturedAtEpochMs,
            eventName = eventName,
            performanceName = performanceName,
            releaseAtEpochMs = releaseAt,
            ticketPreferences = page.ticketTiers.mapIndexed { index, tier ->
                TicketPreference(tier.name, tier.priceCents, index)
            },
            viewers = page.viewers.map { ViewerRef(it.displayName) }
        )
    }

    private fun isReleaseText(value: String): Boolean =
        RELEASE_KEYWORDS.any(value::contains)

    private fun isPerformanceText(value: String): Boolean =
        !isReleaseText(value) && (FULL_DATE_TIME_PATTERN.containsMatchIn(value) || DATE_ONLY_PATTERN.containsMatchIn(value))

    private fun isEventName(
        value: String,
        performanceName: String?,
        releaseText: String?
    ): Boolean {
        if (value == performanceName || value == releaseText) return false
        if (value.length !in 2..80) return false
        if (FULL_DATE_TIME_PATTERN.containsMatchIn(value) || DATE_ONLY_PATTERN.containsMatchIn(value)) return false
        if (PRICE_PATTERN.matches(value)) return false
        return EVENT_EXCLUDED_KEYWORDS.none(value::contains)
    }

    private fun parseDateTime(value: String): Long? {
        val fullMatch = FULL_DATE_TIME_PATTERN.find(value)
        if (fullMatch != null) {
            return toEpochMillis(
                year = fullMatch.groupValues[1].toInt(),
                month = fullMatch.groupValues[2].toInt(),
                day = fullMatch.groupValues[3].toInt(),
                hour = fullMatch.groupValues[4].toInt(),
                minute = fullMatch.groupValues[5].toIntOrNull() ?: 0
            )
        }

        val shortMatch = SHORT_DATE_TIME_PATTERN.find(value) ?: return null
        val now = LocalDateTime.now(ZONE_ID)
        val currentYear = now.year
        val currentYearTime = toLocalDateTime(
            year = currentYear,
            month = shortMatch.groupValues[1].toInt(),
            day = shortMatch.groupValues[2].toInt(),
            hour = shortMatch.groupValues[3].toInt(),
            minute = shortMatch.groupValues[4].toInt()
        ) ?: return null
        val selected = if (currentYearTime.isBefore(now)) currentYearTime.plusYears(1) else currentYearTime
        return selected.atZone(ZONE_ID).toInstant().toEpochMilli()
    }

    private fun toEpochMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long? =
        toLocalDateTime(year, month, day, hour, minute)
            ?.atZone(ZONE_ID)
            ?.toInstant()
            ?.toEpochMilli()

    private fun toLocalDateTime(year: Int, month: Int, day: Int, hour: Int, minute: Int): LocalDateTime? =
        try {
            LocalDateTime.of(year, month, day, hour, minute)
        } catch (_: DateTimeException) {
            null
        }

    private val FULL_DATE_TIME_PATTERN = Regex(
        "(20\\d{2})\\s*(?:年|[-/.])\\s*(\\d{1,2})\\s*(?:月|[-/.])\\s*(\\d{1,2})\\s*(?:日|号)?(?:\\s*(?:周|星期)[一二三四五六日天])?\\s*(\\d{1,2})\\s*(?::|：|点)\\s*(\\d{1,2})?"
    )
    private val SHORT_DATE_TIME_PATTERN = Regex(
        "(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*(?:日|号)?\\s*(\\d{1,2})\\s*(?::|：|点)\\s*(\\d{1,2})"
    )
    private val DATE_ONLY_PATTERN = Regex("20\\d{2}\\s*(?:年|[-/.])\\s*\\d{1,2}\\s*(?:月|[-/.])\\s*\\d{1,2}")
    private val PRICE_PATTERN = Regex("(?:¥|￥|RMB|人民币)?\\s*[0-9][0-9,]*(?:\\.[0-9]{1,2})?", RegexOption.IGNORE_CASE)
    private val RELEASE_KEYWORDS = listOf("开售", "售票", "预售", "预约时间", "开始售票")
    private val EVENT_EXCLUDED_KEYWORDS = listOf(
        "预约", "购票", "购买", "立即", "开售", "售票", "票档", "观演人", "场次", "详情",
        "登录", "分享", "收藏", "价格", "可选", "有票", "售罄", "已售罄", "下一步", "提交订单"
    )
    private val ZONE_ID: ZoneId = ZoneId.of("Asia/Shanghai")
}
