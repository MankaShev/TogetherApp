package com.example.togetherapp

import android.app.Application
import com.yandex.mapkit.MapKitFactory

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        MapKitFactory.setApiKey("")
        MapKitFactory.initialize(this)
    }
}
