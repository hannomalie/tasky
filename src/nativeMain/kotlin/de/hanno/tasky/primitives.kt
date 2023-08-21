import kotlin.native.concurrent.AtomicInt

actual fun sleep(milliseconds: UInt) {
    platform.posix.sleep(milliseconds / 1000u)
}

actual typealias AtomicInt = AtomicInt