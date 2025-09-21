package eric.schedule_exporter

import android.content.res.AssetFileDescriptor
import android.content.res.AssetFileDescriptor.UNKNOWN_LENGTH
import android.database.Cursor
import android.database.MatrixCursor
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Document.FLAG_SUPPORTS_DELETE
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import eric.schedule_exporter.util.MIME_TYPE_ANY
import eric.schedule_exporter.util.MIME_TYPE_CSV
import eric.schedule_exporter.util.MIME_TYPE_TEXT
import eric.schedule_exporter.util.MIME_TYPE_TXT
import eric.schedule_exporter.util.ROOT_DIR_ID
import eric.schedule_exporter.util.getScheduleDir
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import kotlin.io.path.name

private val SUPPORTED_MIME_TYPES: Array<String> =
    arrayOf(MIME_TYPE_CSV, MIME_TYPE_TXT, MIME_TYPE_TEXT)
private val EMPTY_STRING_ARRAY: Array<String> = arrayOf()
private val DEFAULT_ROOT_PROJECTION: Array<String> = arrayOf(
    Root.COLUMN_ROOT_ID,
    Root.COLUMN_ICON,
    Root.COLUMN_TITLE,
    Root.COLUMN_FLAGS,
    Root.COLUMN_DOCUMENT_ID
)
private val DEFAULT_DOCUMENT_PROJECTION: Array<String> = arrayOf(
    Document.COLUMN_DOCUMENT_ID,
    Document.COLUMN_DISPLAY_NAME,
    Document.COLUMN_MIME_TYPE,
    Document.COLUMN_FLAGS,
    Document.COLUMN_SIZE,
    Document.COLUMN_LAST_MODIFIED
)

class ScheduleProvider : DocumentsProvider() {
    override fun onCreate() = true

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val cursor = MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION)
        cursor.newRow()
            .add(Root.COLUMN_ROOT_ID, ROOT_DIR_ID)
            .add(Root.COLUMN_TITLE, "Schedule Exporter")
            .add(Root.COLUMN_DOCUMENT_ID, ROOT_DIR_ID)
            .add(Root.COLUMN_FLAGS, Root.FLAG_LOCAL_ONLY)
            .add(Root.COLUMN_ICON, R.mipmap.ic_launcher)
        //.add(Root.COLUMN_MIME_TYPES, SUPPORTED_MIME_TYPES)
        return cursor
    }

    override fun queryChildDocuments(
        parentDocumentId: String?,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val cursor = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        val dir = this.context?.getScheduleDir()
        if (dir !== null && dir.exists() && dir.isDirectory) {
            Files.list(dir.toPath()).use { files ->
                files.filter {
                    Files.isRegularFile(it) && it.name.endsWith(".csv")
                }.forEach {
                    val name = it.name
                    cursor.newRow()
                        .add(Document.COLUMN_DOCUMENT_ID, name)
                        .add(Document.COLUMN_DISPLAY_NAME, name)
                        .add(Document.COLUMN_MIME_TYPE, MIME_TYPE_CSV)
                        .add(Document.COLUMN_FLAGS, FLAG_SUPPORTS_DELETE)
                        .add(Document.COLUMN_SIZE, Files.size(it))
                        .add(Document.COLUMN_LAST_MODIFIED, Files.getLastModifiedTime(it))
                }
            }
        }
        return cursor
    }

    override fun queryDocument(id: String?, projection: Array<out String>?): Cursor {
        val cursor = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        if (id == ROOT_DIR_ID) {
            cursor.newRow()
                .add(Document.COLUMN_DOCUMENT_ID, ROOT_DIR_ID)
                .add(Document.COLUMN_DISPLAY_NAME, "Schedule Exporter")
                .add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR)
                .add(Document.COLUMN_FLAGS, Document.FLAG_VIRTUAL_DOCUMENT)
                .add(Document.COLUMN_SIZE, null)
        } else if (id !== null) {
            val dir = this.context?.getScheduleDir()
            if (dir === null || !id.endsWith(".csv")) return cursor
            val file = File(dir, id)
            if (file.exists() && file.isFile) {
                cursor.newRow()
                    .add(Document.COLUMN_DOCUMENT_ID, id)
                    .add(Document.COLUMN_MIME_TYPE, MIME_TYPE_CSV)
                    .add(Document.COLUMN_DISPLAY_NAME, file.name)
                    .add(Document.COLUMN_LAST_MODIFIED, file.lastModified())
                    .add(Document.COLUMN_FLAGS, FLAG_SUPPORTS_DELETE)
                    .add(Document.COLUMN_SIZE, file.length())

            }
        }
        return cursor
    }

    override fun deleteDocument(documentId: String) {
        val dir = this.context?.getScheduleDir()
        if (dir !== null && documentId.endsWith(".csv")) {
            val file = File(dir, documentId)
            if (file.exists() && file.isFile) {
                file.delete()
            }
        } else throw FileNotFoundException("File not found or invalid for deleting: $documentId")
    }

    override fun openDocument(
        documentId: String,
        mode: String?,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        if (mode === null || !mode.contains("w", true)) {
            val dir = this.context?.getScheduleDir()
            if (dir !== null && documentId.endsWith(".csv")) {
                val file = File(dir, documentId)
                if (file.exists() && file.isFile) return ParcelFileDescriptor.open(
                    file,
                    ParcelFileDescriptor.MODE_READ_ONLY
                )
            }
            throw FileNotFoundException("File not found or invalid for reading: $documentId")
        }
        throw UnsupportedOperationException("Unsupported mode: $mode")
    }

    override fun openTypedDocument(
        documentId: String,
        filter: String?,
        opts: Bundle?,
        signal: CancellationSignal?
    ): AssetFileDescriptor? {
        if (filter !== null && filter != MIME_TYPE_ANY && !filter.startsWith("text/")) {
            throw IllegalArgumentException("The requested MIME type ($filter) is not supported.")
        }
        return AssetFileDescriptor(
            openDocument(documentId, null, signal),
            0,
            UNKNOWN_LENGTH
        )
    }

    override fun getDocumentStreamTypes(id: String, filter: String): Array<String> {
        return when (filter) {
            MIME_TYPE_ANY, MIME_TYPE_TEXT, MIME_TYPE_CSV, MIME_TYPE_TXT -> SUPPORTED_MIME_TYPES
            else -> EMPTY_STRING_ARRAY
        }
    }
}