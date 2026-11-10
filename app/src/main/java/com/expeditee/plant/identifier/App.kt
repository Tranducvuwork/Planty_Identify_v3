package com.expeditee.plant.identifier

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.chibatching.kotpref.Kotpref
import com.dqh.lmtech.ads.admob.Admob
import com.dqh.lmtech.ads.admob.AppOpenManager
import com.dqh.lmtech.ads.ads.DqhAd
import com.dqh.lmtech.ads.application.AdsMultiDexApplication
import com.dqh.lmtech.ads.config.DqhTechAdConfig
import com.expeditee.plant.identifier.data.repository.PlantRepository
import com.expeditee.plant.identifier.di.Feature
import com.expeditee.plant.identifier.di.allFeature
import com.expeditee.plant.identifier.common.Constants
import com.expeditee.plant.identifier.network.networkModule
import com.expeditee.plant.identifier.presenter.splash.SplashActivity
import com.expeditee.plant.identifier.helper.AppConfigSettings
import com.expeditee.plant.identifier.services.LanguageDataPrefetchService
import com.expeditee.plant.identifier.worker.ModelDownloadWorker
import com.google.android.gms.ads.AdActivity
import com.jakewharton.threetenabp.AndroidThreeTen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class App : AdsMultiDexApplication(), Application.ActivityLifecycleCallbacks,
    DefaultLifecycleObserver {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super<AdsMultiDexApplication>.onCreate()
        registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        AndroidThreeTen.init(this)
        Kotpref.init(this)
        startKoin {
            androidContext(this@App)
            androidLogger()
            modules(
                allFeature().map(Feature::diModule) + networkModule
            )
        }

        // Keep ad SDK init minimal so inherited AdsMultiDexApplication is stable,
        // but never show app-open / resume ads. Billing/premium fully removed.
        setupAdsDisabled()
        setupPreload()
        enqueueModelDownload()
    }

    private fun setupAdsDisabled() {
        val environment = if (BuildConfig.DEBUG) {
            DqhTechAdConfig.ENVIRONMENT_DEVELOP
        } else {
            DqhTechAdConfig.ENVIRONMENT_PRODUCTION
        }
        mDqhTechAdConfig = DqhTechAdConfig(this, environment)
        DqhAd.getInstance().init(this, mDqhTechAdConfig)

        Admob.getInstance().setDisableAdResumeWhenClickAds(true)
        Admob.getInstance().setOpenActivityAfterShowInterAds(false)

        AppOpenManager.getInstance().disableAppResumeWithActivity(SplashActivity::class.java)
        AppOpenManager.getInstance().disableAppResume()
    }

    private fun enqueueModelDownload() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            Constants.MODEL_DOWNLOAD_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    private fun setupPreload() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                val plantRepository: PlantRepository = GlobalContext.get().get()
                plantRepository.preloadAllPlants()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        applicationScope.launch(Dispatchers.IO) {
            try {
                val appConfigSettings: AppConfigSettings = GlobalContext.get().get()
                val languageCode = appConfigSettings.currentLanguage?.takeIf { it.isNotBlank() } ?: "en"
                val languagePrefetchService: LanguageDataPrefetchService = GlobalContext.get().get()
                languagePrefetchService.scheduleStartupPrefetch(languageCode)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        // Notifications disabled
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        // Home / exit / scan push notifications disabled
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        if (activity is AdActivity) {
            activity.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
                WindowInsetsControllerCompat(window, window.decorView).hide(
                    WindowInsetsCompat.Type.systemBars(),
                )
            }
        }
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}
