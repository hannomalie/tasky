import java.util.concurrent.atomic.AtomicInteger

actual fun sleep(milliseconds: UInt) = Thread.sleep(milliseconds.toLong())
actual typealias AtomicInt = AtomicInteger