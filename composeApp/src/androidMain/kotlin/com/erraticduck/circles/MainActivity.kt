package com.erraticduck.circles

import CirclesScreen
import OverlayScreen
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaPlayer = MediaPlayer()
        enableEdgeToEdge()

        setContent {
            CirclesScreen(
                onCorrectCircleClicked = {
                    playSound(Sound.Ding)
                },
                onIncorrectCircleClicked = {
                    playSound(Sound.Buzzer)
                }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

    private fun playSound(sound: Sound) {
        mediaPlayer.reset()
        mediaPlayer.setDataSource(applicationContext, Uri.fromFile(File(cacheDir, sound.cachePath)))
        mediaPlayer.prepare()
        mediaPlayer.start()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    CirclesScreen()
}

@Preview
@Composable
fun Preview_Overlay() {
    OverlayScreen(text = "HELLO", showReset = true)
}