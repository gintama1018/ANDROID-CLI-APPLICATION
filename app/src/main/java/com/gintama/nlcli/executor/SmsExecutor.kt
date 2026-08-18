package com.gintama.nlcli.executor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import com.gintama.nlcli.contacts.ContactResolutionResult
import com.gintama.nlcli.contacts.ContactResolver
import com.gintama.nlcli.contacts.PhoneNormalizer
import com.gintama.nlcli.model.Command
import com.gintama.nlcli.model.ExecutionResult
import com.gintama.nlcli.util.Logger
import com.gintama.nlcli.util.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsExecutor(
    private val context: Context,
    private val contactResolver: ContactResolver
) : ICommandExecutor {

    override suspend fun execute(command: Command): ExecutionResult = withContext(Dispatchers.IO) {
        val rawContact = command.contact?.trim()
        val message = command.payload?.trim()

        if (rawContact.isNullOrBlank()) {
            return@withContext ExecutionResult(
                success = false,
                message = "Missing recipient for SMS",
                details = "Syntax: send sms to <contact>: <message>"
            )
        }

        if (message.isNullOrBlank()) {
            return@withContext ExecutionResult(
                success = false,
                message = "Missing message body for SMS",
                details = "Syntax: send sms to <contact>: <message>"
            )
        }

        // Resolve contact
        val targetNumber = when (val resolution = contactResolver.resolveContact(rawContact)) {
            is ContactResolutionResult.Found -> resolution.contact.normalizedPhoneNumber
            is ContactResolutionResult.Ambiguous -> resolution.bestMatch.normalizedPhoneNumber
            is ContactResolutionResult.NotFound -> {
                if (PhoneNormalizer.isValidPhoneNumber(rawContact)) {
                    PhoneNormalizer.normalizeToE164(rawContact)
                } else {
                    return@withContext ExecutionResult(
                        success = false,
                        message = "Could not find contact '${rawContact}' for SMS",
                        details = resolution.reason
                    )
                }
            }
            is ContactResolutionResult.PermissionDenied -> {
                if (PhoneNormalizer.isValidPhoneNumber(rawContact)) {
                    PhoneNormalizer.normalizeToE164(rawContact)
                } else {
                    return@withContext ExecutionResult(
                        success = false,
                        message = "Contacts permission required to resolve '${rawContact}'",
                        details = resolution.message
                    )
                }
            }
        }

        // Direct SMS via SmsManager if permission granted
        if (PermissionHelper.hasSmsPermission(context)) {
            try {
                val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }

                val parts = smsManager.divideMessage(message)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(targetNumber, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(targetNumber, null, message, null, null)
                }

                Logger.i("Direct SMS sent to $targetNumber")
                return@withContext ExecutionResult(
                    success = true,
                    message = "SMS sent successfully to $rawContact ($targetNumber)"
                )
            } catch (e: Exception) {
                Logger.e("Failed to send direct SMS, falling back to intent", e)
            }
        }

        // Fallback: Open SMS app with prefilled body and number
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$targetNumber")
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)

            ExecutionResult(
                success = true,
                message = "Opened SMS app to $rawContact ($targetNumber)",
                details = "Grant SEND_SMS permission for background direct SMS sending without opening the messaging app."
            )
        } catch (e: Exception) {
            Logger.e("Failed to open SMS app", e)
            ExecutionResult(
                success = false,
                message = "Failed to send SMS: ${e.localizedMessage}"
            )
        }
    }
}
