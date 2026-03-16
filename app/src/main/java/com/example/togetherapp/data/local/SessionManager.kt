package com.example.togetherapp.data.local

import android.content.Context
import android.util.Log
import com.example.togetherapp.domain.models.User

class SessionManager private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private val TAG = "SessionManager"

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_LOGIN = "user_login"
        private const val KEY_USER_AVATAR = "user_avatar"

        // Singleton instance
        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun saveUser(user: User) {
        Log.i(TAG, "saveUser: saving user -> id=${user.id}, login=${user.login}, avatar=${user.avatar_url}")
        prefs.edit()
            .putInt(KEY_USER_ID, user.id)
            .putString(KEY_USER_LOGIN, user.login.toString())
            .putString(KEY_USER_AVATAR, user.avatar_url)
            .apply()
        Log.i(TAG, "saveUser: user saved successfully")
    }

    fun getUserId(): Int {
        val userId = prefs.getInt(KEY_USER_ID, -1)
        Log.i(TAG, "getUserId: current userId = $userId")
        return userId
    }

    fun getUserLogin(): String? {
        val login = prefs.getString(KEY_USER_LOGIN, null)
        Log.i(TAG, "getUserLogin: current login = $login")
        return login
    }

    fun getUserAvatar(): String? {
        val avatar = prefs.getString(KEY_USER_AVATAR, null)
        Log.i(TAG, "getUserAvatar: current avatar = $avatar")
        return avatar
    }

    fun isLoggedIn(): Boolean {
        val loggedIn = getUserId() != -1
        Log.i(TAG, "isLoggedIn: $loggedIn")
        return loggedIn
    }

    fun clearSession() {
        Log.i(TAG, "clearSession: clearing user session")
        prefs.edit().clear().apply()
        Log.i(TAG, "clearSession: session cleared")
    }
}