package ohi.andre.consolelauncher.managers.modules

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import ohi.andre.consolelauncher.managers.settings.AppearanceSettings.headerCornerRadius
import ohi.andre.consolelauncher.managers.settings.AppearanceSettings.moduleBodyTextSize
import ohi.andre.consolelauncher.managers.settings.AppearanceSettings.moduleCornerRadius
import ohi.andre.consolelauncher.managers.settings.AppearanceSettings.moduleHeaderTextSize
import ohi.andre.consolelauncher.managers.settings.AppearanceSettings.outputCornerRadius
import ohi.andre.consolelauncher.managers.settings.AppearanceSettings.outputHeaderTextSize
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager
import ohi.andre.consolelauncher.managers.xml.options.Theme
import ohi.andre.consolelauncher.tuils.NetUtils.getNetworkType
import ohi.andre.consolelauncher.tuils.Tuils
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.Locale
import android.net.NetworkInfo
import android.text.TextUtils
import java.util.LinkedHashMap
import java.util.Map
import ohi.andre.consolelauncher.managers.settings.AppearanceSettings
import ohi.andre.consolelauncher.tuils.NetUtils

object ModuleVariableManager {
    const val TOKEN_CALENDAR_UPCOMING_MONTH: String = "%RETUI_CALENDAR_UPCOMING_MONTH"
    const val TOKEN_BATTERY_JSON: String = "%RETUI_BATTERY_JSON"
    const val TOKEN_NETWORK_JSON: String = "%RETUI_NETWORK_JSON"
    const val TOKEN_BRIGHTNESS_JSON: String = "%RETUI_BRIGHTNESS_JSON"
    const val TOKEN_THEME_JSON: String = "%RETUI_THEME_JSON"
    const val TOKEN_UI_JSON: String = "%RETUI_UI_JSON"
    const val TOKEN_STORAGE_JSON: String = "%RETUI_STORAGE_JSON"
    const val TOKEN_NOW: String = "%RETUI_NOW"

    @JvmStatic
    fun materialize(context: Context, module: String?): Materialized {
        val now = System.currentTimeMillis()
        val dir = File(Tuils.getFolder(), ".retui/module-vars/" + safeName(module))
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val replacements = LinkedHashMap<String?, String?>()
        replacements.put(
            TOKEN_CALENDAR_UPCOMING_MONTH,
            writeFile(
                dir,
                "calendar_upcoming_month.tsv",
                UpcomingEventsManager.formatUpcomingTsv(context)
            )
        )
        replacements.put(TOKEN_BATTERY_JSON, writeFile(dir, "battery.json", batteryJson(context)))
        replacements.put(TOKEN_NETWORK_JSON, writeFile(dir, "network.json", networkJson(context)))
        replacements.put(
            TOKEN_BRIGHTNESS_JSON,
            writeFile(dir, "brightness.json", brightnessJson(context))
        )
        replacements.put(TOKEN_THEME_JSON, writeFile(dir, "theme.json", themeJson()))
        replacements.put(TOKEN_UI_JSON, writeFile(dir, "ui.json", uiJson()))
        replacements.put(TOKEN_STORAGE_JSON, writeFile(dir, "storage.json", storageJson()))
        replacements.put(TOKEN_NOW, now.toString())

        return Materialized(dir, replacements)
    }

    private fun safeName(module: String?): String {
        val value = ModuleManager.normalize(module)
        return (if (android.text.TextUtils.isEmpty(value)) "module" else value)!!
    }

    private fun writeFile(dir: File?, name: String, text: String?): String {
        val file = File(dir, name)
        try {
            FileOutputStream(file, false).use { out ->
                out.write(
                    (if (text == null) "" else text).toByteArray(
                        StandardCharsets.UTF_8
                    )
                )
            }
        } catch (ignored: Exception) {
        }
        return file.getAbsolutePath()
    }

    private fun batteryJson(context: Context): String {
        val battery = context.getApplicationContext().registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = if (battery == null) -1 else battery.getIntExtra("level", -1)
        val scale = if (battery == null) -1 else battery.getIntExtra("scale", -1)
        val status = if (battery == null) -1 else battery.getIntExtra("status", -1)
        val percent = if (scale > 0 && level >= 0) Math.round(level * 100f / scale) else -1
        return ("{"
                + "\"percent\":" + percent + ","
                + "\"status\":" + status
                + "}\n")
    }

    private fun networkJson(context: Context): String {
        var connected = false
        var wifi = false
        try {
            val manager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            val active = if (manager == null) null else manager.getActiveNetworkInfo()
            connected = active != null && active.isConnected()
            wifi = active != null && active.getType() == ConnectivityManager.TYPE_WIFI
        } catch (ignored: Exception) {
        }
        return ("{"
                + "\"connected\":" + connected + ","
                + "\"wifi\":" + wifi + ","
                + "\"mobile_type\":\"" + escapeJson(getNetworkType(context)) + "\""
                + "}\n")
    }

    private fun brightnessJson(context: Context): String {
        var brightness = -1
        var mode = -1
        try {
            brightness = Settings.System.getInt(
                context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS
            )
            brightness = Math.round(brightness * 100f / 255f)
        } catch (ignored: Exception) {
        }
        try {
            mode = Settings.System.getInt(
                context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE
            )
        } catch (ignored: Exception) {
        }
        return ("{"
                + "\"percent\":" + brightness + ","
                + "\"auto\":" + (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
                + "}\n")
    }

    private fun themeJson(): String {
        return ("{"
                + "\"bg\":\"" + color(Theme.bg_color) + "\","
                + "\"output\":\"" + color(Theme.output_color) + "\","
                + "\"input\":\"" + color(Theme.input_color) + "\""
                + "}\n")
    }

    private fun uiJson(): String {
        return ("{"
                + "\"module_corner_radius\":" + moduleCornerRadius() + ","
                + "\"header_corner_radius\":" + headerCornerRadius() + ","
                + "\"output_corner_radius\":" + outputCornerRadius() + ","
                + "\"module_header_text_size\":" + moduleHeaderTextSize() + ","
                + "\"module_body_text_size\":" + moduleBodyTextSize() + ","
                + "\"output_header_text_size\":" + outputHeaderTextSize()
                + "}\n")
    }

    private fun storageJson(): String {
        try {
            val stat = StatFs(Environment.getExternalStorageDirectory().getAbsolutePath())
            val total = stat.getBlockCountLong() * stat.getBlockSizeLong()
            val free = stat.getAvailableBlocksLong() * stat.getBlockSizeLong()
            val used = total - free
            return ("{"
                    + "\"total_bytes\":" + total + ","
                    + "\"free_bytes\":" + free + ","
                    + "\"used_bytes\":" + used
                    + "}\n")
        } catch (e: Exception) {
            return "{\"total_bytes\":0,\"free_bytes\":0,\"used_bytes\":0}\n"
        }
    }

    private fun color(theme: Theme): String {
        return String.format(Locale.US, "#%08X", XMLPrefsManager.getColor(theme))
    }

    private fun escapeJson(value: String?): String {
        return if (value == null) "" else value.replace("\\", "\\\\").replace("\"", "\\\"")
    }

    class Materialized internal constructor(
        val directory: File?,
        val replacements: LinkedHashMap<String?, String?>?
    ) {
        fun asMap(): MutableMap<String?, String?>? {
            return replacements
        }
    }
}
