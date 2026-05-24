package ohi.andre.consolelauncher.commands

import java.util.HashMap
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsSave
import ohi.andre.consolelauncher.managers.xml.options.Cmd

class CommandsPreferences {
    private val preferenceHashMap = HashMap<String?, String?>()

    init {
        for (save in Cmd.values()) {
            preferenceHashMap[save.label()] = XMLPrefsManager.get(save)
        }
    }

    operator fun get(s: String?): String? {
        val v = preferenceHashMap[s]
        return v ?: if (s == null) null else XMLPrefsManager.get(XMLPrefsManager.XMLPrefsRoot.CMD, s)
    }

    operator fun get(save: XMLPrefsSave): String? {
        var v = get(save.label())
        if (v == null || v.isEmpty()) {
            v = save.defaultValue()
        }
        return v
    }

    fun userSetPriority(c: CommandAbstraction): Int {
        return try {
            val p = get(c.javaClass.simpleName + PRIORITY_SUFFIX)
            p!!.toInt()
        } catch (e: Exception) {
            Int.MAX_VALUE
        }
    }

    fun getPriority(c: CommandAbstraction): Int {
        val priority = userSetPriority(c)
        return if (priority == Int.MAX_VALUE) c.priority() else priority
    }

    companion object {
        const val PRIORITY_SUFFIX: String = "_priority"
    }
}
