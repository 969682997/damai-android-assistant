package com.example.damaiassistant

import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.damaiassistant.data.LatestDamaiPageStore
import com.example.damaiassistant.data.SharedPreferencesStore
import com.example.damaiassistant.data.TaskRepository
import com.example.damaiassistant.domain.TaskValidator
import com.example.damaiassistant.model.TaskConfig
import com.example.damaiassistant.model.TicketPreference
import com.example.damaiassistant.model.ViewerRef
import java.math.BigDecimal
import java.math.RoundingMode

class TaskEditorActivity : AppCompatActivity() {
    private lateinit var eventName: EditText
    private lateinit var performanceName: EditText
    private lateinit var releaseAt: EditText
    private lateinit var ticketCount: EditText
    private lateinit var viewers: EditText
    private lateinit var ticketPreferences: EditText
    private lateinit var readStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
        readCurrentDamaiPage(showToast = false)
    }

    private fun buildContent() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(24, 24, 24, 24)
        addView(TextView(context).apply { text = "单任务配置"; textSize = 22f })
        addView(TextView(context).apply {
            text = "请先打开大麦目标活动页面，助手会尝试读取当前可见信息。"
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        })
        addView(Button(context).apply {
            text = "读取当前大麦页面"
            setOnClickListener { readCurrentDamaiPage(showToast = true) }
        })
        readStatus = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            text = "尚未读取页面"
        }
        addView(readStatus)
        eventName = input("活动名称，例如：周杰伦上海演唱会").also { addView(it) }
        performanceName = input("场次名称，例如：2026-08-01 周六 19:30").also { addView(it) }
        releaseAt = input(
            "开售时间 Unix 毫秒，例如：1785585600000",
            inputType = InputType.TYPE_CLASS_NUMBER
        ).also { addView(it) }
        ticketCount = input(
            "购票数量，例如：2",
            inputType = InputType.TYPE_CLASS_NUMBER
        ).also { addView(it) }
        viewers = input("观演人姓名，例如：张三,李四").also { addView(it) }
        ticketPreferences = input(
            "票档模板，每行：名称|价格元|优先级\n例如：内场|1280|0",
            multiline = true
        ).also { addView(it) }
        addView(Button(context).apply {
            text = "保存任务"
            setOnClickListener { saveTask() }
        })
    }

    private fun input(
        hintText: String,
        inputType: Int = InputType.TYPE_CLASS_TEXT,
        multiline: Boolean = false
    ) = EditText(this).also {
        it.hint = hintText
        it.setHintTextColor(ContextCompat.getColor(this, R.color.text_hint))
        it.inputType = if (multiline) {
            inputType or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        } else {
            inputType
        }
        if (multiline) {
            it.minLines = 3
            it.gravity = android.view.Gravity.TOP
        }
    }

    private fun readCurrentDamaiPage(showToast: Boolean) {
        val draft = LatestDamaiPageStore(
            SharedPreferencesStore(getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE))
        ).load()
        if (draft == null || System.currentTimeMillis() - draft.capturedAtEpochMs > DRAFT_MAX_AGE_MS) {
            readStatus.text = "未读取到最近 5 分钟内的大麦页面，下面的灰色文字是填写模板。"
            if (showToast) Toast.makeText(this, "未读取到最近的大麦页面", Toast.LENGTH_SHORT).show()
            return
        }

        draft.eventName?.let(eventName::setText)
        draft.performanceName?.let(performanceName::setText)
        draft.releaseAtEpochMs?.let { releaseAt.setText(it.toString()) }
        if (draft.ticketPreferences.isNotEmpty()) {
            ticketPreferences.setText(draft.ticketPreferences.joinToString("\n") { preference ->
                "${preference.name}|${formatPrice(preference.priceCents)}|${preference.priority}"
            })
        }
        readStatus.text = buildString {
            append("已读取大麦页面")
            if (draft.ticketPreferences.isNotEmpty()) {
                append("，票档 ").append(draft.ticketPreferences.size).append(" 个")
            }
            if (draft.viewers.isNotEmpty()) {
                append("，观演人 ").append(draft.viewers.size).append(" 位；请按购票数量选择姓名")
            }
        }
        if (showToast) Toast.makeText(this, "已回填当前大麦页面信息", Toast.LENGTH_SHORT).show()
    }

    private fun formatPrice(priceCents: Int): String =
        BigDecimal(priceCents).movePointLeft(2).stripTrailingZeros().toPlainString()

    private fun saveTask() {
        val task = try {
            TaskConfig(
                eventName = eventName.text.toString(),
                performanceName = performanceName.text.toString(),
                releaseAtEpochMs = releaseAt.text.toString().trim().toLong(),
                ticketCount = ticketCount.text.toString().trim().toInt(),
                viewers = viewers.text.toString().split(',').map { ViewerRef(it.trim()) }.filter { it.displayName.isNotEmpty() },
                ticketPreferences = ticketPreferences.text.toString().lines().filter { it.isNotBlank() }.mapIndexed { index, line ->
                    val parts = line.split('|').map(String::trim)
                    if (parts.size != 3) error("票档格式错误：$line")
                    TicketPreference(parts[0], parsePriceCents(parts[1]), parts[2].toIntOrNull() ?: index)
                },
                enabled = true
            )
        } catch (error: Exception) {
            Toast.makeText(this, error.message ?: "配置格式错误", Toast.LENGTH_LONG).show()
            return
        }
        val errors = TaskValidator.validate(task, System.currentTimeMillis())
        if (errors.isNotEmpty()) {
            Toast.makeText(this, "任务校验失败：${errors.joinToString { it.code.name }}", Toast.LENGTH_LONG).show()
            return
        }
        val store = SharedPreferencesStore(getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE))
        TaskRepository(store).save(task)
        Toast.makeText(this, "任务已保存", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun parsePriceCents(value: String): Int =
        BigDecimal(value.replace("¥", "").replace("￥", "").trim())
            .setScale(2, RoundingMode.UNNECESSARY)
            .movePointRight(2)
            .intValueExact()

    private companion object {
        const val PREFERENCES_NAME = "damai_assistant"
        const val DRAFT_MAX_AGE_MS = 5 * 60 * 1000L
    }
}
