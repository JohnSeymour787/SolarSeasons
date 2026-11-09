package com.johnseymour.solarseasons

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.time.LocalDateTime

object LoggingUtils
{
    private const val LOG_FILE_NAME = "solar_seasons_app_log.txt"

    fun writeLogMediaStore(context: Context, message: String, append: Boolean = true) {
        try {
            val resolver = context.contentResolver

            if (append) {
                // Try to append to existing file
                val existingUri = findExistingLogFile(context, LOG_FILE_NAME)
                if (existingUri != null) {
                    appendToLogFile(resolver, existingUri, message)
                    return
                }
            }

            // Create new file or overwrite existing
            createNewLogFile(resolver, message)

        }
        catch (e: Exception) { }
    }

    private fun findExistingLogFile(context: Context, fileName: String): Uri? {
        return try {
            val resolver = context.contentResolver
            val projection = arrayOf(MediaStore.MediaColumns._ID)
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(fileName)

            val queryUri = MediaStore.Files.getContentUri("external")
            resolver.query(queryUri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    Uri.withAppendedPath(queryUri, id.toString())
                } else {
                    null
                }
            }
        }
        catch (e: Exception) {
            null
        }
    }

    private fun appendToLogFile(resolver: ContentResolver, uri: Uri, message: String) {
        try {
            resolver.openOutputStream(uri, "wa")?.use { outputStream ->
                val content = "\n$message" // Add new line before appending
                outputStream.write(content.toByteArray())
            }
        }
        catch (e: Exception) { }
    }

    private fun createNewLogFile(resolver: ContentResolver, message: String) {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, LOG_FILE_NAME)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/SolarSeasons")
            }

            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(message.toByteArray())
                }
            }
        }
        catch (e: Exception) { }
    }

    fun writeLog(context: Context, message: String)
    {
        val toWrite = "${LocalDateTime.now()} - $message"
        writeLogMediaStore(context, toWrite)

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.MANAGE_DOCUMENTS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val logFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), LOG_FILE_NAME)

        try
        {
            if (!logFile.exists())
            {
                logFile.createNewFile()
            }
            val fos = FileOutputStream(logFile, true)
            val osw = OutputStreamWriter(fos)
            val bw = BufferedWriter(osw)
            bw.write(toWrite)
            bw.newLine()
            bw.close()
            osw.close()
            fos.close()
        }
        catch (e: IOException)
        {
            Log.e(LoggingUtils.javaClass.name, "Log writing failed: ${e.message}")
        }
    }
}