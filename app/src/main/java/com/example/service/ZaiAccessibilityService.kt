package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Real Android AccessibilityService that grants the Z.ai AI Agent full autonomous
 * capability to inspect screen UI nodes, click buttons by text, perform coordinate taps,
 * swipe gestures, and trigger global Android navigation actions.
 */
class ZaiAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        _instance.value = this
        _isConnected.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        _activePackage.value = pkg

        val root = rootInActiveWindow
        if (root != null) {
            val nodes = mutableListOf<String>()
            var count = 0
            fun traverse(node: AccessibilityNodeInfo?, depth: Int) {
                if (node == null || count > 80) return
                count++
                val text = node.text?.toString()?.trim().orEmpty()
                val desc = node.contentDescription?.toString()?.trim().orEmpty()
                val cls = node.className?.toString()?.substringAfterLast('.') ?: "View"
                if (text.isNotEmpty() || desc.isNotEmpty()) {
                    val label = if (text.isNotEmpty()) text else desc
                    nodes.add("[$cls] $label")
                }
                for (i in 0 until node.childCount) {
                    traverse(node.getChild(i), depth + 1)
                }
            }
            traverse(root, 0)
            _nodeCount.value = count
            if (nodes.isNotEmpty()) {
                _screenTreeSnapshot.value = nodes.take(35)
            }
        }
    }

    override fun onInterrupt() {
        // Handle service interruption gracefully
    }

    override fun onDestroy() {
        super.onDestroy()
        _instance.value = null
        _isConnected.value = false
    }

    fun performTap(x: Float, y: Float): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 90L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    fun performSwipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 320L): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs.coerceIn(80L, 2000L))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    fun performGlobalNavigation(action: Int): Boolean {
        return performGlobalAction(action)
    }

    fun clickNodeByText(targetText: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val matches = root.findAccessibilityNodeInfosByText(targetText)
        if (matches.isNullOrEmpty()) return false
        for (node in matches) {
            if (node.isClickable) {
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) {
                    return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                parent = parent.parent
            }
        }
        return false
    }

    fun inputTextIntoFocusedField(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return focus.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun captureCurrentUiSummary(): String {
        val root = rootInActiveWindow
        if (root == null) {
            return "Package: ${_activePackage.value} | Nodes: ${_screenTreeSnapshot.value.joinToString(" ; ")}"
        }
        val items = mutableListOf<String>()
        fun collect(node: AccessibilityNodeInfo?) {
            if (node == null || items.size >= 40) return
            val t = node.text?.toString()?.trim().orEmpty()
            val d = node.contentDescription?.toString()?.trim().orEmpty()
            if (t.isNotEmpty()) items.add(t)
            else if (d.isNotEmpty()) items.add("($d)")
            for (i in 0 until node.childCount) {
                collect(node.getChild(i))
            }
        }
        collect(root)
        return "ActivePkg=${root.packageName ?: _activePackage.value} | Elements=${items.joinToString(", ")}"
    }

    companion object {
        private val _instance = MutableStateFlow<ZaiAccessibilityService?>(null)
        val instance: StateFlow<ZaiAccessibilityService?> = _instance.asStateFlow()

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        private val _activePackage = MutableStateFlow("com.example (Z.ai AgentOS)")
        val activePackage: StateFlow<String> = _activePackage.asStateFlow()

        private val _nodeCount = MutableStateFlow(18)
        val nodeCount: StateFlow<Int> = _nodeCount.asStateFlow()

        private val _screenTreeSnapshot = MutableStateFlow<List<String>>(
            listOf(
                "[WebView] جلسة Z.ai النشطة (https://chat.z.ai)",
                "[Bridge] window.ZaiPhoneAgent متصل وجاهز",
                "[Controller] بوابة التحكم التلقائي بالهاتف"
            )
        )
        val screenTreeSnapshot: StateFlow<List<String>> = _screenTreeSnapshot.asStateFlow()
    }
}
