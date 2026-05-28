package ohi.andre.consolelauncher.commands.main.raw

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack
import ohi.andre.consolelauncher.commands.main.MainPack
import ohi.andre.consolelauncher.managers.settings.AppearanceSettings
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager
import ohi.andre.consolelauncher.managers.xml.options.Theme
import ohi.andre.consolelauncher.managers.xml.options.Ui
import ohi.andre.consolelauncher.tuils.Tuils

class files : CommandAbstraction {
    override fun exec(info: ExecutePack): String? {
        var command: String? = null
        val args = info.args
        if (args != null && args.isNotEmpty()) {
            val arg = info.get()
            if (arg != null) {
                command = arg.toString()
            }
        }

        val intent = Intent(FM_ACTION)
        intent.setPackage(FM_PACKAGE)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (info is MainPack && info.currentDirectory != null) {
            intent.putExtra("path", info.currentDirectory.absolutePath)
        }
        if (command != null && command.trim().isNotEmpty()) {
            intent.putExtra("command", command)
        }

        val terminalSurfaceColor = terminalSurfaceColor()
        val terminalHeaderColor = AppearanceSettings.terminalHeaderTabBackground()
        val terminalBorderColor = AppearanceSettings.terminalBorderColor()
        val outputSurfaceColor = ColorUtils.blendARGB(terminalSurfaceColor, Color.BLACK, 0.10f)
        val inputSurfaceColor = ColorUtils.blendARGB(terminalSurfaceColor, Color.BLACK, 0.16f)

        intent.putExtra("theme_bg", XMLPrefsManager.getColor(Theme.background_color))
        intent.putExtra("theme_text", XMLPrefsManager.getColor(Theme.output_text_color))
        intent.putExtra("theme_border", terminalBorderColor)
        intent.putExtra("terminal_bg", terminalSurfaceColor)
        intent.putExtra("terminal_window_background_color", terminalSurfaceColor)
        intent.putExtra("module_bg_color", terminalSurfaceColor)
        intent.putExtra("module_text_color", AppearanceSettings.moduleNameTextColor())
        intent.putExtra("module_border_color", terminalBorderColor)
        intent.putExtra("module_header_bg_color", terminalHeaderColor)
        intent.putExtra("module_header_text_color", AppearanceSettings.moduleNameTextColor())
        intent.putExtra("module_button_bg_color", AppearanceSettings.moduleButtonBackgroundColor())
        intent.putExtra("module_button_background_color", AppearanceSettings.moduleButtonBackgroundColor())
        intent.putExtra("module_button_text_color", AppearanceSettings.moduleNameTextColor())
        intent.putExtra("module_button_border_color", terminalBorderColor)
        intent.putExtra("input_bg_color", inputSurfaceColor)
        intent.putExtra("input_background_color", inputSurfaceColor)
        intent.putExtra("input_text_color", XMLPrefsManager.getColor(Theme.input_text_color))
        intent.putExtra("output_bg_color", outputSurfaceColor)
        intent.putExtra("output_background_color", outputSurfaceColor)
        intent.putExtra("output_text_color", XMLPrefsManager.getColor(Theme.output_text_color))
        intent.putExtra("output_border_color", terminalBorderColor)
        intent.putExtra("top_margin", 18)
        intent.putExtra("input_font_size", XMLPrefsManager.getInt(Ui.input_output_size))
        val topDisplayMargin = XMLPrefsManager.get(Ui.display_margin_top_section)
        intent.putExtra("display_margin_mm", topDisplayMargin)
        intent.putExtra("display_margin_top_section", topDisplayMargin)
        intent.putExtra("display_margin_bottom_section", XMLPrefsManager.get(Ui.display_margin_bottom_section))
        intent.putExtra("module_corner_radius", AppearanceSettings.moduleCornerRadius())
        intent.putExtra("header_corner_radius", AppearanceSettings.headerCornerRadius())
        intent.putExtra("output_corner_radius", AppearanceSettings.outputCornerRadius())
        intent.putExtra("module_header_text_size", AppearanceSettings.moduleHeaderTextSize())
        intent.putExtra("module_body_text_size", AppearanceSettings.moduleBodyTextSize())
        intent.putExtra("output_header_text_size", AppearanceSettings.outputHeaderTextSize())
        intent.putExtra("enable_cyberdeck_mode", AppearanceSettings.cyberdeckMode())
        intent.putExtra("cyberdeck_mode", AppearanceSettings.cyberdeckMode())
        intent.putExtra("enable_crt_filter", AppearanceSettings.crtFilter())
        intent.putExtra("crt_filter", AppearanceSettings.crtFilter())

        Tuils.getTypeface(info.context)
        val fontPath = Tuils.fontPath
        if (fontPath != null && fontPath.startsWith("/")) {
            intent.putExtra("font_path", fontPath)
        } else if (AppearanceSettings.useSystemFont()) {
            intent.putExtra("font_name", "system")
        } else {
            intent.putExtra("font_name", "lucida_console")
        }

        try {
            info.context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            return "Re:T-UI Files is not installed."
        }
        return null
    }

    private fun terminalSurfaceColor(): Int {
        val terminalBg = AppearanceSettings.terminalWindowBackground()
        if (Color.alpha(terminalBg) > 0) {
            return terminalBg
        }
        val outputBg = XMLPrefsManager.getColor(Theme.output_background_color)
        return if (Color.alpha(outputBg) > 0) outputBg else terminalBg
    }

    override fun argType(): IntArray = IntArray(0)

    override fun priority(): Int = 4

    override fun helpRes(): Int = R.string.help_files

    override fun onArgNotFound(info: ExecutePack, indexNotFound: Int): String =
        info.context.getString(R.string.help_files)

    override fun onNotArgEnough(info: ExecutePack, nArgs: Int): String? = exec(info)

    companion object {
        private const val FM_PACKAGE = "com.dvil.retui.fm"
        private const val FM_ACTION = "com.dvil.retui.fm.OPEN_CONSOLE"
    }
}
