package de.hanno.tasky.cache

import okio.FileSystem
import okio.Path.Companion.toPath

fun getLastModifiedTime(filePath: String): Long? {
    val metaData = FileSystem.SYSTEM.metadata(filePath.toPath())
    return metaData.lastModifiedAtMillis
}

fun getHashForFile(filePath: String): String {
    val metaData = FileSystem.SYSTEM.metadata(filePath.toPath())
    return if(metaData.isDirectory) {
        val filesInFolder = FileSystem.SYSTEM.list(filePath.toPath())
        filesInFolder.joinToString { path ->
            getHashForFile(path.toString())
        }
    } else {
        val lastModificationOrNull = metaData.lastModifiedAtMillis
        """${filePath.toPath(normalize = true)}:$lastModificationOrNull""".hashCode().toString()
    }
}