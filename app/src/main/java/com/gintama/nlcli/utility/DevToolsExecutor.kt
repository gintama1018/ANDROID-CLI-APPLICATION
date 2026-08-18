package com.gintama.nlcli.utility

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.wifi.WifiManager
import com.gintama.nlcli.model.ExecutionResult
import java.math.BigInteger
import java.net.InetAddress
import java.net.NetworkInterface
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import java.util.Collections
import java.util.UUID

class DevToolsExecutor(private val context: Context) {

    private val clipboardManager by lazy {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    }

    fun generateUuid(): ExecutionResult {
        val uuid = UUID.randomUUID().toString()
        copyToClipboard(uuid, "UUID")
        return ExecutionResult(
            success = true,
            message = "UUID: $uuid",
            details = "Copied to clipboard automatically"
        )
    }

    fun generateSha256(text: String): ExecutionResult {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(text.toByteArray(StandardCharsets.UTF_8))
            val hex = hash.joinToString("") { "%02x".format(it) }
            copyToClipboard(hex, "SHA-256")
            ExecutionResult(
                success = true,
                message = "SHA-256: $hex",
                details = "Copied to clipboard automatically"
            )
        } catch (e: Exception) {
            ExecutionResult(false, "SHA-256 generation failed: ${e.localizedMessage}")
        }
    }

    fun encodeBase64(text: String): ExecutionResult {
        return try {
            val encoded = Base64.getEncoder().encodeToString(text.toByteArray(StandardCharsets.UTF_8))
            copyToClipboard(encoded, "Base64")
            ExecutionResult(
                success = true,
                message = "Base64 Encoded: $encoded",
                details = "Copied to clipboard automatically"
            )
        } catch (e: Exception) {
            ExecutionResult(false, "Base64 encoding failed: ${e.localizedMessage}")
        }
    }

    fun decodeBase64(encoded: String): ExecutionResult {
        return try {
            val decodedBytes = Base64.getDecoder().decode(encoded.trim())
            val decoded = String(decodedBytes, StandardCharsets.UTF_8)
            copyToClipboard(decoded, "Base64 Decoded")
            ExecutionResult(
                success = true,
                message = "Base64 Decoded: $decoded",
                details = "Copied to clipboard automatically"
            )
        } catch (e: Exception) {
            ExecutionResult(false, "Invalid Base64 string: ${e.localizedMessage}")
        }
    }

    fun copyText(text: String): ExecutionResult {
        copyToClipboard(text, "Text")
        return ExecutionResult(
            success = true,
            message = "Copied to clipboard: '$text'"
        )
    }

    fun pasteText(): ExecutionResult {
        val cm = clipboardManager ?: return ExecutionResult(false, "Clipboard unavailable")
        val clip = cm.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString() ?: ""
            return ExecutionResult(
                success = true,
                message = "Clipboard content: '$text'"
            )
        }
        return ExecutionResult(false, "Clipboard is empty")
    }

    fun getLocalIp(): ExecutionResult {
        return try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            val ipList = mutableListOf<String>()

            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr.hostAddress?.contains(':') == false) {
                        ipList.add("${intf.displayName}: ${addr.hostAddress}")
                    }
                }
            }

            if (ipList.isNotEmpty()) {
                ExecutionResult(
                    success = true,
                    message = "Local IP: ${ipList.joinToString(" | ")}"
                )
            } else {
                ExecutionResult(
                    success = true,
                    message = "Local IP: 127.0.0.1 (No active local network interface)"
                )
            }
        } catch (e: Exception) {
            ExecutionResult(false, "Could not determine local IP: ${e.localizedMessage}")
        }
    }

    private fun copyToClipboard(text: String, label: String) {
        val clip = ClipData.newPlainText(label, text)
        clipboardManager?.setPrimaryClip(clip)
    }
}
