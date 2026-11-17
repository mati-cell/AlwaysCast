package dev.matiribco.alwayscast.core.proxy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ByteRangeTest {
    @Test
    fun parse_openEndedRange_endsAtLastByte() {
        val range = ByteRange.parse("bytes=4-", size = 10)

        assertEquals(ByteRange(4, 9), range)
    }

    @Test
    fun parse_closedRange_keepsRequestedEnd() {
        val range = ByteRange.parse("bytes=2-5", size = 10)

        assertEquals(ByteRange(2, 5), range)
    }

    @Test
    fun parse_suffixRange_returnsLastBytes() {
        val range = ByteRange.parse("bytes=-3", size = 10)

        assertEquals(ByteRange(7, 9), range)
    }

    @Test
    fun parse_rangeEndingBeyondSize_clampsToLastByte() {
        val range = ByteRange.parse("bytes=8-100", size = 10)

        assertEquals(ByteRange(8, 9), range)
    }

    @Test
    fun parse_headerThatIsNotBytes_returnsNull() {
        val range = ByteRange.parse("items=0-1", size = 10)

        assertNull(range)
    }

    @Test
    fun parse_startAtOrBeyondSize_returnsNull() {
        val range = ByteRange.parse("bytes=10-", size = 10)

        assertNull(range)
    }

    @Test
    fun contentRangeHeader_closedRange_formatsTotalSize() {
        val range = ByteRange(2, 5)

        assertEquals("bytes 2-5/10", range.contentRangeHeader(size = 10))
    }
}
