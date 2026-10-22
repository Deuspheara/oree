package fr.deuspheara.oree

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.bottom
import androidx.compose.ui.test.cancel
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.top
import androidx.test.platform.app.InstrumentationRegistry
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.LocalProfile
import fr.deuspheara.oree.core.model.ProfileSnapshot
import fr.deuspheara.oree.core.model.ThemeMode
import fr.deuspheara.oree.feature.profiles.presentation.screen.ProfilePanel
import java.util.Locale
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MenuAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    private fun editor(locale: Locale, theme: ThemeMode) {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
        val context = base.createConfigurationContext(config)
        compose.setContent {
            CompositionLocalProvider(
                LocalContext provides context,
                LocalConfiguration provides config,
                LocalResources provides context.resources,
            ) {
                HazeTestTheme(
                    LauncherPreferences(
                        theme = theme,
                        textScale = 2f,
                        highContrast = true,
                        reduceMotion = true,
                    ),
                ) {
                    ProfilePanel(
                        ProfileSnapshot(
                            "a",
                            listOf(LocalProfile("a", "Alpha"), LocalProfile("b", "Beta")),
                        ),
                        true,
                        true,
                        false,
                        false,
                        {},
                        { _, _, _ -> },
                        {},
                        {},
                    )
                }
            }
        }
        compose.onNodeWithTag("add-profile").performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.OnClick,
        ) {
            it()
        }
        compose.onNodeWithTag("profile-name").performTextInput("New")
        compose.onNodeWithTag("profile-avatar-row").performScrollToIndex(7)
        compose.onNodeWithTag("profile-avatar:avatar_08").performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.RequestFocus,
        ) {
            it()
        }
        compose.onNodeWithTag("profile-avatar:avatar_08").assertIsDisplayed()
        compose
            .onNodeWithTag("save-profile")
            .performScrollTo()
            .assertIsDisplayed()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus) {
                it()
            }
        val panel = compose.onNodeWithTag("profile-panel").fetchSemanticsNode().boundsInRoot
        val button = compose.onNodeWithTag("save-profile").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Save is fully inside scroll viewport",
            button.top >= panel.top && button.bottom <= panel.bottom,
        )
        compose
            .onNodeWithText(context.getString(R.string.cancel))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun frenchLargeTextLightEditorActionsRemainReachable() = editor(Locale.FRENCH, ThemeMode.LIGHT)

    @Test
    fun englishLargeTextDarkEditorActionsRemainReachable() = editor(Locale.ENGLISH, ThemeMode.DARK)
}
