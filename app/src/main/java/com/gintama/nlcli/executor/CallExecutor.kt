package com.gintama.nlcli.executor

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.gintama.nlcli.contacts.ContactResolutionResult
import com.gintama.nlcli.contacts.ContactResolver
import com.gintama.nlcli.contacts.PhoneNormalizer
import com.gintama.nlcli.model.Command
import com.gintama.nlcli.model.ExecutionResult
import com.gintama.nlcli.util.Logger
import com.gintama.nlcli.util.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CallExecutor(
    private val context: Context,
    private val contactResolver: ContactResolver
) : ICommandExecutor {

    override suspend fun execute(command: Command): ExecutionResult = withContext(Dispatchers.IO) {
        val rawContact = command.contact?.trim()

        if (rawContact.isNullOrBlank()) {
            return@withContext ExecutionResult(
                success = false,
                message = "Missing recipient to call",
                details = "Syntax: call <contact>"
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
                        message = "Could not find contact '${rawContact}' to call",
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

        val hasCallPermission = PermissionHelper.hasCallPermission(context)
        val action = if (hasCallPermission) Intent.ACTION_CALL else Intent.ACTION_DIAL

        try {
            val intent = Intent(action).apply {
                data = Uri.parse("tel:$targetNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)

            if (hasCallPermission) {
                ExecutionResult(
                    success = true,
                    message = "Calling $rawContact ($targetNumber)..."
                )
            } else {
                ExecutionResult(
                    success = true,
                    message = "Opened dialer for $rawContact ($targetNumber)",
                    details = "Grant CALL_PHONE permission for direct dialing."
                )
            }
        } catch (e: Exception) {
            Logger.e("Failed to initiate call", e)
            ExecutionResult(
                success = false,
                message = "Failed to place call: ${e.localizedMessage}"
            )
        }
    }
}
