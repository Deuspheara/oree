package fr.deuspheara.oree

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.platform.app.InstrumentationRegistry
import fr.deuspheara.oree.core.platform.launch.AppUninstall
import fr.deuspheara.oree.core.platform.launch.resolveContent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Only run with the explicitly installed, disposable removal fixture. */
@OptIn(ExperimentalTestApi::class)
@org.junit.FixMethodOrder(org.junit.runners.MethodSorters.NAME_ASCENDING)
class SystemActionsFixtureTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val fixture = "fr.deuspheara.oree.removalfixture"
    private val instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private fun installed(): Boolean = try {
        @Suppress("DEPRECATION")
        instrumentation.targetContext.packageManager.getApplicationInfo(
            fixture,
            0,
        )
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun activate(tag: String) {
        compose.onNodeWithTag(tag).performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.OnClick,
        ) {
            it()
        }
        compose.waitForIdle()
    }

    private fun nativeButton(texts: Set<String>): AccessibilityNodeInfo {
        val automation = instrumentation.uiAutomation
        automation.serviceInfo =
            automation.serviceInfo.apply {
                flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            }
        var found: AccessibilityNodeInfo? = null
        fun scan(node: AccessibilityNodeInfo) {
            if (
                node.packageName?.toString()?.contains("packageinstaller") == true &&
                node.text?.toString() in texts
            ) {
                var button = node
                while (!button.isClickable && button.parent != null) button = button.parent
                if (button.isClickable) found = button
            }
            for (i in 0 until node.childCount) node.getChild(i)?.let(::scan)
        }
        fun containsFixture(node: AccessibilityNodeInfo): Boolean {
            if (node.text?.toString() == "TVL removal fixture") return true
            return (0 until node.childCount).any {
                node.getChild(it)?.let(::containsFixture) == true
            }
        }
        val deadline = System.currentTimeMillis() + 10000
        while (found == null && System.currentTimeMillis() < deadline) {
            automation.windows
                .filter { it.isFocused || it.isActive }
                .mapNotNull { it.root }
                .filter(::containsFixture)
                .forEach(::scan)
            if (found == null) Thread.sleep(100)
        }
        return checkNotNull(found) {
            "Expected system package-installer confirmation button: $texts"
        }
    }

    @Test
    fun aSourceOwnedContentResolvesToDisposableFixture() = runBlocking {
        assumeTrue(installed())
        val request =
            resolveContent(instrumentation.targetContext, "tvlfixture://episode/1", fixture)
        assertNotNull(request)
        assertEquals(fixture, request!!.component!!.packageName)
        assertNull(
            resolveContent(
                instrumentation.targetContext,
                "tvlfixture://episode/1",
                "org.example.foreign",
            ),
        )
    }

    @Test
    fun bLauncherConfirmationThenAndroidCancelAndConfirmedFixtureRemoval() = runBlocking {
        assumeTrue(installed())
        val target = "app:$fixture/android.app.Activity"
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag("home-ready").fetchSemanticsNodes().isNotEmpty()
        }
        fun openFixtureMenu() {
            compose.onNodeWithTag("home").performScrollToNode(hasTestTag(target))
            compose.onNodeWithTag(target).performSemanticsAction(
                androidx.compose.ui.semantics.SemanticsActions.RequestFocus,
            ) {
                it()
            }
            compose.onNodeWithTag(target).performSemanticsAction(
                androidx.compose.ui.semantics.SemanticsActions.OnLongClick,
            ) {
                it()
            }
            compose.waitUntil(10000) {
                compose.onAllNodesWithTag("menu-uninstall").fetchSemanticsNodes().isNotEmpty()
            }
            activate("menu-uninstall")
            compose.onNodeWithTag("uninstall-cancel").assertIsFocused()
            activate("uninstall-confirm")
        }
        openFixtureMenu()
        assertTrue(
            nativeButton(setOf("Cancel", "Annuler"))
                .performAction(AccessibilityNodeInfo.ACTION_CLICK),
        )
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag("app-menu").fetchSemanticsNodes().isEmpty()
        }
        assertTrue(installed())
        openFixtureMenu()
        assertTrue(
            nativeButton(setOf("OK", "Uninstall", "Désinstaller"))
                .performAction(AccessibilityNodeInfo.ACTION_CLICK),
        )
        compose.waitUntil(10000) { !installed() }
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag(target).fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithTag(target).assertDoesNotExist()
        assertNull(AppUninstall(instrumentation.targetContext).intent(fixture))
    }
}
