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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun CirclesScreen() {
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
                CirclesGame(widthPx, heightPx, LocalDensity.current)
            }
        }
    }
}

@Composable
fun CirclesGame(
    widthPx: Int,
    heightPx: Int,
    density: Density,
) = with(density) {
    fun newCircle() = (16..32).random().dp.let { rDp ->
        val r = rDp.roundToPx()
        Circle(
            x = (r..widthPx - r).random().toFloat(),
            y = (r..heightPx - r).random().toFloat(),
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

    var gameState by remember { mutableStateOf(GameState.NONE) }
    var level by remember { mutableIntStateOf(1) }
    var showLevel by remember { mutableStateOf(false) }
    LaunchedEffect(level) {
        showLevel = true
        delay(1000)
        addCircle().let { added -> if (!added) gameState = GameState.WON }
        showLevel = false
    }
    fun resetGame() {
        gameState = GameState.NONE
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
                        val c = circles.findClickedCircle(click.x, click.y)
                        if (c == circles.last()) {
                            level++
                        } else if (c != null) {
                            gameState = GameState.LOST
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
        OverlayText("YOU WIN", true) { resetGame() }
    }

    LevelScreen(showLevel, level)
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
        OverlayText("YOU LOSE", true, onReset)
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
        OverlayText("Level $level")
    }
}

@Composable
fun OverlayText(
    text: String,
    showReset: Boolean = false,
    onReset: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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
            Button(
                onClick = onReset
            ) {
                Text("RESET", style = MaterialTheme.typography.button)
            }
        }
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
    NONE, WON, LOST
}