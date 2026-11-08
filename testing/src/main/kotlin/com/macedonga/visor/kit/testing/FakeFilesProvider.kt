package com.macedonga.visor.kit.testing

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.FileNotFoundException
import kotlin.concurrent.thread

/**
 * Serves the captures and live photos FakeVisor hands out, so an app under test reads their `content://` URIs as it
 * reads Visor's. Read-only, and only to the app itself: kit-testing's manifest declares it unexported.
 */
public class FakeFilesProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        FakeFiles.authority = "${context!!.packageName}$AUTHORITY_SUFFIX"
        return true
    }

    override fun getType(uri: Uri): String? = file(uri)?.first

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw FileNotFoundException("FakeVisor's files are read-only.")
        val bytes = file(uri)?.second ?: throw FileNotFoundException("FakeVisor has no file at $uri.")
        val (read, write) = ParcelFileDescriptor.createPipe()
        thread(name = "FakeFilesProvider") {
            ParcelFileDescriptor.AutoCloseOutputStream(write).use { it.write(bytes) }
        }
        return read
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, order: String?): Cursor? {
        val bytes = file(uri)?.second ?: return null
        val columns = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        return MatrixCursor(columns, 1).apply {
            addRow(columns.map { if (it == OpenableColumns.SIZE) bytes.size.toLong() else uri.lastPathSegment })
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, args: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?): Int = 0

    private fun file(uri: Uri): Pair<String, ByteArray>? = uri.path?.removePrefix("/")?.let(FakeFiles::get)

    private companion object {
        const val AUTHORITY_SUFFIX = ".visorkit.fake"
    }
}
