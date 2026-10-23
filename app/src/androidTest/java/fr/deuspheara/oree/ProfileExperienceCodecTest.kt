package fr.deuspheara.oree

import fr.deuspheara.oree.core.data.serialization.decodeBackup
import fr.deuspheara.oree.core.data.serialization.decodeProfiles
import fr.deuspheara.oree.core.data.serialization.encodeBackup
import fr.deuspheara.oree.core.data.serialization.encodeProfiles
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.LocalProfile
import fr.deuspheara.oree.core.model.ProfileSnapshot
import java.io.IOException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ProfileExperienceCodecTest {
    private val snapshot =
        ProfileSnapshot(
            "fixture",
            listOf(LocalProfile("fixture", "Sample", preferences = LauncherPreferences(motionIntensity = .55f, setupComplete = false))),
        )

    @Test fun newSetupAndMotionRoundTripWithStableProfileIdentity() {
        assertEquals(snapshot, decodeProfiles(encodeProfiles(snapshot)))
        assertEquals(snapshot, decodeBackup(encodeBackup(snapshot)))
    }

    @Test fun existingProfilesSkipWelcomeAndRetainFullMotion() {
        val json = JSONObject(encodeProfiles(snapshot))
        val settings = json.getJSONArray("profiles").getJSONObject(0).getJSONObject("settings")
        settings.remove("motionIntensity")
        settings.remove("setupComplete")
        val value = decodeProfiles(json.toString()).active!!
        assertTrue(value.preferences.setupComplete)
        assertEquals(1f, value.preferences.motionIntensity)
        assertEquals(snapshot.activeId, value.id)
    }

    @Test fun importedExperienceFieldsAreValidated() {
        for ((key, value) in listOf("motionIntensity" to 2, "setupComplete" to "yes")) {
            val json = JSONObject(encodeBackup(snapshot))
            json.getJSONObject("profiles").getJSONArray("profiles").getJSONObject(0).getJSONObject("settings").put(key, value)
            try {
                decodeBackup(json.toString())
                fail("Invalid experience field accepted: $key")
            } catch (_: IOException) { }
        }
    }
}
