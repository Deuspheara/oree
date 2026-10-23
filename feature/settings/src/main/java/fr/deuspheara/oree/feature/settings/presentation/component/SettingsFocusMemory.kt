package fr.deuspheara.oree.feature.settings.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag

private class SettingsFocusMemory(var selected: String? = null) {
    val rows = mutableStateMapOf<String, FocusRequester>()
}

private val LocalSettingsFocusMemory = staticCompositionLocalOf<SettingsFocusMemory?> { null }

/** Lives inside the category's SaveableStateProvider, together with its scroll position. */
@Composable
internal fun SettingsFocusScope(content: @Composable () -> Unit) {
    val memory = rememberSaveable(
        saver = androidx.compose.runtime.saveable.Saver<SettingsFocusMemory, String>(
            save = { it.selected.orEmpty() },
            restore = { SettingsFocusMemory(it.takeIf(String::isNotEmpty)) },
        ),
    ) { SettingsFocusMemory() }
    CompositionLocalProvider(LocalSettingsFocusMemory provides memory, content = content)
}

@Composable
internal fun Modifier.restoreSettingsFocus(): Modifier {
    // Logical restoration is explicit; observing registration here would invalidate the list
    // whenever a lazy row registers. Native restoration keeps its own surviving child.
    return focusRestorer()
}

@Composable
internal fun Modifier.settingsFocus(id: String): Modifier {
    val memory = LocalSettingsFocusMemory.current
    val requester = remember(id) { FocusRequester() }
    DisposableEffect(id, memory) {
        memory?.rows?.set(id, requester)
        onDispose { memory?.rows?.remove(id) }
    }
    return focusRequester(requester).onFocusChanged {
        if (it.isFocused) memory?.selected = id
    }.testTag(id)
}

/** Lazy rows may register after their category's saved viewport has been restored. */
@Composable
internal fun rememberSettingsFocusRestoration(
    list: androidx.compose.foundation.lazy.LazyListState,
    itemKeys: List<String>,
): suspend () -> Boolean {
    val memory = LocalSettingsFocusMemory.current
    return {
        val selected = memory?.selected
        if (memory == null || selected == null) {
            false
        } else {
            val itemKey = when (selected) {
                "settings-ambient", "settings-dream" -> "appearance:ambient"
                else -> "appearance:${selected.removePrefix("settings-")}"
            }
            val index = itemKeys.indexOf(itemKey)
            if (index >= 0) list.scrollToItem(index)
            androidx.compose.runtime.withFrameNanos {}
            fr.deuspheara.oree.core.designsystem.focus.awaitRegisteredFocus { memory.rows[selected] }
                ?.requestFocus() == true
        }
    }
}
