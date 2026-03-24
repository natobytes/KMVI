package io.github.natobytes.tesladrive

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.StatFs
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.natobytes.tesladrive.contract.ExportProgress
import io.github.natobytes.tesladrive.contract.SelectedFile
import io.github.natobytes.tesladrive.contract.UsbExportEffect
import io.github.natobytes.tesladrive.contract.UsbExportIntent
import io.github.natobytes.tesladrive.contract.UsbExportState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TeslaDriveApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeslaDriveApp(
    vm: UsbExportViewModel = viewModel(factory = UsbExportViewModel.Factory),
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    // Declare all remember state vars first, before launchers that reference them
    var pendingFileUri: Uri? by remember { mutableStateOf(null) }
    var pendingFileName: String by remember { mutableStateOf("") }
    var pendingFileSize: Long by remember { mutableStateOf(0L) }
    var showFolderPicker: Boolean by remember { mutableStateOf(false) }

    val drivePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val availableBytes = try {
                val path = uri.path ?: ""
                StatFs(path).run { availableBlocksLong * blockSizeLong }
            } catch (e: Exception) {
                0L
            }
            val name = uri.lastPathSegment ?: "USB Drive"
            vm.process(UsbExportIntent.DriveSelected(uri, name, availableBytes))
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            var name = uri.lastPathSegment ?: "file"
            var size = 0L
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (nameIndex >= 0) name = it.getString(nameIndex)
                    if (sizeIndex >= 0) size = it.getLong(sizeIndex)
                }
            }
            pendingFileUri = uri
            pendingFileName = name
            pendingFileSize = size
            showFolderPicker = true
        }
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                vm.process(UsbExportIntent.UsbDisconnected)
            }
        }
        val filter = IntentFilter(UsbConnectionReceiver.ACTION_USB_DISCONNECTED)
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    LaunchedEffect(Unit) {
        vm.effects.collect { effect ->
            when (effect) {
                is UsbExportEffect.LaunchDrivePicker -> drivePickerLauncher.launch(null)
                is UsbExportEffect.ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("TeslaDrive") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DriveSection(
                state = state,
                onSelectDrive = { vm.process(UsbExportIntent.OpenDrivePicker) },
            )

            if (state.usbDriveUri != null) {
                Button(
                    onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Add File to Export")
                }

                if (state.selectedFiles.isNotEmpty()) {
                    Text("Selected Files (${state.selectedFiles.size})", style = MaterialTheme.typography.titleSmall)
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(state.selectedFiles) { file ->
                            SelectedFileRow(
                                file = file,
                                onRemove = { vm.process(UsbExportIntent.RemoveFile(file.uri)) },
                            )
                        }
                    }

                    if (state.isExporting) {
                        state.exportProgress?.let { progress ->
                            ExportProgressView(progress)
                        }
                        OutlinedButton(
                            onClick = { vm.process(UsbExportIntent.CancelExport) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Cancel")
                        }
                    } else {
                        Button(
                            onClick = { vm.process(UsbExportIntent.StartExport) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Export to Tesla Drive")
                        }
                    }
                }

                if (state.exportCompleted) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Export Complete!", style = MaterialTheme.typography.titleMedium)
                            Text("${state.verifiedFiles} files verified on drive.")
                        }
                    }
                }
            }
        }
    }

    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = { vm.process(UsbExportIntent.DismissError) },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { vm.process(UsbExportIntent.DismissError) }) {
                    Text("OK")
                }
            },
        )
    }

    if (showFolderPicker && pendingFileUri != null) {
        FolderPickerDialog(
            fileName = pendingFileName,
            onFolderSelected = { folder ->
                vm.process(
                    UsbExportIntent.AddFile(
                        uri = pendingFileUri!!,
                        name = pendingFileName,
                        sizeBytes = pendingFileSize,
                        targetFolder = folder,
                    )
                )
                showFolderPicker = false
                pendingFileUri = null
            },
            onDismiss = {
                showFolderPicker = false
                pendingFileUri = null
            },
        )
    }
}

@Composable
private fun DriveSection(state: UsbExportState, onSelectDrive: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("USB Drive", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (state.usbDriveUri != null) {
                Text("Connected: ${state.usbDriveName ?: "Unknown"}")
                state.availableBytes?.let { bytes ->
                    Text("Available: ${formatBytes(bytes)}")
                }
                TextButton(onClick = onSelectDrive) { Text("Change Drive") }
            } else {
                Text("No drive connected", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onSelectDrive, modifier = Modifier.fillMaxWidth()) {
                    Text("Select USB Drive")
                }
            }
        }
    }
}

@Composable
private fun SelectedFileRow(file: SelectedFile, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${file.targetFolder.displayName} · ${formatBytes(file.sizeBytes)}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = onRemove) { Text("Remove") }
    }
}

@Composable
private fun ExportProgressView(progress: ExportProgress) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            "Exporting: ${progress.currentFileName}",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text("${progress.completedFiles}/${progress.totalFiles} files")
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress.progressFraction },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "${formatBytes(progress.bytesTransferred)} / ${formatBytes(progress.totalBytes)}",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun FolderPickerDialog(
    fileName: String,
    onFolderSelected: (TeslaFolder) -> Unit,
    onDismiss: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Tesla Folder") },
        text = {
            Column {
                Text("Where should \"$fileName\" go?")
                Spacer(Modifier.height(8.dp))
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Choose folder")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        TeslaFolder.entries.forEach { folder ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(folder.displayName)
                                        Text(
                                            folder.description,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                },
                                onClick = {
                                    expanded = false
                                    onFolderSelected(folder)
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    return "%.1f GB".format(mb / 1024.0)
}
