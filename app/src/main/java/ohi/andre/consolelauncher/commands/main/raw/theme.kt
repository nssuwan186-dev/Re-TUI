package ohi.andre.consolelauncher.commands.main.raw

import android.content.Intent
import android.graphics.Color
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.util.Locale
import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack
import ohi.andre.consolelauncher.commands.main.MainPack
import ohi.andre.consolelauncher.commands.main.specific.ParamCommand
import ohi.andre.consolelauncher.managers.PresetManager
import ohi.andre.consolelauncher.managers.ThemeManager
import ohi.andre.consolelauncher.managers.settings.LauncherSettings
import ohi.andre.consolelauncher.managers.xml.options.Theme
import ohi.andre.consolelauncher.tuils.Tuils
import ohi.andre.consolelauncher.tuils.interfaces.Reloadable

class theme : ParamCommand() {
    private enum class Param : ohi.andre.consolelauncher.commands.main.Param {
        apply {
            override fun args(): IntArray = intArrayOf(CommandAbstraction.PLAIN_TEXT)

            override fun exec(pack: ExecutePack): String? {
                val intent = Intent(ThemeManager.ACTION_APPLY)
                intent.putExtra(ThemeManager.NAME, pack.getString())
                LocalBroadcastManager.getInstance(pack.context.applicationContext).sendBroadcast(intent)
                return null
            }
        },
        set {
            override fun args(): IntArray = intArrayOf(CommandAbstraction.CONFIG_ENTRY, CommandAbstraction.PLAIN_TEXT)

            override fun exec(pack: ExecutePack): String {
                val value = pack.get()
                if (value !is Theme) {
                    return "Invalid theme element."
                }
                val element = value
                val color = pack.getString()!!

                try {
                    Color.parseColor(color)
                } catch (e: Exception) {
                    return "Invalid color format. Use #RRGGBB or #AARRGGBB"
                }

                LauncherSettings.setTheme(element, color)

                try {
                    if (pack.context is Reloadable) {
                        (pack.context as Reloadable).reload()
                    }
                } catch (ignored: Exception) {
                }

                return element.label() + " updated to " + color
            }
        },
        preset {
            override fun args(): IntArray = intArrayOf(CommandAbstraction.THEME_PRESET)

            override fun exec(pack: ExecutePack): String {
                val name = pack.getString()!!.lowercase(Locale.getDefault())
                if (!PresetManager.applyBuiltIn(name)) {
                    return "Unknown preset. Available: " + Tuils.toPlanString(PresetManager.listBuiltInPresets(), ", ")
                }

                try {
                    if (pack.context is Reloadable) {
                        (pack.context as Reloadable).reload()
                    }
                } catch (ignored: Exception) {
                }

                return "Applied $name preset!"
            }
        },
        standard {
            override fun args(): IntArray = IntArray(0)

            override fun exec(pack: ExecutePack): String? {
                LocalBroadcastManager.getInstance(pack.context.applicationContext)
                    .sendBroadcast(Intent(ThemeManager.ACTION_STANDARD))
                return null
            }
        },
        old {
            override fun exec(pack: ExecutePack): String? {
                LocalBroadcastManager.getInstance(pack.context.applicationContext)
                    .sendBroadcast(Intent(ThemeManager.ACTION_REVERT))
                return null
            }
        };

        override fun label(): String = Tuils.MINUS + name

        override fun onNotArgEnough(pack: ExecutePack, n: Int): String =
            pack.context.getString(R.string.help_theme)

        override fun onArgNotFound(pack: ExecutePack, index: Int): String? = null

        override fun args(): IntArray = IntArray(0)

        companion object {
            fun get(p: String): Param? {
                val value = p.lowercase(Locale.getDefault())
                for (p1 in entries) {
                    if (value.endsWith(p1.label())) {
                        return p1
                    }
                }
                return null
            }

            fun labels(): Array<String> {
                val ps = entries
                val ss = Array(ps.size) { "" }

                for (count in ps.indices) {
                    ss[count] = ps[count].label()
                }

                return ss
            }
        }
    }

    override fun params(): Array<String> = Param.labels()

    override fun paramForString(pack: MainPack, param: String): ohi.andre.consolelauncher.commands.main.Param? =
        Param.get(param)

    override fun priority(): Int = 4

    override fun helpRes(): Int = R.string.help_theme

    override fun doThings(pack: ExecutePack): String? = null
}
