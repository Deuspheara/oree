package fr.deuspheara.oree

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.width
import androidx.compose.ui.unit.dp
import fr.deuspheara.oree.core.designsystem.artwork.FocusArtwork
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeCardResponseTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun immediateLiftCompletesWithinTwoFramesAndReleasesWithoutTail() {
        var focused by mutableStateOf(false)
        compose.setContent {
            Column(Modifier.padding(32.dp)) {
                FocusArtwork(
                    focused,
                    false,
                    false,
                    Offset.Zero,
                    Modifier.size(200.dp, 100.dp),
                    immediateFocus = true,
                ) {
                    Box(Modifier.size(200.dp, 100.dp).testTag("immediate"))
                }
            }
        }
        compose.waitForIdle()
        fun width(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.width
        val base = width("immediate")
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { focused = true }
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        assertTrue("Immediate selected lift is complete", width("immediate") >= base * 1.099f)
        compose.runOnIdle { focused = false }
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        assertTrue(
            "Previous card releases without spring tail",
            width("immediate") <= base * 1.001f,
        )
        compose.mainClock.autoAdvance = true
    }
}
