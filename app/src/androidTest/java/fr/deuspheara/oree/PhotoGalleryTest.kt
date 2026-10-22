package fr.deuspheara.oree

import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.height
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.platform.app.InstrumentationRegistry
import fr.deuspheara.oree.core.designsystem.theme.LauncherTheme
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.PhotoAvailability
import fr.deuspheara.oree.core.platform.datasource.photos.MediaStorePhotoSource
import fr.deuspheara.oree.core.platform.image.impl.AndroidWallpaperStore
import fr.deuspheara.oree.feature.profiles.presentation.route.PhotoGalleryPanel
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
class PhotoGalleryTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun deniedAccessOffersAnExplicitGrantAndSystemPicker() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val denied =
            object : ContextWrapper(context) {
                override fun checkSelfPermission(permission: String) = PackageManager.PERMISSION_DENIED
            }
        compose.setContent {
            val owner =
                checkNotNull(androidx.activity.compose.LocalActivityResultRegistryOwner.current)
            CompositionLocalProvider(
                LocalContext provides denied,
                androidx.activity.compose.LocalActivityResultRegistryOwner provides owner,
            ) {
                LauncherTheme(LauncherPreferences()) {
                    PhotoGalleryPanel(true, false, false, {
                    }, {}, {}, requestOnOpen = false, loadPhotos = { error("Denied access must not query photos") })
                }
            }
        }
        compose.onNodeWithTag("photo-permission").assertIsDisplayed().assertIsFocused()
        compose.onNodeWithTag("local-photos").assertDoesNotExist()
    }

    @Test
    fun realGalleryDpadSelectionCopiesOnlyTheChosenImage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val loaded = runBlocking { MediaStorePhotoSource(context).load() }
        org.junit.Assume.assumeTrue(
            "Requires a reference device with user-authorized local photos; denial is tested separately",
            loaded.availability == PhotoAvailability.AVAILABLE,
        )
        var chosen: android.net.Uri? = null
        var count = 0
        compose.setContent {
            LauncherTheme(LauncherPreferences()) {
                PhotoGalleryPanel(
                    true,
                    false,
                    false,
                    {
                        chosen = it
                        count++
                    },
                    {},
                    {},
                    loadPhotos = { MediaStorePhotoSource(context).load() },
                )
            }
        }
        compose.waitUntil(5000) {
            compose.onAllNodesWithTag("local-photo:0").fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
        compose.onNodeWithTag("local-photo:0").assertIsDisplayed().assertIsFocused()
        assertTrue(
            "Photo has visible height",
            compose.onNodeWithTag("local-photo:0").fetchSemanticsNode().boundsInRoot.height > 80f,
        )
        compose.onNodeWithTag("local-photo:0").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.runOnIdle {
            assertEquals(1, count)
            assertEquals(loaded.photos.first().uri, chosen?.toString())
        }
        val repository = AndroidWallpaperStore(context)
        runBlocking {
            val name = repository.importImage(checkNotNull(chosen))
            try {
                assertTrue(File(context.filesDir, name).length() > 0)
            } finally {
                repository.discard(name)
            }
        }
    }
}
