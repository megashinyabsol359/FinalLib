package com.example.finallib.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object FileUtils {
    fun copyFileToInternalStorage(context: Context, sourceUri: Uri, fileName: String): File {
        val destinationFile = File(context.filesDir, fileName)
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destinationFile).use { output ->
                input.copyTo(output)
            }
        }
        return destinationFile
    }
}