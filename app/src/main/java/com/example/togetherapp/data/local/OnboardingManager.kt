package com.example.togetherapp.data.local

import android.content.Context

class OnboardingManager private constructor(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, completed)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

        @Volatile
        private var instance: OnboardingManager? = null

        fun getInstance(context: Context): OnboardingManager {
            return instance ?: synchronized(this) {
                instance ?: OnboardingManager(context.applicationContext).also { instance = it }
            }
        }
    }
}