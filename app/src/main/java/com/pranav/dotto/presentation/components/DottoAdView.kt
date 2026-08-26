package com.pranav.dotto.presentation.components

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
                adUnitId = "ca-app-pub-7601883112918707/9171712738"
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        onAdLoaded()
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        onAdFailed()
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
