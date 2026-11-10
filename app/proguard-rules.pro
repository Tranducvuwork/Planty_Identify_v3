# ULTIMATE ProGuard Rules - Khắc phục triệt để ClassCastException
# Sử dụng khi cần bảo vệ tối đa

# =============================================================================
# CORE PROTECTION - BẢO VỆ TẤT CẢ
# =============================================================================

# Keep ALL attributes
-keepattributes *
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes Exceptions
-keepattributes SourceFile
-keepattributes LineNumberTable

# Keep ALL classes in the main package
-keep class com.expeditee.plant.identifier.model.** { *; }
-keep interface com.expeditee.plant.identifier.model.** { *; }
-keep enum com.expeditee.plant.identifier.model.** { *; }

# Keep ALL Parcelable implementations
-keep class * implements android.os.Parcelable { *; }
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep ALL Parcelize annotations
-keep @kotlinx.parcelize.Parcelize class * { *; }
-keep class kotlinx.parcelize.** { *; }
-keep class kotlinx.android.parcel.** { *; }

# =============================================================================
# RETROFIT PROTECTION - BẢO VỆ TỐI ĐA
# =============================================================================

# Keep ALL Retrofit classes
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keep enum retrofit2.** { *; }

# Keep ALL Retrofit annotations
-keep @retrofit2.http.* class * { *; }
-keep @retrofit2.http.* interface * { *; }
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers interface * {
    @retrofit2.http.* <methods>;
}

# Keep ALL Retrofit method signatures
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers,allowshrinking,allowobfuscation class * {
    @retrofit2.http.* <methods>;
}

# Keep ALL Retrofit data classes (app package)
-keep class com.expeditee.plant.identifier.network.** { *; }
-keep interface com.expeditee.plant.identifier.network.** { *; }

# Keep ALL model classes for JSON parsing (app package)
-keep class com.expeditee.plant.identifier.model.** { *; }
-keep interface com.expeditee.plant.identifier.model.** { *; }
-keep enum com.expeditee.plant.identifier.model.** { *; }

# Keep ALL domain model classes (app package)
-keep class com.expeditee.plant.identifier.domain.** { *; }
-keep interface com.expeditee.plant.identifier.domain.** { *; }
-keep enum com.expeditee.plant.identifier.domain.** { *; }

# Keep ALL Retrofit Response types
-keep class retrofit2.Response { *; }
-keep class okhttp3.ResponseBody { *; }
-keep class okhttp3.** { *; }

# =============================================================================
# JSON & SERIALIZATION PROTECTION
# =============================================================================

# Keep ALL Gson classes
-keep class com.google.gson.** { *; }
-keep interface com.google.gson.** { *; }
-keep enum com.google.gson.** { *; }

# Keep ALL Gson TypeToken
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }
-keepclassmembers class * extends com.google.gson.reflect.TypeToken {
    <init>(...);
}

# Keep ALL Gson adapters
-keep class * implements com.google.gson.TypeAdapterFactory { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }
-keep class * implements com.google.gson.JsonDeserializer { *; }

# =============================================================================
# KOTLIN PROTECTION
# =============================================================================

# Keep ALL Kotlin classes
-keep class kotlin.** { *; }
-keep interface kotlin.** { *; }
-keep enum kotlin.** { *; }

# Keep ALL Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }
-keep interface kotlinx.coroutines.** { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep ALL Kotlin reflection
-keep class kotlin.reflect.** { *; }
-keep class kotlin.Metadata { *; }

# =============================================================================
# ANDROIDX PROTECTION
# =============================================================================

# Keep ALL AndroidX classes
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-keep enum androidx.** { *; }

# Keep ALL Room classes
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }

# Keep ALL Navigation classes
-keep class androidx.navigation.** { *; }

# Keep ALL Lifecycle classes
-keep class androidx.lifecycle.** { *; }

# Keep ALL ViewBinding classes
-keep class * extends androidx.viewbinding.ViewBinding { *; }

# Keep ALL DataBinding classes
-keep class androidx.databinding.** { *; }

# =============================================================================
# THIRD-PARTY LIBRARIES PROTECTION
# =============================================================================

# Keep ALL Koin classes
-keep class org.koin.** { *; }

# Keep ALL Glide classes
-keep class com.bumptech.glide.** { *; }

# Keep ALL Markwon classes
-keep class io.noties.markwon.** { *; }

# Keep ALL PDFBox classes
-keep class com.tom_roush.pdfbox.** { *; }

# Keep ALL TensorFlow Lite classes
-keep class org.tensorflow.lite.** { *; }
# Keep ALL ML Kit classes
-keep class com.google.mlkit.** { *; }

# Bảo vệ native libraries của TensorFlow Lite - Fix lỗi DT_HASH/DT_GNU_HASH trên Android 15
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# Không optimize/strip native libraries - Critical cho Android 15
-keep,allowoptimization,allowobfuscation class org.tensorflow.lite.** { *; }
-keepnames class org.tensorflow.lite.Interpreter
-keepnames class org.tensorflow.lite.Interpreter$Options
-keepnames class org.tensorflow.lite.TensorFlowLite
-keepnames class org.tensorflow.lite.NativeInterpreterWrapper
-keepnames class org.tensorflow.lite.NativeInterpreterWrapperExperimental

# Giữ nguyên JNI methods để native libraries hoạt động đúng
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Keep ALL other third-party libraries
-keep class com.github.** { *; }
-keep class com.chibatching.** { *; }
-keep class com.airbnb.** { *; }
-keep class permissions.** { *; }
-keep class com.tbuonomo.** { *; }
-keep class com.makeramen.** { *; }
-keep class com.eightbitlab.** { *; }
-keep class org.threeten.** { *; }

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep callback classes
-keep class * implements com.google.android.gms.tasks.OnCompleteListener { *; }
-keep class * implements com.google.android.gms.tasks.OnSuccessListener { *; }
-keep class * implements com.google.android.gms.tasks.OnFailureListener { *; }

# =============================================================================
# ENUM PROTECTION
# =============================================================================

# Keep ALL enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# =============================================================================
# REFLECTION PROTECTION
# =============================================================================

# Keep ALL reflection classes
-keep class java.lang.reflect.** { *; }
-keep class * extends java.lang.reflect.ParameterizedType { *; }
-keep class * extends java.lang.reflect.GenericArrayType { *; }
-keep class * extends java.lang.reflect.WildcardType { *; }

# =============================================================================
# ADMOB PROTECTION - BẢO VỆ ADMOB CLASSES
# =============================================================================

# Keep ALL AdMob classes
-keep class com.google.android.gms.ads.** { *; }
-keep interface com.google.android.gms.ads.** { *; }
-keep enum com.google.android.gms.ads.** { *; }

# Keep custom ads library
-keep class com.dqh.lmtech.ads.** { *; }
-keep interface com.dqh.lmtech.ads.** { *; }
-keep enum com.dqh.lmtech.ads.** { *; }

# Keep AdMob mediation classes
-keep class * implements com.google.android.gms.ads.mediation.** { *; }
-keep class com.google.ads.mediation.** { *; }

# Keep AdMob listeners
-keep class * implements com.google.android.gms.ads.OnAdLoadedListener { *; }
-keep class * implements com.google.android.gms.ads.OnAdFailedToLoadListener { *; }
-keep class * implements com.google.android.gms.ads.OnAdOpenedListener { *; }
-keep class * implements com.google.android.gms.ads.OnAdClosedListener { *; }
-keep class * implements com.google.android.gms.ads.OnAdClickedListener { *; }

# Keep BuildConfig fields
-keep class com.expeditee.plant.identifier.BuildConfig { *; }
-keep class com.expeditee.plant.identifier.** { *; }

# Keep Facebook SDK for mediation
-keep class com.facebook.** { *; }

# Keep AppLovin for mediation
-keep class com.applovin.** { *; }

# Keep Unity Ads for mediation
-keep class com.unity3d.ads.** { *; }

# Keep Vungle for mediation
-keep class com.vungle.** { *; }

# Keep Mintegral for mediation
-keep class com.mbridge.** { *; }

# Keep Pangle for mediation
-keep class com.bytedance.** { *; }

# Keep InMobi for mediation
-keep class com.inmobi.** { *; }

# =============================================================================
# WARNINGS SUPPRESSION
# =============================================================================

-dontwarn **
-dontnote **
-dontoptimize
-dontobfuscate


-keep class javax.mail.** { *; }
-keep class com.sun.mail.** { *; }
-keep class javax.activation.** { *; }

-dontwarn javax.mail.**
-dontwarn com.sun.mail.**
-dontwarn javax.activation.**
