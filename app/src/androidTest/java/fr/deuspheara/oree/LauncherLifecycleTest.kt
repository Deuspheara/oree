package fr.deuspheara.oree

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import fr.deuspheara.oree.core.designsystem.R
import org.junit.Rule
import org.junit.Test

/** Real entry-point/Koin/navigation integration; no preferences or external apps are changed. */
class LauncherLifecycleTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun awaitTag(tag: String) {
        compose.waitUntil(10_000) {
            compose.onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitGone(tag: String) {
        compose.waitUntil(10_000) {
            compose.onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().isEmpty()
        }
    }

    private fun back() {
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("input keyevent KEYCODE_BACK"),
        ).use { it.readBytes() }
    }

    private fun activate(tag: String) {
        compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithTag(tag).performKeyInput { pressKey(Key.DirectionCenter) }
    }

    @Test
    fun recreationPreservesNestedDiagnosticsAndSearchReturnTarget() {
        compose.waitUntil(10_000) {
            compose.onAllNodes(androidx.compose.ui.test.hasTestTag("home-ready"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        activate("search")
        awaitTag("search-query")
        val query = compose.activity.getString(R.string.home_settings)
        compose.onNodeWithTag("search-query").performTextInput(query)
        compose.waitUntil(5_000) {
            compose.onAllNodes(androidx.compose.ui.test.hasTestTag("search-result:action:HOME"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        activate("search-result:action:HOME")
        awaitTag("launcher-settings")
        compose.onNodeWithTag("settings-content-diagnostics").performScrollTo()
        activate("settings-content-diagnostics")
        awaitTag("content-diagnostics")
        compose.onNodeWithTag("content-diagnostics").assertExists()
        compose.activityRule.scenario.recreate()
        awaitTag("content-diagnostics")
        compose.onNodeWithTag("content-diagnostics").assertExists()
        back()
        awaitGone("content-diagnostics")
        awaitTag("launcher-settings")
        compose.onNodeWithTag("launcher-settings").assertExists()
        back()
        awaitGone("launcher-settings")
        awaitTag("search-query")
        compose.onNodeWithTag("search-query").assertTextContains(query)
        back()
        compose.waitUntil(5_000) {
            androidx.core.view.ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) != true
        }
        compose.waitForIdle()
        if (compose.onAllNodes(androidx.compose.ui.test.hasTestTag("search-panel")).fetchSemanticsNodes().isNotEmpty()) back()
        awaitGone("search-panel")
        compose.onNodeWithTag("home-ready").assertExists()
    }
}
