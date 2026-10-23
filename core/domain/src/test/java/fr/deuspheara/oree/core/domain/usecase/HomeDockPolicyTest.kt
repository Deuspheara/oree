package fr.deuspheara.oree.core.domain.usecase

import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.core.model.setFavoriteApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class HomeDockPolicyTest {
    @Test
    fun seventhFavoriteIsRejectedButRemovalFreesASlot() {
        val six = (1..6).map { "app$it" }
        assertThrows(IllegalArgumentException::class.java) { setFavoriteApp(six, "seventh", true) }
        val five = setFavoriteApp(six, "app3", false)
        assertEquals(listOf("app1", "app2", "app4", "app5", "app6", "seventh"), setFavoriteApp(five, "seventh", true))
        assertEquals(six, setFavoriteApp(six, "app3", true))
    }

    @Test
    fun legacyOverflowRemainsAccessibleInCatalogWithoutErasingStoredIdentities() {
        val stored = (1..8).map { "app$it" }
        val apps = stored.map { TvApp(it, it, it, 0) }
        val projection = ProjectHomeUseCase()(apps, stored, emptyList())
        assertEquals(stored.take(6), projection.favorites.map { it.id })
        assertEquals(stored.drop(6), projection.catalog.map { it.id })
        assertEquals(8, stored.size)
    }

    @Test
    fun blurPreferenceKeepsBoundedFiniteValues() {
        assertEquals(1f, LauncherPreferences(backdropBlur = Float.NaN).normalized().backdropBlur)
        assertEquals(0f, LauncherPreferences(backdropBlur = -1f).normalized().backdropBlur)
        assertEquals(1f, LauncherPreferences(backdropBlur = 2f).normalized().backdropBlur)
    }
}
