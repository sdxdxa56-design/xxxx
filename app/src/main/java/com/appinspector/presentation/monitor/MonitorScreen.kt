package com.appinspector.presentation.monitor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appinspector.presentation.theme.AccentRed
import com.appinspector.presentation.theme.PrimaryEmerald
import com.appinspector.presentation.theme.SurfaceDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorScreen(
    viewModel: MonitorViewModel,
    onBack: () -> Unit
) {
    val liveLogs by viewModel.liveLogs.collectAsState()
    val isListening by viewModel.isListening.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startListening()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Live Logcat Stream", color = Color.White, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearLiveLogs() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color.Gray)
                    }
                    IconButton(onClick = {
                        if (isListening) viewModel.stopListening() else viewModel.startListening()
                    }) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isListening) "Stop" else "Start",
                            tint = if (isListening) AccentRed else PrimaryEmerald
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.85f)
            ) {
                if (liveLogs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isListening) "Listening for errors & logs..." else "Streaming paused",
                            color = Color.DarkGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(liveLogs) { log ->
                            Text(
                                text = log,
                                color = if (log.contains("FATAL") || log.contains("ANR")) AccentRed else Color.LightGray,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
