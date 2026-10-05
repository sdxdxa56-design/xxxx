package com.appinspector.presentation.launcher

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.appinspector.services.ApkInstaller
import com.appinspector.services.LogcatService
import com.appinspector.services.ProcessTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class LaunchState {
    data object Idle : LaunchState()
    data class Installing(val packageName: String) : LaunchState()
    data class WaitingForLaunch(val packageName: String, val seconds: Long) : LaunchState()
    data class Tracking(val packageName: String, val pid: Int) : LaunchState()
    data class Error(val message: String) : LaunchState()
}

class LaunchViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _launchState = MutableStateFlow<LaunchState>(LaunchState.Idle)
    val launchState: StateFlow<LaunchState> = _launchState.asStateFlow()

    private var trackingJob: Job? = null

    fun installAndTrack(apkFile: File) {
        trackingJob?.cancel()
        trackingJob = viewModelScope.launch {
            val context = getApplication<Application>()
            val packageName = ApkInstaller.getPackageName(context, apkFile)

            if (packageName.isNullOrBlank()) {
                _launchState.value = LaunchState.Error("Unable to extract application ID from the generated APK file.")
                return@launch
            }

            _launchState.value = LaunchState.Installing(packageName)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                _launchState.value = LaunchState.Error("Permission required to install unknown apps. Please allow and retry.")
                return@launch
            }

            try {
                val apkUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(installIntent)
            } catch (e: Exception) {
                _launchState.value = LaunchState.Error("Failed to trigger installation: ${e.message}")
                return@launch
            }

            val startTime = System.currentTimeMillis()
            val timeoutMs = 60000L
            var foundPid: Int? = null

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000
                _launchState.value = LaunchState.WaitingForLaunch(packageName, elapsedSeconds)

                foundPid = ProcessTracker.findPidForPackage(packageName)
                if (foundPid != null && foundPid > 0) {
                    break
                }
                delay(1000)
            }

            if (foundPid != null && foundPid > 0) {
                LogcatService.setTargetPackage(context, packageName, foundPid)
                _launchState.value = LaunchState.Tracking(packageName, foundPid)
            } else {
                LogcatService.setTargetPackage(context, packageName, null)
                _launchState.value = LaunchState.Tracking(packageName, 0)
            }
        }
    }

    fun resetState() {
        trackingJob?.cancel()
        _launchState.value = LaunchState.Idle
    }
}
