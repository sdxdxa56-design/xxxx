package com.appinspector.services

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File

object ApkInstaller {

    fun getPackageName(context: Context, apkFile: File): String? {
        if (!apkFile.exists()) return null
        return try {
            val packageManager = context.packageManager
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageArchiveInfo(
                    apkFile.absolutePath,
                    PackageManager.PackageInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
            }
            packageInfo?.packageName
        } catch (_: Exception) {
            null
        }
    }

    fun getAppLabel(context: Context, apkFile: File): String? {
        if (!apkFile.exists()) return null
        return try {
            val packageManager = context.packageManager
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageArchiveInfo(
                    apkFile.absolutePath,
                    PackageManager.PackageInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
            }

            val appInfo = packageInfo?.applicationInfo ?: return packageInfo?.packageName
            appInfo.sourceDir = apkFile.absolutePath
            appInfo.publicSourceDir = apkFile.absolutePath
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            getPackageName(context, apkFile)
        }
    }
}
