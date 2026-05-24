package ohi.andre.consolelauncher.commands.main.raw

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import java.util.Locale
import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack
import ohi.andre.consolelauncher.managers.callback.CallbackAuthManager
import ohi.andre.consolelauncher.tuils.Tuils

class retuitoken : CommandAbstraction {
    override fun exec(info: ExecutePack): String {
        var command = ""
        val args = info.args
        if (args != null && args.isNotEmpty()) {
            val arg = info.get()
            if (arg != null) {
                command = arg.toString().trim().lowercase(Locale.getDefault())
            }
        }

        if (command.isEmpty() || command == "-status" || command == "status") {
            return status(info)
        }
        if (command == "-show" || command == "show") {
            val token = CallbackAuthManager.getOrCreateToken(info.context)
            CallbackAuthManager.setEnabled(info.context, true)
            copyToken(info.context, token)
            return tokenOutput("Callback auth enabled.", token)
        }
        if (command == "-rotate" || command == "rotate") {
            val token = CallbackAuthManager.rotateToken(info.context)
            copyToken(info.context, token)
            return tokenOutput("Callback token rotated.", token)
        }
        if (command == "-on" || command == "on") {
            val token = CallbackAuthManager.getOrCreateToken(info.context)
            CallbackAuthManager.setEnabled(info.context, true)
            copyToken(info.context, token)
            return tokenOutput("Callback auth enabled.", token)
        }
        if (command == "-off" || command == "off") {
            CallbackAuthManager.setEnabled(info.context, false)
            return "Callback auth disabled."
        }

        return info.context.getString(R.string.help_retuitoken)
    }

    private fun status(info: ExecutePack): String =
        "Callback auth: " + (if (CallbackAuthManager.isEnabled(info.context)) "enabled" else "disabled") +
            Tuils.NEWLINE +
            "Token present: " + CallbackAuthManager.getToken(info.context).isNotEmpty()

    private fun copyToken(context: Context, token: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        clipboard?.setPrimaryClip(ClipData.newPlainText("Re-TUI callback token", token))
    }

    private fun tokenOutput(message: String, token: String): String =
        message +
            Tuils.NEWLINE +
            "Token copied to clipboard." +
            Tuils.NEWLINE +
            "Token: " + token

    override fun argType(): IntArray = intArrayOf(CommandAbstraction.PLAIN_TEXT)

    override fun priority(): Int = 2

    override fun helpRes(): Int = R.string.help_retuitoken

    override fun onArgNotFound(info: ExecutePack, indexNotFound: Int): String =
        info.context.getString(R.string.help_retuitoken)

    override fun onNotArgEnough(info: ExecutePack, nArgs: Int): String = status(info)
}
