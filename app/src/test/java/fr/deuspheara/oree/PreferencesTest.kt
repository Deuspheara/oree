package fr.deuspheara.oree

import fr.deuspheara.oree.core.domain.concurrency.PreferenceSaveStatus
import fr.deuspheara.oree.core.domain.concurrency.PreferenceWriter
import fr.deuspheara.oree.core.model.AccentColor
import fr.deuspheara.oree.core.model.ControlId
import fr.deuspheara.oree.core.model.ControlPosition
import fr.deuspheara.oree.core.model.ControlSize
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.ThemeMode
import fr.deuspheara.oree.core.model.TvInput
import fr.deuspheara.oree.core.model.TvInputConnection
import fr.deuspheara.oree.core.model.placeControls
import fr.deuspheara.oree.core.model.restoreInputId
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PreferencesTest {
    @Test
    fun persistedValuesCannotBreakLayout() {
        assertEquals(
            .75f,
            LauncherPreferences(glassIntensity = Float.NaN).normalized().glassIntensity,
        )
        assertEquals(.25f, LauncherPreferences(glassIntensity = -2f).normalized().glassIntensity)
        assertEquals(1f, LauncherPreferences(glassIntensity = 9f).normalized().glassIntensity)
        assertEquals(1f, LauncherPreferences(textScale = Float.NaN).normalized().textScale)
        assertEquals(2f, LauncherPreferences(textScale = 99f).normalized().textScale)
        assertEquals(1f, LauncherPreferences(textScale = -4f).normalized().textScale)
        assertEquals(
            ControlSize(1, 2),
            LauncherPreferences(controls = mapOf(ControlId.AUDIO to ControlSize(-1, 99)))
                .normalized()
                .controls[ControlId.AUDIO],
        )
    }

    @Test
    fun everyControlSizeCombinationPacksWithoutOverlapAndPreservesOrder() {
        val ids = ControlId.entries
        repeat(256) { combination ->
            var bits = combination
            val sizes = ids.associateWith {
                val n = bits % 4
                bits /= 4
                ControlSize(1 + n % 2, 1 + n / 2)
            }
            val placements = placeControls(ids, sizes)
            assertEquals(ids, placements.map { it.id })
            val occupied = hashSetOf<Pair<Int, Int>>()
            placements.forEach { p ->
                assertTrue(p.column + p.size.columns <= 4)
                assertTrue(p.row + p.size.rows <= 4)
                repeat(p.size.rows) { y ->
                    repeat(p.size.columns) { x ->
                        assertTrue(occupied.add(p.column + x to p.row + y))
                    }
                }
            }
        }
    }

    @Test
    fun positionsAreBoundedAndPinnedMovesRepackNeighbours() {
        assertEquals(ControlPosition(2, 0), ControlPosition(99, -4).normalized(ControlSize(2, 2)))
        val ids = ControlId.entries
        val before = placeControls(ids, emptyMap())
        val positions = before.associate { it.id to ControlPosition(it.column, it.row) }
        val moved =
            placeControls(
                ids,
                emptyMap(),
                positions + (ControlId.SETTINGS to ControlPosition(3, 3)),
                ControlId.SETTINGS,
            )
        assertEquals(
            ControlPosition(3, 3),
            moved.first().let { ControlPosition(it.column, it.row) },
        )
        val collisions =
            placeControls(
                ids,
                ids.associateWith { ControlSize(2, 2) },
                ids.associateWith { ControlPosition(3, 3) },
            )
        val occupied = hashSetOf<Pair<Int, Int>>()
        collisions.forEach { p ->
            repeat(2) { y ->
                repeat(2) { x -> assertTrue(occupied.add(p.column + x to p.row + y)) }
            }
        }
        assertEquals(16, occupied.size)
    }

    @Test
    fun rapidWritesCannotAcknowledgeAnOlderValueAndCaptureTheirProfile() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val gate = CompletableDeferred<Unit>()
        val saved = mutableListOf<Pair<String, LauncherPreferences>>()
        val writer =
            PreferenceWriter(scope) { id, value ->
                if (saved.isEmpty()) gate.await()
                saved += id to value
            }
        try {
            writer.submit("first", LauncherPreferences(theme = ThemeMode.LIGHT))
            writer.submit("first", LauncherPreferences(accent = AccentColor.GREEN))
            writer.submit("second", LauncherPreferences(accent = AccentColor.PINK))
            assertEquals(AccentColor.GREEN, writer.states.value["first"]!!.value.accent)
            gate.complete(Unit)
            yield()
            assertEquals(listOf("first", "first", "second"), saved.map { it.first })
            assertEquals(AccentColor.GREEN, saved[1].second.accent)
            assertEquals(PreferenceSaveStatus.SAVED, writer.states.value["first"]!!.status)
            assertEquals(AccentColor.PINK, writer.states.value["second"]!!.value.accent)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun failedWriteRetainsChoiceAndRetryPersistsLatest() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var fail = true
        var saved: LauncherPreferences? = null
        val writer =
            PreferenceWriter(scope) { _, value ->
                if (fail) throw IOException("fixture")
                saved = value
            }
        try {
            writer.submit("first", LauncherPreferences(accent = AccentColor.RED))
            yield()
            assertEquals(PreferenceSaveStatus.FAILED, writer.states.value["first"]!!.status)
            assertEquals(AccentColor.RED, writer.states.value["first"]!!.value.accent)
            fail = false
            writer.retry("first")
            yield()
            assertEquals(AccentColor.RED, saved!!.accent)
            assertEquals(PreferenceSaveStatus.SAVED, writer.states.value["first"]!!.status)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun removedInputRestoresNeighbourAndEmptyListHasNoTarget() {
        val inputs =
            listOf(
                TvInput("one", "HDMI 1", TvInputConnection.CONNECTED),
                TvInput("three", "HDMI 3", TvInputConnection.DISCONNECTED),
            )
        assertEquals("three", restoreInputId("two", 1, inputs))
        assertEquals("one", restoreInputId("one", 1, inputs))
        assertNull(restoreInputId("one", 0, emptyList()))
    }
}
