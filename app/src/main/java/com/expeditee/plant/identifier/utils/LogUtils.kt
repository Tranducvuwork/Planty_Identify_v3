package com.expeditee.plant.identifier.utils

import android.util.Log

object LogUtils {
    private const val TAG = "PlantyApp"

    fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    fun e(tag: String, message: String) {
        Log.e(tag, message)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
    }

    fun w(tag: String, message: String) {
        Log.w(tag, message)
    }

    fun v(tag: String, message: String) {
        Log.v(tag, message)
    }
}
