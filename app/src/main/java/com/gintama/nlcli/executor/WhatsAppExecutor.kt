package com.gintama.nlcli.executor

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.gintama.nlcli.accessibility.NLCliAccessibilityService
import com.gintama.nlcli.contacts.ContactResolutionResult
import com.gintama.nlcli.contacts.ContactResolver
import com.gintama.nlcli.contacts.PhoneNormalizer
import com.gintama.nlcli.model.Command
import com.gintama.nlcli.model.ExecutionResult
import com.gintama.nlcli.util.Logger
import com.gintama.nlcli.util.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class WhatsAppExecutor(
    private val context: Context,
    private val contactResolver: ContactResolver
) : ICommandExecutor {

    override suspend fun execute(command: Command): ExecutionResult = withContext(Dispatchers.IO) {
        val rawContact = command.contact?.trim()
        val message = command.payload?.trim()

        if (rawContact.isNullOrBlank()) {
            return@withContext ExecutionResult(
                success = false,
                message = "Missing recipient for WhatsApp message",
                details = "Syntax: send whatsapp to <contact>: <message>"
            )
        }

        if (message.isNullOrBlank()) {
            return@withContext ExecutionResult(
                success = false,
                message = "Missing message body for WhatsApp message",
                details = "Syntax: send whatsapp to <contact>: <message>"
            )
        }

        // Check if WhatsApp is installed
        val isStandardInstalled = isPackageInstalled("com.whatsapp")
        val isBusinessInstalled = isPackageInstalled("com.whatsapp.w4b")

        if (!isStandardInstalled && !isBusinessInstalled) {
            return@withContext ExecutionResult(
                success = false,
                message = "WhatsApp is not installed on this device",
                details = "Neither com.whatsapp nor com.whatsapp.w4b were found"
            )
        }

        val targetPackage = if (isStandardInstalled) "com.whatsapp" else "com.whatsapp.w4b"
        val isAccessibilityEnabled = PermissionHelper.isAccessibilityServiceEnabled(context)

        // Guard against intent collision if another automated send job is actively inspecting WhatsApp UI
        if (isAccessibilityEnabled && NLCliAccessibilityService.isAutomationBusy) {
            return@withContext ExecutionResult(
                success = false,
                message = "WhatsApp automation is currently in progress for another message. Please wait a moment.",
                details = "Wait for the active hands-free send to complete before dispatching another message."
            )
        }

        // Resolve contact
        val resolvedPhone = when (val resolution = contactResolver.resolveContact(rawContact)) {
            is ContactResolutionResult.Found -> {
                Logger.d("Resolved contact '${rawContact}' -> '${resolution.contact.displayName}' (${Logger.maskPhoneNumber(resolution.contact.normalizedPhoneNumber)})")
                PhoneNormalizer.toWhatsAppUrlNumber(resolution.contact.normalizedPhoneNumber)
            }
            is ContactResolutionResult.Ambiguous -> {
                Logger.d("Ambiguous contact match for '${rawContact}', selecting best match '${resolution.bestMatch.displayName}'")
                PhoneNormalizer.toWhatsAppUrlNumber(resolution.bestMatch.normalizedPhoneNumber)
            }
            is ContactResolutionResult.NotFound -> {
                if (PhoneNormalizer.isValidPhoneNumber(rawContact)) {
                    PhoneNormalizer.toWhatsAppUrlNumber(rawContact)
                } else {
                    return@withContext ExecutionResult(
                        success = false,
                        message = "Could not find contact '${rawContact}' in contacts",
                        details = resolution.reason
                    )
                }
            }
            is ContactResolutionResult.PermissionDenied -> {
                return@withContext ExecutionResult(
                    success = false,
                    message = "Contacts permission not granted",
                    details = resolution.message
                )
            }
        }

        if (resolvedPhone.isBlank()) {
            return@withContext ExecutionResult(
                success = false,
                message = "Invalid phone number resolved for '${rawContact}'"
            )
        }

        // Sanitize and URL-encode payload
        val encodedMessage = try {
            URLEncoder.encode(message, StandardCharsets.UTF_8.name())
        } catch (e: Exception) {
            Logger.e("Failed to encode message for URL", e)
            return@withContext ExecutionResult(
                success = false,
                message = "Failed to encode message text for WhatsApp intent"
            )
        }

        val url = "https://wa.me/$resolvedPhone?text=$encodedMessage"
        Logger.d("Launching WhatsApp wa.me URL targeting $targetPackage")

        // Register pending send in accessibility service if enabled
        if (isAccessibilityEnabled) {
            NLCliAccessibilityService.registerPendingSend(rawContact)
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage(targetPackage)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)

            if (isAccessibilityEnabled) {
                ExecutionResult(
                    success = true,
                    message = "Dispatching WhatsApp message to $rawContact...",
                    details = "Auto-send active via Accessibility Service"
                )
            } else {
                ExecutionResult(
                    success = true,
                    message = "Opened WhatsApp chat with pre-filled message for $rawContact",
                    details = "Accessibility Service is not enabled. Tap Send manually or enable NLCLI in Accessibility Settings for 100% hands-free automation."
                )
            }
        } catch (e: Exception) {
            NLCliAccessibilityService.cancelPendingSend()
            Logger.e("Failed to launch WhatsApp intent", e)
            ExecutionResult(
                success = false,
                message = "Failed to open WhatsApp: ${e.localizedMessage}",
                details = e.stackTraceToString()
            )
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}
