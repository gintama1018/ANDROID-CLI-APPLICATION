package com.gintama.nlcli.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import com.gintama.nlcli.util.Logger
import java.util.ArrayDeque

object NodeFinder {

    // Known WhatsApp send button view ID resource names across versions
    private val KNOWN_SEND_VIEW_IDS = listOf(
        "com.whatsapp:id/send",
        "com.whatsapp:id/send_button",
        "com.whatsapp.w4b:id/send",
        "com.whatsapp.w4b:id/send_button"
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
        sb.appendLine("${indent}[${node.className}] id=${node.viewIdResourceName} desc='${node.contentDescription}' text='${node.text}' clickable=${node.isClickable}")
        for (i in 0 until node.childCount) {
            dumpNode(node.getChild(i), sb, depth + 1, maxDepth)
        }
    }
}
