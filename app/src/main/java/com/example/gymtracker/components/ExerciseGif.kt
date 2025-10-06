package com.example.gymtracker.components

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.gymtracker.utils.GifUtils
import pl.droidsonroids.gif.GifImageView
import android.util.Log

@Composable
fun ExerciseGif(
    gifPath: String,
    modifier: Modifier = Modifier,
    cornerRadius: Float = 24f
) {
    ExerciseImage(
        imagePath = gifPath,
        modifier = modifier,
        cornerRadius = cornerRadius
    )
}

@Composable
fun ExerciseImage(
    imagePath: String,
    modifier: Modifier = Modifier,
    cornerRadius: Float = 24f
) {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var hasError by remember { mutableStateOf(false) }
    var isGif by remember { mutableStateOf(false) }

    // Debug logging
    LaunchedEffect(imagePath) {
        Log.d("ExerciseImage", "Image path: $imagePath")
        
        // Try to find the image in different formats
        val foundUri = findImageUri(context, imagePath)
        imageUri = foundUri.first
        isGif = foundUri.second
        
        Log.d("ExerciseImage", "Found URI: $imageUri, isGif: $isGif")
    }

    if (imageUri != null && !hasError) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(cornerRadius.dp),
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.ui.graphics.Color.White
            )
        ) {
            if (isGif) {
                // Use GifImageView for GIF files
                AndroidView(
                    factory = { context ->
                        GifImageView(context).apply {
                            try {
                                setImageURI(imageUri)
                                Log.d("ExerciseImage", "Successfully set GIF URI: $imageUri")
                            } catch (e: Exception) {
                                Log.e("ExerciseImage", "Error loading GIF: ${e.message}", e)
                                hasError = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        try {
                            view.setImageURI(imageUri)
                        } catch (e: Exception) {
                            Log.e("ExerciseImage", "Error updating GIF: ${e.message}", e)
                            hasError = true
                        }
                    }
                )
            } else {
                // Use Coil for static images (JPEG, PNG)
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Loading...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    },
                    error = {
                        Log.e("ExerciseImage", "Error loading static image: $imageUri")
                        hasError = true
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Failed to load image",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                )
            }
        }
    } else {
        // Fallback: show blank card with same styling (no error message to avoid ugly flashing)
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(cornerRadius.dp),
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.ui.graphics.Color.White
            )
        ) {
            // Empty - no content, just maintains the card shape
        }
    }
}

/**
 * Find image URI by trying different formats in order of preference
 * Returns Pair<Uri?, Boolean> where Boolean indicates if it's a GIF
 */
private fun findImageUri(context: Context, imagePath: String): Pair<Uri?, Boolean> {
    // Try GIF first (original behavior) - case insensitive
    val gifUri = GifUtils.getGifUri(context, imagePath)
    if (gifUri != null) {
        Log.d("ExerciseImage", "Found GIF: $gifUri")
        return Pair(gifUri, true)
    }
    
    // Define all possible extensions to try (case insensitive)
    val extensions = listOf(
        "gif", "GIF", "Gif",
        "jpeg", "JPEG", "Jpeg", "Jpeg",
        "jpg", "JPG", "Jpg", "Jpg",
        "png", "PNG", "Png", "Png"
    )
    
    // Try each extension
    for (ext in extensions) {
        val testPath = imagePath.replaceAfterLast(".", ext)
        val testUri = GifUtils.getGifUri(context, testPath)
        if (testUri != null) {
            val isGif = ext.lowercase() == "gif"
            Log.d("ExerciseImage", "Found ${ext.uppercase()}: $testUri")
            return Pair(testUri, isGif)
        }
    }
    
    Log.w("ExerciseImage", "No image found for path: $imagePath")
    return Pair(null, false)
} 