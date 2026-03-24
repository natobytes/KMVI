package io.github.natobytes.tesladrive

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import io.github.natobytes.kmvi.KMVIViewModel
import io.github.natobytes.tesladrive.contract.UsbExportAction
import io.github.natobytes.tesladrive.contract.UsbExportEffect
import io.github.natobytes.tesladrive.contract.UsbExportIntent
import io.github.natobytes.tesladrive.contract.UsbExportState

class UsbExportViewModel(
    driveWriter: UsbDriveWriter,
) : KMVIViewModel<UsbExportIntent, UsbExportAction, UsbExportEffect, UsbExportState>(
    initialState = UsbExportState(),
    processor = UsbExportProcessor(driveWriter),
    reducer = UsbExportReducer(),
) {
    override fun onError(throwable: Throwable) {
        process(UsbExportIntent.DismissError)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return UsbExportViewModel(UsbDriveWriter(application.applicationContext)) as T
            }
        }
    }
}
