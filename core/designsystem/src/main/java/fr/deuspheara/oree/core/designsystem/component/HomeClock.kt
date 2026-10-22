package fr.deuspheara.oree.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.tv.material3.Text
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.delay

@Composable
fun HomeClock(modifier: Modifier = Modifier) {
    var time by remember {
        mutableStateOf(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date()))
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())
                delay(60_000 - System.currentTimeMillis() % 60_000)
            }
        }
    }
    Text(
        time,
        modifier.testTag("home-clock"),
        color = Color.White,
        fontSize = 18.sp,
        style =
            androidx.compose.ui.text.TextStyle(
                shadow =
                    androidx.compose.ui.graphics.Shadow(
                        Color.Black.copy(alpha = .7f),
                        Offset(0f, 2f),
                        6f,
                    ),
            ),
    )
}
