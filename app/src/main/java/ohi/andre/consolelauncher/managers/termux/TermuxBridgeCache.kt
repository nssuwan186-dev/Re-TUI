package ohi.andre.consolelauncher.managers.termux

import java.util.ArrayList
import java.util.Collections
import java.util.LinkedHashMap
import java.util.Locale
import java.util.Map

object TermuxBridgeCache {
    private const val REQUEST_TTL_MS = 2500L
    private const val MAX_CACHE_ENTRIES = 64
    private const val MAX_REQUEST_ENTRIES = 128
    private val dirs = newBoundedMap<Entry>(MAX_CACHE_ENTRIES)
    private val files = newBoundedMap<Entry>(MAX_CACHE_ENTRIES)
    private val requests = newBoundedMap<Long>(MAX_REQUEST_ENTRIES)

    private fun <T> newBoundedMap(maxEntries: Int): MutableMap<String, T> =
        object : LinkedHashMap<String, T>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, T>?): Boolean = size > maxEntries
        }

    @JvmStatic
    @Synchronized
    fun putDirs(path: String, stdout: String?) {
        dirs[path] = Entry(parse(stdout))
    }

    @JvmStatic
    @Synchronized
    fun putFiles(path: String, stdout: String?) {
        files[path] = Entry(parse(stdout))
    }

    @JvmStatic
    @Synchronized
    fun dirs(path: String): List<String> {
        val entry = dirs[path]
        return entry?.values ?: Collections.emptyList()
    }

    @JvmStatic
    @Synchronized
    fun files(path: String): List<String> {
        val entry = files[path]
        return entry?.values ?: Collections.emptyList()
    }

    @JvmStatic
    @Synchronized
    fun shouldRequest(type: String, path: String): Boolean {
        val key = "$type:$path"
        val now = System.currentTimeMillis()
        val last = requests[key]
        if (last != null && now - last < REQUEST_TTL_MS) {
            return false
        }
        requests[key] = now
        return true
    }

    private fun parse(stdout: String?): List<String> {
        val values: MutableList<String> = ArrayList()
        if (stdout == null) {
            return values
        }

        val lines = stdout.trim().split("\\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (line in lines) {
            var value = line.trim()
            if (value.endsWith("/")) {
                value = value.substring(0, value.length - 1)
            }
            if (value.isNotEmpty()) {
                values.add(value)
            }
        }
        Collections.sort(values, String.CASE_INSENSITIVE_ORDER)
        return values
    }

    private class Entry(val values: List<String>)
}
