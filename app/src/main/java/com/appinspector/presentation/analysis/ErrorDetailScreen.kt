package com.appinspector.presentation.analysis

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appinspector.domain.model.AppError
import com.appinspector.domain.model.ErrorSeverity
import com.appinspector.presentation.theme.AccentRed
import com.appinspector.presentation.theme.AccentYellow
import com.appinspector.presentation.theme.PrimaryEmerald
import com.appinspector.presentation.theme.SecondaryTeal
import com.appinspector.presentation.theme.SurfaceDark
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorDetailScreen(
    errorId: Long,
    viewModel: ErrorDetailViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val errorState = viewModel.getError(errorId).collectAsState(initial = null)
    val error = errorState.value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Error Diagnostics", color = Color.White, fontWeight = FontWeight.Bold) },
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
                    if (error != null) {
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Logcat Error", "${error.message}\n\n${error.stackTrace}")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White)
                        }
                        IconButton(onClick = {
                            scope.launch {
                                viewModel.deleteError(error)
                                onBack()
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (error == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryEmerald)
            }
        } else {
            val severityColor = when (error.severity) {
                ErrorSeverity.CRASH -> AccentRed
                ErrorSeverity.ANR -> AccentYellow
                ErrorSeverity.ERROR -> SecondaryTeal
                ErrorSeverity.WARNING -> Color.LightGray
            }

            val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()) }
            val formattedTime = remember(error.timestamp) { dateFormat.format(Date(error.timestamp)) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = severityColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = error.severity.name,
                                    color = severityColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = formattedTime,
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = error.appName.ifBlank { error.packageName },
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = error.packageName,
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                if (!error.solutionSuggestion.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = AccentYellow)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AI Suggested Fix",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error.solutionSuggestion,
                                color = Color.LightGray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Exception Message & Stack Trace",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = error.stackTrace,
                                color = Color(0xFFFCA5A5),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
