package com.rookie.damaihelper

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationCompat

class IMaotaiHelperService : AccessibilityService() {

    companion object {
        const val I_MAOTAI_PKG_NAME = "com.moutai.mall"

        const val STEP_READY = 0
        const val STEP_FIRST = 1
        const val STEP_SECOND = 2
        const val STEP_THIRD = 3
        const val STEP_FINISH = 4
    }

    private var isStop = false
    private var step = STEP_READY

    private var overlayView: View? = null
    private var windowManager: WindowManager? = null

    private val iMaotaiSkipTexts = listOf("跳过")
    private val iMaotaiEntryTexts = listOf("购", "i购", "享约申购", "申购")
    private val iMaotaiCategoryTexts = listOf("贵州茅台酒")
    private val iMaotaiActionTexts = listOf("立即申购", "预约申购", "申购", "去申购", "去预约")
    private val iMaotaiGlobalActionTexts = listOf("立即申购", "预约申购", "去申购", "去预约")
    private val iMaotaiConfirmTexts = listOf("提交申购", "确认申购", "确认")
    private val iMaotaiUnavailableTexts = listOf("即将开始", "申购结束", "本场申购已结束", "暂未开始", "已结束", "售罄")

    override fun onCreate() {
        super.onCreate()
        createForegroundNotification()?.let { startForeground(1, it) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isStop = false
        step = STEP_READY
        showWindow()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun showWindow() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                logD("showWindow removeView error: ${e.message}")
            }
        }
        
        overlayView = (getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
            .inflate(R.layout.float_app_view, null) as ConstraintLayout
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.END or Gravity.TOP
        overlayView?.findViewById<TextView>(R.id.tv_switch)?.setOnClickListener {
            stopForeground(true)
            overlayView?.let { view -> windowManager?.removeView(view) }
            overlayView = null
            windowManager = null
            stopSelf()
            isStop = true
        }
        overlayView?.let { view -> windowManager?.addView(view, params) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        logD("event_name=$event")
        if (event == null || isStop || step == STEP_FINISH) {
            return
        }
        handleIMaotaiEvent(event)
    }

    private fun handleIMaotaiEvent(event: AccessibilityEvent) {
        if (event.packageName?.toString() != I_MAOTAI_PKG_NAME) {
            return
        }
        val source = event.source ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }

        if (skipIMaotaiSplash(source)) {
            return
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            fullPrintNode("imaotai_content_change", source)
        }

        if (openIMaotaiBuyTab(source)) {
            return
        }

        when (step) {
            STEP_READY, STEP_FIRST -> {
                if (openIMaotaiPage(source)) {
                    return
                }
                clickIMaotaiApply(source)
            }

            STEP_SECOND -> clickIMaotaiApply(source)
            STEP_THIRD -> confirmIMaotaiOrder(source)
        }
    }

    private fun skipIMaotaiSplash(source: AccessibilityNodeInfo): Boolean {
        val skipNode = source.findNodeByLabels(iMaotaiSkipTexts)
        if (skipNode != null) {
            logD("skipIMaotaiSplash click 跳过")
            skipNode.click()
            step = STEP_READY
            return true
        }
        return false
    }

    private fun openIMaotaiBuyTab(source: AccessibilityNodeInfo): Boolean {
        if (source.findNodeByLabels(listOf(UserManager.keyword)) != null) {
            return false
        }

        val buyTabNode = source.findNodeByLabels(iMaotaiEntryTexts, exactMatch = true)
        if (buyTabNode != null) {
            step = STEP_FIRST
            logD("openIMaotaiBuyTab click 购")
            buyTabNode.click()
            return true
        }

        val categoryNode = source.findNodeByLabels(iMaotaiCategoryTexts, exactMatch = true)
        if (categoryNode != null) {
            step = STEP_FIRST
            logD("openIMaotaiBuyTab click 贵州茅台酒")
            categoryNode.click()
            return true
        }
        return false
    }

    private fun openIMaotaiPage(source: AccessibilityNodeInfo): Boolean {
        val keywordNode = source.findNodeByLabels(listOf(UserManager.keyword))
        if (keywordNode != null) {
            val actionNode = keywordNode.findNodeAroundByLabels(iMaotaiActionTexts)
            if (actionNode != null && !isIMaotaiUnavailable(actionNode.labelText())) {
                step = STEP_THIRD
                logD("openIMaotaiPage click action near keyword=${UserManager.keyword}")
                actionNode.click()
                return true
            }

            step = STEP_SECOND
            logD("openIMaotaiPage click keyword=${UserManager.keyword}")
            keywordNode.click()
            return true
        }

        val entryNode = source.findNodeByLabels(iMaotaiEntryTexts, exactMatch = true)
        if (entryNode != null) {
            step = STEP_FIRST
            logD("openIMaotaiPage click i茅台 entry")
            entryNode.click()
            return true
        }
        return false
    }

    private fun clickIMaotaiApply(source: AccessibilityNodeInfo) {
        val keywordNode = source.findNodeByLabels(listOf(UserManager.keyword))
        val actionNode = keywordNode?.findNodeAroundByLabels(iMaotaiActionTexts)
            ?: source.findNodeByLabels(iMaotaiGlobalActionTexts)
            ?: source.findNodeByLabels(listOf("申购"))

        if (actionNode == null) {
            return
        }

        val label = actionNode.labelText()
        if (isIMaotaiUnavailable(label)) {
            logD("clickIMaotaiApply unavailable label=$label")
            return
        }

        logD("clickIMaotaiApply click label=$label")
        step = STEP_THIRD
        actionNode.click()
    }

    private fun confirmIMaotaiOrder(source: AccessibilityNodeInfo) {
        val confirmNode = source.findNodeByLabels(iMaotaiConfirmTexts)
        if (confirmNode != null) {
            logD("confirmIMaotaiOrder click label=${confirmNode.labelText()}")
            confirmNode.click()
            step = STEP_FINISH
        }
    }

    private fun isIMaotaiUnavailable(label: String): Boolean {
        if (label.isBlank()) {
            return false
        }
        return iMaotaiUnavailableTexts.any { label.contains(it) }
    }

    override fun onInterrupt() {
    }

    private fun createForegroundNotification(): Notification? {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val channelId = "imaotai-helper"
        notificationManager?.createNotificationChannel(
            NotificationChannel(channelId, getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH)
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    FLAG_IMMUTABLE
                )
            )
            .setSmallIcon(R.drawable.ic_app)
            .setContentTitle(getString(R.string.acc_des))
            .setContentText(getString(R.string.app_name))
            .setTicker(getString(R.string.app_name))
            .build()
    }

    override fun onDestroy() {
        stopForeground(true)
        overlayView?.let { view -> 
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                logD("onDestroy removeView error: ${e.message}")
            }
        }
        overlayView = null
        windowManager = null
        super.onDestroy()
    }
}