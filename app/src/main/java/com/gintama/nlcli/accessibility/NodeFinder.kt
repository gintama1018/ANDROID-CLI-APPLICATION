package com.gintama.nlcli.accessibility

import android.graphics.Point
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.gintama.nlcli.util.Logger
import java.util.ArrayDeque

object NodeFinder {

    // Known WhatsApp send button view ID resource names across versions
    private val KNOWN_SEND_VIEW_IDS = listOf(
        "com.whatsapp:id/send",
        "com.whatsapp:id/send_button",
        "com.whatsapp:id/send_btn",
        "com.whatsapp.w4b:id/send",
        "com.whatsapp.w4b:id/send_button",
        "com.whatsapp.w4b:id/send_btn"
    )

    // Localized and standard content descriptions for the Send button
    private val SEND_CONTENT_DESCRIPTIONS = listOf(
        "send",
        "send message",
        "भेजें",           // Hindi
        "भेजो",
        "enviar",          // Spanish / Portuguese
        "envoyer",         // French
        "senden",          // German
        "invia",           // Italian
        "kirim",           // Indonesian
        "gửi",             // Vietnamese
        "ارسال",          // Arabic
        "отправить"        // Russian
    )

    // Known intermediary "Continue to chat" button descriptors when opening wa.me on new contacts
    private val KNOWN_CONTINUE_VIEW_IDS = listOf(
        "com.whatsapp:id/continue_to_chat",
        "com.whatsapp:id/action_button",
        "com.whatsapp:id/btn_action",
        "com.whatsapp:id/primary_action",
        "com.whatsapp.w4b:id/continue_to_chat",
        "com.whatsapp.w4b:id/action_button"
    )

    private val CONTINUE_DESCRIPTIONS = listOf(
        "continue to chat",
        "continue",
        "chat",
        "चैट जारी रखें",
        "जारी रखें",
        "continuar al chat",
        "continuar",
        "continuer vers la discussion",
        "weiter zum chat"
    )

    /**
     * Searches for WhatsApp's Send button node in the active accessibility tree using BFS.
     */
    fun findSendButton(rootNode: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (rootNode == null) return null

        // 1. First priority: Try direct view ID lookup via AccessibilityNodeInfo API
        for (viewId in KNOWN_SEND_VIEW_IDS) {
            val matchingNodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
            if (!matchingNodes.isNullOrEmpty()) {
                val clickableNode = matchingNodes.firstOrNull { it.isClickable } ?: matchingNodes.first()
                Logger.d("Found Send button by viewId: $viewId")
                return clickableNode
            }
        }

        // 2. Second priority: BFS traversal matching view ID or contentDescription
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(rootNode)

        var foundNode: AccessibilityNodeInfo? = null

        while (queue.isNotEmpty()) {
            val current = queue.poll() ?: continue

            if (isSendButtonNode(current)) {
                foundNode = current
                break
            }

            for (i in 0 until current.childCount) {
                val child = current.getChild(i)
                if (child != null) {
                    queue.add(child)
                }
            }
        }

        return foundNode
    }

    /**
     * Searches for WhatsApp's intermediary "Continue to chat" node when wa.me opens a new thread.
     */
    fun findContinueToChatButton(rootNode: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (rootNode == null) return null

        for (viewId in KNOWN_CONTINUE_VIEW_IDS) {
            val matchingNodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
            if (!matchingNodes.isNullOrEmpty()) {
                Logger.d("Found Continue-to-chat button by viewId: $viewId")
                return matchingNodes.firstOrNull { it.isClickable } ?: matchingNodes.first()
            }
        }

        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(rootNode)

        while (queue.isNotEmpty()) {
            val current = queue.poll() ?: continue

            if (isContinueNode(current)) {
                Logger.d("Found Continue-to-chat button via hierarchy walk: desc='${current.contentDescription}' text='${current.text}'")
                return current
            }

            for (i in 0 until current.childCount) {
                val child = current.getChild(i)
                if (child != null) {
                    queue.add(child)
                }
            }
        }

        return null
    }

    private fun isSendButtonNode(node: AccessibilityNodeInfo): Boolean {
        // Check view id
        val viewId = node.viewIdResourceName
        if (viewId != null && KNOWN_SEND_VIEW_IDS.any { viewId.equals(it, ignoreCase = true) }) {
            return true
        }

        // Check content description
        val contentDesc = node.contentDescription?.toString()?.trim()?.lowercase()
        if (contentDesc != null) {
            if (SEND_CONTENT_DESCRIPTIONS.any { contentDesc == it || contentDesc.startsWith(it) }) {
                return true
            }
        }

        // Check node text if view is a button
        val text = node.text?.toString()?.trim()?.lowercase()
        if (text != null) {
            if (SEND_CONTENT_DESCRIPTIONS.any { text == it }) {
                return true
            }
        }

        return false
    }

    private fun isContinueNode(node: AccessibilityNodeInfo): Boolean {
        val viewId = node.viewIdResourceName
        if (viewId != null && KNOWN_CONTINUE_VIEW_IDS.any { viewId.equals(it, ignoreCase = true) }) {
            return true
        }

        val contentDesc = node.contentDescription?.toString()?.trim()?.lowercase()
        if (contentDesc != null && CONTINUE_DESCRIPTIONS.any { contentDesc.contains(it) }) {
            return true
        }

        val text = node.text?.toString()?.trim()?.lowercase()
        if (text != null && CONTINUE_DESCRIPTIONS.any { text.contains(it) }) {
            return true
        }

        return false
    }

    /**
     * Extracts screen center coordinates of an AccessibilityNodeInfo for coordinate-based gesture tapping.
     */
    fun getNodeCenterCoordinates(node: AccessibilityNodeInfo?): Point? {
        if (node == null) return null
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (rect.width() <= 0 || rect.height() <= 0) return null
        return Point(rect.centerX(), rect.centerY())
    }

    /**
     * Dumps the node hierarchy into a readable string for debugging selector issues.
     */
    fun dumpHierarchy(rootNode: AccessibilityNodeInfo?, maxDepth: Int = 4): String {
        if (rootNode == null) return "<Root node is null>"
        val sb = StringBuilder()
        dumpNode(rootNode, sb, 0, maxDepth)
        return sb.toString()
    }

    private fun dumpNode(node: AccessibilityNodeInfo?, sb: StringBuilder, depth: Int, maxDepth: Int) {
        if (node == null || depth > maxDepth) return
        val indent = "  ".repeat(depth)
        val rect = Rect()
        node.getBoundsInScreen(rect)
        sb.appendLine("${indent}[${node.className}] id=${node.viewIdResourceName} desc='${node.contentDescription}' text='${node.text}' clickable=${node.isClickable} bounds=$rect")
        for (i in 0 until node.childCount) {
            dumpNode(node.getChild(i), sb, depth + 1, maxDepth)
        }
    }
}
