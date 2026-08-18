package com.gintama.nlcli.system

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import com.gintama.nlcli.model.ExecutionResult
import com.gintama.nlcli.util.Logger

class TorchController(private val context: Context) {

    private val cameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    }

    companion object {
        var isTorchOn: Boolean = false
            private set
    }

    fun setTorch(enable: Boolean): ExecutionResult {
        val manager = cameraManager ?: return ExecutionResult(
            success = false,
            message = "Camera / Torch service unavailable on this device"
        )

        return try {
            val cameraId = findTorchCameraId(manager) ?: return ExecutionResult(
                success = false,
                message = "No flashlight/torch found on device"
            )

            manager.setTorchMode(cameraId, enable)
            isTorchOn = enable
            Logger.i("Torch turned ${if (enable) "ON" else "OFF"}")
            ExecutionResult(
                success = true,
                message = if (enable) "Flashlight turned ON 🔦" else "Flashlight turned OFF"
            )
        } catch (e: CameraAccessException) {
            Logger.e("CameraAccessException when setting torch mode", e)
            ExecutionResult(
                success = false,
                message = "Flashlight error: ${e.localizedMessage}"
            )
        } catch (e: Exception) {
            Logger.e("Unexpected error when setting torch mode", e)
            ExecutionResult(
                success = false,
                message = "Failed to toggle torch: ${e.localizedMessage}"
            )
        }
    }

    fun toggleTorch(): ExecutionResult {
        return setTorch(!isTorchOn)
    }

    private fun findTorchCameraId(manager: CameraManager): String? {
        for (id in manager.cameraIdList) {
            val characteristics = manager.getCameraCharacteristics(id)
            val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                return id
            }
        }
        return manager.cameraIdList.firstOrNull()
    }
}
