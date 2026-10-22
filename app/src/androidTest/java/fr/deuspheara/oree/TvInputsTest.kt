package fr.deuspheara.oree

import android.content.Intent
import android.media.tv.TvContract
import androidx.test.platform.app.InstrumentationRegistry
import fr.deuspheara.oree.core.platform.datasource.input.AndroidTvInputSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TvInputsTest {
    @Test
    fun ordinaryCallerDiscoversRealInputsWithoutAdditionalPermissions() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val inputs = withTimeout(5000) { AndroidTvInputSource(context).inputs.first() }
        assertFalse("Public discovery must not fail on the reference TV", inputs.unavailable)
        assertEquals(inputs.entries.size, inputs.entries.distinctBy { it.id }.size)
        val resolvable =
            inputs.entries.count { input ->
                Intent(
                    Intent.ACTION_VIEW,
                    TvContract.buildChannelUriForPassthroughInput(input.id),
                )
                    .resolveActivity(context.packageManager) != null
            }
        android.util.Log.i(
            "TVLInputProbe",
            "HDMI count=${inputs.entries.size}; ACTION_VIEW resolvable=$resolvable; ordinary caller",
        )
        Unit
    }
}
