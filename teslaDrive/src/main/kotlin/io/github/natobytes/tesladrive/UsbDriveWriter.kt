package io.github.natobytes.tesladrive

import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import io.github.natobytes.tesladrive.contract.ExportProgress
import io.github.natobytes.tesladrive.contract.SelectedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

private const val TAG = "UsbDriveWriter"
private const val BUFFER_SIZE = 8 * 1024

class UsbDriveWriter(private val context: Context) {

    fun getAvailableBytes(rootUri: Uri): Long {
        return try {
            val path = rootUri.path ?: return 0L
            val statFs = StatFs(path)
            statFs.availableBlocksLong * statFs.blockSizeLong
        } catch (e: Exception) {
            Log.w(TAG, "Could not get available bytes via StatFs: ${e.message}")
            0L
        }
    }

    fun exportFiles(
        rootUri: Uri,
        files: List<SelectedFile>,
    ): Flow<ExportProgress> = flow {
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri)
            ?: error("Cannot access USB drive at $rootUri")

        val totalBytes = files.sumOf { it.sizeBytes }
        var bytesTransferred = 0L
        var completedFiles = 0

        files.forEach { selectedFile ->
            if (!coroutineContext.isActive) return@flow

            val folderDoc = getOrCreateFolder(rootDoc, selectedFile.targetFolder)
                ?: error("Cannot create folder ${selectedFile.targetFolder.path}")

            emit(
                ExportProgress(
                    currentFileName = selectedFile.name,
                    completedFiles = completedFiles,
                    totalFiles = files.size,
                    bytesTransferred = bytesTransferred,
                    totalBytes = totalBytes,
                )
            )

            val existingFile = folderDoc.findFile(selectedFile.name)
            existingFile?.delete()

            val mimeType = context.contentResolver.getType(selectedFile.uri) ?: "application/octet-stream"
            val targetFile = folderDoc.createFile(mimeType, selectedFile.name)
                ?: error("Cannot create file ${selectedFile.name}")

            context.contentResolver.openInputStream(selectedFile.uri)?.use { input ->
                context.contentResolver.openOutputStream(targetFile.uri)?.use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (!coroutineContext.isActive) return@flow
                        output.write(buffer, 0, bytesRead)
                        bytesTransferred += bytesRead
                        emit(
                            ExportProgress(
                                currentFileName = selectedFile.name,
                                completedFiles = completedFiles,
                                totalFiles = files.size,
                                bytesTransferred = bytesTransferred,
                                totalBytes = totalBytes,
                            )
                        )
                    }
                }
            }

            completedFiles++
        }

        emit(
            ExportProgress(
                currentFileName = "",
                completedFiles = completedFiles,
                totalFiles = files.size,
                bytesTransferred = bytesTransferred,
                totalBytes = totalBytes,
            )
        )
    }.flowOn(Dispatchers.IO)

    fun verifyFiles(
        rootUri: Uri,
        files: List<SelectedFile>,
    ): Int {
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return 0
        var verified = 0
        files.forEach { selectedFile ->
            val folderDoc = getOrCreateFolder(rootDoc, selectedFile.targetFolder) ?: return@forEach
            if (folderDoc.findFile(selectedFile.name) != null) verified++
        }
        return verified
    }

    private fun getOrCreateFolder(rootDoc: DocumentFile, folder: TeslaFolder): DocumentFile? {
        val segments = folder.path.split("/")
        var current: DocumentFile = rootDoc
        for (segment in segments) {
            current = current.findFile(segment) ?: current.createDirectory(segment) ?: return null
        }
        return current
    }
}
