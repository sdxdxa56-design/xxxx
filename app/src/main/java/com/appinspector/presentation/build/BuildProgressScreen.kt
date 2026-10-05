package com.appinspector.presentation.build

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appinspector.presentation.github.GitHubViewModel
import com.appinspector.presentation.launcher.LaunchState
import com.appinspector.presentation.launcher.LaunchViewModel
import com.appinspector.presentation.theme.AccentYellow
import com.appinspector.presentation.theme.CrashRed
import com.appinspector.presentation.theme.PrimaryEmerald
import com.appinspector.presentation.theme.SurfaceDark
import com.appinspector.presentation.theme.SurfaceVariantDark
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildProgressScreen(
    owner: String,
    repo: String,
    branch: String,
    viewModel: GitHubViewModel,
    launchViewModel: LaunchViewModel,
    onNavigateToMonitor: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val buildState by viewModel.buildState.collectAsState()
    val launchState by launchViewModel.launchState.collectAsState()
    val localProjectDir = remember(repo) { File(context.filesDir, "projects/$repo") }

    LaunchedEffect(owner, repo, branch) {
        if (buildState is BuildUiState.Idle) {
            viewModel.buildProject(owner, repo, branch, localProjectDir)
        }
    }

    LaunchedEffect(launchState) {
        if (launchState is LaunchState.Tracking) {
            delay(1500)
            onNavigateToMonitor()
        }
    }

    val isBuilding = remember(buildState) {
        buildState is BuildUiState.Pushing || buildState is BuildUiState.Queued || buildState is BuildUiState.Running
    }

    fun openBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    if (launchState !is LaunchState.Idle) {
        AlertDialog(
            onDismissRequest = {
                if (launchState is LaunchState.Error) {
                    launchViewModel.resetState()
                }
            },
            icon = {
                when (launchState) {
                    is LaunchState.Installing, is LaunchState.WaitingForLaunch -> {
                        CircularProgressIndicator(color = PrimaryEmerald, modifier = Modifier.size(32.dp))
                    }
                    is LaunchState.Tracking -> {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryEmerald, modifier = Modifier.size(36.dp))
                    }
                    is LaunchState.Error -> {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CrashRed, modifier = Modifier.size(36.dp))
                    }
                    else -> {}
                }
            },
            title = {
                Text(
                    text = when (launchState) {
                        is LaunchState.Installing -> "تثبيت التطبيق..."
                        is LaunchState.WaitingForLaunch -> "في انتظار تشغيل التطبيق..."
                        is LaunchState.Tracking -> "تم ربط التطبيق بنجاح!"
                        is LaunchState.Error -> "خطأ أثناء التثبيت"
                        else -> ""
                    },
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                when (val state = launchState) {
                    is LaunchState.Installing -> {
                        Text(
                            text = "جاري فتح حزمة التثبيت لـ ${state.packageName}...",
                            color = Color.LightGray
                        )
                    }
                    is LaunchState.WaitingForLaunch -> {
                        Column {
                            Text(
                                text = "يرجى إكمال التثبيت وتشغيل التطبيق على الهاتف (${state.seconds}s)",
                                color = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "يقوم App Inspector بالاستماع لظهور PID لمراقبة الـ Crashes فوراً.",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                    is LaunchState.Tracking -> {
                        Column {
                            Text(
                                text = "تم بدء المراقبة لتطبيق: ${state.packageName} (PID: ${if (state.pid > 0) state.pid else "Active"})",
                                color = PrimaryEmerald,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "جاري التحويل لشاشة Live Monitor...",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                    is LaunchState.Error -> {
                        Text(
                            text = state.message,
                            color = CrashRed
                        )
                    }
                    else -> {}
                }
            },
            confirmButton = {
                if (launchState is LaunchState.Error) {
                    Button(
                        onClick = { launchViewModel.resetState() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                    ) {
                        Text("إغلاق", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else if (launchState is LaunchState.Tracking) {
                    Button(
                        onClick = onNavigateToMonitor,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                    ) {
                        Text("الذهاب للمراقبة الحية", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            containerColor = SurfaceDark
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Cloud APK Build: $repo",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "$owner/$repo ($branch)",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
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
                    if (isBuilding) {
                        TextButton(
                            onClick = { viewModel.cancelBuild() },
                            colors = ButtonDefaults.textButtonColors(contentColor = CrashRed)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            when (val state = buildState) {
                is BuildUiState.Idle -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = PrimaryEmerald,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Ready to Build APK",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Builds directly on GitHub Actions infrastructure with Gradle 8.11.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                viewModel.buildProject(owner, repo, branch, localProjectDir)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                        ) {
                            Text("Start Remote Build", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is BuildUiState.Pushing -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = PrimaryEmerald)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Pushing Project to GitHub",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 17.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Uploaded ${state.current} of ${state.total} files",
                                color = Color.LightGray,
                                fontSize = 13.sp
                            )
                            if (state.total > 0) {
                                Spacer(modifier = Modifier.height(14.dp))
                                LinearProgressIndicator(
                                    progress = if (state.total > 0) state.current.toFloat() / state.total.toFloat() else 0f,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = PrimaryEmerald,
                                    trackColor = SurfaceVariantDark
                                )
                            }
                        }
                    }
                }

                is BuildUiState.Queued -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = AccentYellow)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "GitHub Actions: Queued",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 17.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Waiting for an Ubuntu runner to allocate resources...",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                            if (state.htmlUrl.isNotBlank()) {
                                Spacer(modifier = Modifier.height(20.dp))
                                OutlinedButton(
                                    onClick = { openBrowser(state.htmlUrl) },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open GitHub Actions Workflow")
                                }
                            }
                        }
                    }
                }

                is BuildUiState.Running -> {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseScale"
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    color = PrimaryEmerald,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .scale(pulseScale)
                                )
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    tint = PrimaryEmerald,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "Compiling APK in the Cloud",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 17.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Running gradle assembleDebug... (${state.elapsedSeconds}s)",
                                color = PrimaryEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )

                            if (state.htmlUrl.isNotBlank()) {
                                Spacer(modifier = Modifier.height(20.dp))
                                OutlinedButton(
                                    onClick = { openBrowser(state.htmlUrl) },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("View Live Build Logs")
                                }
                            }
                        }
                    }
                }

                is BuildUiState.Success -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PrimaryEmerald,
                                modifier = Modifier.size(64.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "APK Build Successful!",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 19.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Downloaded artifact binary (${(state.apkFile.length() / (1024 * 1024 * 1.0)).format(2)} MB)",
                                color = Color.LightGray,
                                fontSize = 13.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Surface(
                                color = SurfaceVariantDark,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = state.apkFile.name,
                                    color = PrimaryEmerald,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    launchViewModel.installAndTrack(state.apkFile)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Install & Launch APK", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            if (state.htmlUrl.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                TextButton(onClick = { openBrowser(state.htmlUrl) }) {
                                    Text("Open GitHub Actions Summary", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                is BuildUiState.Failure -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = CrashRed,
                                modifier = Modifier.size(60.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Build Failed",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 18.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = state.message,
                                color = Color.LightGray,
                                fontSize = 13.sp
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    viewModel.buildProject(owner, repo, branch, localProjectDir)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                            ) {
                                Text("Retry Build", color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            if (!state.htmlUrl.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = { openBrowser(state.htmlUrl) }) {
                                    Text("Inspect Error Logs on GitHub", color = AccentYellow, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Double.format(digits: Int) = "%.${digits}f".format(this)
