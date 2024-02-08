import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun CirclesCanvas(
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

    val circles = remember { mutableStateListOf<Circle>().apply { add(newCircle()) } }
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

    Canvas(
        modifier = Modifier
            .background(Color.Black)
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { click ->
                        val c = circles.last()
                        if (sqrt( (click.x - c.x).pow(2) + (click.y - c.y).pow(2)) <= c.r) {
                            addCircle()
                        }
                    }
                )
            }
    ) {
        circles.forEach {
            drawCircle(
                Color.White,
                center = Offset(it.x, it.y),
                radius = it.r,
            )
        }
    }
}

@Composable
fun CirclesScreen() {
    var widthPx by rememberSaveable { mutableStateOf(0) }
    var heightPx by rememberSaveable { mutableStateOf(0) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                widthPx = it.size.width
                heightPx = it.size.height
            }
    ) {
        if (widthPx > 0 && heightPx > 0) {
            CirclesCanvas(widthPx, heightPx, LocalDensity.current)
        }
    }
}

private data class Circle(val x: Float, val y: Float, val r: Float)
private fun Circle.isNotTooCloseToAnotherIn(circles: List<Circle>) = circles.find { c ->
    sqrt( (x - c.x).pow(2) + (y - c.y).pow(2) ) < c.r * 2
} == null
