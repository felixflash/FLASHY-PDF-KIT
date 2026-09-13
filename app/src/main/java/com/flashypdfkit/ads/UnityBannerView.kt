package com.flashypdfkit.ads

import android.app.Activity
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.viewinterop.AndroidView
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize

@Composable
fun UnityBannerView(
    modifier: Modifier = Modifier,
    onAdLoaded: () -> Unit = {},
    onAdFailedToLoad: () -> Unit = {}
) {
    AndroidView(
        modifier = modifier.graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        },
        factory = { context ->
            val activity = context as? Activity
            if (activity != null) {
                BannerView(activity, UnityAdsManager.BANNER_PLACEMENT_ID, UnityBannerSize(320, 50)).apply {
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    listener = object : BannerView.IListener {
                        override fun onBannerLoaded(bannerView: BannerView?) {
                            onAdLoaded()
                        }

                        override fun onBannerFailedToLoad(bannerView: BannerView?, errorInfo: BannerErrorInfo?) {
                            onAdFailedToLoad()
                        }

                        override fun onBannerClick(bannerView: BannerView?) {}
                        override fun onBannerShown(bannerView: BannerView?) {}
                        override fun onBannerLeftApplication(bannerView: BannerView?) {}
                    }
                    load()
                }
            } else {
                View(context)
            }
        }
    )
}

