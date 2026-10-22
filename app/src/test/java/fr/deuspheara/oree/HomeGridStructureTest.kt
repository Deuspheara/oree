package fr.deuspheara.oree

import androidx.compose.ui.unit.IntSize
import fr.deuspheara.oree.feature.home.presentation.state.homeArtworkSize
import fr.deuspheara.oree.feature.home.presentation.state.homeGridSlots
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeGridStructureTest {
    @Test
    fun referenceTvArtworkCoversFocusAndFitsTheSharedCache() {
        val size = homeArtworkSize(IntSize(265, 149))
        assertEquals(IntSize(320, 180), size)
        assertEquals(size, homeArtworkSize(IntSize(255, 143)))
        assertTrue(size.width >= 265 * 1.10f && size.height >= 149 * 1.10f)
        assertTrue(
            "31 reference banners fit the unchanged 12MiB cache",
            31L * size.width * size.height * 4 <= 12 * 1024 * 1024,
        )
    }

    @Test
    fun unknownArtworkWaitsForLayoutAndLargeArtworkKeepsItsExistingCap() {
        assertEquals(IntSize.Zero, homeArtworkSize(IntSize.Zero))
        assertEquals(IntSize(480, 270), homeArtworkSize(IntSize(800, 450)))
    }

    @Test
    fun aSingleFavoriteAndPartialLastRowHaveBothEdges() {
        val slots = homeGridSlots(listOf("F", "A", "B", "C", "D", "E"), 1, 4, false)
        assertTrue(slots.getValue("F").atStart && slots.getValue("F").atEnd)
        assertEquals(1, slots.getValue("F").itemIndex)
        assertEquals(3, slots.getValue("A").itemIndex)
        assertTrue(slots.getValue("A").atStart)
        assertFalse(slots.getValue("A").atEnd)
        assertTrue(slots.getValue("D").atEnd)
        assertTrue(slots.getValue("E").atStart && slots.getValue("E").atEnd)
    }

    @Test
    fun failureStatusAndSectionHeadingsCountInActualLazyPositions() {
        val slots = homeGridSlots(listOf("F", "A", "B"), 1, 4, true)
        assertEquals(2, slots.getValue("F").itemIndex)
        assertEquals(4, slots.getValue("A").itemIndex)
        assertEquals(5, slots.getValue("B").itemIndex)
        assertEquals(2, homeGridSlots(listOf("A"), 0, 4, true).getValue("A").itemIndex)
    }
}
