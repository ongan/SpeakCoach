package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CoachGender
import com.example.ui.theme.Amber500
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Indigo500
import com.example.ui.theme.Pink500

enum class AvatarState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

@Composable
fun CoachAvatar(
    coachGender: CoachGender,
    avatarState: AvatarState = AvatarState.IDLE,
    size: Dp = 80.dp,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "avatarPulse")
    val pulseScale by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (avatarState == AvatarState.SPEAKING || avatarState == AvatarState.LISTENING) 1.15f else 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val primaryColor = when (coachGender) {
        CoachGender.MAYA -> Pink500
        CoachGender.LEO -> Cyan400
    }

    val secondaryColor = when (coachGender) {
        CoachGender.MAYA -> Amber500
        CoachGender.LEO -> Indigo500
    }

    Box(
        modifier = modifier
            .size(size)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Animated ring for speaking or listening state
        if (avatarState != AvatarState.IDLE) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerOffset = Offset(size.toPx() / 2, size.toPx() / 2)
                val strokeWidth = 3.dp.toPx()
                val radius = (size.toPx() / 2 - strokeWidth) * pulseScale

                drawCircle(
                    brush = Brush.sweepGradient(listOf(primaryColor, secondaryColor, primaryColor)),
                    center = centerOffset,
                    radius = radius,
                    style = Stroke(width = strokeWidth)
                )
            }
        }

        // Inner Avatar Circle with Original Stylized Visual Art
        Box(
            modifier = Modifier
                .fillMaxSize(0.85f)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.25f),
                            secondaryColor.copy(alpha = 0.15f)
                        )
                    )
                )
                .border(2.dp, primaryColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (coachGender == CoachGender.MAYA) "👩‍🏫" else "👨‍🏫",
                fontSize = (size.value * 0.45).sp
            )
        }
    }
}
