package com.appinspector.presentation.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appinspector.presentation.theme.PrimaryEmerald
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateNext: () -> Unit
) {
    val scale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
        delay(1200)
        onNavigateNext()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale.value)
        ) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = null,
                tint = PrimaryEmerald,
                modifier = Modifier.size(90.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "App Inspector",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Kernel & System Crash Monitor",
                color = Color.Gray,
                fontSize = 13.sp
            )
        }
    }
}
