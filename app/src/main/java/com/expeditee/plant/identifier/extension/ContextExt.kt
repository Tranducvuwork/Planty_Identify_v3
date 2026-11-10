package com.expeditee.plant.identifier.extension

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import com.expeditee.plant.identifier.helper.AppConfigSettings
import org.koin.java.KoinJavaComponent
import java.io.Serializable
import java.util.Locale

fun Context?.showToast(message: String, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, length).show()
}

fun Context?.showToast(messageRes: Int, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, messageRes, length).show()
}

fun Context.drawable(@DrawableRes drawable: Int): android.graphics.drawable.Drawable? {
    return AppCompatResources.getDrawable(this, drawable)
}

fun Context.openWebBrowser(url: String) {
    val intent = android.content.Intent(
        android.content.Intent.ACTION_VIEW,
        Uri.parse(url)
    )
    this.startActivity(intent)
}

fun Context.dpToPx(dp: Int): Float {
    return dp * resources.displayMetrics.density
}

fun heightScreen() = Resources.getSystem().displayMetrics.heightPixels
fun widthScreen() = Resources.getSystem().displayMetrics.widthPixels

fun Context.isNetwork(): Boolean {
    val cm = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return cm.activeNetworkInfo != null && cm.activeNetworkInfo?.isConnected == true
}
