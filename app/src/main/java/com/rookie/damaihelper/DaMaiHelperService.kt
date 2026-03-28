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
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationCompat

class DaMaiHelperService : AccessibilityService(), UserManager.IStartListener {

    companion object {
        const val DAMAI_PKG_NAME = "cn.damai"
        const val I_MAOTAI_PKG_NAME = "com.moutai.mall"

        const val ME_UI = "cn.damai.mine.activity.MineMainActivity"
        const val LIVE_DETAIL_UI =
            "cn.damai.trade.newtradeorder.ui.projectdetail.ui.activity.ProjectDetailActivity"
        const val LIVE_SELECT_DETAIL_UI =
            "cn.damai.commonbusiness.seatbiz.sku.qilin.ui.NcovSkuActivity"
        const val LIVE_TOTAL_UI = "cn.damai.ultron.view.activity.DmOrderActivity"

        const val ID_LIVE_DETAIL_BUY = "tv_left_main_text"
        const val ID_CONFIRM_BUY = "btn_buy"

        const val STEP_READY = 0
        const val STEP_FIRST = 1
        const val STEP_SECOND = 2
        const val STEP_THIRD = 3
        const val STEP_FOURTH = 4
        const val STEP_FINISH = 5
    }

    private var isStop = false
    private var step = STEP_READY

    private var overlayView: View? = null
    private var windowManager: WindowManager? = null

    private val damaiSoonText = "即将"
    private val damaiOutOfStockText = "缺货"
    private val damaiReserveText = "预约"

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
        UserManager.startListener = this
    }

    private fun showWindow() {
        if (windowManager != null) {
            return
        }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = (getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
            .inflate(R.layout.float_app_view, null) as ConstraintLayout
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.END or Gravity.TOP
        overlayView?.findViewById<TextView>(R.id.tv_switch)?.setOnClickListener {
            stopForeground(true)
            windowManager?.removeView(overlayView)
            stopSelf()
            isStop = true
        }
        windowManager?.addView(overlayView, params)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        logD("event_name:$event")
        if (event == null || isStop || step == STEP_FINISH) {
            return
        }
        when (UserManager.taskMode) {
            UserManager.TaskMode.DAMAI -> handleDaMaiEvent(event)
            UserManager.TaskMode.I_MAOTAI -> handleIMaotaiEvent(event)
        }
    }

    private fun handleDaMaiEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.className.toString() == ME_UI) {
                openFavoritePage(event)
                return
            }

            when (event.className.toString()) {
                LIVE_DETAIL_UI -> {
                    step = STEP_SECOND
                    startQ(event)
                }

                LIVE_SELECT_DETAIL_UI -> {
                    step = STEP_THIRD
                    confirmOrder(event)
                }

                LIVE_TOTAL_UI -> {
                    step = STEP_FOURTH
                    requestOrder(event)
                }
            }
            if (event.packageName?.toString() == DAMAI_PKG_NAME && step <= STEP_FIRST) {
                openFavoritePage(event)
            }
        } else if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            fullPrintNode("content_change", event.source)
            if (event.packageName?.toString() == DAMAI_PKG_NAME && step <= STEP_FIRST) {
                openFavoritePage(event)
            }
            when (step) {
                STEP_SECOND -> startQ(event)
                STEP_THIRD -> confirmOrder(event)
                STEP_FOURTH -> requestOrder(event)
            }
        }
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
                if (openIMaotaiPage(event)) {
                    return
                }
                clickIMaotaiApply(event)
            }

            STEP_SECOND -> clickIMaotaiApply(event)
            STEP_THIRD -> confirmIMaotaiOrder(event)
        }
    }

    private fun skipIMaotaiSplash(source: android.view.accessibility.AccessibilityNodeInfo): Boolean {
        val skipNode = source.findNodeByLabels(iMaotaiSkipTexts, exactMatch = false)
        if (skipNode != null) {
            logD("skipIMaotaiSplash click 跳过")
            skipNode.click()
            step = STEP_READY
            return true
        }
        return false
    }

    private fun openIMaotaiBuyTab(source: android.view.accessibility.AccessibilityNodeInfo): Boolean {
        if (source.findNodeByLabels(listOf(UserManager.currentKeyword())) != null) {
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

    private fun openFavoritePage(event: AccessibilityEvent) {
        val source = event.source ?: return
        val keywordNode = source.findNodeByTextOnce(UserManager.currentKeyword())
        if (keywordNode != null) {
            step = STEP_SECOND
            logD("openFavoritePage click singer=${UserManager.currentKeyword()}")
            keywordNode.click()
            return
        }

        val wantView = source.findNodeByTextOnce("想看&想玩")
        if (wantView != null) {
            step = STEP_FIRST
            logD("openFavoritePage click 想看&想玩")
            wantView.click()
            return
        }

        val mineView = source.findNodeByTextOnce("我的", true)
            ?: source.findNodeByTextOnce("我的")
        if (mineView != null) {
            step = STEP_FIRST
            logD("openFavoritePage click 我的")
            mineView.click()
        } else {
            logD("openFavoritePage target not found class=${event.className}")
        }
    }

    private fun openIMaotaiPage(event: AccessibilityEvent): Boolean {
        val source = event.source ?: return false
        val keyword = UserManager.currentKeyword()

        val keywordNode = source.findNodeByLabels(listOf(keyword))
        if (keywordNode != null) {
            val actionNode = keywordNode.findNodeAroundByLabels(iMaotaiActionTexts, exactMatch = false)
            if (actionNode != null && !isIMaotaiUnavailable(actionNode.labelText())) {
                step = STEP_THIRD
                logD("openIMaotaiPage click action near keyword=$keyword")
                actionNode.click()
                return true
            }

            step = STEP_SECOND
            logD("openIMaotaiPage click keyword=$keyword")
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

    private fun clickIMaotaiApply(event: AccessibilityEvent) {
        val source = event.source ?: return
        val keywordNode = source.findNodeByLabels(listOf(UserManager.currentKeyword()))

        val actionNode = keywordNode?.findNodeAroundByLabels(iMaotaiActionTexts, exactMatch = false)
            ?: source.findNodeByLabels(iMaotaiGlobalActionTexts, exactMatch = false)
            ?: source.findNodeByLabels(listOf("申购"), exactMatch = false)

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

    private fun confirmIMaotaiOrder(event: AccessibilityEvent) {
        val source = event.source ?: return
        val confirmNode = source.findNodeByLabels(iMaotaiConfirmTexts, exactMatch = false)
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

    private fun startQ(event: AccessibilityEvent) {
        val source = event.source ?: return
        val startBuy = source.getNodeById(dmNodeId(ID_LIVE_DETAIL_BUY))
        startBuy?.text()?.let {
            if (!it.contains(damaiSoonText) &&
                !it.contains(damaiOutOfStockText) &&
                !it.contains(damaiReserveText)
            ) {
                startBuy.click()
                logD("buy_text:$it")
            }
        }
    }

    private fun confirmOrder(event: AccessibilityEvent) {
        val source = event.source ?: return
        sleep(100)
        source.getNodeById(dmNodeId(ID_CONFIRM_BUY))?.click()
    }

    private fun requestOrder(event: AccessibilityEvent) {
        val source = event.source ?: return
        source.getNodeByText("提交订单", true)?.let {
            it.click()
            step = STEP_FINISH
        }
    }

    override fun onStart() {
        isStop = false
        step = STEP_READY
        showWindow()
    }

    override fun onInterrupt() {
    }

    private fun createForegroundNotification(): Notification? {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val channelId = "damai"
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
        windowManager?.removeView(overlayView)
        super.onDestroy()
    }

}
