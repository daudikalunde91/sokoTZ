package com.example.sokotz.data.util

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

object FirebaseInitHelper {

    fun ensureInitialized(context: Context) {
        val appContext = context.applicationContext ?: context
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            try {
                FirebaseApp.initializeApp(appContext)
            } catch (e: Exception) {
                Log.w("FirebaseInitHelper", "Default FirebaseApp.initializeApp failed: ${e.message}")
            }

            if (FirebaseApp.getApps(appContext).isEmpty()) {
                try {
                    val dbId = try {
                        val resId = appContext.resources.getIdentifier("firestore_database_id", "string", appContext.packageName)
                        if (resId != 0) appContext.getString(resId) else "ai-studio-android-sokotz-17bca192-1896-4b7e-b183-ea8b41319929"
                    } catch (_: Exception) {
                        "ai-studio-android-sokotz-17bca192-1896-4b7e-b183-ea8b41319929"
                    }

                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:732857977004:android:sokotz")
                        .setProjectId("ai-studio-sokotz")
                        .setApiKey("fake-api-key-for-development")
                        .build()
                    FirebaseApp.initializeApp(appContext, options)
                    Log.i("FirebaseInitHelper", "Initialized FirebaseApp with fallback options (databaseId: $dbId)")
                } catch (e: Exception) {
                    Log.e("FirebaseInitHelper", "Failed to initialize FirebaseApp fallback options: ${e.message}", e)
                }
            }
        }
    }
}
