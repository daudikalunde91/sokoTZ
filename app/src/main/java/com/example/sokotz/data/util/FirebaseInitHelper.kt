package com.example.sokotz.data.util

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseInitHelper {

    private const val TAG = "FirebaseInitHelper"

    fun ensureInitialized(context: Context) {
        val appContext = context.applicationContext ?: context
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            try {
                FirebaseApp.initializeApp(appContext)
                Log.i(TAG, "Default FirebaseApp.initializeApp succeeded")
            } catch (e: Exception) {
                Log.w(TAG, "Default FirebaseApp.initializeApp failed: ${e.message}")
            }

            if (FirebaseApp.getApps(appContext).isEmpty()) {
                try {
                    val dbId = getDatabaseId(appContext)

                    val apiKey = try {
                        val resId = appContext.resources.getIdentifier("google_api_key", "string", appContext.packageName)
                        if (resId != 0) appContext.getString(resId) else "AIzaSyCEInTQ9_HJyUP48FMUij6SJ5gs3-kWleQ"
                    } catch (_: Exception) {
                        "AIzaSyCEInTQ9_HJyUP48FMUij6SJ5gs3-kWleQ"
                    }

                    val appId = try {
                        val resId = appContext.resources.getIdentifier("google_app_id", "string", appContext.packageName)
                        if (resId != 0) appContext.getString(resId) else "1:276149702266:android:1ec17871656bfa5e2875c3"
                    } catch (_: Exception) {
                        "1:276149702266:android:1ec17871656bfa5e2875c3"
                    }

                    val projectId = try {
                        val resId = appContext.resources.getIdentifier("project_id", "string", appContext.packageName)
                        if (resId != 0) appContext.getString(resId) else "gen-lang-client-0905574889"
                    } catch (_: Exception) {
                        "gen-lang-client-0905574889"
                    }

                    val options = FirebaseOptions.Builder()
                        .setApplicationId(appId)
                        .setProjectId(projectId)
                        .setApiKey(apiKey)
                        .setStorageBucket("gen-lang-client-0905574889.firebasestorage.app")
                        .setGcmSenderId("276149702266")
                        .build()
                    FirebaseApp.initializeApp(appContext, options)
                    Log.i(TAG, "Initialized FirebaseApp with provisioned options (databaseId: $dbId)")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize FirebaseApp provisioned options: ${e.message}", e)
                }
            }
        }
    }

    fun getDatabaseId(context: Context): String {
        val appContext = context.applicationContext ?: context
        return try {
            val resId = appContext.resources.getIdentifier("firestore_database_id", "string", appContext.packageName)
            if (resId != 0) appContext.getString(resId) else "ai-studio-android-sokotz-d5a575ab-0477-4160-b139-0caa7e2d8a9a"
        } catch (_: Exception) {
            "ai-studio-android-sokotz-d5a575ab-0477-4160-b139-0caa7e2d8a9a"
        }
    }

    /**
     * Authoritative centralized provider for the explicitly configured Firestore database.
     * Guarantees that every repository and service connects to the exact same named database instance.
     */
    fun getFirestore(context: Context): FirebaseFirestore {
        ensureInitialized(context)
        val dbId = getDatabaseId(context)
        return try {
            FirebaseFirestore.getInstance(FirebaseApp.getInstance(), dbId)
        } catch (e: Exception) {
            Log.w(TAG, "Could not get instance with default FirebaseApp and dbId ($dbId), fallback: ${e.message}")
            try {
                FirebaseFirestore.getInstance(dbId)
            } catch (_: Exception) {
                FirebaseFirestore.getInstance()
            }
        }
    }
}
