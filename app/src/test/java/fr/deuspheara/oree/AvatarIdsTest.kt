package fr.deuspheara.oree

import fr.deuspheara.oree.core.model.AvatarIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarIdsTest {
    @Test
    fun legacyOrdinalsHaveAnExplicitBoundedMigration() {
        assertEquals((1..6).map { "avatar_0$it" }, (0..5).map { AvatarIds.fromLegacy(it) })
        assertNull(AvatarIds.fromLegacy(-1))
        assertNull(AvatarIds.fromLegacy(6))
    }

    @Test
    fun externalPathsAndUnrecognizedIdsAreNeverAvatarReferences() {
        listOf(
            "",
            "avatar_00",
            "avatar_09",
            "../avatar_01.png",
            "file:///portrait.png",
            "content://photos/1",
            "https://example.com/a.png",
        )
            .forEach { assertFalse(AvatarIds.isKnown(it)) }
        assertTrue(AvatarIds.isKnown(AvatarIds.DEFAULT))
    }
}
