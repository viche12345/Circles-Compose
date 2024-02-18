import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.coroutines.getIntFlow
import com.russhwolf.settings.coroutines.toSuspendSettings
import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun CirclesScreen(
    onCorrectCircleClicked: () -> Unit = { },
    onIncorrectCircleClicked: () -> Unit = { },
) {
    var widthPx by rememberSaveable { mutableStateOf(0) }
    var heightPx by rememberSaveable { mutableStateOf(0) }
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    widthPx = it.size.width
                    heightPx = it.size.height
                }
        ) {
            if (widthPx > 0 && heightPx > 0) {
                CirclesGame(
                    widthPx,
                    heightPx,
                    WindowInsets.safeContent,
                    LocalDensity.current,
                    onCorrectCircleClicked,
                    onIncorrectCircleClicked,
                )
            }
        }
    }
}

@OptIn(ExperimentalSettingsApi::class)
@Composable
fun CirclesGame(
    widthPx: Int,
    heightPx: Int,
    insets: WindowInsets,
    density: Density,
    onCorrectCircleClicked: () -> Unit,
    onIncorrectCircleClicked: () -> Unit,
) = with(density) {
    fun newCircle() = (16..32).random().dp.let { rDp ->
        val r = rDp.roundToPx()
        val leftInset = insets.getLeft(density, LayoutDirection.Ltr)
        val rightInset = insets.getRight(density, LayoutDirection.Ltr)
        val topInset = insets.getTop(density)
        val bottomInset = insets.getBottom(density)
        Circle(
            x = (leftInset + r..widthPx - r - rightInset).random().toFloat(),
            y = (topInset + r..heightPx - r - bottomInset).random().toFloat(),
            r = r.toFloat()
        )
    }

    val circles = remember { mutableStateListOf<Circle>() }
    fun addCircle(): Boolean {
        // Give up after 1000 attempts
        repeat((0 until 1000).count()) {
            newCircle().let {
                if (it.isNotTooCloseToAnotherIn(circles)) {
                    return circles.add(it)
                }
            }
        }
        return false
    }

    val settings = remember { Settings().toSuspendSettings() }
    var gameState by remember { mutableStateOf(GameState.IDLE) }
    var level by remember { mutableIntStateOf(1) }
    LaunchedEffect(level) {
        gameState = GameState.ADDING_CIRCLE
        delay(1000)
        if (level > settings.getInt(SETTINGS_KEY_HIGH_SCORE, 0)) {
            settings.putInt(SETTINGS_KEY_HIGH_SCORE, level)
        }
        addCircle().let { added -> if (!added) gameState = GameState.WON }
        gameState = GameState.IDLE
    }
    fun resetGame() {
        gameState = GameState.IDLE
        circles.clear()
        level = 1
    }

    Canvas(
        modifier = Modifier
            .background(Color.Black)
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { click ->
                        if (gameState != GameState.IDLE)
                            return@detectTapGestures

                        val c = circles.findClickedCircle(click.x, click.y)
                        if (c == circles.last()) {
                            gameState = GameState.ADDING_CIRCLE
                            level++
                            onCorrectCircleClicked()
                        } else if (c != null) {
                            gameState = GameState.LOST
                            onIncorrectCircleClicked()
                        }
                    }
                )
            }
    ) {
        circles.forEach {
            drawCircle(
                if (gameState == GameState.LOST && it == circles.last()) Color.Red else Color.White,
                center = Offset(it.x, it.y),
                radius = it.r,
            )
        }
    }

    GameLostScreen(gameState == GameState.LOST) { resetGame() }
    if (gameState == GameState.WON) {
        OverlayScreen("YOU WIN", true) { resetGame() }
    }

    LevelScreen(gameState == GameState.ADDING_CIRCLE, level)
}

@Composable
fun GameLostScreen(
    gameLost: Boolean,
    onReset: () -> Unit,
) {
    AnimatedVisibility(
        visible = gameLost,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(durationMillis = 500, delayMillis = 1000)
        ),
        exit = fadeOut(animationSpec = snap())
    ) {
        OverlayScreen("YOU LOSE", true, onReset)
    }
}

@Composable
fun LevelScreen(
    show: Boolean,
    level: Int,
) {
    AnimatedVisibility(
        visibleState = remember(show) {
            MutableTransitionState(!show).apply { targetState = show }
        },
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(durationMillis = 500)
        ),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis = 1000),
            targetOffsetY = { it }
        )
    ) {
        OverlayScreen("Level $level")
    }
}

@OptIn(ExperimentalSettingsApi::class)
@Composable
fun OverlayScreen(
    text: String,
    showReset: Boolean = false,
    onReset: () -> Unit = {},
) {
    val settings = remember { Settings() as? ObservableSettings }
    var showClearHighScoreDialog by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {}
            .background(MaterialTheme.colors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.h2,
            fontWeight = FontWeight.ExtraBold,
        )
        if (showReset) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = onReset
                ) {
                    Text("RESET", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.button)
                }
                Spacer(Modifier.height(48.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val highScore = remember {
                        settings?.getIntFlow(SETTINGS_KEY_HIGH_SCORE, 0)
                    }?.collectAsState(0)

                    Text(
                        text = "High Score: ${highScore?.value}",
                        style = MaterialTheme.typography.subtitle1
                    )
                    Spacer(Modifier.width(16.dp))
                    TextButton(
                        onClick = { showClearHighScoreDialog = true }
                    ) {
                        Text("CLEAR")
                    }
                }
            }
        }
    }

    if (showClearHighScoreDialog) {
        AlertDialog(
            text = {
                Text("Clear high score?")
            },
            onDismissRequest = { showClearHighScoreDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        settings?.putInt(SETTINGS_KEY_HIGH_SCORE, 0)
                        showClearHighScoreDialog = false
                    }
                ) {
                    Text("CONFIRM")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showClearHighScoreDialog = false
                    }
                ) {
                    Text("CANCEL")
                }
            }
        )
    }
}

private data class Circle(val x: Float, val y: Float, val r: Float)
private fun Circle.isNotTooCloseToAnotherIn(circles: List<Circle>) = circles.find { c ->
    sqrt( (x - c.x).pow(2) + (y - c.y).pow(2) ) < c.r * 2
} == null
private fun List<Circle>.findClickedCircle(x: Float, y: Float) = find {
    sqrt( (x - it.x).pow(2) + (y - it.y).pow(2)) <= it.r
}

private enum class GameState {
    IDLE, ADDING_CIRCLE, WON, LOST
}