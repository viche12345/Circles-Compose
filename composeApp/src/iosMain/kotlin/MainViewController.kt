import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.play
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.Foundation.NSBundle
import platform.Foundation.NSURL

fun MainViewController() = ComposeUIViewController {

    val avAudioPlayer = remember { AVPlayer() }
    val dingSound = remember {
        NSURL(fileURLWithPath = NSBundle.mainBundle.pathForResource("compose-resources/files/sounds/ding", "mp3")!!)
    }
    val buzzerSound = remember {
        NSURL(fileURLWithPath = NSBundle.mainBundle.pathForResource("compose-resources/files/sounds/buzzer", "mp3")!!)
    }
    CirclesScreen(
        onCorrectCircleClicked = {
            avAudioPlayer.replaceCurrentItemWithPlayerItem(AVPlayerItem(dingSound))
            avAudioPlayer.play()
        },
        onIncorrectCircleClicked = {
            avAudioPlayer.replaceCurrentItemWithPlayerItem(AVPlayerItem(buzzerSound))
            avAudioPlayer.play()
        },
    )
}
