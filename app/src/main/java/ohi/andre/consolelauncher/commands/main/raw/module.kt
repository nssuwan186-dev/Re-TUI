package ohi.andre.consolelauncher.commands.main.raw

import android.content.Intent
import android.text.TextUtils
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.UIManager
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack
import ohi.andre.consolelauncher.managers.modules.ModuleManager
import ohi.andre.consolelauncher.managers.modules.ModulePromptManager
import ohi.andre.consolelauncher.tuils.Tuils
import java.util.Arrays
import java.util.Locale
import java.util.ArrayList

class module : CommandAbstraction {
    override fun exec(pack: ExecutePack): String? {
        val arg = pack.get(Any::class.java, 0)
        val input = if (arg == null) "" else arg.toString().trim { it <= ' ' }
        if (input.length == 0 || "-ls".equals(input, ignoreCase = true)) {
            return listModules(pack)
        }

        val parts: Array<String?> =
            input.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val option = parts[0]!!.lowercase(Locale.getDefault())

        if ("-show" == option || "-open" == option) {
            if (parts.size < 2) return pack.context.getString(R.string.help_module)
            val module = ModuleManager.normalize(parts[1])
            if (!ModuleManager.isKnown(pack.context, module)) {
                return "Unknown module: " + parts[1]
            }
            send(pack, "show", module)
            return "Module opened: " + module
        }

        if ("-close" == option) {
            send(pack, "close", null)
            return "Module closed."
        }

        if ("-prompt" == option) {
            if (parts.size < 3) return pack.context.getString(R.string.help_module)
            val module = ModuleManager.normalize(parts[1])
            val action = parts[2]!!.lowercase(Locale.getDefault())
            if (ModuleManager.REMINDER != module) {
                return "No native prompt session for module: " + module
            }
            send(pack, "show", ModuleManager.REMINDER)
            if ("add" == action || "-add" == action) {
                ModulePromptManager.startReminderAdd(pack.context)
                return "Reminder prompt started."
            }
            if ("edit" == action || "-edit" == action) {
                ModulePromptManager.startReminderEdit(pack.context)
                return "Reminder edit prompt started."
            }
            if ("remove" == action || "rm" == action || "-rm" == action) {
                ModulePromptManager.startReminderRemove(pack.context)
                return "Reminder remove prompt started."
            }
            return pack.context.getString(R.string.output_invalid_param) + " " + parts[2]
        }

        if ("-hide" == option) {
            if (parts.size < 2) return pack.context.getString(R.string.help_module)
            ModuleManager.hideFromDock(pack.context, parts[1])
            send(pack, "rebuild", null)
            return "Module hidden from dock: " + ModuleManager.normalize(parts[1])
        }

        if ("-add" == option) {
            val args = Tuils.splitArgs(input)
            if (args.size < 3) return pack.context.getString(R.string.help_module)
            val module = ModuleManager.normalize(args.get(1))
            val path = args.get(2)
            ModuleManager.setScriptModule(pack.context, module, path)
            ModuleManager.addToDock(pack.context, Arrays.asList<String?>(module))
            send(pack, "rebuild", null)
            if (ModuleManager.isLauncherSource(
                    ModuleManager.getModuleSource(
                        pack.context,
                        module
                    )
                )
            ) {
                send(pack, "refresh", module)
            }
            return ("Module added: " + module
                    + "\nSource: " + ModuleManager.getModuleSource(pack.context, module)
                    + "\nRun module -refresh " + module + " to update it.")
        }

        if ("-refresh" == option) {
            if (parts.size < 2) return pack.context.getString(R.string.help_module)
            val module = ModuleManager.normalize(parts[1])
            if (!ModuleManager.isKnown(pack.context, module)) {
                return "Unknown module: " + parts[1]
            }
            if (TextUtils.isEmpty(ModuleManager.getModuleSource(pack.context, module))) {
                return "Module has no source: " + module
            }
            send(pack, "refresh", module)
            return "Module refresh dispatched: " + module
        }

        if ("-rm" == option || "-remove" == option) {
            if (parts.size < 2) return pack.context.getString(R.string.help_module)
            val module = ModuleManager.normalize(parts[1])
            if (!ModuleManager.isKnown(pack.context, module)) {
                return "Unknown module: " + parts[1]
            }
            if (ModuleManager.builtIns.contains(module)) {
                return "Built-in modules cannot be removed. Use module -hide " + module + " instead."
            }
            ModuleManager.removeScriptModule(pack.context, module)
            send(pack, "rebuild", null)
            return "Module removed from registry: " + module
        }

        if ("-dock" == option) {
            if (parts.size < 3) return pack.context.getString(R.string.help_module)
            val mode = parts[1]!!.lowercase(Locale.getDefault())
            val verb: String?
            if ("add" == mode || "-add" == mode) {
                verb = "added"
            } else if ("remove" == mode || "-remove" == mode || "rm" == mode || "-rm" == mode) {
                verb = "removed"
            } else {
                return (pack.context.getString(R.string.output_invalid_param) + " " + parts[1]
                        + "\nUse module -dock add [name] or module -dock remove [name].")
            }

            val modules: MutableList<String?> =
                ArrayList<String?>(Arrays.asList<String?>(*parts).subList(2, parts.size))
            if ("added" == verb) {
                ModuleManager.addToDock(pack.context, modules)
            } else {
                ModuleManager.removeFromDock(pack.context, modules)
            }
            send(pack, "rebuild", null)
            return "Module dock " + verb + ": " + formatDock(pack)
        }

        return pack.context.getString(R.string.output_invalid_param) + " " + parts[0]
    }

    private fun listModules(pack: ExecutePack): String {
        return ("Modules: " + TextUtils.join(", ", ModuleManager.listAll(pack.context))
                + "\nDock: " + formatDock(pack)
                + "\nUse module -add [name] termux:/path/script.sh, module -refresh [name], module -show [name], events -access, module -prompt reminder add|edit|remove, module -hide [name], module -dock add [name], module -dock remove [name], module -rm [name], module -close.")
    }

    private fun formatDock(pack: ExecutePack): String? {
        val dock = ModuleManager.getDock(pack.context)
        return if (dock.isEmpty()) "<empty>" else TextUtils.join(", ", dock)
    }

    private fun send(pack: ExecutePack, command: String?, module: String?) {
        val intent = Intent(UIManager.ACTION_MODULE_COMMAND)
        intent.putExtra(UIManager.EXTRA_MODULE_COMMAND, command)
        if (module != null) {
            intent.putExtra(UIManager.EXTRA_MODULE_NAME, module)
        }
        LocalBroadcastManager.getInstance(pack.context.getApplicationContext())
            .sendBroadcast(intent)
    }

    override fun argType(): IntArray? {
        return intArrayOf(CommandAbstraction.PLAIN_TEXT)
    }

    override fun priority(): Int {
        return 3
    }

    override fun helpRes(): Int {
        return R.string.help_module
    }

    override fun onArgNotFound(pack: ExecutePack, index: Int): String? {
        return pack.context.getString(R.string.help_module)
    }

    override fun onNotArgEnough(pack: ExecutePack, nArgs: Int): String {
        return listModules(pack)
    }
}
