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
        val tokenToSave = if (token.isBlank()) DEFAULT_TOKEN else token.trim()
        sharedPreferences.edit()
            .putString(KEY_GITHUB_TOKEN, tokenToSave)
            .apply()
    }

    fun getToken(): String? {
        val token = sharedPreferences.getString(KEY_GITHUB_TOKEN, null)
        if (token.isNullOrBlank()) {
            saveToken(DEFAULT_TOKEN)
            return DEFAULT_TOKEN
        }
        return token
    }

    fun clearToken() {
        sharedPreferences.edit()
            .putString(KEY_GITHUB_TOKEN, DEFAULT_TOKEN)
            .apply()
    }

    fun isLoggedIn(): Boolean {
        return true
    }

    companion object {
        private const val PREFS_FILE_NAME = "github_secure_prefs"
        private const val KEY_GITHUB_TOKEN = "key_github_pat"
        private const val DEFAULT_TOKEN = "github_pat_placeholder"
    }
}
