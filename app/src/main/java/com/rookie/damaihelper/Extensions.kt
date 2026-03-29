package com.rookie.damaihelper

import android.accessibilityservice.AccessibilityService
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

fun Context.isAccessibilitySettingsOn(clazz: Class<out AccessibilityService?>): Boolean {
    val accessibilityEnabled = try {
        Settings.Secure.getInt(
            applicationContext.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED
        ) == 1
    } catch (_: Settings.SettingNotFoundException) {
        false
    }
    val splitter = TextUtils.SimpleStringSplitter(':')
    if (accessibilityEnabled) {
        val settingValue = Settings.Secure.getString(
            applicationContext.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        if (settingValue != null) {
            splitter.setString(settingValue)
            while (splitter.hasNext()) {
                val accessibilityService = splitter.next()
                if (accessibilityService.equals(
                        "${packageName}/${clazz.canonicalName}",
                        ignoreCase = true
                    )
                ) {
                    return true
                }
            }
        }
    }
    return false
}

fun Context.shortToast(msg: String) =
    Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()

const val APP_TAG = "IMaotaiHelper"
const val SEGMENT_SIZE = 3072

fun logD(content: String) {
    if (content.length < SEGMENT_SIZE) {
        Log.d(APP_TAG, content)
    } else {
        Log.d(APP_TAG, content.substring(0, SEGMENT_SIZE))
        logD(content.substring(SEGMENT_SIZE))
    }
}

fun Context.startApp(
    packageName: String,
    activityName: String? = null,
    errorTips: String
): Boolean {
    logD("startApp package=$packageName activity=$activityName")
    if (!isAppInstalled(packageName)) {
        logD("startApp package not visible or not installed: $packageName")
        shortToast(errorTips)
        return false
    }

    val launcherIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (launcherIntent != null) {
        try {
            logD("startApp launch via package launcher")
            startActivity(launcherIntent)
            return true
        } catch (e: Exception) {
            e.message?.let { logD(it) }
        }
    }

    if (!activityName.isNullOrBlank()) {
        val explicitIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(packageName, activityName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            logD("startApp fallback to explicit activity")
            startActivity(explicitIntent)
            return true
        } catch (e: ActivityNotFoundException) {
            logD("startApp explicit activity not found: $packageName/$activityName")
        } catch (e: SecurityException) {
            e.message?.let { logD(it) }
        } catch (e: Exception) {
            e.message?.let { logD(it) }
        }
    }

    shortToast(getString(R.string.toast_manual_open_target))
    logD("startApp failed for package=$packageName")
    return false
}

@Suppress("DEPRECATION")
fun Context.isAppInstalled(packageName: String): Boolean {
    return try {
        packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}

fun AccessibilityNodeInfo.findNodeByTextOnce(
    text: String,
    allMatch: Boolean = false
): AccessibilityNodeInfo? {
    val nodeList = findAccessibilityNodeInfosByText(text)
    if (nodeList.isNullOrEmpty()) {
        return null
    }
    return if (allMatch) {
        nodeList.firstOrNull { it.text == text }
    } else {
        nodeList[0]
    }
}

fun AccessibilityNodeInfo?.labelText(): String {
    if (this == null) {
        return ""
    }
    return text?.toString()?.takeIf { it.isNotBlank() }
        ?: contentDescription?.toString()?.takeIf { it.isNotBlank() }
        ?: ""
}

fun AccessibilityNodeInfo.findNodeByPredicate(
    predicate: (AccessibilityNodeInfo) -> Boolean
): AccessibilityNodeInfo? {
    if (predicate(this)) {
        return this
    }
    for (index in 0 until childCount) {
        getChild(index)?.findNodeByPredicate(predicate)?.let { return it }
    }
    return null
}

fun AccessibilityNodeInfo.findNodeByLabels(
    labels: Collection<String>,
    exactMatch: Boolean = false
): AccessibilityNodeInfo? {
    val candidates = labels.filter { it.isNotBlank() }
    if (candidates.isEmpty()) {
        return null
    }
    return findNodeByPredicate { node ->
        val label = node.labelText().trim()
        if (label.isEmpty()) {
            return@findNodeByPredicate false
        }
        candidates.any { candidate ->
            if (exactMatch) {
                label == candidate
            } else {
                label.contains(candidate)
            }
        }
    }
}

fun AccessibilityNodeInfo.findNodeAroundByLabels(
    labels: Collection<String>,
    exactMatch: Boolean = false,
    maxAncestorDepth: Int = 4
): AccessibilityNodeInfo? {
    var current: AccessibilityNodeInfo? = this
    repeat(maxAncestorDepth) {
        current = current?.parent ?: return null
        current?.findNodeByLabels(labels, exactMatch)?.let { return it }
    }
    return null
}

fun AccessibilityNodeInfo?.click() {
    if (this == null) {
        return
    }
    if (isClickable) {
        sleep(100)
        performAction(AccessibilityNodeInfo.ACTION_CLICK)
    } else {
        parent.click()
    }
}

fun sleep(millisecond: Long) {
    Thread.sleep(millisecond)
}

fun fullPrintNode(
    tag: String,
    parentNode: AccessibilityNodeInfo?,
    spaceCount: Int = 0
): AccessibilityNodeInfo? {
    if (parentNode == null) {
        return null
    }
    val spaceSb = StringBuilder().apply { repeat(spaceCount) { append("  ") } }
    logD("$tag: $spaceSb${parentNode.text} -> ${parentNode.viewIdResourceName} -> ${parentNode.className} -> Clickable: ${parentNode.isClickable}")
    if (parentNode.childCount == 0) {
        return null
    }
    for (i in 0 until parentNode.childCount) {
        fullPrintNode(tag, parentNode.getChild(i), spaceCount + 1)
    }
    return null
}