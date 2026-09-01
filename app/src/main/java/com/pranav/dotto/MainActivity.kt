package com.pranav.dotto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.pranav.dotto.presentation.sound.SoundManager

class MainActivity : ComponentActivity() {
    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize AdMob
        MobileAds.initialize(this) {}

        // Set up test device configuration
        val testDeviceIds = listOf(
            "b321eafa-78c3-4c8d-9715-1cdbf6bf94d7",
            "79E8C4EC7E9D7411F5636C834CF319CE"
        )
        val configuration = RequestConfiguration.Builder()
            .setTestDeviceIds(testDeviceIds)
            .build()
        MobileAds.setRequestConfiguration(configuration)
        
        soundManager = SoundManager(this)
        enableEdgeToEdge()
        setContent {
            DottoApp(soundManager = soundManager)
        }
    }

    override fun onStart() {
        super.onStart()
        // Resume music if sound was enabled
        soundManager.resumeMusic(true)
    }

    override fun onStop() {
        super.onStop()
        // Pause music when backgrounded
        soundManager.stopMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}
