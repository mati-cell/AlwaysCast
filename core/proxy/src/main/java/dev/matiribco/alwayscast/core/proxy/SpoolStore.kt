package dev.matiribco.alwayscast.core.proxy

import java.io.File

interface SpoolStore {
    fun file(key: String): File
}

class FileSpoolStore(
    private val root: File,
    private val maxBytes: Long = SPOOL_MAX_BYTES,
) : SpoolStore {
    override fun file(key: String): File {
        root.mkdirs()
        val target = File(root, key)
        if (!target.exists()) target.createNewFile()
        evict(keeping = target)
        return target
    }

    fun clear() {
        root.listFiles()?.forEach(File::delete)
    }

    private fun evict(keeping: File) {
        val files = root.listFiles()?.sortedBy(File::lastModified) ?: return
        var totalBytes = files.sumOf(File::length)

        files.forEach { file ->
            if (totalBytes > maxBytes && file != keeping) {
                totalBytes -= file.length()
                file.delete()
            }
        }
    }

    companion object {
        const val SPOOL_MAX_BYTES = 1L * 1024L * 1024L * 1024L
    }
}
