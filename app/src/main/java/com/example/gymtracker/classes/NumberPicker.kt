package com.example.gymtracker.classes
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

@Composable
fun NumberPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    unit: String = ""
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = value - range.first-2)
    val coroutineScope = rememberCoroutineScope()

    // Automatically update the value when the middle item changes
    LaunchedEffect(listState.firstVisibleItemIndex) {
        val middleIndex = listState.firstVisibleItemIndex + 2 // Adjust based on your UI
        val newValue = range.first + middleIndex

        if (newValue in range) {
            onValueChange(newValue)
        }
    }

    // Detect when scrolling stops and snap to the nearest item
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            // Scroll has stopped, snap to the nearest item
            val middleIndex = listState.firstVisibleItemIndex + 2
            coroutineScope.launch {
                listState.scrollToItem(middleIndex)
            }
        }
    }

    Box(
        modifier = modifier
            .height(215.dp)
            .width(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            items(range.toList().size) { index ->
                val itemValue = range.first + index
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
                            }
                        }
                )
            }
        }

        // Visual indicator for the selected value
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color.Transparent)
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                .align(Alignment.Center)
        )
    }
}
