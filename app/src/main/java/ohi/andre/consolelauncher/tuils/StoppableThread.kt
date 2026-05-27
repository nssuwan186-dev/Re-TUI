package ohi.andre.consolelauncher.tuils

open class StoppableThread : Thread() {
    @Volatile
    private var stopped = false

    init {
        uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, e ->
            Tuils.log(e)
            Tuils.toFile(e)
        }
    }

    override fun interrupt() {
        super.interrupt()
        synchronized(this) {
            stopped = true
        }
    }

    override fun isInterrupted(): Boolean {
        val wasStopped = synchronized(this) {
            stopped
        }
        return wasStopped || super.isInterrupted()
    }
}
