package com.expeditee.plant.identifier.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri
import com.expeditee.plant.identifier.BuildConfig
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.Constants

object AppUtils {

    internal fun shareApp(context: Context) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(
                Intent.EXTRA_SUBJECT, context.getString(R.string.app_name)
            )
            var shareMessage = "\nLet me recommend you this application\n\n"
            shareMessage =
                "${shareMessage}https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
            shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(Intent.createChooser(shareIntent, "Choose one"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    internal fun openLink(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun rateApp(context: Context) {
        try {
            val uri = Uri.parse("market://details?id=${context.packageName}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NO_HISTORY or
                            Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                            Intent.FLAG_ACTIVITY_NEW_TASK
                )
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val uri =
                Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun sendFeedback(
        context: Context,
        appName: String,
        emailAddress: String,
        message: String? = null
    ) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:")
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
            val subject = context.getString(R.string.feedback_subject, appName)
            intent.putExtra(Intent.EXTRA_SUBJECT, subject)

            if (message != null) {
                intent.putExtra(Intent.EXTRA_TEXT, message)
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.no_email_app_found),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun openTermsAndPolicy(context: Context, termsUrl: String = Constants.POLICY_URL) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(termsUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.cannot_open_terms),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

}
