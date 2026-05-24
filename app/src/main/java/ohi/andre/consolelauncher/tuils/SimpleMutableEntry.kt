package ohi.andre.consolelauncher.tuils

import java.io.Serializable
import java.util.Map

class SimpleMutableEntry<K, V>(
    override val key: K,
    private var currentValue: V
) : MutableMap.MutableEntry<K, V>, Serializable {
    override var value: V
        get() = currentValue
        set(newValue) {
            currentValue = newValue
        }

    override fun setValue(newValue: V): V {
        val result = currentValue
        currentValue = newValue
        return result
    }

    override fun toString(): String = "$key=$currentValue"
}
