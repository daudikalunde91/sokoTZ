package com.example

import android.app.Application
import com.example.sokotz.data.util.FirebaseInitHelper

class SokoTzApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseInitHelper.ensureInitialized(this)
    }
}
