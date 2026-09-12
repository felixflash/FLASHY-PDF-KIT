package com.flashypdfkit.ads

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize

@Composable
fun UnityBannerView(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            BannerView(context as Activity, UnityAdsManager.BANNER_PLACEMENT_ID, UnityBannerSize(320, 50)).apply {
                load()
            }
        },
        update = { view ->
            // Update if needed
        }
    )
}
