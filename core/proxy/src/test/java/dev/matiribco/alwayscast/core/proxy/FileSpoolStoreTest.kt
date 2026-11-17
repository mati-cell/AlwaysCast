package dev.matiribco.alwayscast.core.proxy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileSpoolStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var root: File

    @Before
    fun setUp() {
        root = temporaryFolder.newFolder("spool")
    }

    @Test
    fun file_moreBytesThanCap_evictsOldestFiles() {
        val oldest = fileWithAge(name = "a", ageMillis = 1_000)
        val middle = fileWithAge(name = "b", ageMillis = 2_000)
        val newest = fileWithAge(name = "c", ageMillis = 3_000)

        storeWithCap(10).file("d")

        assertFalse(oldest.exists())
        assertFalse(middle.exists())
        assertTrue(newest.exists())
    }

    @Test
    fun file_underCap_keepsEveryFile() {
        val first = fileWithAge(name = "a", ageMillis = 1_000)
        val second = fileWithAge(name = "b", ageMillis = 2_000)

        storeWithCap(100).file("c")

        assertTrue(first.exists())
        assertTrue(second.exists())
    }

    @Test
    fun clear_filesInTheSpool_deletesThem() {
        val file = fileWithAge(name = "a", ageMillis = 1_000)

        storeWithCap(100).clear()

        assertFalse(file.exists())
    }

    private fun fileWithAge(
        name: String,
        ageMillis: Long,
    ): File =
        storeWithCap(1_000).file(name).apply {
            writeBytes(ByteArray(SIX_BYTES))
            setLastModified(ageMillis)
        }

    private fun storeWithCap(maxBytes: Long): FileSpoolStore = FileSpoolStore(root, maxBytes)

    private companion object {
        const val SIX_BYTES = 6
    }
}
