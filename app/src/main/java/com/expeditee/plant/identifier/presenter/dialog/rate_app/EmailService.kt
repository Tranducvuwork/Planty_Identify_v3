package com.expeditee.plant.identifier.presenter.dialog.rate_app

import android.content.Context
import android.os.Build
import android.util.Log
import com.expeditee.plant.identifier.common.Constants
import com.expeditee.plant.identifier.helper.firebase.RemoteConfigManagement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Locale
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailService : KoinComponent {

    private val remoteConfig: RemoteConfigManagement by inject()
    private const val SMTP_HOST = "smtp.gmail.com"
    private const val SMTP_PORT = "587"

    private fun getUsername(): String {
        val username = remoteConfig.getSmtpUsername()
        return username
    }
    
    private fun getPassword(): String {
        val password = remoteConfig.getSmtpPassword()
        return password
    }

    fun sendFeedbackEmail(
        context: Context,
        feedbackData: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val username = getUsername()
        val password = getPassword()
        
        if (username.isEmpty() || password.isEmpty()) {
            val errorMsg = if (!remoteConfig.isConfigFetched) {
                "SMTP credentials are not configured. Please wait for remote config to load."
            } else {
                "SMTP credentials are not configured. Please configure config_smtp_username and config_smtp_password in Firebase Remote Config."
            }
            Log.e("EmailService", errorMsg)
            onError(errorMsg)
            return
        } else {
            sendEmailViaSMTP(feedbackData, context, username, password, onSuccess, onError)
        }
    }

    private fun buildEmailSubject(context: Context): String {
        val appName = "Plant Identifier"
        val appVersion = getAppVersion(context)
        return "Feedback - $appName - $appVersion"
    }

    private fun buildEmailBody(feedbackData: String, context: Context): String {
        return """
       Feedback : [$feedbackData]
         —————— App Information ——————
         • App Version: ${getAppVersion(context)}
         • Device: ${getDeviceModel()}
         • OS Version: Android ${getAndroidVersion()}
         • Country (Locale): ${getLocale()}
     """.trimIndent()
    }

    private fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getDeviceModel(): String {
        return try {
            "${Build.MANUFACTURER} ${Build.MODEL}"
        } catch (e: Exception) {
            "Unknown Device"
        }
    }

    private fun getAndroidVersion(): String {
        return try {
            Build.VERSION.RELEASE
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getLocale(): String {
        return try {
            Locale.getDefault().language
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun sendEmailViaSMTP(
        feedbackData: String,
        context: Context,
        username: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val properties = Properties().apply {
                    put("mail.smtp.host", SMTP_HOST)
                    put("mail.smtp.port", SMTP_PORT)
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.starttls.required", "true")
                    put("mail.smtp.ssl.trust", SMTP_HOST)
                    put("mail.smtp.ssl.protocols", "TLSv1.2")
                    put("mail.smtp.ssl.checkserveridentity", "true")
                    put("mail.debug", "true")
                }

                val session = Session.getInstance(properties, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(username, password)
                    }
                })

                val fromEmail = username
                val toEmail = Constants.EMAIL
                val subject = buildEmailSubject(context)
                val body = buildEmailBody(feedbackData, context)

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(fromEmail, "Feedback Plant Identifier"))
                    setRecipients(
                        Message.RecipientType.TO,
                        InternetAddress.parse(toEmail)
                    )
                    setSubject(subject)
                    setText(body)
                }
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
                withContext(Dispatchers.Unconfined) {
                    Transport.send(message)
                }
            } catch (e: Exception) {
                Log.e("EmailService", "Failed to send email", e)
                withContext(Dispatchers.Main) {
                    onError("SMTP failed: ${e.message}")
                }
            }
        }
    }
}

