package com.gintama.nlcli.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.gintama.nlcli.model.ExecutionResult
import com.gintama.nlcli.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class NLCliAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var automationJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isServiceRunning = true
        Logger.i("NLCliAccessibilityService connected and ready")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkg = event.packageName?.toString() ?: return
        if (pkg != "com.whatsapp" && pkg != "com.whatsapp.w4b") {
            return
        }

        // If an automated send request is active and pending execution
        val activeRequest = pendingRequest
        if (activeRequest != null && activeRequest.isPending) {
            Logger.d("WhatsApp event detected: ${event.eventType}. Processing pending send request...")
            triggerSendAutomation(activeRequest)
        }
    }

    override fun onInterrupt() {
        Logger.w("NLCliAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        if (instance === this) {
            instance = null
        }
        serviceScope.cancel()
        Logger.i("NLCliAccessibilityService destroyed")
    }

    private fun triggerSendAutomation(request: AutomationRequest) {
        if (automationJob?.isActive == true) {
            return
        }

        automationJob = serviceScope.launch {
            request.isPending = false
            Logger.d("Starting send button search for request ID: ${request.id}")

            val startTime = SystemClock.uptimeMillis()
            val timeoutMs = 4000L
            val pollIntervalMs = 300L
            var clicked = false
            var attempts = 0

            while (SystemClock.uptimeMillis() - startTime < timeoutMs) {
                attempts++
                val rootNode = rootInActiveWindow

                if (rootNode != null) {
                    val sendNode = NodeFinder.findSendButton(rootNode)
                    if (sendNode != null) {
                        // Attempt click on node or nearest clickable parent
                        clicked = performClick(sendNode)
                        if (clicked) {
                            Logger.i("Successfully clicked WhatsApp send button after $attempts attempts (${SystemClock.uptimeMillis() - startTime}ms)")
                            val result = ExecutionResult(
                                success = true,
                                message = "WhatsApp message sent to ${request.contactName} hands-free",
                                details = "Clicked send button after $attempts polling cycles"
                            )
                            _automationResults.emit(result)
                            pendingRequest = null
                            return@launch
                        }
                    }
                }

                delay(pollIntervalMs)
            }

            // If we reached here, send button was not found within timeout
            val rootDump = NodeFinder.dumpHierarchy(rootInActiveWindow, maxDepth = 3)
            Logger.w("Could not find WhatsApp send button within ${timeoutMs}ms. Node dump:\n$rootDump")

            val failureResult = ExecutionResult(
                success = false,
                message = "WhatsApp opened, but could not automatically tap Send (UI may have changed).",
                details = "Timeout after ${timeoutMs}ms ($attempts attempts). Please tap Send manually."
            )
            _automationResults.emit(failureResult)
            pendingRequest = null
        }
    }

    private fun performClick(node: AccessibilityNodeInfo): Boolean {
        var target: AccessibilityNodeInfo? = node
        while (target != null) {
            if (target.isClickable) {
                val actionTaken = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (actionTaken) return true
            }
            target = target.parent
        }
        // Fallback: try click action directly even if not marked clickable
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    data class AutomationRequest(
        val id: String = System.currentTimeMillis().toString(),
        val contactName: String,
        var isPending: Boolean = true,
        val timestampMs: Long = System.currentTimeMillis()
    )

    companion object {
        var instance: NLCliAccessibilityService? = null
            private set

        var isServiceRunning: Boolean = false
            private set

        var pendingRequest: AutomationRequest? = null
            private set

        private val _automationResults = MutableSharedFlow<ExecutionResult>(extraBufferCapacity = 8)
        val automationResults: SharedFlow<ExecutionResult> = _automationResults.asSharedFlow()

        fun registerPendingSend(contactName: String): String {
            val req = AutomationRequest(contactName = contactName)
            pendingRequest = req
            Logger.d("Registered pending send request for '$contactName'")
            return req.id
        }

        fun cancelPendingSend() {
            pendingRequest = null
        }
    }
}
