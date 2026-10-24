package fr.deuspheara.oree.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualExperienceTest {
    @Test fun presetsPreservePersonalizationAndCompletion() {
        val value =
            LauncherPreferences(
                theme = ThemeMode.LIGHT,
                accent = AccentColor.GREEN,
                highContrast = true,
                textScale = 1.5f,
                showSideloaded = true,
                setupComplete = true,
            )
        for (level in VisualExperience.entries) {
            val next = level.applyTo(value)
            assertEquals(value.theme, next.theme)
            assertEquals(value.accent, next.accent)
            assertEquals(value.highContrast, next.highContrast)
            assertEquals(value.textScale, next.textScale)
            assertEquals(value.showSideloaded, next.showSideloaded)
            assertTrue(next.setupComplete)
            assertEquals(next, next.normalized())
        }
        assertFalse(VisualExperience.SIMPLE.applyTo(value).liquidGlass)
        assertTrue(VisualExperience.SIMPLE.applyTo(value).reduceMotion)
        assertTrue(VisualExperience.FULL.applyTo(value).liquidGlass)
        assertEquals(1f, VisualExperience.FULL.applyTo(value).motionIntensity)
    }

    @Test fun liveGlassIsLastAndExplicitlyOptedIn() {
        assertEquals(VisualExperience.LIVE_GLASS, VisualExperience.entries.last())
        assertFalse(LauncherPreferences().liveGlass)
        for (level in VisualExperience.entries) {
            val value = level.applyTo(LauncherPreferences(liveGlass = true))
            assertEquals(level == VisualExperience.LIVE_GLASS, value.liveGlass)
            assertEquals(level, VisualExperience.nearest(value))
        }
    }

    @Test fun motionInputIsBoundedAndLegacyDefaultEffectIsPreserved() {
        assertEquals(1f, LauncherPreferences().motionIntensity)
        assertEquals(1f, LauncherPreferences(motionIntensity = Float.NaN).normalized().motionIntensity)
        assertEquals(0f, LauncherPreferences(motionIntensity = -1f).normalized().motionIntensity)
        assertEquals(1f, LauncherPreferences(motionIntensity = 9f).normalized().motionIntensity)
    }
}
