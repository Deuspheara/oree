package fr.deuspheara.oree

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.height
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.width
import androidx.compose.ui.unit.dp
import fr.deuspheara.oree.feature.home.presentation.component.ArtworkPhase
import fr.deuspheara.oree.feature.home.presentation.component.HomeArtworkPlaceholder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class HomeArtworkLoadingTest {
    @get:Rule val compose = createComposeRule()

    private fun pixels(): List<androidx.compose.ui.graphics.Color> {
        val image = compose.onNodeWithTag("fixture").captureToImage().toPixelMap()
        return (0 until image.height step 8).flatMap { y ->
            (0 until image.width step 8).map { x -> image[x, y] }
        }
    }

    @Test
    fun slowLoadingHasNoPermanentAnimationAndFailureStopsIt() {
        var phase by mutableStateOf(ArtworkPhase.WAITING)
        compose.setContent {
            Box(Modifier.size(240.dp, 135.dp).testTag("fixture")) {
                HomeArtworkPlaceholder("Example", phase, true, false)
            }
        }
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { phase = ArtworkPhase.LOADING }
        compose.mainClock.advanceTimeByFrame()
        val initial = pixels()
        compose.mainClock.advanceTimeBy(350)
        assertNotEquals("Slow loading shows a decorative pass", initial, pixels())
        compose.mainClock.advanceTimeBy(1100)
        compose.waitForIdle()
        val settled = pixels()
        compose.mainClock.advanceTimeBy(500)
        assertEquals("Stuck loading has a static settled placeholder", settled, pixels())
        compose.runOnIdle { phase = ArtworkPhase.FAILED }
        compose.mainClock.advanceTimeBy(200)
        compose.waitForIdle()
        val static = pixels()
        compose.mainClock.advanceTimeBy(500)
        assertEquals(static, pixels())
        compose.onNodeWithTag("fixture").assertIsDisplayed()
        compose.mainClock.autoAdvance = true
    }

    @Test
    fun offscreenAndReducedMotionLoadingStayStatic() {
        var visible by mutableStateOf(false)
        var reduced by mutableStateOf(false)
        compose.setContent {
            Box(Modifier.size(240.dp, 135.dp).testTag("fixture")) {
                HomeArtworkPlaceholder("Example", ArtworkPhase.LOADING, visible, reduced)
            }
        }
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(300)
        compose.waitForIdle()
        val offscreen = pixels()
        compose.mainClock.advanceTimeBy(500)
        assertEquals(offscreen, pixels())
        compose.runOnIdle {
            reduced = true
            visible = true
        }
        compose.mainClock.advanceTimeBy(300)
        compose.waitForIdle()
        val reducedPixels = pixels()
        compose.mainClock.advanceTimeBy(500)
        assertEquals(reducedPixels, pixels())
        compose.mainClock.autoAdvance = true
    }
}
