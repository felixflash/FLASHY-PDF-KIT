package com.flashypdfkit

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

@CapacitorPlugin(name = "SaveToDownloads")
class SaveToDownloadsPlugin : Plugin() {

    @PluginMethod
    fun save(call: PluginCall) {
        val base64Data = call.getString("data") ?: return call.reject("Missing data")
        val filename = call.getString("filename") ?: return call.reject("Missing filename")
        val mimeType = call.getString("mimeType") ?: "application/octet-stream"

        try {
            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
            val resolver = context.contentResolver

            // If file already exists, MediaStore will usually append (1), (2), etc.
            // but we ensure the filename is clean.
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                } else {
                    @Suppress("DEPRECATION")
                    put(
                        MediaStore.MediaColumns.DATA,
                        "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/$filename"
                    )
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Files.getContentUri("external")
            }

            val uri = resolver.insert(collection, values)
                ?: return call.reject("Could not create file entry in Downloads")

            resolver.openOutputStream(uri).use { output ->
                if (output == null) throw IllegalStateException("Could not open output stream for URI: $uri")
                output.write(bytes)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }

            val result = JSObject()
            result.put("uri", uri.toString())
            call.resolve(result)
        } catch (e: OutOfMemoryError) {
            call.reject("File is too large to save (Out of Memory)")
        } catch (t: Throwable) {
            call.reject("Save failed: ${t.localizedMessage ?: "Unknown native error"}")
        }
    }

    @PluginMethod
    fun openFile(call: PluginCall) {
        val uriString = call.getString("uri") ?: return call.reject("Missing URI")
        try {
            val uri = Uri.parse(uriString)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            call.resolve()
        } catch (e: Exception) {
            call.reject("Could not open file: ${e.message}")
        }
    }
}
