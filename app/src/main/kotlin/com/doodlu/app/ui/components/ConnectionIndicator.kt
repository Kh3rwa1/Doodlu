package com.celestial.spire.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.celestial.spire.sync.ConnectionState
import com.celestial.spire.sync.SyncManager

@Composable
fun ConnectionIndicator(
    state: ConnectionState,
    playerCount: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "connPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val dotColor = when (state) {
        ConnectionState.CONNECTED -> Color(0xFF06D6A0)
        ConnectionState.RECONNECTING -> Color(0xFFFFC947)
        else -> Color(0xFFE94560)
    }

    val isAnimating = state == ConnectionState.CONNECTED || state == ConnectionState.RECONNECTING

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box {
            if (isAnimating) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(dotColor.copy(alpha = 0.3f))
                )
            }
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }

        if (playerCount > 0) {
            Text(
                text = "$playerCount",
                color = Color(0xFF8892B0),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
