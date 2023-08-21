package de.hanno.tasky.cache

import de.hanno.tasky.fileSystem
import okio.FileSystem
import okio.Path.Companion.toPath

fun getLastModifiedTime(filePath: String): Long? {
    val metaData = fileSystem.metadata(filePath.toPath())
    return metaData.lastModifiedAtMillis
}

fun getHashForFile(filePath: String): String {
    val metaData = fileSystem.metadata(filePath.toPath())
    return if(metaData.isDirectory) {
        val filesInFolder = fileSystem.list(filePath.toPath())
        filesInFolder.joinToString { path ->
            getHashForFile(path.toString())
        }
    } else {
        val lastModificationOrNull = metaData.lastModifiedAtMillis
        """${filePath.toPath(normalize = true)}:$lastModificationOrNull""".hashCode().toString()
    }
}