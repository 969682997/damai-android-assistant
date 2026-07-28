package com.example.damaiassistant

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.damaiassistant.permission.DamaiPackageResolver
import com.example.damaiassistant.permission.PermissionCoordinator
import com.example.damaiassistant.service.TaskRunnerService
import com.example.damaiassistant.data.SharedPreferencesStore
import com.example.damaiassistant.data.TaskRepository

class MainActivity : AppCompatActivity() {
    private lateinit var permissionText: TextView
    private lateinit var damaiStatusText: TextView
    private lateinit var startButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        permissionText = findViewById(R.id.permissionText)
        damaiStatusText = findViewById(R.id.damaiStatusText)
        startButton = findViewById(R.id.startTaskButton)
        findViewById<Button>(R.id.accessibilityButton).setOnClickListener {
            PermissionCoordinator.openAccessibilitySettings(this)
        }
        findViewById<Button>(R.id.overlayButton).setOnClickListener {
            PermissionCoordinator.openOverlaySettings(this)
        }
        findViewById<Button>(R.id.alarmButton).setOnClickListener {
            PermissionCoordinator.openExactAlarmSettings(this)
        }
        findViewById<Button>(R.id.notificationButton).setOnClickListener {
            requestNotificationPermission()
        }
        findViewById<Button>(R.id.editTaskButton).setOnClickListener {
            startActivity(Intent(this, TaskEditorActivity::class.java))
        }
        findViewById<Button>(R.id.openDamaiButton).setOnClickListener {
            DamaiPackageResolver.launchIntent(this)?.let(::startActivity)
                ?: Toast.makeText(this, "未安装大麦 App", Toast.LENGTH_SHORT).show()
        }
        startButton.setOnClickListener { startTask() }
        findViewById<Button>(R.id.stopTaskButton).setOnClickListener {
            startService(Intent(this, TaskRunnerService::class.java).setAction(TaskRunnerService.ACTION_STOP))
        }
        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
        window.decorView.postDelayed({
            if (!isFinishing) refreshPermissions()
        }, 800L)
    }

    private fun refreshPermissions() {
        val status = PermissionCoordinator.check(this)
        permissionText.text = buildString {
            append("无障碍：").append(if (status.accessibilityEnabled) "已开启" else "未开启")
            append("\n悬浮窗：").append(if (status.overlayGranted) "已授权" else "未授权")
            append("\n通知：").append(if (status.notificationsGranted) "已授权" else "未授权")
            append("\n精确定时：").append(if (status.exactAlarmGranted) "已授权" else "未授权")
        }
        val damaiPackage = DamaiPackageResolver.resolve(this)
        damaiStatusText.text = if (damaiPackage == null) {
            "大麦：未检测到可启动的大麦 App"
        } else {
            "大麦：已检测（$damaiPackage）"
        }
        findViewById<Button>(R.id.openDamaiButton).isEnabled = damaiPackage != null
        startButton.isEnabled = status.allGranted
    }

    private fun startTask() {
        if (!PermissionCoordinator.check(this).allGranted) {
            Toast.makeText(this, "请先完成全部权限设置", Toast.LENGTH_SHORT).show()
            return
        }
        val task = TaskRepository(
            SharedPreferencesStore(getSharedPreferences("damai_assistant", MODE_PRIVATE))
        ).load()
        if (task == null || !task.enabled) {
            Toast.makeText(this, "请先保存一个有效任务", Toast.LENGTH_SHORT).show()
            return
        }
        ContextCompat.startForegroundService(
            this,
            Intent(this, TaskRunnerService::class.java).setAction(TaskRunnerService.ACTION_START)
        )
        Toast.makeText(this, "任务已启动，请保持大麦页面可用", Toast.LENGTH_SHORT).show()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_REQUEST_CODE)
        }
    }

    private companion object {
        const val NOTIFICATION_REQUEST_CODE = 100
    }
}
