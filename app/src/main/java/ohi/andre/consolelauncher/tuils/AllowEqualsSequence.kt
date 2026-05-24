package ohi.andre.consolelauncher.tuils

import java.util.ArrayList
import java.util.Collections
import androidx.annotation.NonNull

class AllowEqualsSequence(values: FloatArray, objs: Array<Any?>) {
    private val sequence: MutableList<Entry> = ArrayList()

    init {
        for (count in values.indices) {
            if (values[count] == Int.MAX_VALUE.toFloat()) {
                continue
            }
            sequence.add(Entry(values[count], objs[count]))
        }
        Collections.sort(sequence)

        var counter = -1
        var last = Int.MAX_VALUE
        for (count in sequence.indices) {
            val entry = sequence[count]

            val i = entry.value.toInt()
            if (i != last) {
                counter++
            }

            entry.key = counter
            last = i
        }
    }

    operator fun get(key: Int): Array<Any?> {
        val o: MutableList<Any?> = ArrayList()
        for (entry in sequence) {
            if (entry.key == key) {
                o.add(entry.obj)
            } else if (o.isNotEmpty()) {
                break
            }
        }

        return o.toTypedArray()
    }

    fun getMaxKey(): Int {
        if (sequence.size == 0) {
            return -1
        }
        return sequence[sequence.size - 1].key
    }

    fun getMinKey(): Int {
        if (sequence.size == 0) {
            return -1
        }
        return sequence[0].key
    }

    fun size(): Int = sequence.size

    private class Entry(
        var value: Float,
        var obj: Any?
    ) : Comparable<Entry> {
        var key: Int = 0

        override fun compareTo(other: Entry): Int {
            val result = value - other.value

            if (result == 0f) {
                return 0
            }
            if (result < 0) {
                return -1
            }
            return 1
        }

        override fun toString(): String = "key: $key: ${obj.toString()}"
    }
}
