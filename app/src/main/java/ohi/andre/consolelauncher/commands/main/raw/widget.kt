package ohi.andre.consolelauncher.commands.main.raw

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.UIManager
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack
import ohi.andre.consolelauncher.commands.tuixt.WidgetEditorActivity
import ohi.andre.consolelauncher.managers.modules.ModuleManager
import ohi.andre.consolelauncher.managers.widgets.LuaWidgetEngine
import ohi.andre.consolelauncher.managers.widgets.LuaWidgetManager
import ohi.andre.consolelauncher.managers.widgets.LuaWidgetManager.TrustStatus
import ohi.andre.consolelauncher.tuils.Tuils
import java.util.Arrays
import java.util.Locale

class widget : CommandAbstraction {
    @Throws(Exception::class)
    override fun exec(pack: ExecutePack): String? {
        val arg = pack.get(Any::class.java, 0)
        val input = if (arg == null) "" else arg.toString().trim { it <= ' ' }
        if (input.length == 0 || "-ls".equals(input, ignoreCase = true)) {
            return listWidgets(pack)
        }

        val args = Tuils.splitArgs(input)
        val option = args.get(0)!!.lowercase(Locale.getDefault())

        if ("-new" == option || "-add" == option) {
            if (args.size < 2) return pack.context.getString(R.string.help_widget)
            val requestedName = args.get(1)
            val id = LuaWidgetManager.idFromName(requestedName)
            if (TextUtils.isEmpty(id)) return "Invalid widget id."
            if (!LuaWidgetManager.exists(id)) {
                LuaWidgetManager.save(
                    id,
                    if (args.size > 2) args.get(2) else requestedName,
                    LuaWidgetManager.newWidgetTemplate(id)
                )
            }
            openEditor(pack, id)
            return "Widget created: " + formatWidget(id)
        }

        if ("-edit" == option) {
            if (args.size < 2) return pack.context.getString(R.string.help_widget)
            val id = LuaWidgetManager.normalizeId(args.get(1))
            if (TextUtils.isEmpty(id)) return "Invalid widget id."
            if (!LuaWidgetManager.exists(id)) {
                LuaWidgetManager.save(id, id, LuaWidgetManager.newWidgetTemplate(id))
            }
            openEditor(pack, id)
            return "Opening widget editor: " + formatWidget(id)
        }

        if ("-show" == option) {
            if (args.size < 2) return pack.context.getString(R.string.help_widget)
            val id = LuaWidgetManager.normalizeId(args.get(1))
            if (!LuaWidgetManager.exists(id)) return "Unknown widget: " + id
            if (!LuaWidgetManager.isDockable(id)) return "Script is not a dock widget: " + formatWidget(
                id
            )
            if (!LuaWidgetManager.isEnabled(id)) return "Widget disabled: " + formatWidget(id) + "\nUse widget -enable " + id + "."
            ModuleManager.setScriptModule(pack.context, id, LuaWidgetManager.SOURCE_PREFIX + id)
            ModuleManager.addToDock(pack.context, Arrays.asList<String?>(id))
            send(pack, "show", id, 0)
            return "Widget opened: " + formatWidget(id)
        }

        if ("-refresh" == option) {
            if (args.size < 2) return pack.context.getString(R.string.help_widget)
            val id = LuaWidgetManager.normalizeId(args.get(1))
            if (!LuaWidgetManager.exists(id)) return "Unknown widget: " + id
            if (!LuaWidgetManager.isDockable(id)) return "Script is not a dock widget: " + formatWidget(
                id
            )
            if (!LuaWidgetManager.isEnabled(id)) return "Widget disabled: " + formatWidget(id) + "\nUse widget -enable " + id + "."
            ModuleManager.setScriptModule(pack.context, id, LuaWidgetManager.SOURCE_PREFIX + id)
            send(pack, "refresh", id, 0)
            return "Widget refresh dispatched: " + formatWidget(id)
        }

        if ("-check" == option) {
            if (args.size < 2) return pack.context.getString(R.string.help_widget)
            val id = LuaWidgetManager.normalizeId(args.get(1))
            if (!LuaWidgetManager.exists(id)) return "Unknown widget: " + id
            val trust = LuaWidgetManager.trustStatus(id)
            if (!trust.trusted) {
                return trustSummary("Widget check blocked", id, trust)
            }
            val engine = LuaWidgetEngine(
                pack.context,
                id,
                LuaWidgetManager.readScript(id),
                LuaWidgetManager.version(id),
                null
            )
            val result = engine.render(true)
            if (!TextUtils.isEmpty(result.error)) {
                return ("Widget check failed: " + formatWidget(id)
                        + (if (TextUtils.isEmpty(result.errorStage)) "" else "\nStage: " + result.errorStage)
                        + "\n" + result.error
                        + "\nUse widget -copy-error " + id + " or widget -edit " + id + ".")
            }
            return ("Widget check OK: " + formatWidget(id)
                    + "\nType: " + LuaWidgetManager.getScriptType(id)
                    + "\nCapabilities: " + LuaWidgetManager.describeCapabilities(
                LuaWidgetManager.readScript(
                    id
                )
            )
                    + "\nPermissions: " + LuaWidgetManager.describeRequiredPermissions(
                LuaWidgetManager.readScript(id)
            )
                    + "\nTrust: approved"
                    + "\nRuntime: API " + LuaWidgetManager.apiVersion(id)
                    + "\nTitle: " + (if (TextUtils.isEmpty(result.title)) LuaWidgetManager.getName(
                id
            ) else result.title)
                    + "\nActions: " + result.buttons.size
                    + "\nCommands: " + result.commands.size)
        }

        if ("-info" == option) {
            if (args.size < 2) return pack.context.getString(R.string.help_widget)
            val id = LuaWidgetManager.normalizeId(args.get(1))
            if (!LuaWidgetManager.exists(id)) return "Unknown widget: " + id
            val meta = LuaWidgetManager.metadata(LuaWidgetManager.readScript(id))
            val trust = LuaWidgetManager.trustStatus(id)
            return ("Widget: " + formatWidget(id)
                    + "\nType: " + LuaWidgetManager.getScriptType(id)
                    + "\nCapabilities: " + LuaWidgetManager.describeCapabilities(
                LuaWidgetManager.readScript(
                    id
                )
            )
                    + "\nPermissions: " + LuaWidgetManager.describeRequiredPermissions(
                LuaWidgetManager.readScript(id)
            )
                    + "\nTrust: " + (if (trust.trusted) "approved" else "needs approval")
                    + "\nRuntime: API " + LuaWidgetManager.apiVersion(id)
                    + "\nState: " + (if (LuaWidgetManager.isEnabled(id)) "enabled" else "disabled")
                    + "\nDescription: " + valueOr(meta.get("description"), "none")
                    + "\nAuthor: " + valueOr(meta.get("author"), "unknown")
                    + "\nVersion: " + valueOr(meta.get("version"), "none"))
        }

        if ("-approve" == option || "-trust" == option) {
            if (args.size < 2) return pack.context.getString(R.string.help_widget)
            val id = LuaWidgetManager.normalizeId(args.get(1))
            LuaWidgetManager.approve(id)
            if (LuaWidgetManager.isDockable(id)) {
                ModuleManager.setScriptModule(pack.context, id, LuaWidgetManager.SOURCE_PREFIX + id)
                send(pack, "update", id, 0)
            }
            return ("Lua widget approved: " + formatWidget(id)
                    + "\nPermissions: " + LuaWidgetManager.describeRequiredPermissions(
                LuaWidgetManager.readScript(id)
            ))
        }

        if ("-copy-error" == option) {
            if (args.size < 2) return pack.context.getString(R.string.help_widget)
            val id = LuaWidgetManager.normalizeId(args.get(1))
            if (!LuaWidgetManager.exists(id)) return "Unknown widget: " + id
            val error = LuaWidgetManager.lastError(id)
            if (TextUtils.isEmpty(error)) return "No saved Lua error: " + formatWidget(id)
            copyToClipboard(pack.context, error)
            return "Lua error copied: " + formatWidget(id)
        }

        if ("-disable" == option) {
            if (args.size < 2) return pack.context.getString(R.string.help_widget)
            val id = LuaWidgetManager.normalizeId(args.get(1))
            LuaWidgetManager.setEnabled(id, false)
            ModuleManager.removeFromDock(pack.context, Arrays.asList<String?>(id))
            send(pack, "rebuild", null, 0)
            return "Widget disabled: " + formatWidget(id)
        }

        if ("-enable" == option) {
            if (args.size < 2) return pack.context.getString(R.string.help_widget)
            val id = LuaWidgetManager.normalizeId(args.get(1))
            LuaWidgetManager.setEnabled(id, true)
            if (LuaWidgetManager.isDockable(id)) {
                ModuleManager.setScriptModule(pack.context, id, LuaWidgetManager.SOURCE_PREFIX + id)
            }
            send(pack, "rebuild", null, 0)
            return "Widget enabled: " + formatWidget(id)
        }

        if ("-export" == option) {
            if (args.size < 2) return pack.context.getString(R.string.help_widget)
            val id = LuaWidgetManager.normalizeId(args.get(1))
            val exported = LuaWidgetManager.exportPackage(id)
            copyToClipboard(pack.context, exported)
            return "Widget package copied to clipboard: " + formatWidget(id)
        }

        if ("-rename" == option || "-mv" == option) {
            if (args.size < 3) return pack.context.getString(R.string.help_widget)
            val oldId = LuaWidgetManager.normalizeId(args.get(1))
            val newId = LuaWidgetManager.idFromName(args.get(2))
            if (TextUtils.isEmpty(newId)) return "Invalid widget id."
            if (!LuaWidgetManager.exists(oldId)) return "Unknown widget: " + oldId
            if (TextUtils.equals(oldId, newId)) return "Widget id unchanged: " + formatWidget(oldId)
            if (ModuleManager.isKnown(pack.context, newId) || LuaWidgetManager.exists(newId)) {
                return "Widget id already exists: " + newId
            }

            val oldLabel = formatWidget(oldId)
            LuaWidgetManager.rename(oldId, newId)
            ModuleManager.renameScriptModule(
                pack.context,
                oldId,
                newId,
                LuaWidgetManager.SOURCE_PREFIX + newId
            )
            if (LuaWidgetManager.isDockable(newId)) {
                ModuleManager.setScriptModule(
                    pack.context,
                    newId,
                    LuaWidgetManager.SOURCE_PREFIX + newId
                )
            } else {
                ModuleManager.removeScriptModule(pack.context, newId)
            }
            send(pack, "rebuild", null, 0)
            return "Widget id changed: " + oldLabel + " -> " + formatWidget(newId)
        }

        if ("-click" == option) {
            if (args.size < 3) return pack.context.getString(R.string.help_widget)
            val id = LuaWidgetManager.normalizeId(args.get(1))
            if (!LuaWidgetManager.exists(id)) return "Unknown widget: " + id
            if (!LuaWidgetManager.isDockable(id)) return "Script is not a dock widget: " + formatWidget(
                id
            )
            if (!LuaWidgetManager.isEnabled(id)) return "Widget disabled: " + formatWidget(id)
            val index: Int
            try {
                index = args.get(2)!!.toInt()
            } catch (e: Exception) {
                return "Invalid widget action index: " + args.get(2)
            }
            send(pack, "lua_click", id, index)
            return null
        }

        if ("-action" == option || "-send" == option || "-input" == option) {
            if (args.size < 3) return pack.context.getString(R.string.help_widget)
            val id = LuaWidgetManager.normalizeId(args.get(1))
            if (!LuaWidgetManager.exists(id)) return "Unknown widget: " + id
            if (!LuaWidgetManager.isDockable(id)) return "Script is not a dock widget: " + formatWidget(
                id
            )
            if (!LuaWidgetManager.isEnabled(id)) return "Widget disabled: " + formatWidget(id)
            send(pack, "lua_action", id, 0, TextUtils.join(" ", args.subList(2, args.size)))
            return null
        }

        if ("-dialog" == option) {
            if (args.size < 3) return pack.context.getString(R.string.help_widget)
            val id = LuaWidgetManager.normalizeId(args.get(1))
            if (!LuaWidgetManager.exists(id)) return "Unknown widget: " + id
            if (!LuaWidgetManager.isDockable(id)) return "Script is not a dock widget: " + formatWidget(
                id
            )
            if (!LuaWidgetManager.isEnabled(id)) return "Widget disabled: " + formatWidget(id)
            val index: Int
            try {
                index = args.get(2)!!.toInt()
            } catch (e: Exception) {
                return "Invalid widget dialog index: " + args.get(2)
            }
            send(pack, "lua_dialog", id, index)
            return null
        }

        if ("-expand" == option || "-collapse" == option || "-toggle" == option) {
            if (args.size < 2) return pack.context.getString(R.string.help_widget)
            val id = LuaWidgetManager.normalizeId(args.get(1))
            if (!LuaWidgetManager.exists(id)) return "Unknown widget: " + id
            if (!LuaWidgetManager.isDockable(id)) return "Script is not a dock widget: " + formatWidget(
                id
            )
            if (!LuaWidgetManager.isEnabled(id)) return "Widget disabled: " + formatWidget(id)
            val command = if ("-expand" == option)
                "lua_expand"
            else
                if ("-collapse" == option) "lua_collapse" else "lua_toggle"
            send(pack, command, id, 0)
            return null
        }

        if ("-rm" == option || "-remove" == option) {
            if (args.size < 2) return pack.context.getString(R.string.help_widget)
            val id = LuaWidgetManager.normalizeId(args.get(1))
            val label = formatWidget(id)
            LuaWidgetManager.delete(id)
            ModuleManager.removeScriptModule(pack.context, id)
            send(pack, "rebuild", null, 0)
            return "Widget removed: " + label
        }

        return pack.context.getString(R.string.output_invalid_param) + " " + args.get(0)
    }

    private fun listWidgets(pack: ExecutePack): String {
        val ids = LuaWidgetManager.listIds()
        return ("Lua widgets: " + (if (ids.isEmpty()) "none" else formatWidgets(ids))
                + "\nUse widget -add [name], widget -new [name], widget -edit [id], widget -rename [old] [new], widget -show [id], widget -refresh [id], widget -check [id], widget -info [id], widget -approve [id], widget -copy-error [id], widget -disable|-enable [id], widget -export [id], widget -expand|-collapse|-toggle [id], widget -rm [id].")
    }

    private fun formatWidgets(ids: MutableList<String?>): String {
        val out = StringBuilder()
        for (id in ids) {
            if (out.length > 0) out.append(", ")
            out.append(formatWidget(id))
        }
        return out.toString()
    }

    private fun formatWidget(id: String?): String? {
        val label = LuaWidgetManager.getName(id)
        return if (TextUtils.equals(label, id)) id else label + " (" + id + ")"
    }

    private fun valueOr(value: String?, fallback: String?): String? {
        return if (TextUtils.isEmpty(value)) fallback else value
    }

    private fun trustSummary(prefix: String, id: String?, trust: TrustStatus): String {
        val out = StringBuilder(prefix).append(": ").append(formatWidget(id))
        out.append("\nPermissions: ")
            .append(
                if (trust.requiredPermissions.isEmpty()) "none" else TextUtils.join(
                    ", ",
                    trust.requiredPermissions
                )
            )
        if (!trust.missingDeclarations.isEmpty()) {
            out.append("\nDeclare first: ").append(TextUtils.join(", ", trust.missingDeclarations))
        }
        if (!trust.unsupportedPermissions.isEmpty()) {
            out.append("\nUnsupported: ").append(TextUtils.join(", ", trust.unsupportedPermissions))
        }
        if (trust.canApprove()) {
            out.append("\nUse widget -approve ").append(LuaWidgetManager.normalizeId(id))
                .append(" to allow this script.")
        } else {
            out.append("\nEdit the script metadata before approval.")
        }
        return out.toString()
    }

    private fun openEditor(pack: ExecutePack, id: String?) {
        val intent = Intent(pack.context, WidgetEditorActivity::class.java)
        intent.putExtra(WidgetEditorActivity.EXTRA_WIDGET_ID, id)
        (pack.context as Activity).startActivity(intent)
    }

    private fun send(
        pack: ExecutePack,
        command: String?,
        module: String?,
        actionIndex: Int,
        actionValue: String? = null
    ) {
        val intent = Intent(UIManager.ACTION_MODULE_COMMAND)
        intent.putExtra(UIManager.EXTRA_MODULE_COMMAND, command)
        if (module != null) {
            intent.putExtra(UIManager.EXTRA_MODULE_NAME, module)
        }
        if (actionIndex != 0) {
            intent.putExtra(UIManager.EXTRA_WIDGET_ACTION_INDEX, actionIndex)
        }
        if (actionValue != null) {
            intent.putExtra(UIManager.EXTRA_WIDGET_ACTION_VALUE, actionValue)
        }
        LocalBroadcastManager.getInstance(pack.context.getApplicationContext())
            .sendBroadcast(intent)
    }

    private fun copyToClipboard(context: Context, text: String?) {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        if (manager != null) {
            manager.setPrimaryClip(
                ClipData.newPlainText(
                    "Re:TUI widget package",
                    if (text == null) "" else text
                )
            )
        }
    }

    override fun argType(): IntArray? {
        return intArrayOf(CommandAbstraction.PLAIN_TEXT)
    }

    override fun priority(): Int {
        return 3
    }

    override fun helpRes(): Int {
        return R.string.help_widget
    }

    override fun onArgNotFound(pack: ExecutePack, indexNotFound: Int): String? {
        return pack.context.getString(R.string.help_widget)
    }

    override fun onNotArgEnough(pack: ExecutePack, nArgs: Int): String {
        return listWidgets(pack)
    }
}
