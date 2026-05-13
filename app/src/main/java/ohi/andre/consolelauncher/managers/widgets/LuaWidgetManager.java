package ohi.andre.consolelauncher.managers.widgets;

import android.content.Context;
import android.text.TextUtils;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ohi.andre.consolelauncher.tuils.Tuils;

public final class LuaWidgetManager {

    public static final String ENGINE = "lua";
    public static final String SOURCE_PREFIX = "lua:";
    public static final String WIDGETS_DIR = "widgets";
    public static final String SCRIPT_FILE = "main.lua";
    public static final String MANIFEST_FILE = "manifest.json";
    private static final String[] BUNDLED_SAMPLE_IDS = new String[] {
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
    };
    private static final String[] BUNDLED_SAMPLE_MARKERS = new String[] {
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
    };
    private static final Set<String> SENSITIVE_CAPABILITIES = new HashSet<>();

    static {
        Collections.addAll(SENSITIVE_CAPABILITIES,
                "network",
                "clipboard",
                "vibrate",
                "local-files",
                "active-tick");
    }

    private LuaWidgetManager() {}

    public static List<String> bundledSampleIds() {
        ArrayList<String> ids = new ArrayList<>();
        Collections.addAll(ids, BUNDLED_SAMPLE_IDS);
        return ids;
    }

    public static boolean isBundledSample(String id) {
        String normalized = normalizeId(id);
        for (String sampleId : BUNDLED_SAMPLE_IDS) {
            if (TextUtils.equals(normalized, sampleId)) {
                return true;
            }
        }
        String script = readScript(normalized);
        if (TextUtils.isEmpty(script)) {
            return false;
        }
        for (String marker : BUNDLED_SAMPLE_MARKERS) {
            if (script.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    public static File widgetsDir() {
        File dir = new File(Tuils.getFolder(), WIDGETS_DIR);
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    public static File widgetDir(String id) {
        return new File(widgetsDir(), normalizeId(id));
    }

    public static File scriptFile(String id) {
        return new File(widgetDir(id), SCRIPT_FILE);
    }

    public static File manifestFile(String id) {
        return new File(widgetDir(id), MANIFEST_FILE);
    }

    public static boolean exists(String id) {
        return scriptFile(id).isFile();
    }

    public static List<String> listIds() {
        ArrayList<String> ids = new ArrayList<>();
        File[] files = widgetsDir().listFiles();
        if (files == null) {
            return ids;
        }
        for (File file : files) {
            if (file.isDirectory() && new File(file, SCRIPT_FILE).isFile()) {
                ids.add(file.getName());
            }
        }
        Collections.sort(ids, String.CASE_INSENSITIVE_ORDER);
        return ids;
    }

    public static String readScript(String id) {
        File file = scriptFile(id);
        if (!file.isFile()) {
            return "";
        }
        try (FileInputStream in = new FileInputStream(file)) {
            return Tuils.convertStreamToString(in);
        } catch (Exception e) {
            return "";
        }
    }

    public static String getName(String id) {
        String normalized = normalizeId(id);
        File manifest = manifestFile(normalized);
        if (manifest.isFile()) {
            try (FileInputStream in = new FileInputStream(manifest)) {
                JSONObject object = new JSONObject(Tuils.convertStreamToString(in));
                String name = object.optString("name", "");
                if (!TextUtils.isEmpty(name)) {
                    return name;
                }
            } catch (Exception ignored) {
            }
        }
        String scriptName = metadata(readScript(normalized)).get("name");
        if (!TextUtils.isEmpty(scriptName)) {
            return scriptName;
        }
        return displayName(normalized);
    }

    public static String getScriptType(String id) {
        return getScriptTypeFromScript(readScript(normalizeId(id)));
    }

    public static String getScriptTypeFromScript(String script) {
        String type = metadata(script).get("type");
        return TextUtils.isEmpty(type) ? "widget" : type.toLowerCase(Locale.US);
    }

    public static String apiVersion(String id) {
        return apiVersionFromScript(readScript(normalizeId(id)));
    }

    public static String apiVersionFromScript(String script) {
        Map<String, String> meta = metadata(script);
        String version = meta.get("retui");
        if (TextUtils.isEmpty(version)) {
            version = meta.get("retui_version");
        }
        return TextUtils.isEmpty(version) ? "1" : version;
    }

    public static boolean isDockableScript(String script) {
        String type = getScriptTypeFromScript(script);
        return !"suggest".equals(type) && !"command".equals(type);
    }

    public static boolean isDockable(String id) {
        return isDockableScript(readScript(normalizeId(id)));
    }

    public static boolean isEnabled(String id) {
        JSONObject manifest = readManifest(normalizeId(id));
        return manifest == null || !manifest.optBoolean("disabled", false);
    }

    public static void setEnabled(String id, boolean enabled) throws Exception {
        String normalized = normalizeId(id);
        if (!exists(normalized)) {
            throw new IllegalArgumentException("Unknown widget: " + normalized);
        }
        JSONObject manifest = readManifest(normalized);
        if (manifest == null) {
            manifest = new JSONObject();
            manifest.put("type", "retui-widget");
            manifest.put("schema", 1);
            manifest.put("id", normalized);
            manifest.put("name", getName(normalized));
            manifest.put("engine", ENGINE);
        }
        manifest.put("disabled", !enabled);
        write(manifestFile(normalized), manifest.toString(2) + "\n");
    }

    public static long version(String id) {
        File file = scriptFile(id);
        return file.isFile() ? file.lastModified() : 0L;
    }

    public static void save(String id, String name, String script) throws Exception {
        String normalized = normalizeId(id);
        if (TextUtils.isEmpty(normalized)) {
            throw new IllegalArgumentException("Widget id is required");
        }
        String code = script == null ? "" : script;
        Map<String, String> meta = metadata(code);
        String scriptName = meta.get("name");
        String effectiveName = name == null ? "" : name.trim();
        if (!TextUtils.isEmpty(scriptName)
                && (TextUtils.isEmpty(effectiveName)
                || TextUtils.equals(effectiveName, normalized)
                || TextUtils.equals(effectiveName, displayName(normalized)))) {
            effectiveName = scriptName;
        }
        File dir = widgetDir(normalized);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Unable to create widget folder");
        }
        JSONObject previousManifest = readManifest(normalized);
        String previousApprovedHash = previousManifest == null ? "" : previousManifest.optString("approvedScriptHash", "");
        String previousApprovedPermissions = previousManifest == null ? "" : previousManifest.optString("approvedPermissions", "");
        String hash = scriptHash(code);
        List<String> requiredPermissions = requiredPermissions(code);
        write(scriptFile(normalized), code);

        JSONObject manifest = new JSONObject();
        manifest.put("type", "retui-widget");
        manifest.put("schema", 1);
        manifest.put("id", normalized);
        manifest.put("name", TextUtils.isEmpty(effectiveName) ? displayName(normalized) : effectiveName);
        manifest.put("engine", ENGINE);
        manifest.put("devOnly", true);
        JSONObject metadata = new JSONObject();
        for (Map.Entry<String, String> entry : meta.entrySet()) {
            metadata.put(entry.getKey(), entry.getValue());
        }
        manifest.put("metadata", metadata);
        manifest.put("scriptHash", hash);
        manifest.put("permissions", TextUtils.join(",", requiredPermissions));
        if (TextUtils.equals(previousApprovedHash, hash)
                && permissionList(previousApprovedPermissions).containsAll(requiredPermissions)) {
            manifest.put("approvedScriptHash", previousApprovedHash);
            manifest.put("approvedPermissions", previousApprovedPermissions);
        }
        if (previousManifest != null && previousManifest.optBoolean("disabled", false)) {
            manifest.put("disabled", true);
        }
        write(manifestFile(normalized), manifest.toString(2) + "\n");
    }

    public static void delete(String id) {
        File dir = widgetDir(id);
        if (dir.exists()) {
            Tuils.delete(dir);
        }
    }

    public static String exportPackage(String id) throws Exception {
        String normalized = normalizeId(id);
        if (!exists(normalized)) {
            throw new IllegalArgumentException("Unknown widget: " + normalized);
        }
        JSONObject object = new JSONObject();
        object.put("type", "retui-widget-package");
        object.put("schema", 1);
        object.put("id", normalized);
        object.put("name", getName(normalized));
        object.put("engine", ENGINE);
        object.put("script", readScript(normalized));
        return object.toString(2);
    }

    public static void rename(String oldId, String newId) throws Exception {
        String oldNormalized = normalizeId(oldId);
        String newNormalized = normalizeId(newId);
        if (TextUtils.isEmpty(oldNormalized) || TextUtils.isEmpty(newNormalized)) {
            throw new IllegalArgumentException("Widget id is required");
        }
        if (TextUtils.equals(oldNormalized, newNormalized)) {
            return;
        }

        File oldDir = widgetDir(oldNormalized);
        if (!new File(oldDir, SCRIPT_FILE).isFile()) {
            throw new IllegalArgumentException("Unknown widget: " + oldNormalized);
        }

        File newDir = widgetDir(newNormalized);
        if (newDir.exists()) {
            throw new IllegalArgumentException("Widget id already exists: " + newNormalized);
        }

        if (!oldDir.renameTo(newDir)) {
            throw new IllegalStateException("Unable to rename widget folder");
        }

        String name = getName(newNormalized);
        String script = readScript(newNormalized);
        save(newNormalized, name, script);
    }

    public static String newWidgetTemplate(String id) {
        String normalized = normalizeId(id);
        String title = displayName(normalized);
        return "-- name = \"" + title + "\"\n"
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
                + "end\n";
    }

    public static List<String> installSamples() throws Exception {
        ArrayList<String> ids = new ArrayList<>();
        for (Map.Entry<String, Sample> entry : samples().entrySet()) {
            String id = entry.getKey();
            Sample sample = entry.getValue();
            save(id, sample.name, sample.script);
            ids.add(id);
        }
        return ids;
    }

    public static String modulePayload(String id, LuaWidgetEngine.RenderResult result) {
        StringBuilder out = new StringBuilder();
        String title = result.title;
        if (TextUtils.isEmpty(title)) {
            title = getName(id);
        }
        appendDirective(out, "title", title);

        if (!TextUtils.isEmpty(result.error)) {
            appendDirective(out, "body", "Lua error: " + result.error);
            if (!TextUtils.isEmpty(result.errorStage)) {
                appendDirective(out, "body", "Stage: " + result.errorStage);
            }
        } else if (!TextUtils.isEmpty(result.body)) {
            for (String line : result.body.split("\\r?\\n", -1)) {
                appendDirective(out, "body", line);
            }
        } else {
            appendDirective(out, "body", "No widget output yet.");
        }

        int index = 1;
        boolean hasRefreshButton = false;
        for (String button : result.buttons) {
            appendSuggest(out, button, "widget -click " + normalizeId(id) + " " + index);
            hasRefreshButton = hasRefreshButton || "refresh".equals(normalizeActionLabel(button));
            index += 1;
        }
        for (LuaWidgetEngine.RenderValueAction action : result.valueActions) {
            appendSuggest(out, action.label, "widget -action " + normalizeId(id) + " " + quoteArg(action.value));
        }
        if (result.dialogOpen) {
            int dialogIndex = 1;
            for (String item : result.dialogItems) {
                String label = dialogIndex == result.dialogSelected ? "* " + item : item;
                appendSuggest(out, label, "widget -dialog " + normalizeId(id) + " " + dialogIndex);
                dialogIndex += 1;
            }
            appendSuggest(out, "cancel", "widget -dialog " + normalizeId(id) + " -1");
        }
        for (LuaWidgetEngine.RenderAction action : result.commands) {
            appendSuggest(out, action.label, action.command);
        }
        if (result.expandable) {
            appendSuggest(out, result.expanded ? "collapse" : "expand",
                    "widget " + (result.expanded ? "-collapse " : "-expand ") + normalizeId(id));
        }
        if (!hasRefreshButton) {
            appendSuggest(out, "refresh", "module -refresh " + normalizeId(id));
        }
        if (!TextUtils.isEmpty(result.error)) {
            appendSuggest(out, "copy error", "widget -copy-error " + normalizeId(id));
            appendSuggest(out, "check", "widget -check " + normalizeId(id));
            appendSuggest(out, "disable", "widget -disable " + normalizeId(id));
        }
        return out.toString().trim();
    }

    public static String disabledPayload(String id) {
        String normalized = normalizeId(id);
        StringBuilder out = new StringBuilder();
        appendDirective(out, "title", getName(normalized));
        appendDirective(out, "body", "Lua widget disabled.");
        appendSuggest(out, "enable", "widget -enable " + normalized);
        appendSuggest(out, "check", "widget -check " + normalized);
        return out.toString().trim();
    }

    public static String consentPayload(String id) {
        String normalized = normalizeId(id);
        TrustStatus status = trustStatus(normalized);
        StringBuilder out = new StringBuilder();
        appendDirective(out, "title", getName(normalized));
        appendDirective(out, "body", "Lua script approval required.");
        appendDirective(out, "body", "Permissions: " + (status.requiredPermissions.isEmpty() ? "none" : TextUtils.join(", ", status.requiredPermissions)));
        if (!status.missingDeclarations.isEmpty()) {
            appendDirective(out, "body", "Missing metadata: " + TextUtils.join(", ", status.missingDeclarations));
        }
        if (!status.unsupportedPermissions.isEmpty()) {
            appendDirective(out, "body", "Unsupported permissions: " + TextUtils.join(", ", status.unsupportedPermissions));
        }
        if (status.scriptChanged) {
            appendDirective(out, "body", "Script changed since last approval.");
        }
        if (status.canApprove()) {
            appendSuggest(out, "approve", "widget -approve " + normalized);
        }
        appendSuggest(out, "check", "widget -check " + normalized);
        return out.toString().trim();
    }

    public static boolean isTrusted(String id) {
        return trustStatus(id).trusted;
    }

    public static boolean isPermissionApproved(String id, String permission) {
        return approvedPermissions(id).contains(normalizePermission(permission));
    }

    public static List<String> approvedPermissions(String id) {
        String normalized = normalizeId(id);
        JSONObject manifest = readManifest(normalized);
        if (manifest == null) {
            return new ArrayList<>();
        }
        String script = readScript(normalized);
        String approvedHash = manifest.optString("approvedScriptHash", "");
        if (!TextUtils.equals(approvedHash, scriptHash(script))) {
            return new ArrayList<>();
        }
        return permissionList(manifest.optString("approvedPermissions", ""));
    }

    public static TrustStatus trustStatus(String id) {
        String normalized = normalizeId(id);
        String script = readScript(normalized);
        TrustStatus status = new TrustStatus();
        status.scriptHash = scriptHash(script);
        status.declaredPermissions = declaredPermissions(script);
        status.requiredPermissions = requiredPermissions(script);
        status.inferredCapabilities = inferCapabilities(script);
        status.missingDeclarations = subtract(status.requiredPermissions, status.declaredPermissions);
        status.unsupportedPermissions = unsupportedPermissions(status.declaredPermissions);

        JSONObject manifest = readManifest(normalized);
        String approvedHash = manifest == null ? "" : manifest.optString("approvedScriptHash", "");
        List<String> approvedPermissions = permissionList(manifest == null ? "" : manifest.optString("approvedPermissions", ""));
        status.scriptChanged = !TextUtils.isEmpty(approvedHash) && !TextUtils.equals(approvedHash, status.scriptHash);
        status.trusted = status.missingDeclarations.isEmpty()
                && status.unsupportedPermissions.isEmpty()
                && (status.requiredPermissions.isEmpty()
                || (TextUtils.equals(approvedHash, status.scriptHash)
                && approvedPermissions.containsAll(status.requiredPermissions)));
        return status;
    }

    public static void approve(String id) throws Exception {
        String normalized = normalizeId(id);
        if (!exists(normalized)) {
            throw new IllegalArgumentException("Unknown widget: " + normalized);
        }
        TrustStatus status = trustStatus(normalized);
        if (!status.unsupportedPermissions.isEmpty()) {
            throw new IllegalArgumentException("Unsupported permissions: " + TextUtils.join(", ", status.unsupportedPermissions));
        }
        if (!status.missingDeclarations.isEmpty()) {
            throw new IllegalArgumentException("Declare permissions first: " + TextUtils.join(", ", status.missingDeclarations));
        }
        JSONObject manifest = readManifest(normalized);
        if (manifest == null) {
            manifest = new JSONObject();
        }
        manifest.put("approvedScriptHash", status.scriptHash);
        manifest.put("approvedPermissions", TextUtils.join(",", status.requiredPermissions));
        manifest.put("scriptHash", status.scriptHash);
        manifest.put("permissions", TextUtils.join(",", status.requiredPermissions));
        write(manifestFile(normalized), manifest.toString(2) + "\n");
    }

    public static void saveLastError(String id, String stage, String error) {
        String normalized = normalizeId(id);
        if (TextUtils.isEmpty(normalized)) {
            return;
        }
        try {
            JSONObject manifest = readManifest(normalized);
            if (manifest == null) {
                manifest = new JSONObject();
                manifest.put("type", "retui-widget");
                manifest.put("schema", 1);
                manifest.put("id", normalized);
                manifest.put("name", getName(normalized));
                manifest.put("engine", ENGINE);
            }
            manifest.put("lastError", error == null ? "" : error);
            manifest.put("lastErrorStage", stage == null ? "" : stage);
            manifest.put("lastErrorAt", System.currentTimeMillis());
            write(manifestFile(normalized), manifest.toString(2) + "\n");
        } catch (Exception ignored) {
        }
    }

    public static String lastError(String id) {
        JSONObject manifest = readManifest(normalizeId(id));
        if (manifest == null) {
            return "";
        }
        String error = manifest.optString("lastError", "");
        String stage = manifest.optString("lastErrorStage", "");
        if (TextUtils.isEmpty(error)) {
            return "";
        }
        return TextUtils.isEmpty(stage) ? error : "Stage: " + stage + "\n" + error;
    }

    public static String normalizeId(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9_-]", "");
    }

    public static String idFromName(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    public static Map<String, String> metadata(String script) {
        LinkedHashMap<String, String> meta = new LinkedHashMap<>();
        if (script == null) {
            return meta;
        }
        Pattern pattern = Pattern.compile("^\\s*--\\s*([a-zA-Z0-9_]+)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\r\\n]+))\\s*$");
        String[] lines = script.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() == 0) {
                continue;
            }
            Matcher matcher = pattern.matcher(line);
            if (!matcher.matches()) {
                if (!trimmed.startsWith("--")) {
                    break;
                }
                continue;
            }
            String value = matcher.group(2);
            if (value == null) value = matcher.group(3);
            if (value == null) value = matcher.group(4);
            meta.put(matcher.group(1).toLowerCase(Locale.US), value == null ? "" : value.trim());
        }
        return meta;
    }

    public static String describeCapabilities(String script) {
        List<String> capabilities = inferCapabilities(script);
        return capabilities.isEmpty() ? "none" : TextUtils.join(", ", capabilities);
    }

    public static String describeRequiredPermissions(String script) {
        List<String> permissions = requiredPermissions(script);
        return permissions.isEmpty() ? "none" : TextUtils.join(", ", permissions);
    }

    public static List<String> inferCapabilities(String script) {
        ArrayList<String> capabilities = new ArrayList<>();
        String code = script == null ? "" : script.toLowerCase(Locale.US);
        addCapability(capabilities, code.contains("http:"), "network");
        addCapability(capabilities, code.contains("system:to_clipboard")
                || code.contains("system:copy_to_clipboard")
                || code.contains("system:clipboard"), "clipboard");
        addCapability(capabilities, code.contains("system:vibrate"), "vibrate");
        addCapability(capabilities, code.contains("ui:set_tick")
                || code.contains("ui:set_tick_interval")
                || code.contains("function on_tick"), "active-tick");
        addCapability(capabilities, code.contains("files:read")
                || code.contains("files:write")
                || code.contains("files:append")
                || code.contains("files:delete")
                || code.contains("files:list")
                || code.contains("files:exists"), "local-files");
        addCapability(capabilities, code.contains("suggest:"), "suggestions");
        return capabilities;
    }

    public static List<String> requiredPermissions(String script) {
        ArrayList<String> required = new ArrayList<>();
        for (String capability : inferCapabilities(script)) {
            if (SENSITIVE_CAPABILITIES.contains(capability)) {
                addCapability(required, true, capability);
            }
        }
        for (String declared : declaredPermissions(script)) {
            if (SENSITIVE_CAPABILITIES.contains(declared)) {
                addCapability(required, true, declared);
            }
        }
        return required;
    }

    public static List<String> declaredPermissions(String script) {
        return permissionList(metadata(script).get("permissions"));
    }

    public static List<String> missingPermissionDeclarations(String script) {
        return subtract(requiredPermissions(script), declaredPermissions(script));
    }

    public static List<String> unsupportedPermissions(String script) {
        return unsupportedPermissions(declaredPermissions(script));
    }

    private static List<String> unsupportedPermissions(List<String> declaredPermissions) {
        ArrayList<String> unsupported = new ArrayList<>();
        for (String permission : declaredPermissions) {
            if (!SENSITIVE_CAPABILITIES.contains(permission) && !"none".equals(permission)) {
                unsupported.add(permission);
            }
        }
        return unsupported;
    }

    private static List<String> permissionList(String raw) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (raw == null) {
            return new ArrayList<>();
        }
        for (String part : raw.split("[,\\s]+")) {
            String permission = normalizePermission(part);
            if (permission.length() == 0 || "none".equals(permission)) {
                continue;
            }
            values.add(permission);
        }
        return new ArrayList<>(values);
    }

    private static String normalizePermission(String permission) {
        return permission == null ? "" : permission.trim().toLowerCase(Locale.US);
    }

    private static List<String> subtract(List<String> source, List<String> remove) {
        ArrayList<String> out = new ArrayList<>();
        for (String item : source) {
            if (!remove.contains(item)) {
                out.add(item);
            }
        }
        return out;
    }

    private static JSONObject readManifest(String id) {
        File manifest = manifestFile(id);
        if (!manifest.isFile()) {
            return null;
        }
        try (FileInputStream in = new FileInputStream(manifest)) {
            return new JSONObject(Tuils.convertStreamToString(in));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String scriptHash(String script) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((script == null ? "" : script).getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : hash) {
                out.append(String.format(Locale.US, "%02x", b));
            }
            return out.toString();
        } catch (Exception e) {
            return String.valueOf((script == null ? "" : script).hashCode());
        }
    }

    private static void addCapability(List<String> capabilities, boolean enabled, String capability) {
        if (enabled && !capabilities.contains(capability)) {
            capabilities.add(capability);
        }
    }

    public static final class TrustStatus {
        public String scriptHash = "";
        public boolean trusted;
        public boolean scriptChanged;
        public List<String> inferredCapabilities = new ArrayList<>();
        public List<String> declaredPermissions = new ArrayList<>();
        public List<String> requiredPermissions = new ArrayList<>();
        public List<String> missingDeclarations = new ArrayList<>();
        public List<String> unsupportedPermissions = new ArrayList<>();

        public boolean canApprove() {
            return missingDeclarations.isEmpty() && unsupportedPermissions.isEmpty();
        }
    }

    private static String displayName(String id) {
        if (TextUtils.isEmpty(id)) {
            return "Lua Widget";
        }
        StringBuilder out = new StringBuilder();
        for (String part : id.split("[_-]+")) {
            if (part.length() == 0) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) out.append(part.substring(1));
        }
        return out.length() == 0 ? id : out.toString();
    }

    private static String normalizeActionLabel(String label) {
        return label == null ? "" : label.trim().toLowerCase(Locale.US);
    }

    private static String quoteArg(String value) {
        String safe = value == null ? "" : value;
        safe = safe.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", " ")
                .replace("\n", " ");
        return "\"" + safe + "\"";
    }

    private static void appendDirective(StringBuilder out, String key, String value) {
        if (out.length() > 0) out.append('\n');
        out.append("::").append(key).append(' ').append(value == null ? "" : value);
    }

    private static void appendSuggest(StringBuilder out, String label, String command) {
        if (TextUtils.isEmpty(label) || TextUtils.isEmpty(command)) {
            return;
        }
        if (out.length() > 0) out.append('\n');
        out.append("::suggest ")
                .append(label.replace("|", "/"))
                .append(" | command | ")
                .append(command);
    }

    private static void write(File file, String text) throws Exception {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Unable to create widget folder");
        }
        try (FileOutputStream out = new FileOutputStream(file, false)) {
            out.write((text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
        }
    }

    private static Map<String, Sample> samples() {
        LinkedHashMap<String, Sample> samples = new LinkedHashMap<>();
        samples.put("retui_counter", new Sample("Re:TUI Counter", ""
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
                + "end\n"));
        samples.put("retui_progress", new Sample("Re:TUI Progress", ""
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
                + "    ui:show_progress_bar(\"Widget progress\", prefs.progress, 100)\n"
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
                + "end\n"));
        samples.put("retui_clock", new Sample("Re:TUI Clock", ""
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
                + "end\n"));
        samples.put("retui_expandable", new Sample("Re:TUI Expandable", ""
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
                + "end\n"));
        samples.put("retui_ticker", new Sample("Re:TUI Ticker", ""
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
                + "end\n"));
        samples.put("retui_toolkit", new Sample("Re:TUI Toolkit", ""
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
                + "        progress = fmt.percent(42, 100),\n"
                + "        has_tui = tostring(strings.contains(\"Re:TUI\", \"TUI\")),\n"
                + "        accent = colors.accent,\n"
                + "    })\n"
                + "    ui:show_buttons({\"Debug\", \"Copy\"})\n"
                + "end\n"
                + "\n"
                + "function on_click(index)\n"
                + "    if index == 1 then debug:show() end\n"
                + "    if index == 2 then system:to_clipboard(date.format(\"%H:%M:%S\")) end\n"
                + "end\n"));
        samples.put("retui_suggest", new Sample("Re:TUI Suggest", ""
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
                + "end\n"));
        samples.put("retui_prefs", new Sample("Re:TUI Prefs", ""
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
                + "end\n"));
        samples.put("retui_files", new Sample("Re:TUI Files", ""
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
                + "end\n"));
        samples.put("retui_platform", new Sample("Re:TUI Platform", ""
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
                + "end\n"));
        samples.put("retui_public_ip", new Sample("Re:TUI Public IP", ""
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
                + "end\n"));
        return samples;
    }

    private static final class Sample {
        final String name;
        final String script;

        Sample(String name, String script) {
            this.name = name;
            this.script = script;
        }
    }
}
