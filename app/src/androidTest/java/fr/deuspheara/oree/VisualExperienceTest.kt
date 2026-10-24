package fr.deuspheara.oree

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.feature.settings.presentation.screen.VisualExperienceScreen
import fr.deuspheara.oree.feature.settings.presentation.state.ExperienceAction
import fr.deuspheara.oree.feature.settings.presentation.state.ExperienceControl
import fr.deuspheara.oree.feature.settings.presentation.state.ExperienceSaveStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class VisualExperienceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun finalLiveStepIsReachableByRemoteAndAccessibilityAndOnlyAppliesExplicitly() {
        val actions = mutableListOf<ExperienceAction>()
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                VisualExperienceScreen(LauncherPreferences(), { actions.add(it) }, welcome = false)
            }
        }
        compose.onNodeWithTag("experience-rail").assertIsFocused().performSemanticsAction(SemanticsActions.SetProgress) {
            assertTrue(it(5f))
        }
        compose.runOnIdle { assertTrue(actions.isEmpty()) }
        val range = compose.onNodeWithTag("experience-rail").fetchSemanticsNode().config[
            androidx.compose.ui.semantics.SemanticsProperties.ProgressBarRangeInfo,
        ]
        assertEquals(5f, range.current)
        assertEquals(5f, range.range.endInclusive)
        compose.onNodeWithTag("experience-rail").performKeyInput {
            pressKey(Key.DirectionLeft)
            pressKey(Key.DirectionRight)
            pressKey(Key.DirectionRight)
            pressKey(Key.DirectionCenter)
        }
        compose.onNodeWithTag("experience-apply").assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        compose.runOnIdle { assertTrue((actions.single() as ExperienceAction.Apply).value.liveGlass) }
    }

    @Test fun previewSurvivesRecreationAndOnlyExplicitApplySubmits() {
        val actions = mutableListOf<ExperienceAction>()
        val restoration = StateRestorationTester(compose)
        var status by mutableStateOf(ExperienceSaveStatus.IDLE)
        restoration.setContent {
            HazeTestTheme(LauncherPreferences()) {
                VisualExperienceScreen(LauncherPreferences(), { actions.add(it) }, welcome = true, saveStatus = status)
            }
        }
        compose.onNodeWithTag("setup-start").assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onNodeWithTag("experience-rail").assertIsFocused().performKeyInput {
            repeat(5) { pressKey(Key.DirectionLeft) }
        }
        compose.runOnIdle { assertTrue(actions.isEmpty()) }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("experience-rail").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onNodeWithTag("experience-apply").assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        compose.runOnIdle {
            assertEquals(1, actions.size)
            val saved = (actions.single() as ExperienceAction.Apply).value
            assertTrue(saved.reduceMotion)
            assertFalse(saved.liquidGlass)
            status = ExperienceSaveStatus.SAVING
        }
        compose.onNodeWithTag("experience-apply").assertIsNotEnabled()
        compose.runOnIdle { status = ExperienceSaveStatus.FAILED }
        compose.onNodeWithTag("setup-retry").assertIsFocused()
        compose.onNodeWithTag("setup-retry").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.runOnIdle { assertEquals(ExperienceAction.Retry, actions.last()) }
    }

    @Test fun keepingCurrentIsAnExplicitSeparateChoice() {
        val actions = mutableListOf<ExperienceAction>()
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                VisualExperienceScreen(LauncherPreferences(), {
                    actions.add(it)
                }, welcome = true, controls = setOf(ExperienceControl.KEEP_CURRENT))
            }
        }
        compose.runOnIdle { assertTrue(actions.isEmpty()) }
        compose.onNodeWithTag("setup-keep-current").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithTag("setup-keep-current").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.runOnIdle { assertEquals(listOf(ExperienceAction.KeepCurrent), actions) }
    }
}
