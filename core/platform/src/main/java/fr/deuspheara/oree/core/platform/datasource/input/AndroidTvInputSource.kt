package fr.deuspheara.oree.core.platform.datasource.input

import android.content.Context
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.os.Handler
import android.os.Looper
import fr.deuspheara.oree.core.domain.concurrency.AppDispatchers
import fr.deuspheara.oree.core.domain.source.TvInputDataSource
import fr.deuspheara.oree.core.model.TvInput
import fr.deuspheara.oree.core.model.TvInputConnection
import fr.deuspheara.oree.core.model.TvInputs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.any
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.withContext

/**
 * Public system inputs only. Collection owns callbacks; binder and package-label work stay
 * off-main.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AndroidTvInputSource(
    context: Context,
    private val dispatchers: AppDispatchers = AppDispatchers(Dispatchers.IO, Dispatchers.Default),
) : TvInputDataSource {
    private val context = context.applicationContext
    override val inputs: Flow<TvInputs> = callbackFlow {
        val manager = context.getSystemService(TvInputManager::class.java)
        val callback =
            object : TvInputManager.TvInputCallback() {
                override fun onInputAdded(inputId: String) {
                    trySend(Unit)
                }

                override fun onInputRemoved(inputId: String) {
                    trySend(Unit)
                }

                override fun onInputUpdated(inputId: String) {
                    trySend(Unit)
                }

                override fun onInputStateChanged(inputId: String, state: Int) {
                    trySend(Unit)
                }

                override fun onTvInputInfoUpdated(inputInfo: TvInputInfo) {
                    trySend(Unit)
                }
            }
        try {
            manager?.registerCallback(callback, Handler(Looper.getMainLooper()))
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: RuntimeException) {
            /* Initial read still supplies an explicit outcome. */
        }
        trySend(Unit)
        awaitClose { runCatching { manager?.unregisterCallback(callback) } }
    }
        .buffer(Channel.CONFLATED)
        .mapLatest { load() }
        .distinctUntilChanged()

    private suspend fun load(): TvInputs = withContext(dispatchers.io) {
        try {
            val manager =
                context.getSystemService(TvInputManager::class.java)
                    ?: return@withContext TvInputs()
            val entries =
                manager.tvInputList
                    .filter { it.type == TvInputInfo.TYPE_HDMI && it.isPassthroughInput }
                    .distinctBy { it.id }
                    .take(32)
                    .map { input ->
                        TvInput(
                            input.id,
                            input.loadCustomLabel(context)?.toString()?.takeIf {
                                it.isNotBlank()
                            } ?: input.loadLabel(context).toString(),
                            when (manager.getInputState(input.id)) {
                                TvInputManager.INPUT_STATE_CONNECTED ->
                                    TvInputConnection.CONNECTED

                                TvInputManager.INPUT_STATE_CONNECTED_STANDBY ->
                                    TvInputConnection.STANDBY

                                TvInputManager.INPUT_STATE_DISCONNECTED ->
                                    TvInputConnection.DISCONNECTED

                                else -> TvInputConnection.UNKNOWN
                            },
                        )
                    }
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            TvInputs(entries)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: RuntimeException) {
            TvInputs(unavailable = true)
        }
    }

    /** Revalidate discovery before dispatch; no external input ID is accepted from an Intent. */
    override suspend fun contains(id: String): Boolean = load().entries.any { it.id == id }
}
