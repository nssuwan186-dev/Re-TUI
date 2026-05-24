package ohi.andre.consolelauncher.managers

import android.net.Uri
import ohi.andre.consolelauncher.tuils.Tuils
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.Locale
import java.util.ArrayList

object SearchProviderManager {
    const val PATH: String = "search.txt"
    private const val ASSIGN = "="
    private const val FALLBACK = "=>"
    private const val COMMENT = "#"

    fun get(name: String?): Provider? {
        val normalized = normalizeName(name)
        if (normalized == null) return null

        for (provider in load()) {
            if (provider.name.equals(normalized, ignoreCase = true)) {
                return provider
            }
        }

        return null
    }

    fun load(): MutableList<Provider> {
        ensureFile()
        val providers: MutableList<Provider> = ArrayList<Provider>()

        try {
            val reader = BufferedReader(FileReader(file()))
            var line: String?
            while ((reader.readLine().also { line = it }) != null) {
                val provider = parse(line)
                if (provider != null) providers.add(provider)
            }
            reader.close()
        } catch (e: Exception) {
            Tuils.log(e)
        }

        return providers
    }

    @JvmOverloads
    fun add(name: String?, template: String, fallbackTemplate: String? = null): Boolean {
        val normalized = normalizeName(name)
        if (normalized == null || !validTemplate(template)) {
            return false
        }

        val providers = load()
        for (i in providers.indices.reversed()) {
            if (providers.get(i).name.equals(normalized, ignoreCase = true)) {
                providers.removeAt(i)
            }
        }

        providers.add(Provider(normalized, template.trim { it <= ' ' }, clean(fallbackTemplate)))
        return write(providers)
    }

    fun remove(name: String?): Boolean {
        val normalized = normalizeName(name)
        if (normalized == null) return false

        val providers = load()
        var removed = false
        for (i in providers.indices.reversed()) {
            if (providers.get(i).name.equals(normalized, ignoreCase = true)) {
                providers.removeAt(i)
                removed = true
            }
        }

        return removed && write(providers)
    }

    fun reset(): Boolean {
        return write(defaultProviders())
    }

    fun file(): File {
        return File(Tuils.getFolder(), PATH)
    }

    fun labelsWith(baseLabels: Array<String?>): Array<String?> {
        val providers = load()
        val labels = arrayOfNulls<String>(baseLabels.size + providers.size)
        System.arraycopy(baseLabels, 0, labels, 0, baseLabels.size)

        for (i in providers.indices) {
            labels[baseLabels.size + i] = Tuils.MINUS + providers.get(i).name
        }

        return labels
    }

    private fun ensureFile() {
        try {
            val file = file()
            if (!file.exists() || file.length() == 0L) {
                write(defaultProviders())
            }
        } catch (e: Exception) {
            Tuils.log(e)
        }
    }

    private fun parse(rawLine: String?): Provider? {
        if (rawLine == null) return null

        val line = rawLine.trim { it <= ' ' }
        if (line.length == 0 || line.startsWith(COMMENT)) return null

        val pair: Array<String?> = line.split(ASSIGN.toRegex(), limit = 2).toTypedArray()
        if (pair.size != 2) return null

        val name = normalizeName(pair[0])
        var value = pair[1]!!.trim { it <= ' ' }
        var fallback: String? = null

        val fallbackIndex = value.indexOf(FALLBACK)
        if (fallbackIndex >= 0) {
            fallback = value.substring(fallbackIndex + FALLBACK.length).trim { it <= ' ' }
            value = value.substring(0, fallbackIndex).trim { it <= ' ' }
        }

        if (name == null || !validTemplate(value)) {
            return null
        }

        return Provider(name, value, clean(fallback))
    }

    private fun normalizeName(name: String?): String? {
        var name = name
        if (name == null) return null

        name = name.trim { it <= ' ' }.lowercase(Locale.getDefault())
        while (name!!.startsWith(Tuils.MINUS)) {
            name = name.substring(1)
        }

        if (name.length == 0) return null

        for (i in 0..<name.length) {
            val c = name.get(i)
            val valid = (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9')
                    || c == '_' || c == '-'
            if (!valid) return null
        }

        return name
    }

    private fun validTemplate(template: String?): Boolean {
        var template = template
        template = clean(template)
        return template != null && containsToken(template)
    }

    private fun containsToken(value: String): Boolean {
        return value.contains("{query}")
                || value.contains("{query+}")
                || value.contains("{slug}")
                || value.contains("{raw}")
                || value.contains("{url}")
    }

    private fun clean(value: String?): String? {
        var value = value
        if (value == null) return null
        value = value.trim { it <= ' ' }
        return if (value.length == 0) null else value
    }

    private fun write(providers: MutableList<Provider>): Boolean {
        try {
            val file = file()
            val writer = BufferedWriter(FileWriter(file, false))
            writer.write("# Re:T-UI search providers")
            writer.newLine()
            writer.write("# Format: name=url_template")
            writer.newLine()
            writer.write("# Optional fallback: name=primary_template => fallback_template")
            writer.newLine()
            writer.write("# Tokens: {query}=URL encoded, {query+}=plus encoded, {slug}=spaces_to_underscores, {raw}=unchanged, {url}=open URL")
            writer.newLine()
            for (provider in providers) {
                writer.write(provider.name)
                writer.write(ASSIGN)
                writer.write(provider.template)
                if (provider.fallbackTemplate != null) {
                    writer.write(" ")
                    writer.write(FALLBACK)
                    writer.write(" ")
                    writer.write(provider.fallbackTemplate)
                }
                writer.newLine()
            }
            writer.close()
            return true
        } catch (e: Exception) {
            Tuils.log(e)
            return false
        }
    }

    private fun defaultProviders(): MutableList<Provider> {
        val providers: MutableList<Provider> = ArrayList<Provider>()
        providers.add(Provider("gg", "https://www.google.com/search?q={query}", null))
        providers.add(
            Provider(
                "ps",
                "market://search?q={query}",
                "https://play.google.com/store/search?q={query}&c=apps"
            )
        )
        providers.add(Provider("yt", "https://www.youtube.com/results?search_query={query+}", null))
        providers.add(Provider("dd", "https://duckduckgo.com/?q={query}", null))
        providers.add(Provider("u", "{url}", null))
        return providers
    }

    class Provider(val name: String, val template: String, val fallbackTemplate: String?) {
        fun render(query: String?): String {
            return renderTemplate(template, query)
        }

        fun renderFallback(query: String?): String? {
            if (fallbackTemplate == null) return null
            return renderTemplate(fallbackTemplate, query)
        }

        companion object {
            private fun renderTemplate(template: String, query: String?): String {
                val raw = if (query == null) Tuils.EMPTYSTRING else query.trim { it <= ' ' }
                val encoded = Uri.encode(raw)
                val plus = encoded.replace("%20", "+")
                val slug = Uri.encode(raw.replace("\\s+".toRegex(), "_"))
                var url = raw

                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url
                }

                return template
                    .replace("{query+}", plus)
                    .replace("{query}", encoded)
                    .replace("{slug}", slug)
                    .replace("{raw}", raw)
                    .replace("{url}", url)
            }
        }
    }
}
