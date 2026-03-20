package com.example.togetherapp.data.local

import android.content.Context
import android.util.Log
import com.example.togetherapp.domain.models.User

class SessionManager private constructor(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "SessionManager"

        private const val PREFS_NAME = "app_prefs"

        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_LOGIN = "user_login"
        private const val KEY_USER_AVATAR = "user_avatar"

        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }


    // SAVE

    fun saveUser(user: User) {
        Log.d(TAG, "Saving user: id=${user.id}, login=${user.login}")

        prefs.edit()
            .putInt(KEY_USER_ID, user.id)
            .putString(KEY_USER_LOGIN, user.login ?: "")
            .putString(KEY_USER_AVATAR, user.avatar_url ?: "")
            .apply()
    }

    // GETTERS

    fun getUserId(): Int {
        val id = prefs.getInt(KEY_USER_ID, -1)
        Log.d(TAG, "getUserId: $id")
        return id
    }

    fun getUserLogin(): String {
        val login = prefs.getString(KEY_USER_LOGIN, "") ?: ""
        Log.d(TAG, "getUserLogin: $login")
        return login
    }

    fun getUserAvatar(): String {
        val avatar = prefs.getString(KEY_USER_AVATAR, "") ?: ""
        Log.d(TAG, "getUserAvatar: $avatar")
        return avatar
    }


    // AUTH STATE

    fun isLoggedIn(): Boolean {
        val loggedIn = getUserId() != -1
        Log.d(TAG, "isLoggedIn: $loggedIn")
        return loggedIn
    }


    // CLEAR

    fun clearSession() {
        Log.d(TAG, "Clearing session")
        prefs.edit().clear().apply()
    }
}