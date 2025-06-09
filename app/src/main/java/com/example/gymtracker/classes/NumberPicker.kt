package com.example.gymtracker.classes
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext

@Composable
fun NumberPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    unit: String = ""
) {
    val context = LocalContext.current

    fun vibrateOnValueChange() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(20)
        }
    }

    val itemCount = range.count()
    val visibleItems = 5 // Total visible items in the picker
    val offsetFromCenter = 2 // Move selected value 2 rows below the center
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (Int.MAX_VALUE / 2) -
                (Int.MAX_VALUE / 2) % itemCount +
                (value - range.first) - offsetFromCenter)
    val coroutineScope = rememberCoroutineScope()
    val itemHeight = remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current // Access the current density
    val isHeightInitialized = remember { mutableStateOf(false) }

    // Automatically update the value when the middle item changes
    LaunchedEffect(listState.firstVisibleItemIndex) {
        val middleIndex = (listState.firstVisibleItemIndex + offsetFromCenter) % itemCount
        val newValue = range.first + middleIndex

        if (newValue in range && newValue != value) {
            onValueChange(newValue)
            vibrateOnValueChange()
        }
    }

    // Detect when scrolling stops and snap to the nearest item
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            // Scroll has stopped, snap to the nearest item
            val middleIndex = listState.firstVisibleItemIndex
            coroutineScope.launch {
                listState.scrollToItem(middleIndex)
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .height(itemHeight.value * 5) // Set the height to display 5 items
            .width(120.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            items(count = Int.MAX_VALUE) { index ->
                val itemValue = range.first + (index % itemCount)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { size ->
                            // Convert pixels to Dp using the current density
                            if (!isHeightInitialized.value) {
                                itemHeight.value = with(density) { size.height.toDp() }
                                isHeightInitialized.value = true
                            }
                        }
                ) {
                    Text(
                        text = "$itemValue $unit",
                        fontSize = if (itemValue == value) 24.sp else 18.sp,
                        color = if (itemValue == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .padding(horizontal = 11.dp)
                            .clickable {
                                coroutineScope.launch {
                                    listState.scrollToItem(index)
                                    onValueChange(itemValue)
                                    vibrateOnValueChange()
                                }
                            }
                    )
                }
            }
        }

        // Visual indicator for the selected value
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight.value)
                .background(Color.Transparent)
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                .align(Alignment.Center)
        )
    }
}