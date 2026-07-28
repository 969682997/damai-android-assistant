package com.example.damaiassistant

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
    }

    private fun buildContent() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(24, 24, 24, 24)
        addView(TextView(context).apply { text = "单任务配置"; textSize = 22f })
        eventName = input("活动名称").also { addView(it) }
        performanceName = input("场次名称").also { addView(it) }
        releaseAt = input("开售时间 Unix 毫秒").also { addView(it) }
        ticketCount = input("购票数量").also { addView(it) }
        viewers = input("观演人姓名，多个用英文逗号分隔").also { addView(it) }
        ticketPreferences = input("票档，每行：名称|价格元|优先级").also { addView(it) }
        addView(Button(context).apply {
            text = "保存任务"
            setOnClickListener { saveTask() }
        })
    }

    private fun input(hintText: String) = EditText(this).also {
        it.hint = hintText
    }

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
        val store = SharedPreferencesStore(getSharedPreferences("damai_assistant", MODE_PRIVATE))
        TaskRepository(store).save(task)
        Toast.makeText(this, "任务已保存", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun parsePriceCents(value: String): Int =
        BigDecimal(value.replace("¥", "").replace("￥", "").trim())
            .setScale(2, RoundingMode.UNNECESSARY)
            .movePointRight(2)
            .intValueExact()
}
