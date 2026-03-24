package io.github.natobytes.tesladrive.contract

import android.net.Uri
import io.github.natobytes.kmvi.contract.Action
import io.github.natobytes.kmvi.contract.Effect
import io.github.natobytes.kmvi.contract.Intent
import io.github.natobytes.kmvi.contract.State
import io.github.natobytes.tesladrive.TeslaFolder

data class SelectedFile(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val targetFolder: TeslaFolder,
)

data class ExportProgress(
    val currentFileName: String,
    val completedFiles: Int,
    val totalFiles: Int,
    val bytesTransferred: Long,
    val totalBytes: Long,
) {
    val progressFraction: Float
        get() = if (totalBytes > 0) bytesTransferred.toFloat() / totalBytes else 0f
}

data class UsbExportState(
    val usbDriveUri: Uri? = null,
    val usbDriveName: String? = null,
    val availableBytes: Long? = null,
    val selectedFiles: List<SelectedFile> = emptyList(),
    val isExporting: Boolean = false,
    val exportProgress: ExportProgress? = null,
    val error: String? = null,
    val exportCompleted: Boolean = false,
    val verifiedFiles: Int = 0,
) : State

sealed interface UsbExportIntent : Intent {
    data object OpenDrivePicker : UsbExportIntent
    data class DriveSelected(val uri: Uri, val name: String, val availableBytes: Long) : UsbExportIntent
    data class AddFile(
        val uri: Uri,
        val name: String,
        val sizeBytes: Long,
        val targetFolder: TeslaFolder,
    ) : UsbExportIntent
    data class RemoveFile(val uri: Uri) : UsbExportIntent
    data object StartExport : UsbExportIntent
    data object CancelExport : UsbExportIntent
    data object DismissError : UsbExportIntent
    data object UsbDisconnected : UsbExportIntent
}

sealed interface UsbExportAction : Action {
    data class SetDrive(val uri: Uri, val name: String, val availableBytes: Long) : UsbExportAction
    data class AddFile(val file: SelectedFile) : UsbExportAction
    data class RemoveFile(val uri: Uri) : UsbExportAction
    data class SetExporting(val isExporting: Boolean) : UsbExportAction
    data class UpdateProgress(val progress: ExportProgress) : UsbExportAction
    data class SetError(val message: String) : UsbExportAction
    data object ClearError : UsbExportAction
    data class ExportComplete(val verifiedFiles: Int) : UsbExportAction
    data object ClearDrive : UsbExportAction
}

sealed interface UsbExportEffect : Effect {
    data object LaunchDrivePicker : UsbExportEffect
    data class ShowToast(val message: String) : UsbExportEffect
}
