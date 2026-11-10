package com.expeditee.plant.identifier.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import com.expeditee.plant.identifier.admob.LammaAdManager
import com.expeditee.plant.identifier.admob.intro.IntroAdHelper
import com.expeditee.plant.identifier.admob.manager.PreloadInterstitialAdManagement
import com.expeditee.plant.identifier.admob.manager.PreloadNativeAdManagement
import com.expeditee.plant.identifier.admob.manager.PreloadRewardAdManagement
import com.expeditee.plant.identifier.classifier.IdentificationClassifier
import com.expeditee.plant.identifier.common.CommonViewModel
import com.expeditee.plant.identifier.data.AppDatabase
import com.expeditee.plant.identifier.data.RemoteDataManager
import com.expeditee.plant.identifier.data.local.ScanLimitPreferences
import com.expeditee.plant.identifier.data.mapper.MyPlantMapper
import com.expeditee.plant.identifier.data.mapper.PlantMapper
import com.expeditee.plant.identifier.data.mapper.ReminderMapper
import com.expeditee.plant.identifier.data.repository.DiseaseRepository
import com.expeditee.plant.identifier.data.repository.DiseaseRepositoryImpl
import com.expeditee.plant.identifier.data.repository.MyPlantRepository
import com.expeditee.plant.identifier.data.repository.MyPlantRepositoryImpl
import com.expeditee.plant.identifier.data.repository.OfflinePlantIdentifier
import com.expeditee.plant.identifier.data.repository.OnlinePlantIdentifier
import com.expeditee.plant.identifier.data.repository.PlantDiseaseScanRepository
import com.expeditee.plant.identifier.data.repository.PlantRepository
import com.expeditee.plant.identifier.data.repository.PlantRepositoryImpl
import com.expeditee.plant.identifier.data.repository.NvidiaVietnameseNameRepository
import com.expeditee.plant.identifier.data.repository.PlantScanRepository
import com.expeditee.plant.identifier.data.repository.ReminderRepositoryImpl
import com.expeditee.plant.identifier.data.repository.VietnameseTranslationRepository
import com.expeditee.plant.identifier.network.NvidiaApiClient
import com.expeditee.plant.identifier.data.upload.UploadImageFileResolver
import com.expeditee.plant.identifier.domain.MainApi
import com.expeditee.plant.identifier.domain.github.GithubApi
import com.expeditee.plant.identifier.domain.impl.PlantDiagnoseRepositoryImpl
import com.expeditee.plant.identifier.domain.impl.PlantIdentifyRepositoryImpl
import com.expeditee.plant.identifier.domain.repository.PlantDiagnoseRepository
import com.expeditee.plant.identifier.domain.repository.PlantIdentifyRepository
import com.expeditee.plant.identifier.domain.repository.ReminderRepository
import com.expeditee.plant.identifier.helper.AppConfigSettings
import com.expeditee.plant.identifier.helper.SafeImageDecoder
import com.expeditee.plant.identifier.helper.firebase.RemoteConfigManagement
import com.expeditee.plant.identifier.presenter.camera.ScanCameraViewModel
import com.expeditee.plant.identifier.presenter.careguide.CareGuideDetailViewModel
import com.expeditee.plant.identifier.presenter.careguide.CareGuidesViewModel
import com.expeditee.plant.identifier.presenter.chat.PlantAssistantChatViewModel
import com.expeditee.plant.identifier.presenter.detail.PlantDetailViewModel
import com.expeditee.plant.identifier.presenter.disease_detail.DiseaseDetailViewModel
import com.expeditee.plant.identifier.presenter.disease_list.DiseaseListViewModel
import com.expeditee.plant.identifier.presenter.faq.FaqListViewModel
import com.expeditee.plant.identifier.presenter.faq.tag.FaqTagViewModel
import com.expeditee.plant.identifier.presenter.home.HomeViewModel
import com.expeditee.plant.identifier.presenter.home.explore.ExploreTabViewModel
import com.expeditee.plant.identifier.presenter.home.mygardentab.MyGardenTabViewModel
import com.expeditee.plant.identifier.presenter.home.search.SearchViewModel
import com.expeditee.plant.identifier.presenter.home.uihome.HomeTabViewModel
import com.expeditee.plant.identifier.presenter.language.LanguageViewModel
import com.expeditee.plant.identifier.presenter.lightmeter.LightMeterViewModel
import com.expeditee.plant.identifier.presenter.onboarding.OnboardingViewModel
import com.expeditee.plant.identifier.presenter.photo_library.PhotoLibraryViewModel
import com.expeditee.plant.identifier.presenter.processing.ScanProcessingViewModel
import com.expeditee.plant.identifier.presenter.reminder.ReminderViewModel
import com.expeditee.plant.identifier.presenter.reminderall.ReminderAllViewModel
import com.expeditee.plant.identifier.presenter.result_identify.IdentifyResultViewModel
import com.expeditee.plant.identifier.presenter.splash.SplashInterHelper
import com.expeditee.plant.identifier.reminder.ReminderScheduler
import com.expeditee.plant.identifier.repository.PlantAssistantChatRepository
import com.expeditee.plant.identifier.data.repository.PlantChatRepository
import com.expeditee.plant.identifier.services.OfflineModeService
import com.expeditee.plant.identifier.services.LanguageDataPrefetchService
import com.expeditee.plant.identifier.services.ScanLimitService
import com.expeditee.plant.identifier.services.ScanProcessingService
import com.expeditee.plant.identifier.thesis.FolkRemediesViewModel
import com.expeditee.plant.identifier.thesis.HerbalMapViewModel
import com.expeditee.plant.identifier.thesis.PlantDiaryDetailViewModel
import com.expeditee.plant.identifier.thesis.PlantDiaryListViewModel
import com.expeditee.plant.identifier.presenter.home.history.HistoryTabViewModel
import com.expeditee.plant.identifier.thesis.MedicinalCatalogViewModel
import com.expeditee.plant.identifier.thesis.MushroomGuideViewModel
import com.expeditee.plant.identifier.thesis.SymptomRemedyViewModel
import com.expeditee.plant.identifier.thesis.ThesisLocalStore
import com.expeditee.plant.identifier.thesis.ThesisPlantDetailViewModel
import com.expeditee.plant.identifier.thesis.ThesisPlantEditViewModel
import com.expeditee.plant.identifier.thesis.PlantImagePreviewViewModel
import com.expeditee.plant.identifier.thesis.YoutubeCareVideoRepository
import com.expeditee.plant.identifier.thesis.MorphologyImageRepository
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import okhttp3.OkHttpClient
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

interface Feature {
    fun name(): String
    fun diModule(): Module
}

fun allFeature() = listOf(
    coreFeature(),
    MainApi(),
    adsFeature(),
    serviceFeature(),
    viewModels(),
    databaseFeature()
)

private val serviceModule = module {
    single { ScanLimitPreferences(get()) }
    singleOf(::ScanLimitService)
    single<ContentResolver> { get<Context>().contentResolver }
    single {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    singleOf(::IdentificationClassifier)
    singleOf(::RemoteDataManager)
    singleOf(::PlantChatRepository)
    singleOf(::PlantAssistantChatRepository)
    singleOf(::OfflineModeService)
    singleOf(::UploadImageFileResolver)
    single { NvidiaApiClient.createService() }
    singleOf(::NvidiaVietnameseNameRepository)
    singleOf(::YoutubeCareVideoRepository)
    singleOf(::MorphologyImageRepository)
    singleOf(::PlantScanRepository)
    singleOf(::PlantDiseaseScanRepository)
    singleOf(::VietnameseTranslationRepository)
    singleOf(::OnlinePlantIdentifier)
    singleOf(::OfflinePlantIdentifier)
    singleOf(::PlantIdentifyRepositoryImpl)
    singleOf(::ReminderScheduler)
    singleOf(::LanguageDataPrefetchService)
    single { SafeImageDecoder(get(), get<Context>().contentResolver) }
    single<PlantIdentifyRepository> { get<PlantIdentifyRepositoryImpl>() }
    single<PlantDiagnoseRepository> { get<PlantDiagnoseRepositoryImpl>() }
    single { PlantDiagnoseRepositoryImpl(get<Context>(), get(), get(), get(), get()) }
    single { DiseaseRepositoryImpl(get(), get()) } bind DiseaseRepository::class
    singleOf(::ScanProcessingService)
}

private fun serviceFeature() = object : Feature {
    override fun name() = "service"
    override fun diModule() = serviceModule
}

private fun coreFeature() = object : Feature {
    override fun name() = "core"
    override fun diModule() = module {
        singleOf(::AppConfigSettings)
        single { RemoteConfigManagement(Firebase.remoteConfig) }
    }
}

private fun adsFeature() = object : Feature {
    override fun name() = "ads"
    override fun diModule() = module {
        singleOf(::PreloadInterstitialAdManagement)
        singleOf(::PreloadNativeAdManagement)
        singleOf(::PreloadRewardAdManagement)
        singleOf(::LammaAdManager)
        singleOf(::IntroAdHelper)

        factoryOf(::SplashInterHelper)
    }
}

private fun databaseFeature() = object : Feature {
    override fun name() = "database"

    override fun diModule() = module {
        single {
            Room.databaseBuilder(
                get(),
                AppDatabase::class.java,
                AppDatabase.DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }

        singleOf(::PlantMapper)
        singleOf(::MyPlantMapper)
        singleOf(::ReminderMapper)

        single { get<AppDatabase>().reminderDao() }

        single {
            PlantRepositoryImpl(
                get<AppDatabase>(),
                get<GithubApi>(),
                get<PlantMapper>(),
                get<RemoteDataManager>(),
                get<RemoteConfigManagement>(),
                get<AppConfigSettings>(),
            )
        } bind PlantRepository::class

        single {
            MyPlantRepositoryImpl(
                get<AppDatabase>(),
                get<MyPlantMapper>(),
            )
        } bind MyPlantRepository::class

        single {
            ReminderRepositoryImpl(
                get(),
                get()
            )
        } bind ReminderRepository::class
    }
}

private fun viewModels() = object : Feature {
    override fun name() = "viewmodel"
    override fun diModule() = module {
        viewModelOf(::CommonViewModel)
        viewModelOf(::OnboardingViewModel)
        viewModelOf(::LanguageViewModel)
        viewModelOf(::HomeViewModel)
        viewModelOf(::HomeTabViewModel)
        viewModelOf(::ScanCameraViewModel)
        viewModelOf(::ScanProcessingViewModel)
        viewModelOf(::IdentifyResultViewModel)
        viewModelOf(::ReminderViewModel)
        viewModelOf(::ExploreTabViewModel)
        viewModelOf(::MyGardenTabViewModel)
        viewModelOf(::PlantAssistantChatViewModel)
        viewModelOf(::SearchViewModel)
        viewModelOf(::CareGuidesViewModel)
        viewModelOf(::CareGuideDetailViewModel)
        viewModelOf(::FaqListViewModel)
        viewModelOf(::FaqTagViewModel)
        viewModelOf(::DiseaseListViewModel)
        viewModelOf(::DiseaseDetailViewModel)
        viewModelOf(::LightMeterViewModel)
        viewModelOf(::ReminderAllViewModel)
        viewModelOf(::PlantDetailViewModel)
        viewModelOf(::PhotoLibraryViewModel)
        single { ThesisLocalStore(get()) }
        viewModelOf(::ThesisPlantDetailViewModel)
        viewModelOf(::ThesisPlantEditViewModel)
        viewModelOf(::PlantImagePreviewViewModel)
        viewModelOf(::FolkRemediesViewModel)
        viewModelOf(::SymptomRemedyViewModel)
        viewModelOf(::PlantDiaryListViewModel)
        viewModelOf(::PlantDiaryDetailViewModel)
        viewModelOf(::HistoryTabViewModel)
        viewModelOf(::MushroomGuideViewModel)
        viewModelOf(::MedicinalCatalogViewModel)
        viewModelOf(::HerbalMapViewModel)
    }
}
