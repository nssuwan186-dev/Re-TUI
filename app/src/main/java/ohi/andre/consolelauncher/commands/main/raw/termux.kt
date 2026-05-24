@file:Suppress("DEPRECATION")

package ohi.andre.consolelauncher.commands.main.raw

import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.UIManager
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack

class termux : CommandAbstraction {
    override fun exec(pack: ExecutePack): String? {
        var command: String? = null
        val currentArgs = pack.args
        if (currentArgs != null && currentArgs.isNotEmpty()) {
            val arg = pack.get()
            if (arg != null) {
                command = arg.toString()
            }
        }

        openConsole(pack, command)
        return null
    }

    private fun openConsole(pack: ExecutePack, command: String?) {
        val intent = Intent(UIManager.ACTION_TERMUX_CONSOLE)
        intent.putExtra(UIManager.EXTRA_TERMUX_COMMAND, command)
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            LocalBroadcastManager
                .getInstance(pack.context.applicationContext)
                .sendBroadcast(intent)
        }
    }

    override fun argType(): IntArray = intArrayOf(CommandAbstraction.PLAIN_TEXT)

    override fun priority(): Int = 3

    override fun helpRes(): Int = R.string.help_termux

    override fun onArgNotFound(pack: ExecutePack, indexNotFound: Int): String = pack.context.getString(R.string.help_termux)

    override fun onNotArgEnough(pack: ExecutePack, nArgs: Int): String? {
        openConsole(pack, null)
        return null
    }
}
