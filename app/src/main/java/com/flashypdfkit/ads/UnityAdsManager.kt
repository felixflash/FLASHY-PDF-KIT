package com.flashypdfkit.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.flashypdfkit.BuildConfig
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.services.banners.BannerView

object UnityAdsManager {
    private const val TAG = "UnityAdsManager"
    const val GAME_ID = "6187085"
    const val BANNER_PLACEMENT_ID = "Banner_Android"
    const val REWARDED_PLACEMENT_ID = "Rewarded_Android"
    
    private var isInitialized = false
    var isRewardedAdLoaded = false
        private set

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            UnityAds.initialize(context, GAME_ID, BuildConfig.DEBUG, object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    isInitialized = true
                    Log.d(TAG, "Unity Ads initialized successfully — preloading rewarded ad")
                    loadRewardedAd()
                }

                override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError, message: String) {
                    Log.w(TAG, "Unity Ads initialization failed: $error - $message")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Unity Ads", e)
        }
    }

    fun loadRewardedAd(
        onLoaded: (() -> Unit)? = null,
        onFailed: ((error: UnityAds.UnityAdsLoadError, message: String) -> Unit)? = null
    ) {
        if (!NetworkMonitor.isOnline.value) {
            Log.d(TAG, "Skipping loadRewardedAd: device is offline")
            onFailed?.invoke(UnityAds.UnityAdsLoadError.INITIALIZE_FAILED, "Device is offline")
            return
        }
        if (!isInitialized) {
            onFailed?.invoke(UnityAds.UnityAdsLoadError.INITIALIZE_FAILED, "SDK not initialized yet")
            return
        }
        try {
            UnityAds.load(REWARDED_PLACEMENT_ID, object : com.unity3d.ads.IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(placementId: String) {
                    isRewardedAdLoaded = true
                    Log.d(TAG, "Unity rewarded ad loaded successfully ($placementId)")
                    onLoaded?.invoke()
                }

                override fun onUnityAdsFailedToLoad(placementId: String, error: UnityAds.UnityAdsLoadError, message: String) {
                    isRewardedAdLoaded = false
                    Log.w(TAG, "Unity Ads failed to load ($placementId): $error - $message")
                    onFailed?.invoke(error, message)
                }
            })
        } catch (e: Exception) {
            isRewardedAdLoaded = false
            Log.e(TAG, "Error loading rewarded ad", e)
            onFailed?.invoke(UnityAds.UnityAdsLoadError.INTERNAL_ERROR, e.message ?: "Unknown error")
        }
    }

    fun showRewardedAd(activity: Activity, onCompleted: () -> Unit, onFailed: () -> Unit) {
        if (!isInitialized) {
            onFailed()
            return
        }
        try {
            val showListener = object : IUnityAdsShowListener {
                override fun onUnityAdsShowComplete(placementId: String, state: UnityAds.UnityAdsShowCompletionState) {
                    isRewardedAdLoaded = false
                    // Preload next rewarded ad immediately
                    loadRewardedAd()
                    if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                        onCompleted()
                    } else {
                        onFailed()
                    }
                }

                override fun onUnityAdsShowFailure(placementId: String, error: UnityAds.UnityAdsShowError, message: String) {
                    isRewardedAdLoaded = false
                    Log.w(TAG, "Unity Ads show failure: $error - $message")
                    // Attempt reload
                    loadRewardedAd()
                    onFailed()
                }

                override fun onUnityAdsShowStart(placementId: String) {}
                override fun onUnityAdsShowClick(placementId: String) {}
            }
            UnityAds.show(activity, REWARDED_PLACEMENT_ID, showListener)
        } catch (e: Exception) {
            isRewardedAdLoaded = false
            Log.e(TAG, "Error showing rewarded ad", e)
            loadRewardedAd()
            onFailed()
        }
    }

    fun showOrLoadRewardedAd(
        activity: Activity,
        onLoading: () -> Unit,
        onCompleted: () -> Unit,
        onFailed: (reason: String) -> Unit
    ) {
        if (!NetworkMonitor.isOnline.value) {
            onFailed("Internet connection required to load ads. Please check your connection and try again.")
            return
        }
        if (!isInitialized) {
            onFailed("Ad service is still connecting. Please try again in a moment.")
            return
        }
        if (isRewardedAdLoaded) {
            showRewardedAd(
                activity,
                onCompleted = onCompleted,
                onFailed = { onFailed("Ad was closed before completion.") }
            )
        } else {
            onLoading()
            loadRewardedAd(
                onLoaded = {
                    showRewardedAd(
                        activity,
                        onCompleted = onCompleted,
                        onFailed = { onFailed("Ad was closed before completion.") }
                    )
                },
                onFailed = { error, message ->
                    onFailed("Ad not available right now. Please try again shortly.")
                }
            )
        }
    }
}
