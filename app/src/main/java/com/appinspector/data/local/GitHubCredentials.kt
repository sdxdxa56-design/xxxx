package com.appinspector.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class GitHubCredentials(context: Context) {

    private val sharedPreferences: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        sharedPreferences = EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveToken(token: String) {
        sharedPreferences.edit()
            .putString(KEY_GITHUB_TOKEN, token.trim())
            .apply()
    }

    fun getToken(): String? {
        return sharedPreferences.getString(KEY_GITHUB_TOKEN, null)
    }

    fun clearToken() {
        sharedPreferences.edit()
            .remove(KEY_GITHUB_TOKEN)
            .apply()
    }

    fun isLoggedIn(): Boolean {
        val token = getToken()
        return !token.isNullOrBlank()
    }

    companion object {
        private const val PREFS_FILE_NAME = "github_secure_prefs"
        private const val KEY_GITHUB_TOKEN = "key_github_pat"
    }
}
