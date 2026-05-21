package ohi.andre.consolelauncher.managers.modules;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import ohi.andre.consolelauncher.managers.settings.AppearanceSettings;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Theme;
import ohi.andre.consolelauncher.tuils.NetUtils;
import ohi.andre.consolelauncher.tuils.Tuils;

public final class ModuleVariableManager {

    public static final String TOKEN_CALENDAR_UPCOMING_MONTH = "%RETUI_CALENDAR_UPCOMING_MONTH";
    public static final String TOKEN_BATTERY_JSON = "%RETUI_BATTERY_JSON";
    public static final String TOKEN_NETWORK_JSON = "%RETUI_NETWORK_JSON";
    public static final String TOKEN_BRIGHTNESS_JSON = "%RETUI_BRIGHTNESS_JSON";
    public static final String TOKEN_THEME_JSON = "%RETUI_THEME_JSON";
    public static final String TOKEN_UI_JSON = "%RETUI_UI_JSON";
    public static final String TOKEN_STORAGE_JSON = "%RETUI_STORAGE_JSON";
    public static final String TOKEN_NOW = "%RETUI_NOW";

    private ModuleVariableManager() {}

    public static Materialized materialize(Context context, String module) {
        long now = System.currentTimeMillis();
        File dir = new File(Tuils.getFolder(), ".retui/module-vars/" + safeName(module));
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }

        LinkedHashMap<String, String> replacements = new LinkedHashMap<>();
        replacements.put(TOKEN_CALENDAR_UPCOMING_MONTH,
                writeFile(dir, "calendar_upcoming_month.tsv", UpcomingEventsManager.formatUpcomingTsv(context)));
        replacements.put(TOKEN_BATTERY_JSON, writeFile(dir, "battery.json", batteryJson(context)));
        replacements.put(TOKEN_NETWORK_JSON, writeFile(dir, "network.json", networkJson(context)));
        replacements.put(TOKEN_BRIGHTNESS_JSON, writeFile(dir, "brightness.json", brightnessJson(context)));
        replacements.put(TOKEN_THEME_JSON, writeFile(dir, "theme.json", themeJson()));
        replacements.put(TOKEN_UI_JSON, writeFile(dir, "ui.json", uiJson()));
        replacements.put(TOKEN_STORAGE_JSON, writeFile(dir, "storage.json", storageJson()));
        replacements.put(TOKEN_NOW, String.valueOf(now));

        return new Materialized(dir, replacements);
    }

    private static String safeName(String module) {
        String value = ModuleManager.normalize(module);
        return TextUtils.isEmpty(value) ? "module" : value;
    }

    private static String writeFile(File dir, String name, String text) {
        File file = new File(dir, name);
        try (FileOutputStream out = new FileOutputStream(file, false)) {
            out.write((text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
        return file.getAbsolutePath();
    }

    private static String batteryJson(Context context) {
        Intent battery = context.getApplicationContext().registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = battery == null ? -1 : battery.getIntExtra("level", -1);
        int scale = battery == null ? -1 : battery.getIntExtra("scale", -1);
        int status = battery == null ? -1 : battery.getIntExtra("status", -1);
        int percent = scale > 0 && level >= 0 ? Math.round(level * 100f / scale) : -1;
        return "{"
                + "\"percent\":" + percent + ","
                + "\"status\":" + status
                + "}\n";
    }

    private static String networkJson(Context context) {
        boolean connected = false;
        boolean wifi = false;
        try {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo active = manager == null ? null : manager.getActiveNetworkInfo();
            connected = active != null && active.isConnected();
            wifi = active != null && active.getType() == ConnectivityManager.TYPE_WIFI;
        } catch (Exception ignored) {
        }
        return "{"
                + "\"connected\":" + connected + ","
                + "\"wifi\":" + wifi + ","
                + "\"mobile_type\":\"" + escapeJson(NetUtils.getNetworkType(context)) + "\""
                + "}\n";
    }

    private static String brightnessJson(Context context) {
        int brightness = -1;
        int mode = -1;
        try {
            brightness = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            brightness = Math.round(brightness * 100f / 255f);
        } catch (Exception ignored) {
        }
        try {
            mode = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);
        } catch (Exception ignored) {
        }
        return "{"
                + "\"percent\":" + brightness + ","
                + "\"auto\":" + (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
                + "}\n";
    }

    private static String themeJson() {
        return "{"
                + "\"bg\":\"" + color(Theme.bg_color) + "\","
                + "\"output\":\"" + color(Theme.output_color) + "\","
                + "\"input\":\"" + color(Theme.input_color) + "\""
                + "}\n";
    }

    private static String uiJson() {
        return "{"
                + "\"module_corner_radius\":" + AppearanceSettings.moduleCornerRadius() + ","
                + "\"header_corner_radius\":" + AppearanceSettings.headerCornerRadius() + ","
                + "\"output_corner_radius\":" + AppearanceSettings.outputCornerRadius() + ","
                + "\"module_header_text_size\":" + AppearanceSettings.moduleHeaderTextSize() + ","
                + "\"module_body_text_size\":" + AppearanceSettings.moduleBodyTextSize() + ","
                + "\"output_header_text_size\":" + AppearanceSettings.outputHeaderTextSize()
                + "}\n";
    }

    private static String storageJson() {
        try {
            StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
            long total = stat.getBlockCountLong() * stat.getBlockSizeLong();
            long free = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
            long used = total - free;
            return "{"
                    + "\"total_bytes\":" + total + ","
                    + "\"free_bytes\":" + free + ","
                    + "\"used_bytes\":" + used
                    + "}\n";
        } catch (Exception e) {
            return "{\"total_bytes\":0,\"free_bytes\":0,\"used_bytes\":0}\n";
        }
    }

    private static String color(Theme theme) {
        return String.format(Locale.US, "#%08X", XMLPrefsManager.getColor(theme));
    }

    private static String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static final class Materialized {
        public final File directory;
        public final LinkedHashMap<String, String> replacements;

        Materialized(File directory, LinkedHashMap<String, String> replacements) {
            this.directory = directory;
            this.replacements = replacements;
        }

        public Map<String, String> asMap() {
            return replacements;
        }
    }
}
