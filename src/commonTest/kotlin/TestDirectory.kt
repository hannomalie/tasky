import de.hanno.tasky.fileSystem
import okio.Path
import okio.Path.Companion.toPath

object TestDirectory {
    init {
        require(fileSystem.metadata(root).isDirectory) {
            "Cannot find root test directory $root! Should have been created by the build tool!"
        }
    }
    val root: Path get() = "./build/test".toPath()
}