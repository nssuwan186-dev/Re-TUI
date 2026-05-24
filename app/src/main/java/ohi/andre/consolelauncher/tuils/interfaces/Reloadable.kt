package ohi.andre.consolelauncher.tuils.interfaces

import android.text.TextUtils
import java.util.ArrayList
import ohi.andre.consolelauncher.tuils.Tuils

interface Reloadable {
    fun reload()

    fun addMessage(header: String?, message: String?)

    class ReloadMessageCategory(@JvmField var header: String) {
        @JvmField val lines: MutableList<String> = ArrayList()

        fun text(): CharSequence {
            val sequence = TextUtils.concat(header, Tuils.NEWLINE)

            val builder = StringBuilder()
            val dash = "-"
            for (c in lines.indices) {
                builder.append(Tuils.SPACE).append(dash).append(Tuils.SPACE).append(lines[c]).append(Tuils.NEWLINE)
            }

            return TextUtils.concat(sequence, builder.toString())
        }

        override fun toString(): String = text().toString()
    }

    companion object {
        const val MESSAGE: String = "msg"
    }
}
