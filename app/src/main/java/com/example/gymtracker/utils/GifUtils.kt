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

    fun getGifUri(context: Context, gifPath: String): Uri? {
        return try {
            Log.d(TAG, "Getting GIF URI for path: $gifPath")
            // Check if it's an asset path
            if (gifPath.startsWith("exercise_gifs/")) {
                val gifName = gifPath.substringAfterLast("/")
                Log.d(TAG, "Loading GIF from assets: $gifName")
                
                // Create a temporary file to copy the asset
                val tempFile = File(context.cacheDir, gifName)
                if (!tempFile.exists()) {
                    Log.d(TAG, "Copying GIF from assets to cache: $gifName")
                    context.assets.open(gifPath).use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.d(TAG, "Successfully copied GIF to cache: $gifName")
                } else {
                    Log.d(TAG, "Using cached GIF: $gifName")
                }
                
                val uri = Uri.fromFile(tempFile)
                Log.d(TAG, "Created URI for GIF: $uri")
                uri
            } else {
                // Handle existing file-based GIFs
                val file = File(context.filesDir, gifPath)
                if (file.exists()) {
                    Log.d(TAG, "Using existing file-based GIF: $gifPath")
                    Uri.fromFile(file)
                } else {
                    Log.e(TAG, "GIF file not found: $gifPath")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting GIF URI: ${e.message}", e)
            null
        }
    }

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

    fun deleteGif(context: Context, gifPath: String) {
        val file = File(context.filesDir, gifPath)
        if (file.exists()) {
            file.delete()
        }
    }
} 