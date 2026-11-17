package dev.matiribco.alwayscast.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class BuildInfoTest {

    @Test
    fun versionName_whenRead_isTheMvpVersion() {
        val expectedVersion = "0.1.0"

        val actualVersion = BuildInfo.VERSION_NAME

        assertEquals(expectedVersion, actualVersion)
    }
}
