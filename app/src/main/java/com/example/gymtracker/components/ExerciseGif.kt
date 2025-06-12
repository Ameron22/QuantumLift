package com.example.gymtracker.components

import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

@Composable
fun ExerciseGif(
    gifPath: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val gifUri = GifUtils.getGifUri(context, gifPath)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            gifUri?.let { uri ->
                AndroidView(
                    factory = { context ->
                        GifImageView(context).apply {
                            setImageURI(uri)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        view.setImageURI(uri)
                    }
                )
            }
        }
    }
} 