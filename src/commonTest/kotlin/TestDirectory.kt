import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

object TestDirectory {
    init {
        if(!FileSystem.SYSTEM.metadata(root).isDirectory) {
            FileSystem.SYSTEM.createDirectory("./build/test".toPath(), mustCreate = true)
        }
    }
    val root: Path get() = "./build/test".toPath()
}