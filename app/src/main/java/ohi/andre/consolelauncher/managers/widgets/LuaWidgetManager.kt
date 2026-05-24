package ohi.andre.consolelauncher.managers.widgets

import android.text.TextUtils
import ohi.andre.consolelauncher.tuils.Tuils
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Collections
import java.util.Locale
import java.util.regex.Pattern
import android.content.Context
import java.util.ArrayList
import java.util.HashSet
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.Map
import java.util.Set
import java.util.regex.Matcher

object LuaWidgetManager {
    const val ENGINE: String = "lua"
    const val SOURCE_PREFIX: String = "lua:"
    const val WIDGETS_DIR: String = "widgets"
    const val SCRIPT_FILE: String = "main.lua"
    const val MANIFEST_FILE: String = "manifest.json"
    const val SYSTEM_TIMER_WIDGET_ID: String = "timer"
    private val BUNDLED_SAMPLE_IDS: Array<String?> = arrayOf<String?>(
        "aio_counter",
        "aio_progress",
        "aio_clock",
        "retui_counter",
        "retui_progress",
        "retui_clock",
        "retui_expandable",
        "retui_ticker",
        "retui_toolkit",
        "retui_suggest",
        "retui_prefs",
        "retui_files",
        "retui_platform",
        "retui_public_ip"
    )
    private val BUNDLED_SAMPLE_MARKERS: Array<String?> = arrayOf<String?>(
        "Persistent counter using Re:TUI prefs and dock actions",
        "Progress widget using Re:TUI prefs, system state, and progress APIs",
        "Clock widget using Re:TUI system helpers",
        "Compact and expanded rendering using Re:TUI widget state",
        "Active-widget ticking lifecycle demo",
        "Small standard library demo",
        "Lua suggestion script demo",
        "Preferences helper demo",
        "Widget-local file helper demo",
        "System/platform helper demo",
        "https://api.ipify.org?format=json"
    )
    private val SENSITIVE_CAPABILITIES: MutableSet<String?> = HashSet<String?>()

    init {
        Collections.addAll<String?>(
            SENSITIVE_CAPABILITIES,
            "network",
            "clipboard",
            "vibrate",
            "local-files",
            "active-tick"
        )
    }

    fun bundledSampleIds(): MutableList<String?> {
        val ids = ArrayList<String?>()
        Collections.addAll<String?>(ids, *BUNDLED_SAMPLE_IDS)
        return ids
    }

    fun isBundledSample(id: String?): Boolean {
        val normalized = normalizeId(id)
        for (sampleId in BUNDLED_SAMPLE_IDS) {
            if (TextUtils.equals(normalized, sampleId)) {
                return true
            }
        }
        val script = readScript(normalized)
        if (TextUtils.isEmpty(script)) {
            return false
        }
        for (marker in BUNDLED_SAMPLE_MARKERS) {
            if (script!!.contains(marker!!)) {
                return true
            }
        }
        return false
    }

    fun widgetsDir(): File {
        val dir = File(Tuils.getFolder(), WIDGETS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun widgetDir(id: String?): File {
        return File(widgetsDir(), normalizeId(id))
    }

    fun scriptFile(id: String?): File {
        return File(widgetDir(id), SCRIPT_FILE)
    }

    fun manifestFile(id: String?): File {
        return File(widgetDir(id), MANIFEST_FILE)
    }

    fun exists(id: String?): Boolean {
        return scriptFile(id).isFile()
    }

    fun listIds(): MutableList<String?> {
        val ids = ArrayList<String?>()
        val files = widgetsDir().listFiles()
        if (files == null) {
            return ids
        }
        for (file in files) {
            if (file.isDirectory() && File(file, SCRIPT_FILE).isFile()) {
                ids.add(file.getName())
            }
        }
        Collections.sort<String?>(ids, String.CASE_INSENSITIVE_ORDER)
        return ids
    }

    fun readScript(id: kotlin.String?): kotlin.String? {
        val file = scriptFile(id)
        if (!file.isFile()) {
            return ""
        }
        try {
            FileInputStream(file).use { `in` ->
                return Tuils.convertStreamToString(`in`)
            }
        } catch (e: Exception) {
            return ""
        }
    }

    fun getName(id: kotlin.String?): kotlin.String? {
        val normalized = normalizeId(id)
        val manifest = manifestFile(normalized)
        if (manifest.isFile()) {
            try {
                FileInputStream(manifest).use { `in` ->
                    val `object` = JSONObject(Tuils.convertStreamToString(`in`))
                    val name = `object`.optString("name", "")
                    if (!TextUtils.isEmpty(name)) {
                        return name
                    }
                }
            } catch (ignored: Exception) {
            }
        }
        val scriptName = metadata(readScript(normalized)).get("name")
        if (!TextUtils.isEmpty(scriptName)) {
            return scriptName
        }
        return displayName(normalized)
    }

    fun getScriptType(id: kotlin.String?): kotlin.String {
        return getScriptTypeFromScript(readScript(normalizeId(id)))
    }

    fun getScriptTypeFromScript(script: kotlin.String?): kotlin.String {
        val type = metadata(script).get("type")
        return if (TextUtils.isEmpty(type)) "widget" else type!!.lowercase()
    }

    fun apiVersion(id: kotlin.String?): kotlin.String {
        return apiVersionFromScript(readScript(normalizeId(id)))
    }

    fun apiVersionFromScript(script: kotlin.String?): kotlin.String {
        val meta = metadata(script)
        var version = meta.get("retui")
        if (TextUtils.isEmpty(version)) {
            version = meta.get("retui_version")
        }
        return (if (android.text.TextUtils.isEmpty(version)) "1" else version)!!
    }

    fun isDockableScript(script: kotlin.String?): Boolean {
        val type = getScriptTypeFromScript(script)
        return "suggest" != type && "command" != type
    }

    fun isDockable(id: kotlin.String?): Boolean {
        return isDockableScript(readScript(normalizeId(id)))
    }

    fun isEnabled(id: kotlin.String?): Boolean {
        val manifest = readManifest(normalizeId(id))
        return manifest == null || !manifest.optBoolean("disabled", false)
    }

    @Throws(Exception::class)
    fun setEnabled(id: kotlin.String?, enabled: Boolean) {
        val normalized = normalizeId(id)
        require(exists(normalized)) { "Unknown widget: " + normalized }
        var manifest = readManifest(normalized)
        if (manifest == null) {
            manifest = JSONObject()
            manifest.put("type", "retui-widget")
            manifest.put("schema", 1)
            manifest.put("id", normalized)
            manifest.put("name", getName(normalized))
            manifest.put("engine", ENGINE)
        }
        manifest.put("disabled", !enabled)
        write(manifestFile(normalized), manifest.toString(2) + "\n")
    }

    fun version(id: kotlin.String?): Long {
        val file = scriptFile(id)
        return if (file.isFile()) file.lastModified() else 0L
    }

    @Throws(Exception::class)
    fun save(id: kotlin.String?, name: kotlin.String?, script: kotlin.String?) {
        val normalized = normalizeId(id)
        require(!TextUtils.isEmpty(normalized)) { "Widget id is required" }
        val code = if (script == null) "" else script
        val meta = metadata(code)
        val scriptName = meta.get("name")
        var effectiveName: kotlin.String? = if (name == null) "" else name.trim { it <= ' ' }
        if (!TextUtils.isEmpty(scriptName)
            && (TextUtils.isEmpty(effectiveName)
                    || TextUtils.equals(effectiveName, normalized)
                    || TextUtils.equals(effectiveName, displayName(normalized)))
        ) {
            effectiveName = scriptName
        }
        val dir = widgetDir(normalized)
        check(!(!dir.exists() && !dir.mkdirs())) { "Unable to create widget folder" }
        val previousManifest = readManifest(normalized)
        val previousApprovedHash = if (previousManifest == null) "" else previousManifest.optString(
            "approvedScriptHash",
            ""
        )
        val previousApprovedPermissions =
            if (previousManifest == null) "" else previousManifest.optString(
                "approvedPermissions",
                ""
            )
        val hash = scriptHash(code)
        val requiredPermissions = requiredPermissions(code)
        write(scriptFile(normalized), code)

        val manifest = JSONObject()
        manifest.put("type", "retui-widget")
        manifest.put("schema", 1)
        manifest.put("id", normalized)
        manifest.put(
            "name",
            if (TextUtils.isEmpty(effectiveName)) displayName(normalized) else effectiveName
        )
        manifest.put("engine", ENGINE)
        manifest.put("devOnly", true)
        val metadata = JSONObject()
        for (entry in meta.entries) {
            metadata.put(entry.key, entry.value)
        }
        manifest.put("metadata", metadata)
        manifest.put("scriptHash", hash)
        manifest.put("permissions", TextUtils.join(",", requiredPermissions))
        if (TextUtils.equals(previousApprovedHash, hash)
            && permissionList(previousApprovedPermissions).containsAll(requiredPermissions)
        ) {
            manifest.put("approvedScriptHash", previousApprovedHash)
            manifest.put("approvedPermissions", previousApprovedPermissions)
        }
        if (previousManifest != null && previousManifest.optBoolean("disabled", false)) {
            manifest.put("disabled", true)
        }
        write(manifestFile(normalized), manifest.toString(2) + "\n")
    }

    @Throws(Exception::class)
    fun ensureSystemTimerWidget() {
        ensureSystemWidget(SYSTEM_TIMER_WIDGET_ID, "Timer", systemTimerScript())
    }

    @Throws(Exception::class)
    private fun ensureSystemWidget(
        id: kotlin.String?,
        name: kotlin.String?,
        script: kotlin.String?
    ) {
        val normalized = normalizeId(id)
        val manifest = readManifest(normalized)
        if (exists(normalized) && (manifest == null || !manifest.optBoolean(
                "systemManaged",
                false
            ))
        ) {
            return
        }
        saveSystem(normalized, name, script)
    }

    @Throws(Exception::class)
    private fun saveSystem(id: kotlin.String?, name: kotlin.String?, script: kotlin.String?) {
        val normalized = normalizeId(id)
        require(!TextUtils.isEmpty(normalized)) { "Widget id is required" }
        val code = if (script == null) "" else script
        val meta = metadata(code)
        val hash = scriptHash(code)
        val requiredPermissions = requiredPermissions(code)
        write(scriptFile(normalized), code)

        val manifest = JSONObject()
        manifest.put("type", "retui-widget")
        manifest.put("schema", 1)
        manifest.put("id", normalized)
        manifest.put("name", if (TextUtils.isEmpty(name)) displayName(normalized) else name)
        manifest.put("engine", ENGINE)
        manifest.put("devOnly", false)
        manifest.put("systemManaged", true)
        val metadata = JSONObject()
        for (entry in meta.entries) {
            metadata.put(entry.key, entry.value)
        }
        manifest.put("metadata", metadata)
        manifest.put("scriptHash", hash)
        manifest.put("permissions", TextUtils.join(",", requiredPermissions))
        manifest.put("approvedScriptHash", hash)
        manifest.put("approvedPermissions", TextUtils.join(",", requiredPermissions))
        write(manifestFile(normalized), manifest.toString(2) + "\n")
    }

    fun delete(id: kotlin.String?) {
        val dir = widgetDir(id)
        if (dir.exists()) {
            Tuils.delete(dir)
        }
    }

    @Throws(Exception::class)
    fun exportPackage(id: kotlin.String?): kotlin.String {
        val normalized = normalizeId(id)
        require(exists(normalized)) { "Unknown widget: " + normalized }
        val `object` = JSONObject()
        `object`.put("type", "retui-widget-package")
        `object`.put("schema", 1)
        `object`.put("id", normalized)
        `object`.put("name", getName(normalized))
        `object`.put("engine", ENGINE)
        `object`.put("script", readScript(normalized))
        return `object`.toString(2)
    }

    @Throws(Exception::class)
    fun rename(oldId: kotlin.String?, newId: kotlin.String?) {
        val oldNormalized = normalizeId(oldId)
        val newNormalized = normalizeId(newId)
        require(!(TextUtils.isEmpty(oldNormalized) || TextUtils.isEmpty(newNormalized))) { "Widget id is required" }
        if (TextUtils.equals(oldNormalized, newNormalized)) {
            return
        }

        val oldDir = widgetDir(oldNormalized)
        require(File(oldDir, SCRIPT_FILE).isFile()) { "Unknown widget: " + oldNormalized }

        val newDir = widgetDir(newNormalized)
        require(!newDir.exists()) { "Widget id already exists: " + newNormalized }

        check(oldDir.renameTo(newDir)) { "Unable to rename widget folder" }

        val name = getName(newNormalized)
        val script = readScript(newNormalized)
        save(newNormalized, name, script)
    }

    fun newWidgetTemplate(id: kotlin.String?): kotlin.String {
        val normalized = normalizeId(id)
        val title = displayName(normalized)
        return ("-- name = \"" + title + "\"\n"
                + "-- type = \"widget\"\n"
                + "-- retui = \"1\"\n"
                + "\n"
                + "local count = 0\n"
                + "\n"
                + "local function render()\n"
                + "    ui:set_title(\"" + title + "\")\n"
                + "    ui:show_text(\"Hello from \" .. \"" + normalized + "\" .. \"\\nCount: \" .. count)\n"
                + "    ui:show_buttons({\"Increase\", \"Reset\"})\n"
                + "end\n"
                + "\n"
                + "function on_load()\n"
                + "    render()\n"
                + "end\n"
                + "\n"
                + "function on_click(index)\n"
                + "    if index == 1 then count = count + 1 end\n"
                + "    if index == 2 then count = 0 end\n"
                + "    render()\n"
                + "end\n")
    }

    @Throws(Exception::class)
    fun installSamples(): MutableList<kotlin.String?> {
        val ids = ArrayList<kotlin.String?>()
        for (entry in samples().entries) {
            val id = entry.key
            val sample: Sample = entry.value!!
            save(id, sample.name, sample.script)
            ids.add(id)
        }
        return ids
    }

    fun modulePayload(id: kotlin.String?, result: LuaWidgetEngine.RenderResult): kotlin.String {
        val out = StringBuilder()
        var title = result.title
        if (TextUtils.isEmpty(title)) {
            title = getName(id)
        }
        appendDirective(out, "title", title)

        if (!TextUtils.isEmpty(result.error)) {
            appendDirective(out, "body", "Lua error: " + result.error)
            if (!TextUtils.isEmpty(result.errorStage)) {
                appendDirective(out, "body", "Stage: " + result.errorStage)
            }
        } else if (!TextUtils.isEmpty(result.body)) {
            for (line in result.body!!.split("\\r?\\n".toRegex()).toTypedArray()) {
                appendDirective(out, "body", line)
            }
        } else {
            appendDirective(out, "body", "No widget output yet.")
        }

        var index = 1
        var hasRefreshButton = false
        for (button in result.buttons) {
            appendSuggest(out, button, "widget -click " + normalizeId(id) + " " + index)
            hasRefreshButton = hasRefreshButton || "refresh" == normalizeActionLabel(button)
            index += 1
        }
        for (action in result.valueActions) {
            appendSuggest(
                out,
                action!!.label,
                "widget -action " + normalizeId(id) + " " + quoteArg(action.value)
            )
        }
        if (result.dialogOpen) {
            var dialogIndex = 1
            for (item in result.dialogItems) {
                val label = if (dialogIndex == result.dialogSelected) "* " + item else item
                appendSuggest(out, label, "widget -dialog " + normalizeId(id) + " " + dialogIndex)
                dialogIndex += 1
            }
            appendSuggest(out, "cancel", "widget -dialog " + normalizeId(id) + " -1")
        }
        for (action in result.commands) {
            appendSuggest(out, action!!.label, action.command)
        }
        if (result.expandable) {
            appendSuggest(
                out, if (result.expanded) "collapse" else "expand",
                "widget " + (if (result.expanded) "-collapse " else "-expand ") + normalizeId(id)
            )
        }
        if (!hasRefreshButton) {
            appendSuggest(out, "refresh", "module -refresh " + normalizeId(id))
        }
        if (!TextUtils.isEmpty(result.error)) {
            appendSuggest(out, "copy error", "widget -copy-error " + normalizeId(id))
            appendSuggest(out, "check", "widget -check " + normalizeId(id))
            appendSuggest(out, "disable", "widget -disable " + normalizeId(id))
        }
        return out.toString().trim { it <= ' ' }
    }

    fun disabledPayload(id: kotlin.String?): kotlin.String {
        val normalized = normalizeId(id)
        val out = StringBuilder()
        appendDirective(out, "title", getName(normalized))
        appendDirective(out, "body", "Lua widget disabled.")
        appendSuggest(out, "enable", "widget -enable " + normalized)
        appendSuggest(out, "check", "widget -check " + normalized)
        return out.toString().trim { it <= ' ' }
    }

    fun consentPayload(id: kotlin.String?): kotlin.String {
        val normalized = normalizeId(id)
        val status = trustStatus(normalized)
        val out = StringBuilder()
        appendDirective(out, "title", getName(normalized))
        appendDirective(out, "body", "Lua script approval required.")
        appendDirective(
            out,
            "body",
            "Permissions: " + (if (status.requiredPermissions.isEmpty()) "none" else TextUtils.join(
                ", ",
                status.requiredPermissions
            ))
        )
        if (!status.missingDeclarations.isEmpty()) {
            appendDirective(
                out,
                "body",
                "Missing metadata: " + TextUtils.join(", ", status.missingDeclarations)
            )
        }
        if (!status.unsupportedPermissions.isEmpty()) {
            appendDirective(
                out,
                "body",
                "Unsupported permissions: " + TextUtils.join(", ", status.unsupportedPermissions)
            )
        }
        if (status.scriptChanged) {
            appendDirective(out, "body", "Script changed since last approval.")
        }
        if (status.canApprove()) {
            appendSuggest(out, "approve", "widget -approve " + normalized)
        }
        appendSuggest(out, "check", "widget -check " + normalized)
        return out.toString().trim { it <= ' ' }
    }

    fun isTrusted(id: kotlin.String?): Boolean {
        return trustStatus(id).trusted
    }

    fun isPermissionApproved(id: kotlin.String?, permission: kotlin.String?): Boolean {
        return approvedPermissions(id).contains(normalizePermission(permission))
    }

    fun approvedPermissions(id: kotlin.String?): MutableList<kotlin.String?> {
        val normalized = normalizeId(id)
        val manifest = readManifest(normalized)
        if (manifest == null) {
            return ArrayList<kotlin.String?>()
        }
        val script = readScript(normalized)
        val approvedHash = manifest.optString("approvedScriptHash", "")
        if (!TextUtils.equals(approvedHash, scriptHash(script))) {
            return ArrayList<kotlin.String?>()
        }
        return permissionList(manifest.optString("approvedPermissions", ""))
    }

    fun trustStatus(id: kotlin.String?): TrustStatus {
        val normalized = normalizeId(id)
        val script = readScript(normalized)
        val status = TrustStatus()
        status.scriptHash = scriptHash(script)
        status.declaredPermissions = declaredPermissions(script)
        status.requiredPermissions = requiredPermissions(script)
        status.inferredCapabilities = inferCapabilities(script)
        status.missingDeclarations =
            subtract(status.requiredPermissions, status.declaredPermissions)
        status.unsupportedPermissions = unsupportedPermissions(status.declaredPermissions)

        val manifest = readManifest(normalized)
        val approvedHash =
            if (manifest == null) "" else manifest.optString("approvedScriptHash", "")
        val approvedPermissions = permissionList(
            if (manifest == null) "" else manifest.optString(
                "approvedPermissions",
                ""
            )
        )
        status.scriptChanged =
            !TextUtils.isEmpty(approvedHash) && !TextUtils.equals(approvedHash, status.scriptHash)
        status.trusted = status.missingDeclarations.isEmpty()
                && status.unsupportedPermissions.isEmpty()
                && (status.requiredPermissions.isEmpty()
                || (TextUtils.equals(approvedHash, status.scriptHash)
                && approvedPermissions.containsAll(status.requiredPermissions)))
        return status
    }

    @Throws(Exception::class)
    fun approve(id: kotlin.String?) {
        val normalized = normalizeId(id)
        require(exists(normalized)) { "Unknown widget: " + normalized }
        val status = trustStatus(normalized)
        require(status.unsupportedPermissions.isEmpty()) {
            "Unsupported permissions: " + TextUtils.join(
                ", ",
                status.unsupportedPermissions
            )
        }
        require(status.missingDeclarations.isEmpty()) {
            "Declare permissions first: " + TextUtils.join(
                ", ",
                status.missingDeclarations
            )
        }
        var manifest = readManifest(normalized)
        if (manifest == null) {
            manifest = JSONObject()
        }
        manifest.put("approvedScriptHash", status.scriptHash)
        manifest.put("approvedPermissions", TextUtils.join(",", status.requiredPermissions))
        manifest.put("scriptHash", status.scriptHash)
        manifest.put("permissions", TextUtils.join(",", status.requiredPermissions))
        write(manifestFile(normalized), manifest.toString(2) + "\n")
    }

    fun saveLastError(id: kotlin.String?, stage: kotlin.String?, error: kotlin.String?) {
        val normalized = normalizeId(id)
        if (TextUtils.isEmpty(normalized)) {
            return
        }
        try {
            var manifest = readManifest(normalized)
            if (manifest == null) {
                manifest = JSONObject()
                manifest.put("type", "retui-widget")
                manifest.put("schema", 1)
                manifest.put("id", normalized)
                manifest.put("name", getName(normalized))
                manifest.put("engine", ENGINE)
            }
            manifest.put("lastError", if (error == null) "" else error)
            manifest.put("lastErrorStage", if (stage == null) "" else stage)
            manifest.put("lastErrorAt", System.currentTimeMillis())
            write(manifestFile(normalized), manifest.toString(2) + "\n")
        } catch (ignored: Exception) {
        }
    }

    fun lastError(id: kotlin.String?): kotlin.String {
        val manifest = readManifest(normalizeId(id))
        if (manifest == null) {
            return ""
        }
        val error = manifest.optString("lastError", "")
        val stage = manifest.optString("lastErrorStage", "")
        if (TextUtils.isEmpty(error)) {
            return ""
        }
        return if (TextUtils.isEmpty(stage)) error else "Stage: " + stage + "\n" + error
    }

    fun normalizeId(value: kotlin.String?): kotlin.String {
        if (value == null) {
            return ""
        }
        return value.trim { it <= ' ' }
            .lowercase()
            .replace("[^a-z0-9_-]".toRegex(), "")
    }

    fun idFromName(value: kotlin.String?): kotlin.String {
        if (value == null) {
            return ""
        }
        return value.trim { it <= ' ' }
            .lowercase()
            .replace("[^a-z0-9]+".toRegex(), "_")
            .replace("_+".toRegex(), "_")
            .replace("^_|_$".toRegex(), "")
    }

    fun metadata(script: kotlin.String?): MutableMap<kotlin.String?, kotlin.String?> {
        val meta = LinkedHashMap<kotlin.String?, kotlin.String?>()
        if (script == null) {
            return meta
        }
        val pattern =
            Pattern.compile("^\\s*--\\s*([a-zA-Z0-9_]+)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\r\\n]+))\\s*$")
        val lines = script.split("\\r?\\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (line in lines) {
            val trimmed = line.trim { it <= ' ' }
            if (trimmed.length == 0) {
                continue
            }
            val matcher = pattern.matcher(line)
            if (!matcher.matches()) {
                if (!trimmed.startsWith("--")) {
                    break
                }
                continue
            }
            var value = matcher.group(2)
            if (value == null) value = matcher.group(3)
            if (value == null) value = matcher.group(4)
            meta.put(
                matcher.group(1).lowercase(),
                if (value == null) "" else value.trim { it <= ' ' })
        }
        return meta
    }

    fun describeCapabilities(script: kotlin.String?): kotlin.String? {
        val capabilities = inferCapabilities(script)
        return if (capabilities.isEmpty()) "none" else TextUtils.join(", ", capabilities)
    }

    fun describeRequiredPermissions(script: kotlin.String?): kotlin.String? {
        val permissions = requiredPermissions(script)
        return if (permissions.isEmpty()) "none" else TextUtils.join(", ", permissions)
    }

    fun inferCapabilities(script: kotlin.String?): MutableList<kotlin.String?> {
        val capabilities = ArrayList<kotlin.String?>()
        val code = if (script == null) "" else script.lowercase()
        addCapability(capabilities, code.contains("http:"), "network")
        addCapability(
            capabilities, code.contains("system:to_clipboard")
                    || code.contains("system:copy_to_clipboard")
                    || code.contains("system:clipboard"), "clipboard"
        )
        addCapability(capabilities, code.contains("system:vibrate"), "vibrate")
        addCapability(
            capabilities, code.contains("ui:set_tick")
                    || code.contains("ui:set_tick_interval")
                    || code.contains("function on_tick"), "active-tick"
        )
        addCapability(
            capabilities, code.contains("files:read")
                    || code.contains("files:write")
                    || code.contains("files:append")
                    || code.contains("files:delete")
                    || code.contains("files:list")
                    || code.contains("files:exists"), "local-files"
        )
        addCapability(
            capabilities, code.contains("require \"clock\"")
                    || code.contains("require 'clock'")
                    || code.contains("clock:"), "clock"
        )
        addCapability(capabilities, code.contains("suggest:"), "suggestions")
        return capabilities
    }

    fun requiredPermissions(script: kotlin.String?): MutableList<kotlin.String?> {
        val required = ArrayList<kotlin.String?>()
        for (capability in inferCapabilities(script)) {
            if (SENSITIVE_CAPABILITIES.contains(capability)) {
                addCapability(required, true, capability)
            }
        }
        for (declared in declaredPermissions(script)) {
            if (SENSITIVE_CAPABILITIES.contains(declared)) {
                addCapability(required, true, declared)
            }
        }
        return required
    }

    fun declaredPermissions(script: kotlin.String?): MutableList<kotlin.String?> {
        return permissionList(metadata(script).get("permissions"))
    }

    fun missingPermissionDeclarations(script: kotlin.String?): MutableList<kotlin.String?> {
        return subtract(requiredPermissions(script), declaredPermissions(script))
    }

    fun unsupportedPermissions(script: kotlin.String?): MutableList<kotlin.String?> {
        return unsupportedPermissions(declaredPermissions(script))
    }

    private fun unsupportedPermissions(declaredPermissions: MutableList<kotlin.String?>): MutableList<kotlin.String?> {
        val unsupported = ArrayList<kotlin.String?>()
        for (permission in declaredPermissions) {
            if (!SENSITIVE_CAPABILITIES.contains(permission) && "none" != permission) {
                unsupported.add(permission)
            }
        }
        return unsupported
    }

    private fun permissionList(raw: kotlin.String?): MutableList<kotlin.String?> {
        val values = LinkedHashSet<kotlin.String?>()
        if (raw == null) {
            return ArrayList<kotlin.String?>()
        }
        for (part in raw.split("[,\\s]+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            val permission = normalizePermission(part)
            if (permission.length == 0 || "none" == permission) {
                continue
            }
            values.add(permission)
        }
        return ArrayList<kotlin.String?>(values)
    }

    private fun normalizePermission(permission: kotlin.String?): kotlin.String {
        return if (permission == null) "" else permission.trim { it <= ' ' }.lowercase()
    }

    private fun subtract(
        source: MutableList<kotlin.String?>,
        remove: MutableList<kotlin.String?>
    ): MutableList<kotlin.String?> {
        val out = ArrayList<kotlin.String?>()
        for (item in source) {
            if (!remove.contains(item)) {
                out.add(item)
            }
        }
        return out
    }

    private fun readManifest(id: kotlin.String?): JSONObject? {
        val manifest = manifestFile(id)
        if (!manifest.isFile()) {
            return null
        }
        try {
            FileInputStream(manifest).use { `in` ->
                return JSONObject(Tuils.convertStreamToString(`in`))
            }
        } catch (ignored: Exception) {
            return null
        }
    }

    private fun scriptHash(script: kotlin.String?): kotlin.String {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(
                (if (script == null) "" else script).toByteArray(
                    StandardCharsets.UTF_8
                )
            )
            val out = StringBuilder()
            for (b in hash) {
                out.append(kotlin.String.format(Locale.US, "%02x", b))
            }
            return out.toString()
        } catch (e: Exception) {
            return (if (script == null) "" else script).hashCode().toString()
        }
    }

    private fun addCapability(
        capabilities: MutableList<kotlin.String?>,
        enabled: Boolean,
        capability: kotlin.String?
    ) {
        if (enabled && !capabilities.contains(capability)) {
            capabilities.add(capability)
        }
    }

    private fun displayName(id: kotlin.String): kotlin.String {
        if (TextUtils.isEmpty(id)) {
            return "Lua Widget"
        }
        val out = StringBuilder()
        for (part in id.split("[_-]+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            if (part.length == 0) continue
            if (out.length > 0) out.append(' ')
            out.append(part.get(0).uppercaseChar())
            if (part.length > 1) out.append(part.substring(1))
        }
        return if (out.length == 0) id else out.toString()
    }

    private fun normalizeActionLabel(label: kotlin.String?): kotlin.String {
        return if (label == null) "" else label.trim { it <= ' ' }.lowercase()
    }

    private fun quoteArg(value: kotlin.String?): kotlin.String {
        var safe = if (value == null) "" else value
        safe = safe.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", " ")
            .replace("\n", " ")
        return "\"" + safe + "\""
    }

    private fun appendDirective(out: StringBuilder, key: kotlin.String?, value: kotlin.String?) {
        if (out.length > 0) out.append('\n')
        out.append("::").append(key).append(' ').append(if (value == null) "" else value)
    }

    private fun appendSuggest(out: StringBuilder, label: kotlin.String?, command: kotlin.String?) {
        if (TextUtils.isEmpty(label) || TextUtils.isEmpty(command)) {
            return
        }
        if (out.length > 0) out.append('\n')
        out.append("::suggest ")
            .append(label!!.replace("|", "/"))
            .append(" | command | ")
            .append(command)
    }

    @Throws(Exception::class)
    private fun write(file: File, text: kotlin.String?) {
        val parent = file.getParentFile()
        check(!(parent != null && !parent.exists() && !parent.mkdirs())) { "Unable to create widget folder" }
        FileOutputStream(file, false).use { out ->
            out.write(
                (if (text == null) "" else text).toByteArray(
                    StandardCharsets.UTF_8
                )
            )
        }
    }

    private fun systemTimerScript(): kotlin.String {
        return (""
                + "-- name = \"Timer\"\n"
                + "-- type = \"widget\"\n"
                + "-- retui = \"1\"\n"
                + "-- description = \"System timer module backed by Re:T-UI clock state\"\n"
                + "\n"
                + "local clock = require \"clock\"\n"
                + "local fmt = require \"fmt\"\n"
                + "\n"
                + "local BAR_WIDTH = 14\n"
                + "\n"
                + "local function ms(value)\n"
                + "    local n = tonumber(value or 0) or 0\n"
                + "    return math.max(0, n)\n"
                + "end\n"
                + "\n"
                + "local function duration(value)\n"
                + "    return clock:format_duration(ms(value))\n"
                + "end\n"
                + "\n"
                + "local function pct(elapsed, total)\n"
                + "    if total <= 0 then return 0 end\n"
                + "    return math.floor((elapsed * 100 / total) + 0.5)\n"
                + "end\n"
                + "\n"
                + "local function countdown_line(label, state)\n"
                + "    local total = ms(state.total_ms)\n"
                + "    local remaining = ms(state.remaining_ms)\n"
                + "    if state.running and total > 0 then\n"
                + "        local elapsed = math.max(0, total - remaining)\n"
                + "        ui:add_line(label .. \": \" .. fmt.progress_bar(elapsed, total, BAR_WIDTH) .. \" \" .. pct(elapsed, total) .. \"% \" .. duration(remaining))\n"
                + "    else\n"
                + "        ui:add_line(label .. \": idle\")\n"
                + "    end\n"
                + "end\n"
                + "\n"
                + "local function stopwatch_line(state)\n"
                + "    local elapsed = ms(state.elapsed_ms)\n"
                + "    if state.running or elapsed > 0 then\n"
                + "        ui:add_line(\"Stopwatch: \" .. duration(elapsed))\n"
                + "    else\n"
                + "        ui:add_line(\"Stopwatch: idle\")\n"
                + "    end\n"
                + "end\n"
                + "\n"
                + "local function pomodoro_line(state)\n"
                + "    if state.running and state.type == \"finished\" then\n"
                + "        ui:add_line(\"Pomodoro: finished\")\n"
                + "    elseif state.running then\n"
                + "        local label = \"Pomodoro \" .. (state.type or \"focus\")\n"
                + "        countdown_line(label, state)\n"
                + "        if state.task and state.task ~= \"\" then\n"
                + "            ui:add_line(\"Task: \" .. state.task)\n"
                + "        end\n"
                + "    else\n"
                + "        ui:add_line(\"Pomodoro: idle\")\n"
                + "    end\n"
                + "end\n"
                + "\n"
                + "local function render()\n"
                + "    ui:set_title(\"Timer\")\n"
                + "    countdown_line(\"Timer\", clock:timer())\n"
                + "    stopwatch_line(clock:stopwatch())\n"
                + "    pomodoro_line(clock:pomodoro())\n"
                + "    ui:show_command(\"25m\", \"timer 25m\")\n"
                + "    ui:show_command(\"+5m\", \"timer -add 5m\")\n"
                + "    ui:show_command(\"stop\", \"timer -stop\")\n"
                + "    ui:show_command(\"status\", \"timer -status\")\n"
                + "    ui:show_command(\"watch\", \"stopwatch\")\n"
                + "    ui:show_command(\"reset watch\", \"stopwatch -reset\")\n"
                + "    ui:show_command(\"pomodoro\", \"pomodoro\")\n"
                + "    ui:show_command(\"stop pomo\", \"pomodoro -stop\")\n"
                + "end\n"
                + "\n"
                + "function on_load() render() end\n"
                + "function on_resume() render() end\n")
    }

    private fun samples(): MutableMap<kotlin.String?, Sample?> {
        val samples: LinkedHashMap<kotlin.String?, Sample?> =
            LinkedHashMap<kotlin.String?, Sample?>()
        samples.put(
            "retui_counter", Sample(
                "Re:TUI Counter", (""
                        + "-- name = \"Re:TUI Counter\"\n"
                        + "-- type = \"widget\"\n"
                        + "-- retui = \"1\"\n"
                        + "-- description = \"Persistent counter using Re:TUI prefs and dock actions\"\n"
                        + "\n"
                        + "local prefs = require \"prefs\"\n"
                        + "\n"
                        + "function on_load()\n"
                        + "    if prefs.count == nil then prefs.count = 0 end\n"
                        + "    if prefs.step == nil then prefs.step = 1 end\n"
                        + "end\n"
                        + "\n"
                        + "local function render()\n"
                        + "    ui:set_title(\"Re:TUI Counter\")\n"
                        + "    ui:show_text(\"Count: \" .. prefs.count .. \"\\nStep: \" .. prefs.step .. \"\\nWidget: \" .. aio:self_name())\n"
                        + "    ui:show_buttons({\"+\" .. prefs.step, \"-\" .. prefs.step, \"Reset\", \"Prefs\"})\n"
                        + "end\n"
                        + "\n"
                        + "function on_resume() render() end\n"
                        + "\n"
                        + "function on_click(index)\n"
                        + "    if index == 1 then prefs.count = prefs.count + prefs.step end\n"
                        + "    if index == 2 then prefs.count = prefs.count - prefs.step end\n"
                        + "    if index == 3 then prefs.count = 0 end\n"
                        + "    if index == 4 then prefs:show_dialog() else render() end\n"
                        + "end\n")
            )
        )
        samples.put(
            "retui_progress", Sample(
                "Re:TUI Progress", (""
                        + "-- name = \"Re:TUI Progress\"\n"
                        + "-- type = \"widget\"\n"
                        + "-- retui = \"1\"\n"
                        + "-- description = \"Progress widget using Re:TUI prefs, system state, and progress APIs\"\n"
                        + "\n"
                        + "local prefs = require \"prefs\"\n"
                        + "\n"
                        + "function on_load()\n"
                        + "    if prefs.progress == nil then prefs.progress = 50 end\n"
                        + "    if prefs.step == nil then prefs.step = 10 end\n"
                        + "end\n"
                        + "\n"
                        + "local function render()\n"
                        + "    local battery = system:battery_info()\n"
                        + "    ui:set_title(\"Re:TUI Progress\")\n"
                        + "    ui:show_text(\"Progress: \" .. prefs.progress .. \"%\\nBattery: \" .. battery.percent .. \"%\")\n"
                        + "    ui:show_progress_bar(\"Widget progress\", prefs.progress, 100, 14)\n"
                        + "    ui:set_progress(prefs.progress)\n"
                        + "    ui:show_buttons({\"+\" .. prefs.step, \"-\" .. prefs.step, \"Reset\", \"Prefs\"})\n"
                        + "end\n"
                        + "\n"
                        + "function on_resume() render() end\n"
                        + "\n"
                        + "function on_click(index)\n"
                        + "    if index == 1 then prefs.progress = math.min(100, prefs.progress + prefs.step) end\n"
                        + "    if index == 2 then prefs.progress = math.max(0, prefs.progress - prefs.step) end\n"
                        + "    if index == 3 then prefs.progress = 50 end\n"
                        + "    if index == 4 then prefs:show_dialog() else render() end\n"
                        + "end\n")
            )
        )
        samples.put(
            "retui_clock", Sample(
                "Re:TUI Clock", (""
                        + "-- name = \"Re:TUI Clock\"\n"
                        + "-- type = \"widget\"\n"
                        + "-- retui = \"1\"\n"
                        + "-- permissions = \"clipboard,vibrate\"\n"
                        + "-- description = \"Clock widget using Re:TUI system helpers\"\n"
                        + "\n"
                        + "local prefs = require \"prefs\"\n"
                        + "\n"
                        + "function on_load()\n"
                        + "    if prefs.refreshes == nil then prefs.refreshes = 0 end\n"
                        + "end\n"
                        + "\n"
                        + "local function render()\n"
                        + "    prefs.refreshes = prefs.refreshes + 1\n"
                        + "    local battery = system:battery_info()\n"
                        + "    local network = system:network_state()\n"
                        + "    ui:set_title(\"Re:TUI Clock\")\n"
                        + "    ui:show_kv({\n"
                        + "        time = os.date(\"%Y-%m-%d %H:%M:%S\"),\n"
                        + "        timezone = system:tz(),\n"
                        + "        battery = battery.percent .. \"%\" .. (battery.charging and \" charging\" or \"\"),\n"
                        + "        network = network.type .. (network.connected and \" connected\" or \" offline\"),\n"
                        + "        refreshes = prefs.refreshes,\n"
                        + "    })\n"
                        + "    ui:show_buttons({\"Refresh\", \"Copy\", \"Vibrate\"})\n"
                        + "end\n"
                        + "\n"
                        + "function on_resume() render() end\n"
                        + "\n"
                        + "function on_click(index)\n"
                        + "    if index == 2 then system:to_clipboard(os.date(\"%Y-%m-%d %H:%M:%S\")) end\n"
                        + "    if index == 3 then system:vibrate(60) end\n"
                        + "    render()\n"
                        + "end\n")
            )
        )
        samples.put(
            "retui_expandable", Sample(
                "Re:TUI Expandable", (""
                        + "-- name = \"Re:TUI Expandable\"\n"
                        + "-- type = \"widget\"\n"
                        + "-- retui = \"1\"\n"
                        + "-- description = \"Compact and expanded rendering using Re:TUI widget state\"\n"
                        + "\n"
                        + "local prefs = require \"prefs\"\n"
                        + "\n"
                        + "function on_load()\n"
                        + "    if prefs.opens == nil then prefs.opens = 0 end\n"
                        + "end\n"
                        + "\n"
                        + "local function render()\n"
                        + "    ui:set_title(\"Expandable\")\n"
                        + "    ui:set_expandable(true)\n"
                        + "    prefs.opens = prefs.opens + 1\n"
                        + "    if ui:is_expanded() then\n"
                        + "        ui:show_kv({\n"
                        + "            mode = \"expanded\",\n"
                        + "            opens = prefs.opens,\n"
                        + "            time = os.date(\"%H:%M:%S\"),\n"
                        + "        })\n"
                        + "        ui:show_command(\"Settings\", \"config -ls\")\n"
                        + "        ui:show_buttons({\"Refresh\", \"Toast\"})\n"
                        + "    else\n"
                        + "        ui:show_text(\"Standard mode\")\n"
                        + "        ui:show_buttons({\"Refresh\"})\n"
                        + "    end\n"
                        + "end\n"
                        + "\n"
                        + "function on_resume() render() end\n"
                        + "\n"
                        + "function on_click(index)\n"
                        + "    if index == 2 then ui:show_toast(\"Hello from Lua\") end\n"
                        + "    render()\n"
                        + "end\n")
            )
        )
        samples.put(
            "retui_ticker", Sample(
                "Re:TUI Ticker", (""
                        + "-- name = \"Re:TUI Ticker\"\n"
                        + "-- type = \"widget\"\n"
                        + "-- retui = \"1\"\n"
                        + "-- permissions = \"active-tick\"\n"
                        + "-- description = \"Active-widget ticking lifecycle demo\"\n"
                        + "\n"
                        + "local prefs = require \"prefs\"\n"
                        + "\n"
                        + "function on_load()\n"
                        + "    if prefs.ticks == nil then prefs.ticks = 0 end\n"
                        + "end\n"
                        + "\n"
                        + "local function render(label, running)\n"
                        + "    ui:set_title(\"Ticker\")\n"
                        + "    if running then ui:set_tick_interval(1) else ui:disable_tick() end\n"
                        + "    ui:show_kv({\n"
                        + "        state = label,\n"
                        + "        ticks = prefs.ticks,\n"
                        + "        time = os.date(\"%H:%M:%S\"),\n"
                        + "    })\n"
                        + "    ui:show_buttons({\"Reset\", \"Stop\"})\n"
                        + "end\n"
                        + "\n"
                        + "function on_resume() render(\"active\", true) end\n"
                        + "\n"
                        + "function on_tick(n)\n"
                        + "    prefs.ticks = n\n"
                        + "    render(\"tick\", true)\n"
                        + "end\n"
                        + "\n"
                        + "function on_click(index)\n"
                        + "    if index == 1 then prefs.ticks = 0 end\n"
                        + "    render(index == 2 and \"stopped\" or \"reset\", index ~= 2)\n"
                        + "end\n")
            )
        )
        samples.put(
            "retui_toolkit", Sample(
                "Re:TUI Toolkit", (""
                        + "-- name = \"Re:TUI Toolkit\"\n"
                        + "-- type = \"widget\"\n"
                        + "-- retui = \"1\"\n"
                        + "-- permissions = \"clipboard\"\n"
                        + "-- description = \"Small standard library demo\"\n"
                        + "\n"
                        + "local fmt = require \"fmt\"\n"
                        + "local date = require \"date\"\n"
                        + "local strings = require \"strings\"\n"
                        + "local colors = require \"colors\"\n"
                        + "local debug = require \"debug\"\n"
                        + "\n"
                        + "function on_resume()\n"
                        + "    local title = fmt.title(\"retui toolkit\")\n"
                        + "    debug:log(\"render \" .. date.format(\"%H:%M:%S\"))\n"
                        + "    ui:set_title(title)\n"
                        + "    ui:show_kv({\n"
                        + "        time = date.format(\"%Y-%m-%d %H:%M:%S\"),\n"
                        + "        storage = fmt.bytes(123456789),\n"
                        + "        progress = fmt.progress_bar(42, 100, 10) .. \" \" .. fmt.percent(42, 100),\n"
                        + "        has_tui = tostring(strings.contains(\"Re:TUI\", \"TUI\")),\n"
                        + "        accent = colors.accent,\n"
                        + "    })\n"
                        + "    ui:show_buttons({\"Debug\", \"Copy\"})\n"
                        + "end\n"
                        + "\n"
                        + "function on_click(index)\n"
                        + "    if index == 1 then debug:show() end\n"
                        + "    if index == 2 then system:to_clipboard(date.format(\"%H:%M:%S\")) end\n"
                        + "end\n")
            )
        )
        samples.put(
            "retui_suggest", Sample(
                "Re:TUI Suggest", (""
                        + "-- name = \"Re:TUI Suggest\"\n"
                        + "-- type = \"suggest\"\n"
                        + "-- retui = \"1\"\n"
                        + "-- description = \"Lua-powered command suggestion demo\"\n"
                        + "\n"
                        + "local strings = require \"strings\"\n"
                        + "local fmt = require \"fmt\"\n"
                        + "\n"
                        + "function on_suggest(query)\n"
                        + "    local q = strings.trim(fmt.lower(query))\n"
                        + "    if strings.starts_with(q, \"cfg\") or strings.starts_with(q, \"conf\") then\n"
                        + "        suggest:command(\"Config list\", \"config -ls\")\n"
                        + "        suggest:command(\"Edit behavior\", \"config -file behavior.xml\")\n"
                        + "    end\n"
                        + "    if strings.starts_with(q, \"mod\") then\n"
                        + "        suggest:command(\"Modules\", \"module -ls\")\n"
                        + "        suggest:module(\"notifications\", \"Open notifications\")\n"
                        + "    end\n"
                        + "end\n")
            )
        )
        samples.put(
            "retui_prefs", Sample(
                "Re:TUI Prefs", (""
                        + "-- name = \"Re:TUI Prefs\"\n"
                        + "-- type = \"widget\"\n"
                        + "-- retui = \"1\"\n"
                        + "\n"
                        + "local prefs = require \"prefs\"\n"
                        + "\n"
                        + "function on_load()\n"
                        + "    if prefs.count == nil then prefs.count = 0 end\n"
                        + "end\n"
                        + "\n"
                        + "local function render()\n"
                        + "    ui:set_title(\"Prefs Demo\")\n"
                        + "    ui:show_text(\"Persistent count: \" .. prefs.count)\n"
                        + "    ui:show_buttons({\"+1\", \"Reset\", \"Prefs\"})\n"
                        + "end\n"
                        + "\n"
                        + "function on_resume() render() end\n"
                        + "\n"
                        + "function on_click(index)\n"
                        + "    if index == 1 then prefs.count = prefs.count + 1 end\n"
                        + "    if index == 2 then prefs.count = 0 end\n"
                        + "    if index == 3 then prefs:show_dialog() else render() end\n"
                        + "end\n")
            )
        )
        samples.put(
            "retui_files", Sample(
                "Re:TUI Files", (""
                        + "-- name = \"Re:TUI Files\"\n"
                        + "-- type = \"widget\"\n"
                        + "-- retui = \"1\"\n"
                        + "-- permissions = \"local-files\"\n"
                        + "\n"
                        + "local function number()\n"
                        + "    return tonumber(files:read(\"count.txt\") or \"0\") or 0\n"
                        + "end\n"
                        + "\n"
                        + "local function render()\n"
                        + "    local n = number()\n"
                        + "    ui:set_title(\"Files Demo\")\n"
                        + "    ui:show_text(\"Stored in widget local files/count.txt\\nValue: \" .. n)\n"
                        + "    ui:show_buttons({\"Write +1\", \"Delete\"})\n"
                        + "end\n"
                        + "\n"
                        + "function on_resume() render() end\n"
                        + "\n"
                        + "function on_click(index)\n"
                        + "    if index == 1 then files:write(\"count.txt\", tostring(number() + 1)); files:append(\"log.txt\", os.date(\"%H:%M:%S\") .. \" +1\\n\") end\n"
                        + "    if index == 2 then files:delete(\"count.txt\") end\n"
                        + "    render()\n"
                        + "end\n")
            )
        )
        samples.put(
            "retui_platform", Sample(
                "Re:TUI Platform", (""
                        + "-- name = \"Re:TUI Platform\"\n"
                        + "-- type = \"widget\"\n"
                        + "-- retui = \"1\"\n"
                        + "-- permissions = \"local-files,clipboard\"\n"
                        + "-- description = \"Platform helper API demo\"\n"
                        + "\n"
                        + "local prefs = require \"prefs\"\n"
                        + "local fmt = require \"fmt\"\n"
                        + "local strings = require \"strings\"\n"
                        + "\n"
                        + "function on_load()\n"
                        + "    prefs:set(\"opens\", prefs:number(\"opens\", 0))\n"
                        + "end\n"
                        + "\n"
                        + "local function render()\n"
                        + "    local opens = prefs:inc(\"opens\", 1)\n"
                        + "    local names = files:list()\n"
                        + "    ui:set_title(\"Platform\")\n"
                        + "    ui:show_kv({\n"
                        + "        widget = system:widget_name(),\n"
                        + "        id = system:widget_id(),\n"
                        + "        app = system:app_version(),\n"
                        + "        opens = fmt.fixed(opens, 0),\n"
                        + "        files = #names,\n"
                        + "        joined = strings.join(names, \", \"),\n"
                        + "    })\n"
                        + "    ui:show_command(\"Modules\", \"module -ls\")\n"
                        + "    ui:show_buttons({\"Write Log\", \"Copy ID\", \"Clear\"})\n"
                        + "end\n"
                        + "\n"
                        + "function on_resume() render() end\n"
                        + "\n"
                        + "function on_click(index)\n"
                        + "    if index == 1 then files:append(\"platform.log\", os.date(\"%H:%M:%S\") .. \" opened\\n\") end\n"
                        + "    if index == 2 then system:to_clipboard(system:widget_id()) end\n"
                        + "    if index == 3 then files:delete(\"platform.log\"); prefs:unset(\"opens\") end\n"
                        + "    render()\n"
                        + "end\n")
            )
        )
        samples.put(
            "retui_public_ip", Sample(
                "Re:TUI Public IP", (""
                        + "-- name = \"Re:TUI Public IP\"\n"
                        + "-- type = \"widget\"\n"
                        + "-- retui = \"1\"\n"
                        + "-- permissions = \"network\"\n"
                        + "-- data_source = \"https://api.ipify.org\"\n"
                        + "\n"
                        + "local json = require \"json\"\n"
                        + "\n"
                        + "function on_alarm()\n"
                        + "    ui:set_title(\"Public IP\")\n"
                        + "    ui:show_text(\"Loading...\")\n"
                        + "    ui:show_buttons({\"Refresh\"})\n"
                        + "    http:get(\"https://api.ipify.org?format=json\", \"ip\")\n"
                        + "end\n"
                        + "\n"
                        + "function on_network_result_ip(body, code)\n"
                        + "    local data = json.decode(body)\n"
                        + "    ui:set_title(\"Public IP\")\n"
                        + "    if code == 200 and data and data.ip then\n"
                        + "        ui:show_text(data.ip)\n"
                        + "    else\n"
                        + "        ui:show_text(\"Request failed: \" .. tostring(code))\n"
                        + "    end\n"
                        + "    ui:show_buttons({\"Refresh\"})\n"
                        + "end\n"
                        + "\n"
                        + "function on_network_error_ip(error)\n"
                        + "    ui:set_title(\"Public IP\")\n"
                        + "    ui:show_text(\"Network error: \" .. tostring(error))\n"
                        + "    ui:show_buttons({\"Refresh\"})\n"
                        + "end\n"
                        + "\n"
                        + "function on_click(index)\n"
                        + "    on_alarm()\n"
                        + "end\n")
            )
        )
        return samples
    }

    class TrustStatus {
        var scriptHash: kotlin.String = ""
        var trusted: Boolean = false
        var scriptChanged: Boolean = false
        var inferredCapabilities: MutableList<kotlin.String?> = ArrayList<kotlin.String?>()
        var declaredPermissions: MutableList<kotlin.String?> = ArrayList<kotlin.String?>()
        var requiredPermissions: MutableList<kotlin.String?> = ArrayList<kotlin.String?>()
        var missingDeclarations: MutableList<kotlin.String?> = ArrayList<kotlin.String?>()
        var unsupportedPermissions: MutableList<kotlin.String?> = ArrayList<kotlin.String?>()

        fun canApprove(): Boolean {
            return missingDeclarations.isEmpty() && unsupportedPermissions.isEmpty()
        }
    }

    private class Sample(val name: kotlin.String?, val script: kotlin.String?)
}
