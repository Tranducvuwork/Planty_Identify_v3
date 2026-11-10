package com.expeditee.plant.identifier.common

import com.expeditee.plant.identifier.BuildConfig

object Constants {
    const val EMAIL = "lmteammarketing@gmail.com"
    const val POLICY_URL: String = "https://expeditee.netlify.app/policy"

    const val IDENTIFY_MODEL_SUB_PATH = "/models/identify.tflite"
    const val TAXONOMY_FILE_NAME = "taxonomy_v1.csv"
    const val TAXONOMY_SUB_PATH = "/models/taxonomy_v1.csv"
    const val IDENTIFY_MODEL_FILE_NAME = "identify_model_v1.tflite"
    const val MODEL_DOWNLOAD_WORK_NAME = "identify_model_download"

    val PLANT_API_BASE_URL: String
        get() = BuildConfig.PLANT_API_BASE_URL.trimEnd('/') + "/"

    const val PLANT_API_KEY = "lamma.scan.plant"
    const val DUOC_DIEN_URL = "https://duocdienvietnam.com/duoc-lieu/"
    const val SCAN_TIMEOUT_MS = 60_000L
    const val NETWORK_TIMEOUT_CONNECT_SECONDS = 10L
    const val NETWORK_TIMEOUT_READ_SECONDS = 15L
    const val NETWORK_TIMEOUT_WRITE_SECONDS = 15L
    const val SCAN_IMAGE_MAX_DIMENSION = 1920
    const val IMAGE_COMPRESSION_QUALITY = 92
}
