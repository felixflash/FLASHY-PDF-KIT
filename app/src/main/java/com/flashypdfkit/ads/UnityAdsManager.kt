package com.flashypdfkit.ads

import android.app.Activity
import android.content.Context
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.services.banners.BannerView

object UnityAdsManager {
    const val GAME_ID = "6187085"
    const val BANNER_PLACEMENT_ID = "Banner_Android"
    const val REWARDED_PLACEMENT_ID = "Rewarded_Android"
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        UnityAds.initialize(context, GAME_ID, false, object : IUnityAdsInitializationListener {
            override fun onInitializationComplete() {
                isInitialized = true
            }

            override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError, message: String) {
                // Initialization failed
            }
        })
    }

    fun loadRewardedAd(onLoaded: () -> Unit) {
        if (!isInitialized) return
        UnityAds.load(REWARDED_PLACEMENT_ID, object : com.unity3d.ads.IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                onLoaded()
            }

            override fun onUnityAdsFailedToLoad(placementId: String, error: UnityAds.UnityAdsLoadError, message: String) {
                // Failed to load
            }
        })
    }

    fun showRewardedAd(activity: Activity, onCompleted: () -> Unit, onFailed: () -> Unit) {
        if (!isInitialized) {
            onFailed()
            return
        }
        val showListener = object : IUnityAdsShowListener {
            override fun onUnityAdsShowComplete(placementId: String, state: UnityAds.UnityAdsShowCompletionState) {
                if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                    onCompleted()
                } else {
                    onFailed()
                }
            }

            override fun onUnityAdsShowFailure(placementId: String, error: UnityAds.UnityAdsShowError, message: String) {
                onFailed()
            }

            override fun onUnityAdsShowStart(placementId: String) {}
            override fun onUnityAdsShowClick(placementId: String) {}
        }
        UnityAds.show(activity, REWARDED_PLACEMENT_ID, showListener)
    }
}
