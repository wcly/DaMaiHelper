package com.rookie.damaihelper

import android.app.Dialog
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import permissions.dispatcher.ktx.PermissionsRequester
import permissions.dispatcher.ktx.constructSystemAlertWindowPermissionRequest

class MainActivity : BaseActivity() {

    private lateinit var btnStart: Button
    private lateinit var etKeyword: EditText
    private lateinit var rgMode: RadioGroup
    private lateinit var tvTip: TextView
    private var dialog: Dialog? = null
    private var systemAlertRequest: PermissionsRequester? = null

    override fun getLayoutID(): Int = R.layout.activity_main

    override fun init() {
        btnStart = findViewById(R.id.btn_start)
        etKeyword = findViewById(R.id.et_keyword)
        rgMode = findViewById(R.id.rg_mode)
        tvTip = findViewById(R.id.tv_tip)

        rgMode.setOnCheckedChangeListener { _, checkedId ->
            persistKeyword()
            val targetMode = if (checkedId == R.id.rb_i_maotai) {
                UserManager.TaskMode.I_MAOTAI
            } else {
                UserManager.TaskMode.DAMAI
            }
            UserManager.taskMode = targetMode
            renderMode(targetMode)
        }
        rgMode.check(
            if (UserManager.taskMode == UserManager.TaskMode.I_MAOTAI) {
                R.id.rb_i_maotai
            } else {
                R.id.rb_damai
            }
        )
        renderMode(UserManager.taskMode)

        btnStart.setOnClickListener {
            systemAlertRequest?.launch()
        }
        systemAlertRequest = constructSystemAlertWindowPermissionRequest {
            startQiangp()
        }
    }

    private fun startQiangp() {
        if (!isAccessibilitySettingsOn(DaMaiHelperService::class.java)) {
            logD("startQiangp accessibility disabled")
            showAccessDialog()
            return
        }

        persistKeyword()
        logD("startQiangp mode=${UserManager.taskMode} keyword=${UserManager.currentKeyword()}")
        if (startTargetApp()) {
            logD("startQiangp target launched")
            UserManager.startQp()
        } else {
            logD("startQiangp failed to launch target app")
        }
    }

    private fun persistKeyword() {
        val keyword = etKeyword.text?.toString()?.trim().orEmpty()
        if (!TextUtils.isEmpty(keyword)) {
            UserManager.updateKeyword(UserManager.taskMode, keyword)
        }
    }

    private fun renderMode(mode: UserManager.TaskMode) {
        etKeyword.setText(UserManager.keywordOf(mode))
        etKeyword.setSelection(etKeyword.text?.length ?: 0)
        when (mode) {
            UserManager.TaskMode.DAMAI -> {
                etKeyword.hint = getString(R.string.hint_damai_keyword)
                btnStart.text = getString(R.string.action_start_damai)
                tvTip.text = getString(R.string.tip_damai)
            }

            UserManager.TaskMode.I_MAOTAI -> {
                etKeyword.hint = getString(R.string.hint_i_maotai_keyword)
                btnStart.text = getString(R.string.action_start_i_maotai)
                tvTip.text = getString(R.string.tip_i_maotai)
            }
        }
    }

    private fun startTargetApp(): Boolean {
        return when (UserManager.taskMode) {
            UserManager.TaskMode.DAMAI -> {
                startApp(
                    DaMaiHelperService.DAMAI_PKG_NAME,
                    DaMaiHelperService.ME_UI,
                    getString(R.string.error_no_damai)
                )
            }

            UserManager.TaskMode.I_MAOTAI -> {
                startApp(
                    packageName = DaMaiHelperService.I_MAOTAI_PKG_NAME,
                    activityName = null,
                    errorTips = getString(R.string.error_no_i_maotai)
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isAccessibilitySettingsOn(DaMaiHelperService::class.java)) {
            dialog?.dismiss()
        }
    }

    private fun showAccessDialog() {
        if (dialog == null) {
            dialog = AlertDialog.Builder(this)
                .setTitle("请打开<<${getString(R.string.acc_des)}>>无障碍服务")
                .setPositiveButton("确认") { dialog, _ ->
                    dialog.dismiss()
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    startActivity(intent)
                }
                .create()
        }
        dialog!!.show()
    }

}
