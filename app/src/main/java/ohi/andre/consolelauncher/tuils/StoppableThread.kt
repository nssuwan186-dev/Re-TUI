package ohi.andre.consolelauncher.tuils

open class StoppableThread : Thread() {
    @Volatile
    private var stopped = false

    init {
        setDefaultUncaughtExceptionHandler { _, e ->
            Tuils.log(e)
            Tuils.toFile(e)
            System.exit(1)
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
