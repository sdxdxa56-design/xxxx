package com.appinspector.presentation.permission

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appinspector.presentation.theme.PrimaryEmerald
import com.appinspector.services.RootCallback
import com.appinspector.services.RootManager

@Composable
fun PermissionCheckScreen(
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    var isRootAvailable by remember { mutableStateOf(false) }
    var hasRootPermission by remember { mutableStateOf(false) }
    var isCheckingRoot by remember { mutableStateOf(true) }

    val hasReadLogs = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.checkSelfPermission(android.Manifest.permission.READ_LOGS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    LaunchedEffect(Unit) {
        RootManager.checkRoot(object : RootCallback {
            override fun onResult(available: Boolean, granted: Boolean) {
                isRootAvailable = available
                hasRootPermission = granted
                isCheckingRoot = false
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = PrimaryEmerald,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "System Permissions Check",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "App Inspector needs logcat access to monitor crash events across Android OS.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Root Status (libsu)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Root Available (`su`)", color = Color.LightGray)
                        Text(
                            text = if (isRootAvailable) "Available" else "Not Detected",
                            color = if (isRootAvailable) PrimaryEmerald else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Root Permission Granted", color = Color.LightGray)
                        Text(
                            text = if (hasRootPermission) "Granted" else "Not Granted",
                            color = if (hasRootPermission) PrimaryEmerald else Color(0xFFF59E0B),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isRootAvailable && !hasRootPermission) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                RootManager.requestRoot(object : RootCallback {
                                    override fun onResult(available: Boolean, granted: Boolean) {
                                        isRootAvailable = available
                                        hasRootPermission = granted
                                    }
                                })
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                        ) {
                            Text(text = "Grant Root Permission", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "READ_LOGS Permission", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = if (hasReadLogs) "Granted via OS" else "Fallback ADB mode available",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = if (hasReadLogs || hasRootPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (hasReadLogs || hasRootPermission) PrimaryEmerald else Color(0xFFF59E0B)
                    )
                }
            }
        }

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
        ) {
            Text(
                text = "Continue to App",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
