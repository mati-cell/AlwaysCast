package dev.matiribco.alwayscast.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class WindowPlannerTest {

    @Test
    fun rangesFor_fileSmallerThanBothWindows_returnsOneFullRange() {
        val ranges = WindowPlanner.rangesFor(size = 1_000)

        assertEquals(listOf(0L..999L), ranges)
    }

    @Test
    fun rangesFor_fileLargerThanBothWindows_returnsHeadAndTail() {
        val ranges = WindowPlanner.rangesFor(size = 100_000_000)

        assertEquals(2, ranges.size)
        assertEquals(0L..4_194_303L, ranges[0])
        assertEquals(95_805_696L..99_999_999L, ranges[1])
    }

    @Test
    fun rangesFor_unknownSize_returnsHeadOnly() {
        val ranges = WindowPlanner.rangesFor(size = -1)

        assertEquals(listOf(0L..4_194_303L), ranges)
    }

    @Test
    fun rangesFor_emptyFile_returnsNoRanges() {
        val ranges = WindowPlanner.rangesFor(size = 0)

        assertEquals(emptyList<LongRange>(), ranges)
    }

    @Test
    fun rangesFor_fileExactlyBothWindows_doesNotOverlap() {
        val ranges = WindowPlanner.rangesFor(size = 6L * 1024L * 1024L)

        assertEquals(listOf(0L..6_291_455L), ranges)
    }
}
