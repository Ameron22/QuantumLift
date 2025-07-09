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
            
            // Check if it's an asset path (numbered GIFs like "0001_", "0002_")
            val filename = gifPath.substringAfterLast("/")
            if (gifPath.startsWith("exercise_gifs/") && filename.matches(Regex("\\d{4}_.*\\.gif"))) {
                Log.d(TAG, "Loading GIF from assets: $filename")
                
                // Create a temporary file to copy the asset
                val tempFile = File(context.cacheDir, filename)
                if (!tempFile.exists()) {
                    Log.d(TAG, "Copying GIF from assets to cache: $filename")
                    context.assets.open(gifPath).use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.d(TAG, "Successfully copied GIF to cache: $filename")
                } else {
                    Log.d(TAG, "Using cached GIF: $filename")
                }
                
                val uri = Uri.fromFile(tempFile)
                Log.d(TAG, "Created URI for asset GIF: $uri")
                uri
            } else {
                // Handle saved file-based GIFs (including those saved by the app)
                val file = File(context.filesDir, gifPath)
                Log.d(TAG, "Looking for saved GIF file: ${file.absolutePath}")
                if (file.exists()) {
                    Log.d(TAG, "Found saved GIF file: ${file.absolutePath}")
                    val uri = Uri.fromFile(file)
                    Log.d(TAG, "Created URI for saved GIF: $uri")
                    uri
                } else {
                    Log.e(TAG, "GIF file not found: ${file.absolutePath}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting GIF URI: ${e.message}", e)
            null
        }
    }

    fun saveGifToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            Log.d(TAG, "Saving GIF from URI: $uri")
            
            // Create directory if it doesn't exist
            val gifDir = File(context.filesDir, GIF_DIRECTORY)
            if (!gifDir.exists()) {
                val created = gifDir.mkdirs()
                Log.d(TAG, "Created GIF directory: $created")
            }

            // Generate unique filename
            val filename = "gif_${UUID.randomUUID()}.gif"
            val outputFile = File(gifDir, filename)
            
            Log.d(TAG, "Saving to file: ${outputFile.absolutePath}")

            // Copy the GIF file with better error handling
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytes = 0L
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead
                    }
                    
                    Log.d(TAG, "Successfully copied $totalBytes bytes to $filename")
                }
            } ?: run {
                Log.e(TAG, "Failed to open input stream from URI: $uri")
                return null
            }

            // Verify the file was created and has content
            if (!outputFile.exists() || outputFile.length() == 0L) {
                Log.e(TAG, "Output file is empty or doesn't exist: ${outputFile.absolutePath}")
                return null
            }

            // Return the relative path to the GIF
            val relativePath = "$GIF_DIRECTORY/$filename"
            Log.d(TAG, "Successfully saved GIF with relative path: $relativePath")
            relativePath
            
        } catch (e: IOException) {
            Log.e(TAG, "IO Error saving GIF: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error saving GIF: ${e.message}", e)
            null
        }
    }

    fun deleteGif(context: Context, gifPath: String) {
        try {
            val file = File(context.filesDir, gifPath)
            if (file.exists()) {
                val deleted = file.delete()
                Log.d(TAG, "Deleted GIF file: $deleted - ${file.absolutePath}")
            } else {
                Log.w(TAG, "GIF file not found for deletion: $gifPath")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting GIF: ${e.message}", e)
        }
    }
    
    fun isValidGifFile(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.getType(uri)?.let { mimeType ->
                mimeType.startsWith("image/") && (
                    mimeType == "image/gif" || 
                    mimeType == "image/*" ||
                    uri.toString().lowercase().endsWith(".gif")
                )
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking GIF validity: ${e.message}", e)
            false
        }
    }
} 