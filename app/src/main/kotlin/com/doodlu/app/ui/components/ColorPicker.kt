package com.doodlu.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.doodlu.app.ui.theme.DrawingColorsCompose

@Composable
fun ColorPicker(
    selectedColor: String,
    onColorSelected: (String, Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorHexList = listOf(
        "#E94560", "#FF6B35", "#FFC947", "#06D6A0",
        "#118AB2", "#7B2FBE", "#FF69B4", "#FFFFFF"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        colorHexList.forEachIndexed { index, hex ->
            val composeColor = DrawingColorsCompose[index]
            val isSelected = selectedColor == hex

            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.2f else 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "colorScale"
            )

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(composeColor)
                    .then(
                        if (isSelected) Modifier.border(2.dp, Color.White, CircleShape)
                        else Modifier
                    )
                    .clickable { onColorSelected(hex, composeColor) }
            )
        }
    }
}
