package com.example.cocktailbar.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.cocktailbar.data.remote.SupabaseDataSource

class AuthRepository(
    context: Context,
    private val dataSource: SupabaseDataSource = SupabaseDataSource()
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    var isAdminLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        private set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    suspend fun login(username: String, password: String): Result<Boolean> {
        return runCatching {
            val isValid = dataSource.verifyAdminCredentials(username, password)
            if (isValid) {
                isAdminLoggedIn = true
            }
            isValid
        }
    }

    fun logout() {
        isAdminLoggedIn = false
    }

    suspend fun checkConnection(): Boolean {
        return try {
            dataSource.pingConnection()
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val PREFS_NAME = "cocktailbar_prefs"
        private const val KEY_IS_LOGGED_IN = "is_admin_logged_in"
    }
}