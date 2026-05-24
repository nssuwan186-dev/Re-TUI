package ohi.andre.consolelauncher.managers.xml.classes

import java.util.ArrayList
import ohi.andre.consolelauncher.tuils.Tuils

class XMLPrefsList {
    @JvmField val list: MutableList<XMLPrefsEntry> = ArrayList()

    fun add(entry: XMLPrefsEntry) {
        for (count in list.indices) {
            if (list[count].equals(entry.key)) {
                list[count] = entry
                return
            }
        }
        list.add(entry)
    }

    fun add(key: String?, value: String?) {
        add(XMLPrefsEntry(key ?: Tuils.EMPTYSTRING, value ?: Tuils.EMPTYSTRING))
    }

    operator fun get(o: Any?): XMLPrefsEntry? {
        if (o is Int) {
            return at(o)
        }

        for (e in list) {
            if (e.equals(o)) {
                return e
            }
        }
        return null
    }

    fun at(index: Int): XMLPrefsEntry = list[index]

    fun size(): Int = list.size

    fun values(): List<String> {
        val vs: MutableList<String> = ArrayList()
        for (entry in list) {
            vs.add(entry.key + "=" + entry.value)
        }
        return vs
    }

    override fun toString(): String {
        val builder = StringBuilder()

        for (entry in list) {
            builder.append(entry.key).append(" -> ").append(entry.value).append(Tuils.NEWLINE)
        }

        return builder.toString().trim()
    }
}
