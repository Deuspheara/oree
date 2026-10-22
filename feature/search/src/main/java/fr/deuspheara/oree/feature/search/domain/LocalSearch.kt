package fr.deuspheara.oree.feature.search.domain

import fr.deuspheara.oree.core.model.AppFolder
import fr.deuspheara.oree.core.model.ContentEntry
import fr.deuspheara.oree.core.model.LocalPhoto
import fr.deuspheara.oree.core.model.TvApp
import java.text.Normalizer
import java.util.Locale

enum class SearchAction {
    APPEARANCE,
    HOME,
    NAVIGATION,
    ACCESSIBILITY,
    PRIVACY,
    STORAGE,
    ABOUT,
    PROFILES,
    WALLPAPER,
    ANDROID_SETTINGS,
}

sealed interface SearchItem {
    val id: String
    val title: String

    data class App(val app: TvApp) : SearchItem {
        override val id
            get() = "app:${app.id}"

        override val title
            get() = app.label
    }

    data class Folder(val folder: AppFolder) : SearchItem {
        override val id
            get() = folder.homeId

        override val title
            get() = folder.name
    }

    data class Action(val action: SearchAction, override val title: String) : SearchItem {
        override val id
            get() = "action:$action"
    }

    data class Content(val entry: ContentEntry) : SearchItem {
        override val id
            get() = "content:${entry.id}"

        override val title
            get() = entry.title
    }

    data class Photo(val photo: LocalPhoto) : SearchItem {
        override val id
            get() = "photo:${photo.uri}"

        override val title
            get() = photo.name
    }
}

private fun normalized(value: String) = Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")
    .lowercase(Locale.ROOT)
    .trim()

/** Bounded local ranking. No provider query, permission request or launch is performed here. */
fun searchLocal(
    query: String,
    apps: List<TvApp>,
    folders: List<AppFolder>,
    actions: List<SearchItem.Action>,
    content: List<ContentEntry>,
    favorites: List<String>,
    photos: List<LocalPhoto> = emptyList(),
): List<SearchItem> {
    val text = normalized(query.take(120))
    val terms = text.split(Regex("\\s+")).filter(String::isNotEmpty)
    val favoriteOrder = favorites.withIndex().associate { it.value to it.index }
    val items =
        apps.map { SearchItem.App(it) } +
            folders.map { SearchItem.Folder(it) } +
            actions +
            content.map { SearchItem.Content(it) } +
            photos.filter { it.name.isNotBlank() }.map { SearchItem.Photo(it) }
    data class Match(val item: SearchItem, val title: String, val score: Int, val kind: Int, val favorite: Int)
    return items
        .mapNotNull { item ->
            val title = normalized(item.title)
            val words = title.split(Regex("\\s+"))
            val score =
                when {
                    text.isEmpty() -> 0
                    title == text -> 0
                    title.startsWith(text) -> 1
                    words.any { it.startsWith(text) } -> 2
                    title.contains(text) -> 3
                    terms.all { term -> title.contains(term) } -> 4
                    else -> return@mapNotNull null
                }
            val kind =
                when (item) {
                    is SearchItem.App -> 0
                    is SearchItem.Folder -> 1
                    is SearchItem.Action -> 2
                    is SearchItem.Content -> 3
                    is SearchItem.Photo -> 4
                }
            Match(
                item,
                title,
                score,
                kind,
                (item as? SearchItem.App)?.let { favoriteOrder[it.app.id] } ?: Int.MAX_VALUE,
            )
        }
        .sortedWith(
            compareBy<Match> { it.score }
                .thenBy { it.favorite }
                .thenBy { it.kind }
                .thenBy { it.title }
                .thenBy { it.item.id },
        )
        .distinctBy { it.item.id }
        .take(80)
        .map { it.item }
}
