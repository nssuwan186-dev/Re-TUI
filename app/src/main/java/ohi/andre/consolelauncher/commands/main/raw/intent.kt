package ohi.andre.consolelauncher.commands.main.raw

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack
import ohi.andre.consolelauncher.tuils.Tuils
import java.net.URISyntaxException
import java.util.ArrayList

class intent : CommandAbstraction {
    private enum class Mode {
        VIEW,
        ACTIVITY,
        BROADCAST,
        URI,
        CHECK
    }

    override fun exec(pack: ExecutePack): String? {
        val input = pack.getString()
        val args = Tuils.splitArgs(input)
        if (args.isEmpty()) {
            return pack.context.getString(helpRes())
        }

        val mode = modeFor(args.removeAt(0))
        if (mode == null) {
            return pack.context.getString(R.string.output_invalidarg)
        }

        try {
            val built = buildIntent(mode, args)
            if (built == null) {
                return pack.context.getString(helpRes())
            }

            if (mode == Mode.CHECK) {
                return checkIntent(pack, built)
            }

            if (mode == Mode.BROADCAST) {
                if (built.getAction() == null) {
                    return "intent -broadcast requires -a <action>"
                }
                if (built.getPackage() == null && built.getComponent() == null && !args.contains("--unsafe-implicit")) {
                    return "intent -broadcast requires -p or -n. Add --unsafe-implicit to send a broad implicit broadcast."
                }
                pack.context.sendBroadcast(built)
                return "Broadcast sent: " + built.getAction()
            }

            pack.context.startActivity(built)
            return Tuils.EMPTYSTRING
        } catch (e: ActivityNotFoundException) {
            return "No activity found for intent."
        } catch (e: SecurityException) {
            return "Intent blocked by Android security: " + e.message
        } catch (e: IllegalArgumentException) {
            return "Invalid intent: " + e.message
        } catch (e: URISyntaxException) {
            return "Invalid intent: " + e.message
        }
    }

    private fun modeFor(value: String?): Mode? {
        if ("-view".equals(value, ignoreCase = true)) return Mode.VIEW
        if ("-activity".equals(value, ignoreCase = true)) return Mode.ACTIVITY
        if ("-broadcast".equals(value, ignoreCase = true)) return Mode.BROADCAST
        if ("-uri".equals(value, ignoreCase = true)) return Mode.URI
        if ("-check".equals(value, ignoreCase = true)) return Mode.CHECK
        return null
    }

    @Throws(URISyntaxException::class)
    private fun buildIntent(mode: Mode?, args: MutableList<String?>): Intent? {
        if (mode == Mode.VIEW) {
            if (args.isEmpty()) return null
            return Intent(Intent.ACTION_VIEW, Uri.parse(args.get(0)))
        }

        if (mode == Mode.URI) {
            if (args.isEmpty()) return null
            return Intent.parseUri(args.get(0), Intent.URI_INTENT_SCHEME)
        }

        val built = Intent()
        if (mode == Mode.CHECK && !args.isEmpty()) {
            val nested = modeFor(args.get(0))
            if (nested != null && nested != Mode.CHECK) {
                args.removeAt(0)
                return buildIntent(nested, args)
            }
        }

        var data: String? = null
        var type: String? = null
        var hasPayload = false

        var i = 0
        while (i < args.size) {
            val arg = args.get(i)
            if ("--unsafe-implicit" == arg) {
                i++
                continue
            }
            if ("-a" == arg) {
                built.setAction(requiredValue(args, ++i, "-a"))
                hasPayload = true
            } else if ("-d" == arg) {
                data = requiredValue(args, ++i, "-d")
                hasPayload = true
            } else if ("-t" == arg) {
                type = requiredValue(args, ++i, "-t")
                hasPayload = true
            } else if ("-p" == arg) {
                built.setPackage(requiredValue(args, ++i, "-p"))
                hasPayload = true
            } else if ("-n" == arg) {
                built.setComponent(parseComponent(requiredValue(args, ++i, "-n")!!))
                hasPayload = true
            } else if ("--es" == arg) {
                built.putExtra(
                    requiredValue(args, ++i, "--es key"),
                    requiredValue(args, ++i, "--es value")
                )
                hasPayload = true
            } else if ("--ei" == arg) {
                built.putExtra(
                    requiredValue(args, ++i, "--ei key"),
                    requiredValue(args, ++i, "--ei value")!!.toInt()
                )
                hasPayload = true
            } else if ("--ez" == arg) {
                built.putExtra(
                    requiredValue(args, ++i, "--ez key"),
                    requiredValue(args, ++i, "--ez value").toBoolean()
                )
                hasPayload = true
            } else {
                throw IllegalArgumentException("unknown option " + arg)
            }
            i++
        }

        if (data != null && type != null) {
            built.setDataAndType(Uri.parse(data), type)
        } else if (data != null) {
            built.setData(Uri.parse(data))
        } else if (type != null) {
            built.setType(type)
        }

        return if (hasPayload) built else null
    }

    private fun requiredValue(args: MutableList<String?>, index: Int, option: String?): String? {
        require(index < args.size) { option + " needs a value" }
        return args.get(index)
    }

    private fun parseComponent(value: String): ComponentName {
        val component: ComponentName = ComponentName.unflattenFromString(value)!!
        requireNotNull(component) { "invalid component " + value }
        return component
    }

    private fun checkIntent(pack: ExecutePack, built: Intent): String {
        val pm = pack.context.getPackageManager()
        val infos: MutableList<ResolveInfo> = ArrayList<ResolveInfo>()
        infos.addAll(pm.queryIntentActivities(built, PackageManager.MATCH_DEFAULT_ONLY))
        if (built.getAction() != null) {
            infos.addAll(pm.queryBroadcastReceivers(built, PackageManager.MATCH_DEFAULT_ONLY))
        }

        if (infos.isEmpty()) {
            return "No handlers found. Android package visibility may hide some targets."
        }

        val out = StringBuilder()
        for (info in infos) {
            val label = info.loadLabel(pm)
            val name = if (label == null) "Unknown" else label.toString()
            val pkg = if (info.activityInfo != null)
                info.activityInfo.packageName
            else
                if (info.serviceInfo != null) info.serviceInfo.packageName else null
            val cls = if (info.activityInfo != null)
                info.activityInfo.name
            else
                if (info.serviceInfo != null) info.serviceInfo.name else null
            out.append(name)
            if (pkg != null) out.append(" | ").append(pkg)
            if (cls != null) out.append("/").append(cls)
            out.append(Tuils.NEWLINE)
        }
        return out.toString().trim { it <= ' ' }
    }

    override fun argType(): IntArray? {
        return intArrayOf(CommandAbstraction.PLAIN_TEXT)
    }

    override fun priority(): Int {
        return 3
    }

    override fun helpRes(): Int {
        return R.string.help_intent
    }

    override fun onArgNotFound(pack: ExecutePack, indexNotFound: Int): String? {
        return pack.context.getString(helpRes())
    }

    override fun onNotArgEnough(pack: ExecutePack, nArgs: Int): String? {
        return pack.context.getString(helpRes())
    }
}
