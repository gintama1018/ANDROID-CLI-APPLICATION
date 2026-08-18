package com.gintama.nlcli.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Point
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.gintama.nlcli.model.ExecutionResult
import com.gintama.nlcli.util.Logger
import kotlinx.coroutines.CompletableDeferred
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
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentLinkedQueue

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

        // Trigger queue processing if there are pending requests
        checkAndProcessQueue()
    }

    override fun onInterrupt() {
        Logger.w("NLCliAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        isAutomationBusy = false
        if (instance === this) {
            instance = null
        }
        serviceScope.cancel()
        requestQueue.clear()
        Logger.i("NLCliAccessibilityService destroyed")
    }

    private fun checkAndProcessQueue() {
        if (automationJob?.isActive == true) {
            return
        }

        val nextRequest = requestQueue.peek() ?: return
        triggerSendAutomation(nextRequest)
    }

    private fun triggerSendAutomation(request: AutomationRequest) {
        if (automationJob?.isActive == true) {
            return
        }

        automationJob = serviceScope.launch {
            isAutomationBusy = true
            Logger.d("Starting robust auto-send search for request: '${request.contactName}' (ID: ${request.id})")

            val startTime = SystemClock.uptimeMillis()
            val timeoutMs = 15000L // 15s budget for screen render and intermediary steps
            val pollIntervalMs = 300L
            var attempts = 0
            var intermediateTapped = false

            try {
                while (SystemClock.uptimeMillis() - startTime < timeoutMs) {
                    attempts++
                    val rootNode = rootInActiveWindow

                    if (rootNode != null) {
                        // Step 1: Check for the primary Send button
                        val sendNode = NodeFinder.findSendButton(rootNode)
                        if (sendNode != null) {
                            val coords = NodeFinder.getNodeCenterCoordinates(sendNode)
                            Logger.d("Send button detected (bounds center: $coords). Executing physical tap...")

                            // Primary click: Coordinate-based synthetic gesture tap
                            var tapSuccess = false
                            if (coords != null) {
                                tapSuccess = dispatchTapGesture(coords.x.toFloat(), coords.y.toFloat())
                            }

                            // Fallback/parallel click: standard accessibility action click
                            val actionSuccess = performClickAction(sendNode)

                            Logger.i("Executed click on Send button (gestureTap=$tapSuccess, actionClick=$actionSuccess) after $attempts polling cycles (${SystemClock.uptimeMillis() - startTime}ms)")

                            val result = ExecutionResult(
                                success = true,
                                message = "WhatsApp message sent to ${request.contactName} hands-free",
                                details = "Auto-clicked Send button (attempt $attempts, ${SystemClock.uptimeMillis() - startTime}ms)"
                            )
                            _automationResults.emit(result)
                            requestQueue.remove(request)

                            // Check if more requests are in queue
                            if (requestQueue.isNotEmpty()) {
                                delay(500L)
                                checkAndProcessQueue()
                            }
                            return@launch
                        }

                        // Step 2: If Send button not yet visible, check for intermediary "Continue to chat"
                        if (!intermediateTapped) {
                            val continueNode = NodeFinder.findContinueToChatButton(rootNode)
                            if (continueNode != null) {
                                val continueCoords = NodeFinder.getNodeCenterCoordinates(continueNode)
                                Logger.d("Intermediary 'Continue to chat' screen detected. Tapping at $continueCoords...")

                                if (continueCoords != null) {
                                    dispatchTapGesture(continueCoords.x.toFloat(), continueCoords.y.toFloat())
                                }
                                performClickAction(continueNode)
                                intermediateTapped = true

                                // Give WhatsApp 600ms to transition into the chat thread
                                delay(600L)
                                continue
                            }
                        }
                    }

                    delay(pollIntervalMs)
                }

                // Timeout exceeded - only dump node hierarchy if verbose logging is enabled
                if (Logger.verboseLoggingEnabled) {
                    val rootDump = NodeFinder.dumpHierarchy(rootInActiveWindow, maxDepth = 3)
                    Logger.w("Could not find WhatsApp send button within ${timeoutMs}ms. Node dump:\n$rootDump")
                } else {
                    Logger.w("Could not find WhatsApp send button within ${timeoutMs}ms (timeout)")
                }

                val failureResult = ExecutionResult(
                    success = false,
                    message = "WhatsApp opened, but could not automatically tap Send.",
                    details = "Timeout after ${timeoutMs / 1000}s. Please tap Send manually."
                )
                _automationResults.emit(failureResult)
                requestQueue.remove(request)

                if (requestQueue.isNotEmpty()) {
                    delay(500L)
                    checkAndProcessQueue()
                }
            } finally {
                isAutomationBusy = false
            }
        }
    }

    /**
     * Dispatches a synthetic touch gesture at specified screen coordinates.
     * This bypasses WhatsApp's raw touch event handlers that ignore standard ACTION_CLICK.
     */
    private suspend fun dispatchTapGesture(x: Float, y: Float): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 60)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val deferred = CompletableDeferred<Boolean>()

        val dispatched = dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    Logger.d("Gesture tap completed successfully at ($x, $y)")
                    deferred.complete(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    Logger.w("Gesture tap cancelled at ($x, $y)")
                    deferred.complete(false)
                }
            },
            null
        )

        if (!dispatched) {
            Logger.w("dispatchGesture returned false immediately")
            return false
        }

        return withTimeoutOrNull(500L) { deferred.await() } ?: false
    }

    private fun performClickAction(node: AccessibilityNodeInfo): Boolean {
        var target: AccessibilityNodeInfo? = node
        while (target != null) {
            if (target.isClickable) {
                val actionTaken = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (actionTaken) return true
            }
            target = target.parent
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    data class AutomationRequest(
        val id: String = System.currentTimeMillis().toString(),
        val contactName: String,
        val timestampMs: Long = System.currentTimeMillis()
    )

    companion object {
        var instance: NLCliAccessibilityService? = null
            private set

        var isServiceRunning: Boolean = false
            private set

        var isAutomationBusy: Boolean = false
            private set

        private val requestQueue = ConcurrentLinkedQueue<AutomationRequest>()

        private val _automationResults = MutableSharedFlow<ExecutionResult>(extraBufferCapacity = 8)
        val automationResults: SharedFlow<ExecutionResult> = _automationResults.asSharedFlow()

        fun registerPendingSend(contactName: String): String {
            val req = AutomationRequest(contactName = contactName)
            requestQueue.add(req)
            Logger.d("Enqueued pending send request for '$contactName' (Queue size: ${requestQueue.size})")
            instance?.checkAndProcessQueue()
            return req.id
        }

        fun cancelPendingSend() {
            requestQueue.clear()
            isAutomationBusy = false
        }
    }
}
