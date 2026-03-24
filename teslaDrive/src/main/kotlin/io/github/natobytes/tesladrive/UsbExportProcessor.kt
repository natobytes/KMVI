package io.github.natobytes.tesladrive

import io.github.natobytes.kmvi.Processor
import io.github.natobytes.kmvi.contract.Result
import io.github.natobytes.tesladrive.contract.ExportProgress
import io.github.natobytes.tesladrive.contract.SelectedFile
import io.github.natobytes.tesladrive.contract.UsbExportAction
import io.github.natobytes.tesladrive.contract.UsbExportEffect
import io.github.natobytes.tesladrive.contract.UsbExportIntent
import io.github.natobytes.tesladrive.contract.UsbExportState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class UsbExportProcessor(
    private val driveWriter: UsbDriveWriter,
) : Processor<UsbExportIntent, UsbExportState> {

    override fun process(input: UsbExportIntent, state: UsbExportState): Flow<Result> = flow {
        when (input) {
            is UsbExportIntent.OpenDrivePicker -> {
                emit(UsbExportEffect.LaunchDrivePicker)
            }

            is UsbExportIntent.DriveSelected -> {
                emit(UsbExportAction.SetDrive(input.uri, input.name, input.availableBytes))
            }

            is UsbExportIntent.AddFile -> {
                val file = SelectedFile(
                    uri = input.uri,
                    name = input.name,
                    sizeBytes = input.sizeBytes,
                    targetFolder = input.targetFolder,
                )
                emit(UsbExportAction.AddFile(file))
            }

            is UsbExportIntent.RemoveFile -> {
                emit(UsbExportAction.RemoveFile(input.uri))
            }

            is UsbExportIntent.StartExport -> {
                val driveUri = state.usbDriveUri
                if (driveUri == null) {
                    emit(UsbExportAction.SetError("No USB drive connected. Please select a drive first."))
                    return@flow
                }
                if (state.selectedFiles.isEmpty()) {
                    emit(UsbExportAction.SetError("No files selected for export."))
                    return@flow
                }

                emit(UsbExportAction.SetExporting(true))

                try {
                    driveWriter.exportFiles(driveUri, state.selectedFiles).collect { progress ->
                        emit(UsbExportAction.UpdateProgress(progress))
                    }
                    val verified = driveWriter.verifyFiles(driveUri, state.selectedFiles)
                    emit(UsbExportAction.ExportComplete(verified))
                    emit(
                        UsbExportEffect.ShowToast(
                            "Export complete! $verified/${state.selectedFiles.size} files verified."
                        )
                    )
                } catch (e: Exception) {
                    emit(UsbExportAction.SetError(e.message ?: "Export failed"))
                }
            }

            is UsbExportIntent.CancelExport -> {
                emit(UsbExportAction.SetExporting(false))
            }

            is UsbExportIntent.DismissError -> {
                emit(UsbExportAction.ClearError)
            }

            is UsbExportIntent.UsbDisconnected -> {
                emit(UsbExportAction.ClearDrive)
                emit(UsbExportEffect.ShowToast("USB drive disconnected"))
            }
        }
    }
}
