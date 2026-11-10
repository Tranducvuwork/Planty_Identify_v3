package com.expeditee.plant.identifier.admob

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import com.dqh.lmtech.ads.ads.wrapper.ApNativeAd
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.extension.beGone
import com.expeditee.plant.identifier.extension.beInvisible
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

class NativeAdView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = -1
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "NativeAdView"
    }

    private var shimmerLayout = com.dqh.lmtech.ads.R.layout.ads_shimmer_large
    private var nativeAdLayout = R.layout.ads_native_large
    private val viewModificationLock = Any()
    private var nativeAdPopulated: Boolean = false

    var onPopulatedListener: (() -> Unit)? = null

    internal val populated: Boolean
        get() = nativeAdPopulated

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.NativeAdView, defStyleAttr, 0)
        try {
            shimmerLayout = ta.getResourceId(R.styleable.NativeAdView_adShimmer, shimmerLayout)
            nativeAdLayout = ta.getResourceId(R.styleable.NativeAdView_adView, nativeAdLayout)

            initLoading()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            ta.recycle()
        }
    }

    @JvmName("setShimmerLayout")
    fun setShimmerLayout(@LayoutRes layoutRes: Int) {
        shimmerLayout = layoutRes
        initLoading()
    }

    @JvmName("setNativeLayout")
    fun setNativeLayout(@LayoutRes layoutRes: Int) {
        nativeAdLayout = layoutRes
    }

    private fun initLoading() {
        try {
            nativeAdPopulated = false
            removeAllViews()
            val shimmerLayout = LayoutInflater.from(context).inflate(shimmerLayout, this, false)
            if (shimmerLayout is ShimmerFrameLayout) {
                Log.d(TAG, "Starting shimmer animation")
                shimmerLayout.startShimmer()
            }
            addView(shimmerLayout)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing loading shimmer", e)
            e.printStackTrace()
        }
    }

    fun populateUnifiedView(nativeAd: LammaAd<ApNativeAd>?) {
        nativeAd ?: return run {
            Log.w(TAG, "onLoaded called with null data, removing all views")
            removeAllViews()
        }
        populateUnifiedView(nativeAd.ad, nativeAdLayout)
    }

    fun populateUnifiedView(nativeAd: LammaAd<ApNativeAd>?, @LayoutRes layoutRes: Int) {
        nativeAd ?: return run {
            Log.w(TAG, "onLoaded called with null data, removing all views")
            removeAllViews()
        }
        populateUnifiedView(nativeAd.ad, layoutRes)
    }

    fun populateUnifiedView(apNativeAd: ApNativeAd?, @LayoutRes layoutRes: Int) {
        apNativeAd ?: return run {
            Log.w(TAG, "ad is null, removing all views")
            removeAllViews()
        }
        val admobNativeAd = apNativeAd.admobNativeAd ?: return run {
            Log.w(TAG, "admobNativeAd is null, removing all views")
            removeAllViews()
        }
        synchronized(viewModificationLock) {
            runCatching {
                val adView = LayoutInflater.from(context).inflate(layoutRes, this, false)
                if (adView !is NativeAdView) return@runCatching false
                nativeAdPopulated = false
                post {
                    populateUnifiedNativeAdView(admobNativeAd, adView)
                    removeAllViews()
                    addView(adView)
                }
                return@runCatching true
            }.fold(
                onSuccess = { populated ->
                    nativeAdPopulated = populated
                    if (nativeAdPopulated) {
                        onPopulatedListener?.invoke()
                        Log.d(TAG, "Successfully added populated ad view")
                    } else {
                        removeAllViews()
                    }
                },
                onFailure = {
                    Log.e(TAG, "Error populating ad view", it)
                    removeAllViews()
                }
            )
        }
    }

    private fun populateUnifiedNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        Log.d(TAG, "populateUnifiedNativeAdView called for ad: ${nativeAd.headline}")

        val headlineView = adView.safeFindView<TextView>(com.dqh.lmtech.ads.R.id.ad_headline)
        val bodyView = adView.safeFindView<TextView>(com.dqh.lmtech.ads.R.id.ad_body)
        val callToActionView =
            adView.safeFindView<TextView>(com.dqh.lmtech.ads.R.id.ad_call_to_action)
        val iconView = adView.safeFindView<ImageView>(com.dqh.lmtech.ads.R.id.ad_app_icon)
        val priceView = adView.safeFindView<TextView>(com.dqh.lmtech.ads.R.id.ad_price)
        val starRatingView = adView.safeFindView<RatingBar>(com.dqh.lmtech.ads.R.id.ad_stars)
        val mediaView = adView.safeFindView<MediaView>(com.dqh.lmtech.ads.R.id.ad_media)
        val advertiserView = adView.safeFindView<TextView>(com.dqh.lmtech.ads.R.id.ad_advertiser)

        adView.run {
            setHeadlineView(headlineView)
            setBodyView(bodyView)
            setCallToActionView(callToActionView)
            setIconView(iconView)
            setPriceView(priceView)
            setStarRatingView(starRatingView)
            setMediaView(mediaView)
            setAdvertiserView(advertiserView)
        }

        // Populate views with content
        headlineView?.let {
            it.text = nativeAd.headline ?: run {
                Log.d(TAG, "Headline is null, hiding headline view")
                it.beInvisible()
                return@let
            }
            Log.d(TAG, "Set headline: ${nativeAd.headline}")
        }

        bodyView?.let {
            it.text = nativeAd.body ?: run {
                Log.d(TAG, "Body is null, hiding body view")
                it.beInvisible()
                return@let
            }
            Log.d(TAG, "Set body: ${nativeAd.body}")
        }

        callToActionView?.let {
            it.text = nativeAd.callToAction ?: run {
                Log.d(TAG, "CTA is null, hiding CTA view")
                it.beInvisible()
                return@let
            }
            Log.d(TAG, "Set CTA: ${nativeAd.callToAction}")
        }

        iconView?.let {
            it.setImageDrawable(nativeAd.icon?.drawable ?: run {
                Log.d(TAG, "Icon is null, hiding icon view")
                it.beGone()
                return@let
            })
            Log.d(TAG, "Set icon drawable")
        }

        priceView?.let {
            it.text = nativeAd.price ?: run {
                Log.d(TAG, "Price is null, hiding price view")
                it.beInvisible()
                return@let
            }
            Log.d(TAG, "Set price: ${nativeAd.price}")
        }

        starRatingView?.let {
            it.rating = nativeAd.starRating?.toFloat() ?: run {
                Log.d(TAG, "Star rating is null, hiding rating view")
                it.beInvisible()
                return@let
            }
            Log.d(TAG, "Set star rating: ${nativeAd.starRating}")
        }

        mediaView?.let {
            it.mediaContent = nativeAd.mediaContent ?: run {
                Log.d(TAG, "Media content is null, hiding media view")
                it.beInvisible()
                return@let
            }
            Log.d(TAG, "Set media content")
        }

        advertiserView?.let {
            it.text = nativeAd.advertiser ?: run {
                Log.d(TAG, "Advertiser is null, hiding advertiser view")
                it.beInvisible()
                return@let
            }
            Log.d(TAG, "Set advertiser: ${nativeAd.advertiser}")
        }

        adView.setNativeAd(nativeAd)
        Log.d(TAG, "Successfully set native ad to ad view")
    }

    private fun <T : View> View.safeFindView(@IdRes id: Int): T? {
        return try {
            findViewById(id)
        } catch (e: Exception) {
            Log.e(TAG, "Error finding view with ID: $id", e)
            e.printStackTrace()
            null
        }
    }
}
