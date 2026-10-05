package com.appinspector.services

import com.topjohnwu.superuser.Shell

interface RootCallback {
    fun onResult(available: Boolean, granted: Boolean)
}

object RootManager {

    fun checkRoot(callback: RootCallback) {
        Shell.getShell { shell ->
            val isRoot = shell.isRoot
            if (!isRoot) {
                callback.onResult(available = false, granted = false)
                return@getShell
            }

            Shell.cmd("id").submit { result ->
                val isUidZero = result.isSuccess && result.out.any { it.contains("uid=0") }
                callback.onResult(available = true, granted = isUidZero)
            }
        }
    }

    fun requestRoot(callback: RootCallback) {
        Shell.getShell { shell ->
            if (!shell.isRoot) {
                callback.onResult(available = false, granted = false)
                return@getShell
            }

            Shell.cmd("id").submit { result ->
                val granted = result.isSuccess && result.out.any { it.contains("uid=0") }
                callback.onResult(available = true, granted = granted)
            }
        }
    }

    fun isRootAvailableAsync(callback: (Boolean) -> Unit) {
        Shell.getShell { shell ->
            callback(shell.isRoot)
        }
    }

    fun hasRootPermissionAsync(callback: (Boolean) -> Unit) {
        Shell.getShell { shell ->
            if (!shell.isRoot) {
                callback(false)
                return@getShell
            }

            Shell.cmd("id").submit { result ->
                val granted = result.isSuccess && result.out.any { it.contains("uid=0") }
                callback(granted)
            }
        }
    }
}
