package com.pranav.dotto.presentation.components

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
fun DottoAdView(
    modifier: Modifier = Modifier,
    onAdLoaded: () -> Unit = {},
    onAdFailed: () -> Unit = {}
) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                // Live Ad Unit ID
                adUnitId = "ca-app-pub-7601883112918707/9171712738"
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d("DottoAdView", "Ad loaded successfully")
                        onAdLoaded()
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e("DottoAdView", "Ad failed to load: ${error.message}, code: ${error.code}")
                        onAdFailed()
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
