package com.shark.kty

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import roro.stellar.manager.adb.AdbPairingService

/**
 * Reads the six-digit pairing code from the Settings pairing dialog while it is
 * still in the foreground, so the user does not have to type it.
 */
class PairingAccessibilityService : AccessibilityService() {
    companion object {
        private const val TAG = "SharkA11y"
    }

    private var lastCode: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "accessibility connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName?.toString() != "com.android.settings") return
        if (!AdbPairingService.pairingActive) return
        val root = rootInActiveWindow ?: return
        val code = extractCode(root) ?: return
        if (code == lastCode) return
        lastCode = code
        Log.i(TAG, "pairing code detected; submitting")
        startForegroundService(AdbPairingService.pairIntent(this, code))
    }

    override fun onInterrupt() {
    }

    private fun extractCode(root: AccessibilityNodeInfo): String? {
        val texts = mutableListOf<String>()
        collect(root, texts, 0)
        if (texts.isEmpty()) return null
        val joined = texts.joinToString(" ").lowercase()
        val looksLikePairing = joined.contains("pair") || joined.contains("配对") ||
            joined.contains("配对码")
        if (!looksLikePairing) return null
        return texts.firstOrNull { it.matches(Regex("\\d{6}")) }
    }

    private fun collect(node: AccessibilityNodeInfo?, out: MutableList<String>, depth: Int) {
        if (node == null || depth > 30) return
        node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { out.add(it) }
        for (index in 0 until node.childCount) {
            collect(node.getChild(index), out, depth + 1)
        }
    }
}
