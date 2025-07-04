package com.example.gymtracker.components

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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val gifUri = GifUtils.getGifUri(context, gifPath)
    var hasError by remember { mutableStateOf(false) }

    // Debug logging
    LaunchedEffect(gifPath) {
        Log.d("ExerciseGif", "GIF path: $gifPath")
        Log.d("ExerciseGif", "GIF URI: $gifUri")
        Log.d("ExerciseGif", "Has error: $hasError")
    }

    if (gifUri != null && !hasError) {
        AndroidView(
            factory = { context ->
                GifImageView(context).apply {
                    try {
                        setImageURI(gifUri)
                        Log.d("ExerciseGif", "Successfully set GIF URI: $gifUri")
                    } catch (e: Exception) {
                        Log.e("ExerciseGif", "Error loading GIF: ${e.message}", e)
                        hasError = true
                    }
                }
            },
            modifier = modifier,
            update = { view ->
                try {
                    view.setImageURI(gifUri)
                } catch (e: Exception) {
                    Log.e("ExerciseGif", "Error updating GIF: ${e.message}", e)
                    hasError = true
                }
            }
        )
    } else {
        // Fallback: show error message
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (gifUri == null) "GIF not found: $gifPath" else "Failed to load GIF",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
} 