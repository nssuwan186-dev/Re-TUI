package ohi.andre.consolelauncher.tuils.libsuperuser

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

class StreamGobbler : Thread {
    fun interface OnLineListener {
        fun onLine(line: String)
    }

    private var shell: String? = null
    private var reader: BufferedReader? = null
    private var writer: MutableList<String>? = null
    private var listener: OnLineListener? = null

    constructor(shell: String, inputStream: InputStream, outputList: MutableList<String>?) {
        this.shell = shell
        reader = BufferedReader(InputStreamReader(inputStream))
        writer = outputList
    }

    constructor(shell: String, inputStream: InputStream, onLineListener: OnLineListener?) {
        this.shell = shell
        reader = BufferedReader(InputStreamReader(inputStream))
        listener = onLineListener
    }

    override fun run() {
        try {
            while (true) {
                val line = reader!!.readLine() ?: break
                writer?.add(line)
                listener?.onLine(line)
            }
        } catch (ignored: IOException) {
        }

        try {
            reader!!.close()
        } catch (ignored: IOException) {
        }
    }
}
