package com.example.gymtracker.classes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ValuePicker(
    initialValue: Int,
    valueRange: IntRange,
    unit: String,
    onValueSelected: (Int) -> Unit
) {
    val values = valueRange.toList()
    val initialIndex = values.indexOf(initialValue).coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { values.size }
    )

    LaunchedEffect(initialIndex) {
        pagerState.scrollToPage(initialIndex) // Ensure initial alignment
    }

    Box(
        modifier = Modifier
            .height(160.dp) // Height remains the same
            .fillMaxWidth()
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = (-4).dp, // Reduced spacing between pages
            contentPadding = PaddingValues(vertical = 20.dp), // Reduced vertical padding
            horizontalAlignment = Alignment.CenterHorizontally
        ) { page ->
            val isSelected = page == pagerState.currentPage
            Text(
                text = "${values[page]} $unit",
                style = if (isSelected) {
                    MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp)
                } else {
                    MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp)
                },
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Center highlight box remains unchanged
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .align(Alignment.Center)
                .border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
        )
    }

    LaunchedEffect(pagerState.currentPage) {
        pagerState.animateScrollToPage(pagerState.currentPage) // Ensure snapping effect
        onValueSelected(values[pagerState.currentPage])
    }
}

