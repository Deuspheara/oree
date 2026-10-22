package fr.deuspheara.oree

import fr.deuspheara.oree.core.model.AppFolder
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.core.model.folderApps
import fr.deuspheara.oree.feature.search.domain.SearchAction
import fr.deuspheara.oree.feature.search.domain.SearchItem
import fr.deuspheara.oree.feature.search.domain.searchLocal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalSearchTest {
    private fun app(id: String, name: String) = TvApp(id, "org.example", name, 0)

    @Test
    fun accentInsensitiveRankingMatchesLabelsAndLocalActions() {
        val apps = listOf(app("A", "Télévision"), app("B", "Ma télévision"), app("C", "Autre"))
        val actions = listOf(SearchItem.Action(SearchAction.APPEARANCE, "Réglages d’apparence"))
        assertEquals(
            listOf("app:A", "app:B"),
            searchLocal("TELEVISION", apps, emptyList(), actions, emptyList(), emptyList()).map {
                it.id
            },
        )
        assertEquals(
            "action:APPEARANCE",
            searchLocal("apparence reglages", apps, emptyList(), actions, emptyList(), emptyList())
                .single()
                .id,
        )
        assertTrue(
            searchLocal("absent", apps, emptyList(), actions, emptyList(), emptyList()).isEmpty(),
        )
    }

    @Test
    fun blankStatePrioritizesFavoritesAndUsesStableBoundedResults() {
        val apps = (0..100).map { app("app$it", "App $it") }
        val favorites = listOf("app50", "app20")
        val first = searchLocal("", apps, emptyList(), emptyList(), emptyList(), favorites)
        assertEquals(80, first.size)
        assertEquals(listOf("app:app50", "app:app20"), first.take(2).map { it.id })
        assertEquals(
            first,
            searchLocal("", apps.reversed(), emptyList(), emptyList(), emptyList(), favorites),
        )
    }

    @Test
    fun foldersRemainSearchableWhenMembersAreUnavailable() {
        val folder = AppFolder("one", "Cinéma", listOf("org.missing/.TV"))
        val results =
            searchLocal(
                "cinema",
                emptyList(),
                listOf(folder),
                emptyList(),
                emptyList(),
                emptyList(),
            )
        assertEquals(SearchItem.Folder(folder), results.single())
        assertTrue(folderApps(folder, emptyList()).isEmpty())
        assertEquals(listOf("org.missing/.TV"), folder.apps)
    }
}
