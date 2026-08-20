package com.example.util

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import java.net.URLEncoder

object DeviceActionExecutor {
    private const val TAG = "DeviceActionExecutor"

    /**
     * Opens target application by name or common package
     */
    fun openApp(context: Context, appNameOrPackage: String): Boolean {
        val target = appNameOrPackage.lowercase().trim()
        val pm = context.packageManager

        // Direct package ID check
        if (target.contains(".")) {
            try {
                val launchIntent = pm.getLaunchIntentForPackage(target)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error launching package $target", e)
            }
        }

        // Map common app keywords to package IDs and direct intents
        val knownPackages = when {
            target.contains("whatsapp") -> listOf("com.whatsapp", "com.whatsapp.w4b")
            target.contains("instagram") || target.contains("insta") -> listOf("com.instagram.android")
            target.contains("youtube") || target.contains("yt") -> listOf("com.google.android.youtube")
            target.contains("spotify") -> listOf("com.spotify.music")
            target.contains("map") || target.contains("google maps") -> listOf("com.google.android.apps.maps")
            target.contains("messenger") || target.contains("fb messenger") -> listOf("com.facebook.orca")
            target.contains("facebook") || target.contains("fb") -> listOf("com.facebook.katana")
            target.contains("telegram") -> listOf("org.telegram.messenger")
            target.contains("tiktok") -> listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")
            target.contains("twitter") || target.contains(" x") || target == "x" -> listOf("com.twitter.android")
            target.contains("chrome") || target.contains("browser") -> listOf("com.android.chrome")
            else -> emptyList()
        }

        for (pkg in knownPackages) {
            try {
                val intent = pm.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed launching $pkg", e)
            }
        }

        // System Action Fallbacks (Settings, Camera, Browser)
        return try {
            when {
                target.contains("setting") -> {
                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    true
                }
                target.contains("camera") -> {
                    val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    true
                }
                target.contains("browser") || target.contains("web") -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    true
                }
                target.contains("whatsapp") -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    true
                }
                target.contains("youtube") -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    true
                }
                target.contains("instagram") -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallback launch error", e)
            false
        }
    }

    /**
     * Executes Google Search or opens web browser with search query
     */
    fun performGoogleSearch(context: Context, query: String): Boolean {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$encodedQuery")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            true
        } catch (e: Exception) {
            try {
                val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                    putExtra(SearchManager.QUERY, query)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(searchIntent)
                true
            } catch (ex: Exception) {
                Log.e(TAG, "Search execution error", ex)
                false
            }
        }
    }
}
