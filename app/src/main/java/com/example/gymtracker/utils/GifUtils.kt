package com.example.gymtracker.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

object GifUtils {
    private const val TAG = "GifUtils"
    private const val GIF_DIRECTORY = "exercise_gifs"

    fun saveGifToInternalStorage(context: Context, uri: Uri): String? {
        try {
            // Create directory if it doesn't exist
            val gifDir = File(context.filesDir, GIF_DIRECTORY)
            if (!gifDir.exists()) {
                gifDir.mkdirs()
            }

            // Generate unique filename
            val filename = "gif_${UUID.randomUUID()}.gif"
            val outputFile = File(gifDir, filename)

            // Copy the GIF file
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Return the relative path to the GIF
            return "$GIF_DIRECTORY/$filename"
        } catch (e: IOException) {
            Log.e(TAG, "Error saving GIF: ${e.message}")
            return null
        }
    }

    fun getGifUri(context: Context, gifPath: String): Uri? {
        val file = File(context.filesDir, gifPath)
        return if (file.exists()) {
            Uri.fromFile(file)
        } else {
            null
        }
    }

    fun deleteGif(context: Context, gifPath: String) {
        val file = File(context.filesDir, gifPath)
        if (file.exists()) {
            file.delete()
        }
    }
} 