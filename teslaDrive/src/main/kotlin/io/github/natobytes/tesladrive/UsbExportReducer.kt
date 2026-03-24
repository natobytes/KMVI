package io.github.natobytes.tesladrive

import io.github.natobytes.kmvi.Reducer
import io.github.natobytes.tesladrive.contract.UsbExportAction
import io.github.natobytes.tesladrive.contract.UsbExportState

class UsbExportReducer : Reducer<UsbExportAction, UsbExportState> {
    override fun reduce(action: UsbExportAction, state: UsbExportState): UsbExportState =
        when (action) {
            is UsbExportAction.SetDrive -> state.copy(
                usbDriveUri = action.uri,
                usbDriveName = action.name,
                availableBytes = action.availableBytes,
                exportCompleted = false,
                verifiedFiles = 0,
            )
            is UsbExportAction.AddFile -> state.copy(
                selectedFiles = state.selectedFiles + action.file,
            )
            is UsbExportAction.RemoveFile -> state.copy(
                selectedFiles = state.selectedFiles.filter { it.uri != action.uri },
            )
            is UsbExportAction.SetExporting -> state.copy(
                isExporting = action.isExporting,
                exportProgress = if (!action.isExporting) null else state.exportProgress,
            )
            is UsbExportAction.UpdateProgress -> state.copy(exportProgress = action.progress)
            is UsbExportAction.SetError -> state.copy(error = action.message, isExporting = false)
            is UsbExportAction.ClearError -> state.copy(error = null)
            is UsbExportAction.ExportComplete -> state.copy(
                isExporting = false,
                exportProgress = null,
                exportCompleted = true,
                verifiedFiles = action.verifiedFiles,
                selectedFiles = emptyList(),
            )
            is UsbExportAction.ClearDrive -> UsbExportState()
        }
}
