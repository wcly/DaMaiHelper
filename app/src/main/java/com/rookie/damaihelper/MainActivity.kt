package com.rookie.damaihelper

import android.app.Dialog
import android.content.Intent
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import permissions.dispatcher.ktx.PermissionsRequester
import permissions.dispatcher.ktx.constructSystemAlertWindowPermissionRequest

class MainActivity : BaseActivity() {

    private lateinit var btnStart: Button
    private lateinit var etKeyword: EditText
    private lateinit var tvTip: TextView
    private var dialog: Dialog? = null
    private var systemAlertRequest: PermissionsRequester? = null

    override fun getLayoutID(): Int = R.layout.activity_main

    override fun init() {
        btnStart = findViewById(R.id.btn_start)
        etKeyword = findViewById(R.id.et_keyword)
        tvTip = findViewById(R.id.tv_tip)

        etKeyword.setText(UserManager.keyword)
        etKeyword.setSelection(etKeyword.text?.length ?: 0)
        btnStart.text = getString(R.string.action_start_i_maotai)
        tvTip.text = getString(R.string.tip_i_maotai)

        btnStart.setOnClickListener {
            systemAlertRequest?.launch()
        }
        systemAlertRequest = constructSystemAlertWindowPermissionRequest {
            startQiangp()
        }
    }

    private fun startQiangp() {
        if (!isAccessibilitySettingsOn(IMaotaiHelperService::class.java)) {
            logD("startQiangp accessibility disabled")
            showAccessDialog()
            return
        }

        persistKeyword()
        logD("startQiangp keyword=${UserManager.keyword}")
        if (startApp(
                packageName = IMaotaiHelperService.I_MAOTAI_PKG_NAME,
                errorTips = getString(R.string.error_no_i_maotai)
            )
        ) {
            logD("startQiangp target launched")
            UserManager.startQp()
        } else {
            logD("startQiangp failed to launch target app")
        }
    }

    private fun persistKeyword() {
        val keyword = etKeyword.text?.toString()?.trim().orEmpty()
        UserManager.updateKeyword(keyword)
    }

    override fun onResume() {
        super.onResume()
        if (isAccessibilitySettingsOn(IMaotaiHelperService::class.java)) {
            dialog?.dismiss()
        }
    }

    private fun showAccessDialog() {
        if (dialog == null) {
            dialog = AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_accessibility_title, getString(R.string.acc_des)))
                .setPositiveButton(R.string.dialog_confirm) { dialog, _ ->
                    dialog.dismiss()
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    startActivity(intent)
                }
                .create()
        }
        dialog?.show()
    }
}