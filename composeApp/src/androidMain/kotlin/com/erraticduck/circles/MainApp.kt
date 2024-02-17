package com.erraticduck.circles

import android.app.Application
import circles.composeapp.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import java.io.File
import java.io.FileOutputStream

class MainApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MainScope().launch {
            cacheSounds()
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun cacheSounds() = withContext(Dispatchers.IO) {
        File(cacheDir, "sounds").mkdirs()
        for (sound in Sound.entries) {
            val cacheFile = File(cacheDir, sound.cachePath)
            if (!cacheFile.exists()) {
                launch {
                    Res.readBytes(sound.resourcePath).inputStream().use { input ->
                        FileOutputStream(cacheFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }
}