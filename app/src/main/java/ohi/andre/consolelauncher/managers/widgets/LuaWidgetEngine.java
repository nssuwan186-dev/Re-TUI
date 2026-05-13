package ohi.andre.consolelauncher.managers.widgets;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.BaseLib;
import org.luaj.vm2.lib.DebugLib;
import org.luaj.vm2.lib.MathLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.VarArgFunction;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import ohi.andre.consolelauncher.BuildConfig;
import ohi.andre.consolelauncher.managers.settings.AppearanceSettings;
import ohi.andre.consolelauncher.tuils.Tuils;

public final class LuaWidgetEngine {

    private static final long ALARM_INTERVAL_MS = 30L * 60L * 1000L;
    private static final long EXECUTION_TIMEOUT_MS = 1500L;
    private static final int INSTRUCTION_CHECK_INTERVAL = 2048;
    private static final int MAX_HTTP_RESPONSE_BYTES = 256 * 1024;
    private static final long MAX_WIDGET_FILE_BYTES = 256L * 1024L;
    private static final long MAX_WIDGET_FILES_TOTAL_BYTES = 1024L * 1024L;
    private static final MediaType TEXT_MEDIA_TYPE = MediaType.parse("text/plain; charset=utf-8");
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
    private static final String PREF_EXPANDABLE = "_retui_expandable";
    private static final String PREF_EXPANDED = "_retui_expanded";

    private final String id;
    private final String script;
    private final long version;
    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final UpdateListener updateListener;
    private final Set<String> approvedPermissions;

    private Globals globals;
    private LuaTable prefsTable;
    private boolean loaded;
    private long lastAlarmAt;
    private long tickCount;
    private long tickIntervalMs = -1L;
    private long executionDeadlineMs;
    private int executionInstructions;
    private String executionStage = "";
    private RenderResult lastResult = new RenderResult();
    private Map<String, String> httpHeaders = new LinkedHashMap<>();
    private final ArrayList<String> debugLines = new ArrayList<>();

    public LuaWidgetEngine(Context context, String id, String script, long version, UpdateListener updateListener) {
        this.context = context == null ? null : context.getApplicationContext();
        this.id = LuaWidgetManager.normalizeId(id);
        this.script = script == null ? "" : script;
        this.version = version;
        this.updateListener = updateListener;
        this.approvedPermissions = new HashSet<>(LuaWidgetManager.approvedPermissions(this.id));
    }

    public long version() {
        return version;
    }

    public RenderResult render() {
        return render(false);
    }

    public RenderResult render(boolean forceAlarm) {
        synchronized (this) {
            try {
                ensureLoaded();
                boolean runResume = hasFunction("on_resume");
                boolean runAlarm = hasFunction("on_alarm") && shouldRunAlarm(forceAlarm);
                if (runResume || runAlarm) {
                    newResult();
                    if (runResume) {
                        callIfPresent("on_resume");
                    }
                    if (runAlarm) {
                        lastAlarmAt = System.currentTimeMillis();
                        callIfPresent("on_alarm");
                    }
                }
                persistPrefs();
            } catch (Throwable e) {
                lastResult = errorResult(e);
            }
            return lastResult.copy();
        }
    }

    public RenderResult click(int index) {
        synchronized (this) {
            try {
                ensureLoaded();
                RenderResult result = newResult();
                if (!callIfPresent("on_click", LuaValue.valueOf(index))) {
                    result.body = "No on_click handler in " + id + ".";
                }
                persistPrefs();
                lastResult = result;
            } catch (Throwable e) {
                lastResult = errorResult(e);
            }
            return lastResult.copy();
        }
    }

    public RenderResult action(String value) {
        synchronized (this) {
            try {
                ensureLoaded();
                RenderResult result = newResult();
                LuaValue payload = LuaValue.valueOf(value == null ? "" : value);
                if (!callIfPresent("on_action", payload)
                        && !callIfPresent("on_command", payload)
                        && !callIfPresent("on_submit", payload)) {
                    result.body = "No on_action handler in " + id + ".";
                }
                persistPrefs();
                lastResult = result;
            } catch (Throwable e) {
                lastResult = errorResult(e);
            }
            return lastResult.copy();
        }
    }

    public RenderResult dialog(int index) {
        synchronized (this) {
            try {
                ensureLoaded();
                RenderResult result = newResult();
                if (!callIfPresent("on_dialog_action", LuaValue.valueOf(index))) {
                    result.body = "No on_dialog_action handler in " + id + ".";
                }
                persistPrefs();
                lastResult = result;
            } catch (Throwable e) {
                lastResult = errorResult(e);
            }
            return lastResult.copy();
        }
    }

    public RenderResult setExpanded(boolean expanded) {
        synchronized (this) {
            try {
                ensureLoaded();
                setExpandableState(true);
                setExpandedState(expanded);
                persistPrefs();
                return render(false);
            } catch (Throwable e) {
                lastResult = errorResult(e);
                return lastResult.copy();
            }
        }
    }

    public RenderResult toggleExpanded() {
        synchronized (this) {
            try {
                ensureLoaded();
                setExpandableState(true);
                setExpandedState(!isExpandedState());
                persistPrefs();
                return render(false);
            } catch (Throwable e) {
                lastResult = errorResult(e);
                return lastResult.copy();
            }
        }
    }

    public RenderResult tick() {
        synchronized (this) {
            try {
                ensureLoaded();
                RenderResult result = newResult();
                tickCount += 1;
                if (!callIfPresent("on_tick", LuaValue.valueOf(tickCount))) {
                    tickIntervalMs = -1L;
                    result.tickIntervalMs = -1L;
                }
                persistPrefs();
                lastResult = result;
            } catch (Throwable e) {
                lastResult = errorResult(e);
            }
            return lastResult.copy();
        }
    }

    public RenderResult suggest(String query) {
        synchronized (this) {
            try {
                ensureLoaded();
                RenderResult result = newResult();
                if (!callIfPresent("on_suggest", LuaValue.valueOf(query == null ? "" : query))) {
                    callIfPresent("on_query", LuaValue.valueOf(query == null ? "" : query));
                }
                persistPrefs();
                lastResult = result;
            } catch (Throwable e) {
                lastResult = errorResult(e);
            }
            return lastResult.copy();
        }
    }

    private void ensureLoaded() {
        if (loaded) {
            return;
        }

        globals = safeGlobals();
        prefsTable = loadPrefs();
        prefsTable.set("show_dialog", new UiFunction(args -> lastResult.body = prefsSummary()));
        globals.set("io", LuaValue.NIL);
        globals.set("package", LuaValue.NIL);
        globals.set("luajava", LuaValue.NIL);
        globals.set("os", safeOsTable());
        globals.set("require", new RequireFunction());
        globals.set("prefs", prefsTable);
        globals.set("ui", buildUiTable());
        globals.set("suggest", buildSuggestTable());
        globals.set("files", buildFilesTable());
        globals.set("json", buildJsonTable());
        globals.set("http", buildHttpTable());
        globals.set("system", buildSystemTable());
        globals.set("aio", buildAioTable());
        globals.set("debug", buildDebugTable());
        globals.set("date", buildDateTable());
        globals.set("fmt", buildFmtTable());
        globals.set("strings", buildStringsTable());
        globals.set("colors", buildColorsTable());
        buildPrefsHelpers(prefsTable);
        runGuarded("load", () -> globals.load(script, id).call());
        loaded = true;

        RenderResult result = newResult();
        callIfPresent("on_load");
        persistPrefs();
        lastResult = result;
    }

    private Globals safeGlobals() {
        Globals safe = new Globals();
        LuaTable packageTable = new LuaTable();
        packageTable.set("loaded", new LuaTable());
        safe.set("package", packageTable);
        safe.load(new BaseLib());
        safe.load(new TableLib());
        safe.load(new StringLib());
        safe.load(new MathLib());
        LoadState.install(safe);
        LuaC.install(safe);
        safe.load(new GuardedDebugLib());
        safe.set("package", LuaValue.NIL);
        safe.set("dofile", LuaValue.NIL);
        safe.set("loadfile", LuaValue.NIL);
        safe.set("load", LuaValue.NIL);
        safe.set("loadstring", LuaValue.NIL);
        safe.set("collectgarbage", LuaValue.NIL);
        return safe;
    }

    private boolean hasFunction(String name) {
        return globals != null && globals.get(name).isfunction();
    }

    private boolean shouldRunAlarm(boolean forceAlarm) {
        long now = System.currentTimeMillis();
        return forceAlarm || lastAlarmAt <= 0L || now - lastAlarmAt >= ALARM_INTERVAL_MS;
    }

    private RenderResult newResult() {
        RenderResult result = new RenderResult();
        result.expandable = isExpandableState();
        result.expanded = isExpandedState();
        result.tickIntervalMs = tickIntervalMs;
        lastResult = result;
        return result;
    }

    private boolean callIfPresent(String name, LuaValue... args) {
        LuaValue function = globals.get(name);
        if (!function.isfunction()) {
            return false;
        }
        runGuarded(name, () -> {
            if (args == null || args.length == 0) {
                function.call();
            } else if (args.length == 1) {
                function.call(args[0]);
            } else {
                LuaValue[] values = new LuaValue[args.length];
                System.arraycopy(args, 0, values, 0, args.length);
                function.invoke(values);
            }
        });
        return true;
    }

    private void runGuarded(String stage, Runnable runnable) {
        long previousDeadline = executionDeadlineMs;
        String previousStage = executionStage;
        int previousInstructions = executionInstructions;
        executionDeadlineMs = System.currentTimeMillis() + EXECUTION_TIMEOUT_MS;
        executionStage = stage == null ? "lua" : stage;
        executionInstructions = 0;
        try {
            runnable.run();
        } finally {
            executionDeadlineMs = previousDeadline;
            executionStage = previousStage;
            executionInstructions = previousInstructions;
        }
    }

    private void checkTimeout() {
        if (executionDeadlineMs <= 0L) {
            return;
        }
        executionInstructions += 1;
        if ((executionInstructions % INSTRUCTION_CHECK_INTERVAL) != 0) {
            return;
        }
        if (System.currentTimeMillis() > executionDeadlineMs) {
            throw new LuaError("Lua runtime timeout in " + (TextUtils.isEmpty(executionStage) ? "script" : executionStage));
        }
    }

    private void buildPrefsHelpers(LuaTable prefs) {
        prefs.set("get", new ValueFunction(args -> {
            String key = stringAt(args, 1, "");
            LuaValue fallback = args.arg(rawIndex(args, 2));
            LuaValue value = prefs.get(key);
            return value.isnil() ? fallback : value;
        }));
        prefs.set("set", new ValueFunction(args -> {
            String key = stringAt(args, 1, "");
            if (TextUtils.isEmpty(key) || key.startsWith("_")) {
                return LuaValue.FALSE;
            }
            prefs.set(key, args.arg(rawIndex(args, 2)));
            return LuaValue.TRUE;
        }));
        prefs.set("has", new ValueFunction(args -> prefs.get(stringAt(args, 1, "")).isnil() ? LuaValue.FALSE : LuaValue.TRUE));
        prefs.set("unset", new ValueFunction(args -> {
            String key = stringAt(args, 1, "");
            if (TextUtils.isEmpty(key) || key.startsWith("_")) {
                return LuaValue.FALSE;
            }
            prefs.set(key, LuaValue.NIL);
            return LuaValue.TRUE;
        }));
        prefs.set("number", new ValueFunction(args -> {
            LuaValue value = prefs.get(stringAt(args, 1, ""));
            if (value.isnumber()) {
                return LuaValue.valueOf(value.todouble());
            }
            if (value.isstring()) {
                try {
                    return LuaValue.valueOf(Double.parseDouble(value.tojstring()));
                } catch (Exception ignored) {
                }
            }
            return LuaValue.valueOf(numberAt(args, 2, 0));
        }));
        prefs.set("bool", new ValueFunction(args -> {
            LuaValue value = prefs.get(stringAt(args, 1, ""));
            if (value.isboolean()) {
                return value.toboolean() ? LuaValue.TRUE : LuaValue.FALSE;
            }
            if (value.isnumber()) {
                return value.todouble() != 0d ? LuaValue.TRUE : LuaValue.FALSE;
            }
            if (value.isstring()) {
                String text = value.tojstring().trim().toLowerCase(Locale.US);
                return ("true".equals(text) || "yes".equals(text) || "1".equals(text) || "on".equals(text)) ? LuaValue.TRUE : LuaValue.FALSE;
            }
            return booleanAt(args, 2, false) ? LuaValue.TRUE : LuaValue.FALSE;
        }));
        prefs.set("inc", new ValueFunction(args -> {
            String key = stringAt(args, 1, "");
            if (TextUtils.isEmpty(key) || key.startsWith("_")) {
                return LuaValue.NIL;
            }
            LuaValue current = prefs.get(key);
            double base = 0d;
            if (current.isnumber()) {
                base = current.todouble();
            } else if (current.isstring()) {
                try {
                    base = Double.parseDouble(current.tojstring());
                } catch (Exception ignored) {
                }
            }
            double next = base + numberAt(args, 2, 1);
            prefs.set(key, LuaValue.valueOf(next));
            return LuaValue.valueOf(next);
        }));
    }

    private LuaTable buildUiTable() {
        LuaTable ui = new LuaTable();
        ui.set("set_title", new UiFunction(args -> lastResult.title = stringArg(args)));
        ui.set("title", new UiFunction(args -> lastResult.title = stringArg(args)));
        ui.set("default_title", new ValueFunction(args -> LuaValue.valueOf(LuaWidgetManager.getName(id))));
        ui.set("get_default_title", new ValueFunction(args -> LuaValue.valueOf(LuaWidgetManager.getName(id))));
        ui.set("show_text", new UiFunction(args -> lastResult.body = stringArg(args)));
        ui.set("clear", new UiFunction(args -> {
            lastResult.body = "";
            lastResult.buttons.clear();
            lastResult.commands.clear();
            lastResult.valueActions.clear();
            lastResult.dialogOpen = false;
            lastResult.dialogTitle = "";
            lastResult.dialogItems.clear();
            lastResult.dialogSelected = -1;
            lastResult.progress = -1;
        }));
        ui.set("text", new UiFunction(args -> lastResult.body = appendLine(lastResult.body, stringArg(args))));
        ui.set("body", new UiFunction(args -> lastResult.body = appendLine(lastResult.body, stringArg(args))));
        ui.set("add_line", new UiFunction(args -> lastResult.body = appendLine(lastResult.body, stringArg(args))));
        ui.set("show_lines", new UiFunction(args -> lastResult.body = tableLines(tableArg(args))));
        ui.set("lines", new UiFunction(args -> lastResult.body = tableLines(tableArg(args))));
        ui.set("show_table", new UiFunction(args -> lastResult.body = tableRows(tableArg(args))));
        ui.set("show_kv", new UiFunction(args -> lastResult.body = tableKeyValues(tableArg(args))));
        ui.set("kv", new UiFunction(args -> lastResult.body = tableKeyValues(tableArg(args))));
        ui.set("show_buttons", new UiFunction(args -> lastResult.buttons = tableStrings(tableArg(args))));
        ui.set("buttons", new UiFunction(args -> lastResult.buttons = tableStrings(tableArg(args))));
        ui.set("add_button", new UiFunction(args -> {
            String label = stringArg(args);
            if (!TextUtils.isEmpty(label)) {
                lastResult.buttons.add(label);
            }
        }));
        ui.set("show_action", new UiFunction(this::addValueAction));
        ui.set("action", new UiFunction(this::addValueAction));
        ui.set("add_action", new UiFunction(this::addValueAction));
        ui.set("show_command", new UiFunction(args -> {
            String label = stringAt(args, 1, "");
            String command = stringAt(args, 2, "");
            if (!TextUtils.isEmpty(label) && !TextUtils.isEmpty(command)) {
                lastResult.commands.add(new RenderAction(label, command));
            }
        }));
        ui.set("command", new UiFunction(args -> {
            String label = stringAt(args, 1, "");
            String command = stringAt(args, 2, "");
            if (!TextUtils.isEmpty(label) && !TextUtils.isEmpty(command)) {
                lastResult.commands.add(new RenderAction(label, command));
            }
        }));
        ui.set("show_module", new UiFunction(args -> {
            String module = stringAt(args, 1, "");
            String label = stringAt(args, 2, module);
            if (!TextUtils.isEmpty(module)) {
                lastResult.commands.add(new RenderAction(label, "module -show " + module));
            }
        }));
        ui.set("show_progress_bar", new UiFunction(args -> {
            String label = stringAt(args, 1, "Progress");
            double current = numberAt(args, 2, 0);
            double max = Math.max(1, numberAt(args, 3, 100));
            int pct = (int) Math.round(Math.max(0, Math.min(100, (current * 100d) / max)));
            lastResult.body = appendLine(lastResult.body, label + ": " + pct + "%");
        }));
        ui.set("set_progress", new UiFunction(args -> {
            double pct = Math.max(0, Math.min(100, numberAt(args, 1, 0)));
            lastResult.progress = pct;
        }));
        ui.set("set_tick_interval", new UiFunction(args -> setTickInterval(numberAt(args, 1, 1))));
        ui.set("set_tick", new UiFunction(args -> setTickInterval(numberAt(args, 1, 1))));
        ui.set("disable_tick", new UiFunction(args -> setTickInterval(0)));
        ui.set("show_toast", new UiFunction(args -> {
            String text = stringArg(args);
            showToast(text);
            lastResult.body = appendLine(lastResult.body, "[toast] " + text);
        }));
        ui.set("is_folded", new ValueFunction(args -> isFoldedState() ? LuaValue.TRUE : LuaValue.FALSE));
        ui.set("is_expanded", new ValueFunction(args -> isExpandedState() ? LuaValue.TRUE : LuaValue.FALSE));
        ui.set("set_expandable", new UiFunction(args -> setExpandableState(booleanAt(args, 1, true))));
        ui.set("expand", new UiFunction(args -> setExpandedState(true)));
        ui.set("collapse", new UiFunction(args -> setExpandedState(false)));
        ui.set("toggle", new UiFunction(args -> setExpandedState(!isExpandedState())));
        ui.set("show_radio_dialog", new UiFunction(this::showChoiceDialog));
        ui.set("show_list_dialog", new UiFunction(this::showChoiceDialog));
        return ui;
    }

    private LuaTable buildSuggestTable() {
        LuaTable suggest = new LuaTable();
        suggest.set("command", new UiFunction(args -> {
            String label = stringAt(args, 1, "");
            String command = stringAt(args, 2, "");
            if (!TextUtils.isEmpty(label) && !TextUtils.isEmpty(command)) {
                lastResult.commands.add(new RenderAction(label, command));
            }
        }));
        suggest.set("module", new UiFunction(args -> {
            String module = stringAt(args, 1, "");
            String label = stringAt(args, 2, module);
            if (!TextUtils.isEmpty(module)) {
                lastResult.commands.add(new RenderAction(label, "module -show " + module));
            }
        }));
        suggest.set("text", new UiFunction(args -> {
            String text = stringArg(args);
            if (!TextUtils.isEmpty(text)) {
                lastResult.commands.add(new RenderAction(text, text));
            }
        }));
        return suggest;
    }

    private LuaTable buildFilesTable() {
        LuaTable files = new LuaTable();
        files.set("read", new ValueFunction(args -> {
            requirePermission("local-files");
            File file = dataFile(stringAt(args, 1, ""));
            if (file == null || !file.isFile()) {
                return LuaValue.NIL;
            }
            ensureReadableFile(file);
            try (FileInputStream in = new FileInputStream(file)) {
                return LuaValue.valueOf(Tuils.convertStreamToString(in));
            } catch (Exception e) {
                return LuaValue.NIL;
            }
        }));
        files.set("write", new ValueFunction(args -> {
            requirePermission("local-files");
            File file = dataFile(stringAt(args, 1, ""));
            if (file == null) {
                return LuaValue.FALSE;
            }
            try {
                byte[] bytes = stringAt(args, 2, "").getBytes(StandardCharsets.UTF_8);
                ensureFileWriteAllowed(file, bytes.length, false);
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    parent.mkdirs();
                }
                try (FileOutputStream out = new FileOutputStream(file, false)) {
                    out.write(bytes);
                }
                return LuaValue.TRUE;
            } catch (Exception e) {
                return LuaValue.FALSE;
            }
        }));
        files.set("append", new ValueFunction(args -> {
            requirePermission("local-files");
            File file = dataFile(stringAt(args, 1, ""));
            if (file == null) {
                return LuaValue.FALSE;
            }
            try {
                byte[] bytes = stringAt(args, 2, "").getBytes(StandardCharsets.UTF_8);
                ensureFileWriteAllowed(file, bytes.length, true);
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    parent.mkdirs();
                }
                try (FileOutputStream out = new FileOutputStream(file, true)) {
                    out.write(bytes);
                }
                return LuaValue.TRUE;
            } catch (Exception e) {
                return LuaValue.FALSE;
            }
        }));
        files.set("exists", new ValueFunction(args -> {
            requirePermission("local-files");
            File file = dataFile(stringAt(args, 1, ""));
            return file != null && file.exists() ? LuaValue.TRUE : LuaValue.FALSE;
        }));
        files.set("list", new ValueFunction(args -> {
            requirePermission("local-files");
            File dir = new File(LuaWidgetManager.widgetDir(id), "files");
            LuaTable table = new LuaTable();
            File[] filesInDir = dir.listFiles();
            if (filesInDir == null) {
                return table;
            }
            Arrays.sort(filesInDir, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            int index = 1;
            for (File file : filesInDir) {
                if (file.isFile()) {
                    table.set(index++, LuaValue.valueOf(file.getName()));
                }
            }
            return table;
        }));
        files.set("delete", new ValueFunction(args -> {
            requirePermission("local-files");
            File file = dataFile(stringAt(args, 1, ""));
            return file != null && (!file.exists() || file.delete()) ? LuaValue.TRUE : LuaValue.FALSE;
        }));
        return files;
    }

    private LuaTable buildJsonTable() {
        LuaTable json = new LuaTable();
        json.set("decode", new ValueFunction(args -> {
            String value = stringArg(args).trim();
            if (value.length() == 0) {
                return LuaValue.NIL;
            }
            try {
                if (value.startsWith("[")) {
                    return jsonToLua(new JSONArray(value));
                }
                return jsonToLua(new JSONObject(value));
            } catch (Exception e) {
                return LuaValue.NIL;
            }
        }));
        json.set("encode", new ValueFunction(args -> {
            try {
                Object object = luaToJson(args.arg(rawIndex(args, 1)));
                if (object == null || object == JSONObject.NULL) {
                    return LuaValue.valueOf("null");
                }
                if (object instanceof String) {
                    return LuaValue.valueOf(JSONObject.quote((String) object));
                }
                return LuaValue.valueOf(object.toString());
            } catch (Exception e) {
                return LuaValue.NIL;
            }
        }));
        return json;
    }

    private LuaTable buildHttpTable() {
        LuaTable http = new LuaTable();
        http.set("set_headers", new UiFunction(args -> {
            httpHeaders.clear();
            LuaValue table = tableArg(args);
            if (table.istable()) {
                int length = table.length();
                for (int i = 1; i <= length; i++) {
                    String header = table.get(i).tojstring();
                    int split = header.indexOf(':');
                    if (split > 0) {
                        httpHeaders.put(header.substring(0, split).trim(), header.substring(split + 1).trim());
                    }
                }
            }
        }));
        http.set("get", new UiFunction(args -> {
            requirePermission("network");
            enqueueHttp("GET", stringAt(args, 1, ""), null, null, stringAt(args, 2, ""));
        }));
        http.set("delete", new UiFunction(args -> {
            requirePermission("network");
            enqueueHttp("DELETE", stringAt(args, 1, ""), null, null, stringAt(args, 2, ""));
        }));
        http.set("post", new UiFunction(args -> {
            requirePermission("network");
            enqueueHttp("POST", stringAt(args, 1, ""), stringAt(args, 2, ""), stringAt(args, 3, ""), stringAt(args, 4, ""));
        }));
        http.set("put", new UiFunction(args -> {
            requirePermission("network");
            enqueueHttp("PUT", stringAt(args, 1, ""), stringAt(args, 2, ""), stringAt(args, 3, ""), stringAt(args, 4, ""));
        }));
        return http;
    }

    private LuaTable buildSystemTable() {
        LuaTable system = new LuaTable();
        system.set("open_browser", new UiFunction(args -> {
            openUrl(stringArg(args));
        }));
        system.set("open_url", new UiFunction(args -> {
            openUrl(stringArg(args));
        }));
        system.set("to_clipboard", new UiFunction(args -> {
            requirePermission("clipboard");
            copyToClipboard(stringArg(args));
        }));
        system.set("copy_to_clipboard", new UiFunction(args -> {
            requirePermission("clipboard");
            copyToClipboard(stringArg(args));
        }));
        system.set("clipboard", new ValueFunction(args -> {
            requirePermission("clipboard");
            return LuaValue.valueOf(readClipboard());
        }));
        system.set("vibrate", new UiFunction(args -> {
            requirePermission("vibrate");
            vibrate((long) numberAt(args, 1, 80));
        }));
        system.set("lang", new ValueFunction(args -> LuaValue.valueOf(Locale.getDefault().getLanguage())));
        system.set("tz", new ValueFunction(args -> LuaValue.valueOf(TimeZone.getDefault().getID())));
        system.set("tz_offset", new ValueFunction(args -> LuaValue.valueOf(TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000)));
        system.set("battery_info", new ValueFunction(args -> batteryInfo()));
        system.set("network_state", new ValueFunction(args -> networkState()));
        system.set("app_version", new ValueFunction(args -> LuaValue.valueOf(BuildConfig.VERSION_NAME)));
        system.set("app_version_code", new ValueFunction(args -> LuaValue.valueOf(BuildConfig.VERSION_CODE)));
        system.set("widget_id", new ValueFunction(args -> LuaValue.valueOf(id)));
        system.set("widget_name", new ValueFunction(args -> LuaValue.valueOf(LuaWidgetManager.getName(id))));
        return system;
    }

    private LuaTable buildAioTable() {
        LuaTable aio = new LuaTable();
        aio.set("self_name", new ValueFunction(args -> LuaValue.valueOf(id)));
        aio.set("widget_name", new ValueFunction(args -> LuaValue.valueOf(LuaWidgetManager.getName(id))));
        aio.set("retui_version", new ValueFunction(args -> LuaValue.valueOf(BuildConfig.VERSION_NAME)));
        aio.set("show_toast", new UiFunction(args -> {
            showToast(stringArg(args));
        }));
        aio.set("colors", new ValueFunction(args -> {
            LuaTable colors = new LuaTable();
            colors.set("primary_text", hex(AppearanceSettings.moduleNameTextColor()));
            colors.set("secondary_text", hex(Color.argb(190,
                    Color.red(AppearanceSettings.moduleNameTextColor()),
                    Color.green(AppearanceSettings.moduleNameTextColor()),
                    Color.blue(AppearanceSettings.moduleNameTextColor()))));
            colors.set("button", hex(AppearanceSettings.moduleButtonBackgroundColor()));
            colors.set("button_text", hex(AppearanceSettings.moduleNameTextColor()));
            colors.set("progress", hex(AppearanceSettings.moduleNameTextColor()));
            colors.set("accent", hex(AppearanceSettings.moduleButtonBorderColor()));
            return colors;
        }));
        return aio;
    }

    private LuaTable buildDebugTable() {
        LuaTable debug = new LuaTable();
        debug.set("log", new UiFunction(args -> {
            String text = stringArg(args);
            Log.d("ReTUI-Lua", id + ": " + text);
            debugLines.add(text);
            if (debugLines.size() > 50) {
                debugLines.remove(0);
            }
            lastResult.debug = new ArrayList<>(debugLines);
        }));
        debug.set("toast", new UiFunction(args -> showToast(stringArg(args))));
        debug.set("show", new UiFunction(args -> {
            if (debugLines.isEmpty()) {
                lastResult.body = appendLine(lastResult.body, "No debug lines.");
            } else {
                lastResult.body = appendLine(lastResult.body, TextUtils.join("\n", debugLines));
            }
        }));
        debug.set("clear", new UiFunction(args -> {
            debugLines.clear();
            lastResult.debug.clear();
        }));
        return debug;
    }

    private LuaTable buildDateTable() {
        LuaTable date = new LuaTable();
        date.set("now", new ValueFunction(args -> LuaValue.valueOf(System.currentTimeMillis())));
        date.set("seconds", new ValueFunction(args -> LuaValue.valueOf(System.currentTimeMillis() / 1000L)));
        date.set("format", new ValueFunction(args -> LuaValue.valueOf(osDate(stringAt(args, 1, "%Y-%m-%d %H:%M:%S"), numberAt(args, 2, System.currentTimeMillis() / 1000d)))));
        date.set("parts", new ValueFunction(args -> {
            Calendar calendar = Calendar.getInstance();
            LuaTable table = new LuaTable();
            table.set("year", LuaValue.valueOf(calendar.get(Calendar.YEAR)));
            table.set("month", LuaValue.valueOf(calendar.get(Calendar.MONTH) + 1));
            table.set("day", LuaValue.valueOf(calendar.get(Calendar.DAY_OF_MONTH)));
            table.set("hour", LuaValue.valueOf(calendar.get(Calendar.HOUR_OF_DAY)));
            table.set("minute", LuaValue.valueOf(calendar.get(Calendar.MINUTE)));
            table.set("second", LuaValue.valueOf(calendar.get(Calendar.SECOND)));
            table.set("weekday", LuaValue.valueOf(calendar.get(Calendar.DAY_OF_WEEK)));
            return table;
        }));
        return date;
    }

    private LuaTable buildFmtTable() {
        LuaTable fmt = new LuaTable();
        fmt.set("upper", new ValueFunction(args -> LuaValue.valueOf(stringArg(args).toUpperCase(Locale.US))));
        fmt.set("lower", new ValueFunction(args -> LuaValue.valueOf(stringArg(args).toLowerCase(Locale.US))));
        fmt.set("title", new ValueFunction(args -> LuaValue.valueOf(titleCase(stringArg(args)))));
        fmt.set("percent", new ValueFunction(args -> {
            double value = numberAt(args, 1, 0);
            double max = Math.max(1, numberAt(args, 2, 100));
            return LuaValue.valueOf(Math.round((value * 100d) / max) + "%");
        }));
        fmt.set("bytes", new ValueFunction(args -> LuaValue.valueOf(formatBytes(numberAt(args, 1, 0)))));
        fmt.set("round", new ValueFunction(args -> LuaValue.valueOf(Math.round(numberAt(args, 1, 0)))));
        fmt.set("fixed", new ValueFunction(args -> {
            double value = numberAt(args, 1, 0);
            int places = Math.max(0, Math.min(6, (int) numberAt(args, 2, 1)));
            return LuaValue.valueOf(String.format(Locale.US, "%." + places + "f", value));
        }));
        fmt.set("pad_left", new ValueFunction(args -> LuaValue.valueOf(pad(stringAt(args, 1, ""), (int) numberAt(args, 2, 0), true))));
        fmt.set("pad_right", new ValueFunction(args -> LuaValue.valueOf(pad(stringAt(args, 1, ""), (int) numberAt(args, 2, 0), false))));
        return fmt;
    }

    private LuaTable buildStringsTable() {
        LuaTable strings = new LuaTable();
        strings.set("trim", new ValueFunction(args -> LuaValue.valueOf(stringArg(args).trim())));
        strings.set("contains", new ValueFunction(args -> stringAt(args, 1, "").contains(stringAt(args, 2, "")) ? LuaValue.TRUE : LuaValue.FALSE));
        strings.set("starts_with", new ValueFunction(args -> stringAt(args, 1, "").startsWith(stringAt(args, 2, "")) ? LuaValue.TRUE : LuaValue.FALSE));
        strings.set("ends_with", new ValueFunction(args -> stringAt(args, 1, "").endsWith(stringAt(args, 2, "")) ? LuaValue.TRUE : LuaValue.FALSE));
        strings.set("replace", new ValueFunction(args -> LuaValue.valueOf(stringAt(args, 1, "").replace(stringAt(args, 2, ""), stringAt(args, 3, "")))));
        strings.set("split", new ValueFunction(args -> {
            String text = stringAt(args, 1, "");
            String delimiter = stringAt(args, 2, ",");
            LuaTable table = new LuaTable();
            String[] parts = text.split(java.util.regex.Pattern.quote(delimiter), -1);
            for (int i = 0; i < parts.length; i++) {
                table.set(i + 1, LuaValue.valueOf(parts[i]));
            }
            return table;
        }));
        strings.set("join", new ValueFunction(args -> LuaValue.valueOf(TextUtils.join(stringAt(args, 2, ","), tableStrings(tableArg(args))))));
        return strings;
    }

    private LuaTable buildColorsTable() {
        return colorsTable();
    }

    private LuaTable safeOsTable() {
        LuaTable safe = new LuaTable();
        safe.set("clock", new ValueFunction(args -> LuaValue.valueOf(System.nanoTime() / 1_000_000_000d)));
        safe.set("time", new ValueFunction(args -> LuaValue.valueOf(System.currentTimeMillis() / 1000L)));
        safe.set("difftime", new ValueFunction(args -> LuaValue.valueOf(numberAt(args, 1, 0) - numberAt(args, 2, 0))));
        safe.set("date", new ValueFunction(args -> LuaValue.valueOf(osDate(stringAt(args, 1, "%Y-%m-%d %H:%M:%S"), numberAt(args, 2, System.currentTimeMillis() / 1000d)))));
        return safe;
    }

    private void enqueueHttp(String method, String url, String body, String mediaType, String callbackId) {
        if (TextUtils.isEmpty(url)) {
            handleNetworkError(callbackId, "empty url");
            return;
        }
        try {
            Request.Builder builder = new Request.Builder().url(url);
            for (Map.Entry<String, String> header : httpHeaders.entrySet()) {
                builder.header(header.getKey(), header.getValue());
            }
            if ("POST".equals(method) || "PUT".equals(method)) {
                MediaType type = TextUtils.isEmpty(mediaType) ? TEXT_MEDIA_TYPE : MediaType.parse(mediaType);
                RequestBody requestBody = RequestBody.create(body == null ? "" : body, type == null ? TEXT_MEDIA_TYPE : type);
                if ("POST".equals(method)) {
                    builder.post(requestBody);
                } else {
                    builder.put(requestBody);
                }
            } else if ("DELETE".equals(method)) {
                builder.delete();
            } else {
                builder.get();
            }
            HTTP_CLIENT.newCall(builder.build()).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    handleNetworkError(callbackId, e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (ResponseBody responseBody = response.body()) {
                        String text = responseBody == null ? "" : readLimitedResponse(responseBody);
                        handleNetworkResult(callbackId, text, response.code(), response.headers());
                    } catch (IOException e) {
                        handleNetworkError(callbackId, e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            handleNetworkError(callbackId, e.getMessage());
        }
    }

    private void handleNetworkResult(String callbackId, String body, int code, Headers headers) {
        mainHandler.post(() -> {
            RenderResult result;
            synchronized (LuaWidgetEngine.this) {
                try {
                    ensureLoaded();
                    newResult();
                    String callback = callbackName("on_network_result", callbackId);
                    LuaTable headerTable = headersToLua(headers);
                    if (!callIfPresent(callback, LuaValue.valueOf(body), LuaValue.valueOf(code), headerTable)
                            && !"on_network_result".equals(callback)) {
                        callIfPresent("on_network_result", LuaValue.valueOf(body), LuaValue.valueOf(code), headerTable);
                    }
                    persistPrefs();
                } catch (Throwable e) {
                    lastResult = errorResult(e);
                }
                result = lastResult.copy();
            }
            notifyUpdate(result);
        });
    }

    private void handleNetworkError(String callbackId, String error) {
        mainHandler.post(() -> {
            RenderResult result;
            synchronized (LuaWidgetEngine.this) {
                try {
                    ensureLoaded();
                    newResult();
                    String callback = callbackName("on_network_error", callbackId);
                    if (!callIfPresent(callback, LuaValue.valueOf(error == null ? "network error" : error))
                            && !"on_network_error".equals(callback)) {
                        callIfPresent("on_network_error", LuaValue.valueOf(error == null ? "network error" : error));
                    }
                    if (TextUtils.isEmpty(lastResult.body) && lastResult.buttons.isEmpty()) {
                        lastResult.body = "Network error: " + (error == null ? "unknown" : error);
                    }
                    persistPrefs();
                } catch (Throwable e) {
                    lastResult = errorResult(e);
                }
                result = lastResult.copy();
            }
            notifyUpdate(result);
        });
    }

    private void notifyUpdate(RenderResult result) {
        if (updateListener != null) {
            updateListener.onUpdate(id, result.copy());
        }
    }

    private void requirePermission(String permission) {
        String normalized = permission == null ? "" : permission.trim().toLowerCase(Locale.US);
        if (!approvedPermissions.contains(normalized)) {
            throw new LuaError("Permission required: " + normalized
                    + ". Add -- permissions = \"" + normalized + "\" and run widget -approve " + id + ".");
        }
    }

    private static String readLimitedResponse(ResponseBody responseBody) throws IOException {
        long declaredLength = responseBody.contentLength();
        if (declaredLength > MAX_HTTP_RESPONSE_BYTES) {
            throw new IOException("response too large: " + declaredLength + " bytes");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream((int) Math.max(0, Math.min(declaredLength, MAX_HTTP_RESPONSE_BYTES)));
        byte[] buffer = new byte[8192];
        int total = 0;
        try (InputStream in = responseBody.byteStream()) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > MAX_HTTP_RESPONSE_BYTES) {
                    throw new IOException("response too large: " + total + " bytes");
                }
                out.write(buffer, 0, read);
            }
        }
        return out.toString(StandardCharsets.UTF_8.name());
    }

    private static String stringArg(Varargs args) {
        return stringAt(args, 1, "");
    }

    private static String stringAt(Varargs args, int logicalIndex, String fallback) {
        int rawIndex = rawIndex(args, logicalIndex);
        LuaValue value = args.arg(rawIndex);
        return value.isnil() ? fallback : value.tojstring();
    }

    private static double numberAt(Varargs args, int logicalIndex, double fallback) {
        int rawIndex = rawIndex(args, logicalIndex);
        LuaValue value = args.arg(rawIndex);
        return value.isnumber() ? value.todouble() : fallback;
    }

    private static boolean booleanAt(Varargs args, int logicalIndex, boolean fallback) {
        int rawIndex = rawIndex(args, logicalIndex);
        LuaValue value = args.arg(rawIndex);
        if (value.isnil()) {
            return fallback;
        }
        if (value.isboolean()) {
            return value.toboolean();
        }
        if (value.isnumber()) {
            return value.todouble() != 0d;
        }
        if (value.isstring()) {
            String text = value.tojstring().trim().toLowerCase(Locale.US);
            return "true".equals(text) || "yes".equals(text) || "1".equals(text) || "on".equals(text);
        }
        return fallback;
    }

    private static LuaValue tableArg(Varargs args) {
        return args.arg(rawIndex(args, 1));
    }

    private static int rawIndex(Varargs args, int logicalIndex) {
        if (args.narg() > logicalIndex && args.arg1().istable()) {
            return logicalIndex + 1;
        }
        return logicalIndex;
    }

    private static String tableLines(LuaValue table) {
        StringBuilder out = new StringBuilder();
        if (!table.istable()) {
            return table.isnil() ? "" : table.tojstring();
        }
        int length = table.length();
        for (int i = 1; i <= length; i++) {
            String value = table.get(i).tojstring();
            if (out.length() > 0) out.append('\n');
            out.append(value);
        }
        return out.toString();
    }

    private static List<String> tableStrings(LuaValue table) {
        ArrayList<String> values = new ArrayList<>();
        if (!table.istable()) {
            if (!table.isnil()) values.add(table.tojstring());
            return values;
        }
        int length = table.length();
        for (int i = 1; i <= length; i++) {
            LuaValue value = table.get(i);
            if (!value.isnil()) {
                values.add(value.tojstring());
            }
        }
        return values;
    }

    private void addValueAction(Varargs args) {
        String label = stringAt(args, 1, "");
        String value = stringAt(args, 2, label);
        if (!TextUtils.isEmpty(label)) {
            lastResult.valueActions.add(new RenderValueAction(label, value));
        }
    }

    private void showChoiceDialog(Varargs args) {
        String title = stringAt(args, 1, "Choose");
        LuaValue items = args.arg(rawIndex(args, 2));
        int selected = (int) numberAt(args, 3, -1);
        if (items.isnil() && args.arg(rawIndex(args, 1)).istable()) {
            LuaValue table = args.arg(rawIndex(args, 1));
            title = table.get("title").isnil() ? title : table.get("title").tojstring();
            items = table.get("items");
            if (items.isnil()) {
                items = table;
            }
            if (table.get("selected").isnumber()) {
                selected = table.get("selected").toint();
            }
        }
        List<String> values = tableStrings(items);
        lastResult.dialogOpen = true;
        lastResult.dialogTitle = title;
        lastResult.dialogItems = values;
        lastResult.dialogSelected = selected;
        if (!TextUtils.isEmpty(title)) {
            lastResult.body = appendLine(lastResult.body, title);
        }
    }

    private static String tableRows(LuaValue table) {
        if (!table.istable()) {
            return table.isnil() ? "" : table.tojstring();
        }
        StringBuilder out = new StringBuilder();
        int length = table.length();
        for (int i = 1; i <= length; i++) {
            LuaValue row = table.get(i);
            if (out.length() > 0) out.append('\n');
            if (row.istable()) {
                ArrayList<String> cells = new ArrayList<>();
                for (int j = 1; j <= row.length(); j++) {
                    cells.add(row.get(j).tojstring());
                }
                out.append(TextUtils.join(" | ", cells));
            } else {
                out.append(row.tojstring());
            }
        }
        return out.toString();
    }

    private static String tableKeyValues(LuaValue table) {
        if (!table.istable()) {
            return table.isnil() ? "" : table.tojstring();
        }
        StringBuilder out = new StringBuilder();
        LuaValue key = LuaValue.NIL;
        while (true) {
            Varargs next = table.next(key);
            key = next.arg1();
            if (key.isnil()) {
                break;
            }
            if (!key.isstring()) {
                continue;
            }
            if (out.length() > 0) out.append('\n');
            out.append(key.tojstring()).append(": ").append(next.arg(2).tojstring());
        }
        return out.toString();
    }

    private static String osDate(String pattern, double seconds) {
        return new java.text.SimpleDateFormat(convertLuaDatePattern(pattern), Locale.getDefault())
                .format(new java.util.Date((long) (seconds * 1000d)));
    }

    private static String convertLuaDatePattern(String pattern) {
        return pattern
                .replace("%Y", "yyyy")
                .replace("%y", "yy")
                .replace("%m", "MM")
                .replace("%d", "dd")
                .replace("%H", "HH")
                .replace("%M", "mm")
                .replace("%S", "ss");
    }

    private static String titleCase(String text) {
        StringBuilder out = new StringBuilder();
        boolean nextUpper = true;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c) || c == '_' || c == '-') {
                out.append(' ');
                nextUpper = true;
            } else if (nextUpper) {
                out.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                out.append(Character.toLowerCase(c));
            }
        }
        return out.toString().trim();
    }

    private static String pad(String text, int width, boolean left) {
        if (text == null) text = "";
        if (text.length() >= width) return text;
        StringBuilder spaces = new StringBuilder();
        for (int i = text.length(); i < width; i++) spaces.append(' ');
        return left ? spaces + text : text + spaces;
    }

    private static String formatBytes(double value) {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unit = 0;
        while (value >= 1024d && unit < units.length - 1) {
            value /= 1024d;
            unit++;
        }
        return String.format(Locale.US, unit == 0 ? "%.0f %s" : "%.1f %s", value, units[unit]);
    }

    private static String appendLine(String body, String line) {
        if (TextUtils.isEmpty(body)) {
            return line == null ? "" : line;
        }
        return body + "\n" + (line == null ? "" : line);
    }

    private static String errorMessage(Throwable e) {
        if (e instanceof LuaError && e.getMessage() != null) {
            return e.getMessage();
        }
        return e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage());
    }

    private RenderResult errorResult(Throwable e) {
        String message = errorMessage(e);
        String stage = TextUtils.isEmpty(executionStage) ? "script" : executionStage;
        RenderResult result = RenderResult.error(message, stage);
        LuaWidgetManager.saveLastError(id, stage, message);
        return result;
    }

    private LuaTable loadPrefs() {
        File file = prefsFile();
        if (!file.isFile()) {
            return new LuaTable();
        }
        try (FileInputStream in = new FileInputStream(file)) {
            String text = Tuils.convertStreamToString(in);
            return jsonToLua(new JSONObject(text));
        } catch (Exception e) {
            return new LuaTable();
        }
    }

    private void persistPrefs() {
        if (prefsTable == null) {
            return;
        }
        try {
            JSONObject object = luaTableToJsonObject(prefsTable);
            File file = prefsFile();
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                //noinspection ResultOfMethodCallIgnored
                parent.mkdirs();
            }
            try (FileOutputStream out = new FileOutputStream(file, false)) {
                out.write(object.toString(2).getBytes(StandardCharsets.UTF_8));
                out.write('\n');
            }
        } catch (Exception ignored) {
        }
    }

    private File prefsFile() {
        return new File(LuaWidgetManager.widgetDir(id), "prefs.json");
    }

    private boolean isExpandableState() {
        return prefsTable != null && prefsTable.get(PREF_EXPANDABLE).toboolean();
    }

    private boolean isExpandedState() {
        if (!isExpandableState()) {
            return true;
        }
        LuaValue value = prefsTable == null ? LuaValue.NIL : prefsTable.get(PREF_EXPANDED);
        return value.isnil() || value.toboolean();
    }

    private boolean isFoldedState() {
        return isExpandableState() && !isExpandedState();
    }

    private void setExpandableState(boolean expandable) {
        if (prefsTable == null) {
            return;
        }
        prefsTable.set(PREF_EXPANDABLE, expandable ? LuaValue.TRUE : LuaValue.FALSE);
        if (!expandable) {
            prefsTable.set(PREF_EXPANDED, LuaValue.TRUE);
        } else if (prefsTable.get(PREF_EXPANDED).isnil()) {
            prefsTable.set(PREF_EXPANDED, LuaValue.TRUE);
        }
        lastResult.expandable = expandable;
        lastResult.expanded = isExpandedState();
    }

    private void setExpandedState(boolean expanded) {
        if (prefsTable == null) {
            return;
        }
        prefsTable.set(PREF_EXPANDED, expanded ? LuaValue.TRUE : LuaValue.FALSE);
        lastResult.expanded = isExpandedState();
    }

    private void setTickInterval(double seconds) {
        if (seconds <= 0d) {
            tickIntervalMs = -1L;
        } else {
            requirePermission("active-tick");
            tickIntervalMs = Math.max(1000L, Math.min(60000L, Math.round(seconds * 1000d)));
        }
        lastResult.tickIntervalMs = tickIntervalMs;
    }

    private String prefsSummary() {
        ArrayList<String> lines = new ArrayList<>();
        LuaValue key = LuaValue.NIL;
        while (true) {
            Varargs next = prefsTable.next(key);
            key = next.arg1();
            if (key.isnil()) {
                break;
            }
            if (!key.isstring()) {
                continue;
            }
            String name = key.tojstring();
            if (name.startsWith("_") || "show_dialog".equals(name)) {
                continue;
            }
            LuaValue value = next.arg(2);
            if (value.isstring() || value.isnumber() || value.isboolean()) {
                lines.add(name + " = " + value.tojstring());
            }
        }
        return lines.isEmpty() ? "No editable prefs yet." : TextUtils.join("\n", lines);
    }

    private File dataFile(String name) {
        String safe = safeFileName(name);
        if (TextUtils.isEmpty(safe)) {
            return null;
        }
        return new File(new File(LuaWidgetManager.widgetDir(id), "files"), safe);
    }

    private void ensureReadableFile(File file) {
        if (file.length() > MAX_WIDGET_FILE_BYTES) {
            throw new LuaError("Widget local file is too large: " + file.getName());
        }
    }

    private void ensureFileWriteAllowed(File file, long incomingBytes, boolean append) {
        long existing = file.isFile() ? file.length() : 0L;
        long nextSize = append ? existing + incomingBytes : incomingBytes;
        if (nextSize > MAX_WIDGET_FILE_BYTES) {
            throw new LuaError("Widget local file limit exceeded: " + file.getName());
        }
        long total = widgetFilesSize() - existing + nextSize;
        if (total > MAX_WIDGET_FILES_TOTAL_BYTES) {
            throw new LuaError("Widget local storage limit exceeded");
        }
    }

    private long widgetFilesSize() {
        File dir = new File(LuaWidgetManager.widgetDir(id), "files");
        File[] files = dir.listFiles();
        if (files == null) {
            return 0L;
        }
        long total = 0L;
        for (File file : files) {
            if (file.isFile()) {
                total += file.length();
            }
        }
        return total;
    }

    private static String safeFileName(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        if (trimmed.length() == 0 || trimmed.contains("/") || trimmed.contains("\\") || trimmed.equals(".") || trimmed.equals("..")) {
            return "";
        }
        return trimmed.replaceAll("[^a-zA-Z0-9._ -]", "_");
    }

    private static LuaTable jsonToLua(JSONObject object) {
        LuaTable table = new LuaTable();
        JSONArray names = object.names();
        if (names == null) {
            return table;
        }
        for (int i = 0; i < names.length(); i++) {
            String key = names.optString(i, "");
            if (key.length() > 0) {
                table.set(key, jsonToLuaValue(object.opt(key)));
            }
        }
        return table;
    }

    private static LuaTable jsonToLua(JSONArray array) {
        LuaTable table = new LuaTable();
        for (int i = 0; i < array.length(); i++) {
            table.set(i + 1, jsonToLuaValue(array.opt(i)));
        }
        return table;
    }

    private static LuaValue jsonToLuaValue(Object value) {
        if (value == null || value == JSONObject.NULL) return LuaValue.NIL;
        if (value instanceof JSONObject) return jsonToLua((JSONObject) value);
        if (value instanceof JSONArray) return jsonToLua((JSONArray) value);
        if (value instanceof Boolean) return (Boolean) value ? LuaValue.TRUE : LuaValue.FALSE;
        if (value instanceof Number) return LuaValue.valueOf(((Number) value).doubleValue());
        return LuaValue.valueOf(String.valueOf(value));
    }

    private static Object luaToJson(LuaValue value) throws Exception {
        if (value == null || value.isnil()) return JSONObject.NULL;
        if (value.isboolean()) return value.toboolean();
        if (value.isnumber()) return value.todouble();
        if (value.isstring()) return value.tojstring();
        if (value.istable()) {
            LuaTable table = (LuaTable) value;
            if (isArrayTable(table)) {
                JSONArray array = new JSONArray();
                int length = table.length();
                for (int i = 1; i <= length; i++) {
                    array.put(luaToJson(table.get(i)));
                }
                return array;
            }
            return luaTableToJsonObject(table);
        }
        return value.tojstring();
    }

    private static JSONObject luaTableToJsonObject(LuaTable table) throws Exception {
        JSONObject object = new JSONObject();
        LuaValue key = LuaValue.NIL;
        while (true) {
            Varargs next = table.next(key);
            key = next.arg1();
            if (key.isnil()) {
                break;
            }
            LuaValue value = next.arg(2);
            if (key.isstring() && !value.isfunction()
                    && !value.isuserdata() && !value.isthread()) {
                object.put(key.tojstring(), luaToJson(value));
            }
        }
        return object;
    }

    private static boolean isArrayTable(LuaTable table) {
        int length = table.length();
        if (length <= 0) {
            return false;
        }
        LuaValue key = LuaValue.NIL;
        while (true) {
            Varargs next = table.next(key);
            key = next.arg1();
            if (key.isnil()) {
                return true;
            }
            if (!key.isnumber() || key.toint() < 1 || key.toint() > length || key.todouble() != key.toint()) {
                return false;
            }
        }
    }

    private LuaTable headersToLua(Headers headers) {
        LuaTable table = new LuaTable();
        if (headers == null) {
            return table;
        }
        for (String name : headers.names()) {
            table.set(name.toLowerCase(Locale.US), headers.values(name).isEmpty()
                    ? "" : TextUtils.join(", ", headers.values(name)));
        }
        return table;
    }

    private static String callbackName(String base, String callbackId) {
        if (TextUtils.isEmpty(callbackId)) {
            return base;
        }
        return base + "_" + callbackId.trim().replaceAll("[^A-Za-z0-9_]", "_");
    }

    private void showToast(String text) {
        if (context == null || TextUtils.isEmpty(text)) {
            return;
        }
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    private void openUrl(String url) {
        if (context == null || TextUtils.isEmpty(url)) {
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            showToast("Cannot open URL");
        }
    }

    private void copyToClipboard(String text) {
        if (context == null) {
            return;
        }
        ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager != null) {
            manager.setPrimaryClip(ClipData.newPlainText("Re:TUI widget", text == null ? "" : text));
        }
    }

    private String readClipboard() {
        if (context == null) {
            return "";
        }
        ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager == null || !manager.hasPrimaryClip() || manager.getPrimaryClip() == null
                || manager.getPrimaryClip().getItemCount() == 0) {
            return "";
        }
        CharSequence text = manager.getPrimaryClip().getItemAt(0).coerceToText(context);
        return text == null ? "" : text.toString();
    }

    private void vibrate(long millis) {
        if (context == null || millis <= 0) {
            return;
        }
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //noinspection deprecation
            vibrator.vibrate(millis);
        }
    }

    private LuaTable batteryInfo() {
        LuaTable table = new LuaTable();
        if (context == null) {
            return table;
        }
        Intent battery = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery == null) {
            return table;
        }
        int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        table.set("level", LuaValue.valueOf(level));
        table.set("scale", LuaValue.valueOf(scale));
        table.set("percent", LuaValue.valueOf(scale <= 0 ? level : Math.round((level * 100f) / scale)));
        table.set("charging", status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL ? LuaValue.TRUE : LuaValue.FALSE);
        table.set("plugged", LuaValue.valueOf(plugged));
        return table;
    }

    private LuaTable networkState() {
        LuaTable table = new LuaTable();
        table.set("connected", LuaValue.FALSE);
        table.set("type", LuaValue.valueOf("none"));
        table.set("class", LuaValue.valueOf(""));
        table.set("ssid", LuaValue.valueOf(""));
        table.set("operator", LuaValue.valueOf(""));
        table.set("metered", LuaValue.FALSE);
        table.set("roaming", LuaValue.FALSE);
        if (context == null) {
            return table;
        }
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return table;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = manager.getActiveNetwork();
                NetworkCapabilities capabilities = network == null ? null : manager.getNetworkCapabilities(network);
                if (capabilities == null) {
                    return table;
                }
                table.set("connected", LuaValue.TRUE);
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    table.set("type", LuaValue.valueOf("wifi"));
                    table.set("class", LuaValue.valueOf("WiFi"));
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    table.set("type", LuaValue.valueOf("mobile"));
                    table.set("class", LuaValue.valueOf(""));
                }
                table.set("metered", manager.isActiveNetworkMetered() ? LuaValue.TRUE : LuaValue.FALSE);
            } else {
                //noinspection deprecation
                android.net.NetworkInfo info = manager.getActiveNetworkInfo();
                if (info != null && info.isConnected()) {
                    table.set("connected", LuaValue.TRUE);
                    table.set("type", LuaValue.valueOf(info.getType() == ConnectivityManager.TYPE_WIFI ? "wifi" : "mobile"));
                    table.set("class", LuaValue.valueOf(info.getTypeName()));
                    table.set("metered", manager.isActiveNetworkMetered() ? LuaValue.TRUE : LuaValue.FALSE);
                    table.set("roaming", info.isRoaming() ? LuaValue.TRUE : LuaValue.FALSE);
                }
            }
        } catch (Exception ignored) {
        }
        return table;
    }

    private static String hex(int color) {
        return String.format(Locale.US, "#%06X", 0xFFFFFF & color);
    }

    private static LuaTable colorsTable() {
        LuaTable colors = new LuaTable();
        colors.set("primary_text", hex(AppearanceSettings.moduleNameTextColor()));
        colors.set("secondary_text", hex(Color.argb(190,
                Color.red(AppearanceSettings.moduleNameTextColor()),
                Color.green(AppearanceSettings.moduleNameTextColor()),
                Color.blue(AppearanceSettings.moduleNameTextColor()))));
        colors.set("button", hex(AppearanceSettings.moduleButtonBackgroundColor()));
        colors.set("button_text", hex(AppearanceSettings.moduleNameTextColor()));
        colors.set("progress", hex(AppearanceSettings.moduleNameTextColor()));
        colors.set("accent", hex(AppearanceSettings.moduleButtonBorderColor()));
        return colors;
    }

    public static final class RenderResult {
        public String title = "";
        public String body = "";
        public String error = "";
        public String errorStage = "";
        public double progress = -1;
        public boolean expandable;
        public boolean expanded = true;
        public long tickIntervalMs = -1L;
        public List<String> buttons = new ArrayList<>();
        public List<RenderAction> commands = new ArrayList<>();
        public List<RenderValueAction> valueActions = new ArrayList<>();
        public boolean dialogOpen;
        public String dialogTitle = "";
        public List<String> dialogItems = new ArrayList<>();
        public int dialogSelected = -1;
        public List<String> debug = new ArrayList<>();

        static RenderResult error(String message) {
            return error(message, "");
        }

        static RenderResult error(String message, String stage) {
            RenderResult result = new RenderResult();
            result.error = message == null ? "unknown error" : message;
            result.errorStage = stage == null ? "" : stage;
            return result;
        }

        RenderResult copy() {
            RenderResult copy = new RenderResult();
            copy.title = title;
            copy.body = body;
            copy.error = error;
            copy.errorStage = errorStage;
            copy.progress = progress;
            copy.expandable = expandable;
            copy.expanded = expanded;
            copy.tickIntervalMs = tickIntervalMs;
            copy.buttons = new ArrayList<>(buttons);
            copy.commands = new ArrayList<>(commands);
            copy.valueActions = new ArrayList<>(valueActions);
            copy.dialogOpen = dialogOpen;
            copy.dialogTitle = dialogTitle;
            copy.dialogItems = new ArrayList<>(dialogItems);
            copy.dialogSelected = dialogSelected;
            copy.debug = new ArrayList<>(debug);
            return copy;
        }
    }

    public static final class RenderAction {
        public final String label;
        public final String command;

        RenderAction(String label, String command) {
            this.label = label;
            this.command = command;
        }
    }

    public static final class RenderValueAction {
        public final String label;
        public final String value;

        RenderValueAction(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }

    private interface UiAction {
        void run(Varargs args);
    }

    private interface ValueAction {
        LuaValue run(Varargs args);
    }

    private static final class UiFunction extends VarArgFunction {
        private final UiAction action;

        UiFunction(UiAction action) {
            this.action = action;
        }

        @Override
        public Varargs invoke(Varargs args) {
            action.run(args);
            return LuaValue.NONE;
        }
    }

    private static final class ValueFunction extends VarArgFunction {
        private final ValueAction action;

        ValueFunction(ValueAction action) {
            this.action = action;
        }

        @Override
        public Varargs invoke(Varargs args) {
            LuaValue result = action.run(args);
            return result == null ? LuaValue.NIL : result;
        }
    }

    private final class GuardedDebugLib extends DebugLib {
        @Override
        public void onInstruction(int pc, Varargs v, int top) {
            checkTimeout();
            super.onInstruction(pc, v, top);
        }
    }

    private final class RequireFunction extends VarArgFunction {
        @Override
        public Varargs invoke(Varargs args) {
            String name = stringArg(args);
            if ("prefs".equals(name)) {
                return prefsTable;
            }
            if ("json".equals(name)) {
                return globals.get("json");
            }
            if ("date".equals(name) || "fmt".equals(name) || "strings".equals(name)
                    || "colors".equals(name) || "debug".equals(name) || "files".equals(name)
                    || "http".equals(name) || "system".equals(name) || "ui".equals(name)
                    || "suggest".equals(name) || "aio".equals(name)) {
                return globals.get(name);
            }
            return LuaValue.error("module not found: " + name);
        }
    }

    public interface UpdateListener {
        void onUpdate(String widgetId, RenderResult result);
    }
}
