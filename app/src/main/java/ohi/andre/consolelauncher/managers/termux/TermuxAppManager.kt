package ohi.andre.consolelauncher.managers.termux

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import org.json.JSONArray
import org.json.JSONObject
import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.Locale

object TermuxAppManager {
    private const val PREFS = "retui_termux_apps"
    private const val KEY_IDS = "ids"
    private const val KEY_TITLE_PREFIX = "title_"
    private const val KEY_COMMAND_PREFIX = "command_"
    private const val KEY_WORKDIR_PREFIX = "workdir_"
    private const val KEY_ACTIONS_PREFIX = "actions_"
    private const val APP_ROOT = TermuxBridgeManager.TERMUX_HOME + "/.retui/apps"

    const val TERMINALPHONE_ID = "terminalphone"

    fun normalizeId(value: String?): String {
        if (value == null) {
            return ""
        }
        return value.trim { it <= ' ' }
            .lowercase(Locale.getDefault())
            .replace("[^a-z0-9_-]".toRegex(), "")
    }

    fun list(context: Context): MutableList<TermuxApp> {
        val out = ArrayList<TermuxApp>()
        val store = prefs(context)
        out.add(defaultTerminalPhone(store))

        val seen = LinkedHashSet<String>()
        seen.add(TERMINALPHONE_ID)
        for (rawId in store.getStringSet(KEY_IDS, LinkedHashSet<String>()) ?: LinkedHashSet()) {
            val id = normalizeId(rawId)
            if (TextUtils.isEmpty(id) || seen.contains(id)) {
                continue
            }
            val app = storedApp(store, id)
            if (app != null) {
                seen.add(id)
                out.add(app)
            }
        }
        return out
    }

    fun resolve(context: Context, id: String?): TermuxApp? {
        val normalized = normalizeId(id)
        if (TextUtils.isEmpty(normalized)) {
            return null
        }
        val store = prefs(context)
        if (TERMINALPHONE_ID == normalized) {
            return defaultTerminalPhone(store)
        }
        return storedApp(store, normalized)
    }

    fun add(context: Context, id: String?, title: String?, command: String?, workDir: String?): Boolean {
        val normalized = normalizeId(id)
        val normalizedCommand = command?.trim { it <= ' ' } ?: ""
        if (TextUtils.isEmpty(normalized) || TextUtils.isEmpty(normalizedCommand)) {
            return false
        }

        val store = prefs(context)
        val ids = LinkedHashSet<String>(store.getStringSet(KEY_IDS, LinkedHashSet<String>()) ?: LinkedHashSet())
        ids.add(normalized)
        store.edit()
            .putStringSet(KEY_IDS, ids)
            .putString(KEY_TITLE_PREFIX + normalized, cleanTitle(title, normalized))
            .putString(KEY_COMMAND_PREFIX + normalized, normalizedCommand)
            .putString(KEY_WORKDIR_PREFIX + normalized, cleanWorkDir(workDir))
            .apply()
        return true
    }

    fun remove(context: Context, id: String?): Boolean {
        val normalized = normalizeId(id)
        if (TextUtils.isEmpty(normalized) || TERMINALPHONE_ID == normalized) {
            return false
        }
        val store = prefs(context)
        val ids = LinkedHashSet<String>(store.getStringSet(KEY_IDS, LinkedHashSet<String>()) ?: LinkedHashSet())
        val removed = ids.remove(normalized)
        store.edit()
            .putStringSet(KEY_IDS, ids)
            .remove(KEY_TITLE_PREFIX + normalized)
            .remove(KEY_COMMAND_PREFIX + normalized)
            .remove(KEY_WORKDIR_PREFIX + normalized)
            .remove(KEY_ACTIONS_PREFIX + normalized)
            .apply()
        return removed
    }

    fun addAction(context: Context, id: String?, label: String?, send: String?): Boolean {
        val normalized = normalizeId(id)
        val cleanLabel = label?.trim { it <= ' ' } ?: ""
        val cleanSend = send?.trim { it <= ' ' } ?: ""
        if (TextUtils.isEmpty(normalized) || TextUtils.isEmpty(cleanLabel)) {
            return false
        }
        if (resolve(context, normalized) == null) {
            return false
        }

        val store = prefs(context)
        val actions = ArrayList(readStoredActions(store, normalized))
        actions.removeAll { sameActionLabel(it.label, cleanLabel) }
        actions.add(TermuxAppAction(cleanLabel, cleanSend))
        store.edit()
            .putString(KEY_ACTIONS_PREFIX + normalized, actionsToJson(actions))
            .apply()
        return true
    }

    fun removeAction(context: Context, id: String?, label: String?): Boolean {
        val normalized = normalizeId(id)
        val cleanLabel = label?.trim { it <= ' ' } ?: ""
        if (TextUtils.isEmpty(normalized) || TextUtils.isEmpty(cleanLabel)) {
            return false
        }

        val store = prefs(context)
        val actions = ArrayList(readStoredActions(store, normalized))
        val removed = actions.removeAll { sameActionLabel(it.label, cleanLabel) }
        if (removed) {
            store.edit()
                .putString(KEY_ACTIONS_PREFIX + normalized, actionsToJson(actions))
                .apply()
        }
        return removed
    }

    fun tmuxSessionName(id: String?): String {
        return "retui_" + normalizeId(id)
    }

    fun appHomeDir(id: String?): String {
        return APP_ROOT + "/" + normalizeId(id)
    }

    fun manifestJson(app: TermuxApp): String {
        val actions = JSONArray()
        for (action in app.actions) {
            actions.put(
                JSONObject()
                    .put("label", action.label)
                    .put("send", action.send)
            )
        }

        return JSONObject()
            .put("version", 1)
            .put("id", app.id)
            .put("title", app.title)
            .put("type", "terminal")
            .put("command", app.command)
            .put("workDir", app.workDir)
            .put("homeDir", app.homeDir)
            .put("manifest", app.manifestPath)
            .put("state", app.statePath)
            .put("memoryDir", app.memoryDir)
            .put("logsDir", app.logsDir)
            .put("persistent", true)
            .put("memory", true)
            .put("actions", actions)
            .toString(2)
    }

    private fun storedApp(store: SharedPreferences, id: String): TermuxApp? {
        val command = store.getString(KEY_COMMAND_PREFIX + id, "") ?: ""
        if (TextUtils.isEmpty(command.trim { it <= ' ' })) {
            return null
        }
        return TermuxApp(
            id,
            store.getString(KEY_TITLE_PREFIX + id, cleanTitle(null, id)) ?: cleanTitle(null, id),
            command.trim { it <= ' ' },
            cleanWorkDir(store.getString(KEY_WORKDIR_PREFIX + id, null)),
            readStoredActions(store, id)
        )
    }

    private fun defaultTerminalPhone(store: SharedPreferences?): TermuxApp {
        val command = (
                "if [ -f \"\$HOME/terminalphone/terminalphone.sh\" ]; then " +
                        "cd \"\$HOME/terminalphone\" && exec bash terminalphone.sh; " +
                        "elif [ -f \"\$HOME/retui/terminalphone.sh\" ]; then " +
                        "cd \"\$HOME/retui\" && exec bash terminalphone.sh; " +
                        "else " +
                        "printf '%s\\n' 'TerminalPhone was not found in ~/terminalphone or ~/retui.' " +
                        "'Clone it in Termux, then reopen this app.' " +
                        "'git clone https://gitlab.com/here_forawhile/terminalphone.git ~/terminalphone'; " +
                        "exec sh; " +
                        "fi"
                )
        return TermuxApp(
            TERMINALPHONE_ID,
            "TerminalPhone",
            command,
            TermuxBridgeManager.TERMUX_HOME,
            mergeActions(defaultTerminalPhoneActions(), readStoredActions(store, TERMINALPHONE_ID))
        )
    }

    private fun defaultTerminalPhoneActions(): List<TermuxAppAction> {
        val actions = ArrayList<TermuxAppAction>()
        actions.add(TermuxAppAction("start tor", "8"))
        actions.add(TermuxAppAction("status", "6"))
        actions.add(TermuxAppAction("show onion", "3"))
        actions.add(TermuxAppAction("stop tor", "9"))
        return actions
    }

    private fun readStoredActions(store: SharedPreferences?, id: String): List<TermuxAppAction> {
        if (store == null) {
            return emptyList()
        }
        val raw = store.getString(KEY_ACTIONS_PREFIX + id, "[]") ?: "[]"
        val actions = ArrayList<TermuxAppAction>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val label = item.optString("label", "").trim { it <= ' ' }
                val send = item.optString("send", item.optString("input", "")).trim { it <= ' ' }
                if (!TextUtils.isEmpty(label)) {
                    actions.add(TermuxAppAction(label, send))
                }
            }
        } catch (e: Exception) {
            return emptyList()
        }
        return actions
    }

    private fun actionsToJson(actions: List<TermuxAppAction>): String {
        val array = JSONArray()
        for (action in actions) {
            array.put(
                JSONObject()
                    .put("label", action.label)
                    .put("send", action.send)
            )
        }
        return array.toString()
    }

    private fun mergeActions(
        defaults: List<TermuxAppAction>,
        stored: List<TermuxAppAction>
    ): List<TermuxAppAction> {
        val merged = LinkedHashMap<String, TermuxAppAction>()
        for (action in defaults) {
            merged[action.label.lowercase(Locale.getDefault())] = action
        }
        for (action in stored) {
            merged[action.label.lowercase(Locale.getDefault())] = action
        }
        return ArrayList(merged.values)
    }

    private fun sameActionLabel(left: String?, right: String?): Boolean {
        return (left ?: "").trim { it <= ' ' }.equals(
            (right ?: "").trim { it <= ' ' },
            ignoreCase = true
        )
    }

    private fun cleanTitle(title: String?, fallbackId: String): String {
        val clean = title?.trim { it <= ' ' } ?: ""
        if (!TextUtils.isEmpty(clean)) {
            return clean
        }
        return fallbackId.replace("-", " ").replace("_", " ")
    }

    private fun cleanWorkDir(workDir: String?): String {
        val clean = workDir?.trim { it <= ' ' } ?: ""
        return if (TextUtils.isEmpty(clean)) TermuxBridgeManager.TERMUX_HOME else clean
    }

    private fun prefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    data class TermuxApp(
        val id: String,
        val title: String,
        val command: String,
        val workDir: String,
        val actions: List<TermuxAppAction> = emptyList()
    ) {
        val homeDir: String
            get() = appHomeDir(id)
        val manifestPath: String
            get() = homeDir + "/app.json"
        val statePath: String
            get() = homeDir + "/state.json"
        val memoryDir: String
            get() = homeDir + "/memory"
        val logsDir: String
            get() = homeDir + "/logs"
    }

    data class TermuxAppAction(
        val label: String,
        val send: String
    )
}
