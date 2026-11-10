package com.expeditee.plant.identifier.utils

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

object Utils {
    fun openGooglePlayStore(context: Context) {
        val packageName = context.packageName
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = "market://details?id=$packageName".toUri()
            setPackage("com.android.vending")
        }

        if (intent.resolveActivity(context.packageManager) == null) {
            intent.data = "https://play.google.com/store/apps/details?id=$packageName".toUri()
            intent.setPackage(null)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

