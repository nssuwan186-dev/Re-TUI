package ohi.andre.consolelauncher.managers.file

import android.content.Context
import java.util.Locale
import ohi.andre.consolelauncher.managers.termux.TermuxBridgeManager
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager
import ohi.andre.consolelauncher.managers.xml.options.Behavior

object FileBackendManager {
    enum class Mode {
        AUTO,
        NATIVE,
        TERMUX,
        OFF
    }

    enum class Active {
        NATIVE,
        TERMUX,
        OFF
    }

    @JvmStatic
    fun configuredMode(): Mode {
        var value = XMLPrefsManager.get(Behavior.file_backend) ?: return Mode.AUTO

        value = value.trim().lowercase(Locale.getDefault())
        if ("native" == value) {
            return Mode.NATIVE
        }
        if ("termux" == value) {
            return Mode.TERMUX
        }
        if ("off" == value) {
            return Mode.OFF
        }
        return Mode.AUTO
    }

    @JvmStatic
    fun activeBackend(context: Context): Active {
        val mode = configuredMode()
        if (mode == Mode.OFF) {
            return Active.OFF
        }
        if (mode == Mode.NATIVE) {
            return Active.NATIVE
        }
        if (mode == Mode.TERMUX) {
            return if (termuxReady(context)) Active.TERMUX else Active.OFF
        }
        return if (termuxReady(context)) Active.TERMUX else Active.NATIVE
    }

    @JvmStatic
    fun termuxReady(context: Context): Boolean {
        val status = TermuxBridgeManager.status(context)
        return status.termuxInstalled && status.runCommandDeclared && status.runCommandGranted
    }

    @JvmStatic
    fun statusLine(context: Context): String {
        val mode = configuredMode()
        val active = activeBackend(context)
        val status = TermuxBridgeManager.status(context)
        return "file_backend=" + mode.name.lowercase(Locale.getDefault()) +
            "\nactive_backend=" + active.name.lowercase(Locale.getDefault()) +
            "\ntermux_installed=" + yesNo(status.termuxInstalled) +
            "\ntermux_run_command_declared=" + yesNo(status.runCommandDeclared) +
            "\ntermux_run_command_granted=" + yesNo(status.runCommandGranted)
    }

    private fun yesNo(value: Boolean): String = if (value) "yes" else "no"
}
