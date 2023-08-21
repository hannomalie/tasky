expect fun sleep(milliseconds: UInt)

expect class AtomicInt(value: Int) {
    fun addAndGet(delta: Int): Int
}