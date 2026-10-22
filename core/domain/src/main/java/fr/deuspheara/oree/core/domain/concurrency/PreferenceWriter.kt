package fr.deuspheara.oree.core.domain.concurrency

import fr.deuspheara.oree.core.model.LauncherPreferences
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class PreferenceSaveStatus {
    SAVING,
    SAVED,
    FAILED,
}

data class PreferenceSaveState(val value: LauncherPreferences, val status: PreferenceSaveStatus, val revision: Long)

/** Bounded metadata is synchronized; persistence always executes outside the lock. */
class PreferenceWriter(scope: CoroutineScope, private val persist: suspend (String, LauncherPreferences) -> Unit) {
    private data class Request(val id: String, val value: LauncherPreferences, val revision: Long)

    private val lock = Any()
    private val pending = linkedMapOf<String, Request>()
    private val completed = mutableMapOf<String, Long>()
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private val changed = MutableStateFlow(0L)
    private val mutableStates = MutableStateFlow<Map<String, PreferenceSaveState>>(emptyMap())
    val states = mutableStates.asStateFlow()
    private var revision = 0L
    internal val pendingCount: Int
        get() = synchronized(lock) { pending.size }

    init {
        scope.launch {
            for (ignored in wake) {
                while (true) {
                    val request =
                        synchronized(lock) {
                            pending.entries.firstOrNull()?.value?.also { pending.remove(it.id) }
                        } ?: break
                    val status =
                        try {
                            persist(request.id, request.value)
                            PreferenceSaveStatus.SAVED
                        } catch (_: IOException) {
                            PreferenceSaveStatus.FAILED
                        } catch (_: SecurityException) {
                            PreferenceSaveStatus.FAILED
                        }
                    synchronized(lock) {
                        val latest = mutableStates.value[request.id]
                        if (latest != null) {
                            if (latest.revision == request.revision) {
                                mutableStates.value =
                                    mutableStates.value +
                                    (request.id to latest.copy(status = status))
                            }
                            completed[request.id] = request.revision
                        }
                        changed.value++
                    }
                }
            }
        }
    }

    fun submit(id: String, value: LauncherPreferences) {
        synchronized(lock) {
            require(id in mutableStates.value || mutableStates.value.size < 12) {
                "Too many profiles"
            }
            val next = ++revision
            val normalized = value.normalized()
            mutableStates.value =
                mutableStates.value +
                (id to PreferenceSaveState(normalized, PreferenceSaveStatus.SAVING, next))
            pending[id] = Request(id, normalized, next)
        }
        wake.trySend(Unit).getOrThrow()
    }

    fun retry(id: String) {
        mutableStates.value[id]?.let { submit(id, it.value) }
    }

    /**
     * Retire deleted identities, including queued edits which can no longer be applied. In-flight
     * transactions finish normally.
     */
    fun retainProfiles(ids: Set<String>) {
        require(ids.size <= 12)
        synchronized(lock) {
            pending.keys.retainAll(ids)
            completed.keys.retainAll(ids)
            mutableStates.value = mutableStates.value.filterKeys { it in ids }
            changed.value++
        }
    }

    /** Waits through captured revisions, including any replacement of an unstarted snapshot. */
    suspend fun flush() {
        val target = synchronized(lock) { mutableStates.value.mapValues { it.value.revision } }
        changed.first {
            synchronized(lock) {
                target.all { (id, revision) ->
                    id !in mutableStates.value || (completed[id] ?: 0) >= revision
                }
            }
        }
        val failed =
            synchronized(lock) {
                target.any { (id, _) ->
                    mutableStates.value[id]?.status == PreferenceSaveStatus.FAILED
                }
            }
        if (failed) throw IOException("Preferences were not saved")
    }

    fun resetAfterRestore() {
        synchronized(lock) {
            check(
                pending.isEmpty() &&
                    mutableStates.value.values.none { it.status == PreferenceSaveStatus.SAVING },
            )
            mutableStates.value = emptyMap()
            completed.clear()
            changed.value++
        }
    }
}
