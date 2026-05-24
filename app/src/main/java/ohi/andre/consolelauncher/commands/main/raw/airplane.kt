@file:Suppress("DEPRECATION")

package ohi.andre.consolelauncher.commands.main.raw

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack
import ohi.andre.consolelauncher.commands.main.MainPack
import ohi.andre.consolelauncher.commands.main.specific.APICommand

class airplane : APICommand, CommandAbstraction {
    override fun exec(pack: ExecutePack): String? {
        val info = pack as MainPack
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val enabled = isEnabled(info.context)
            Settings.System.putInt(info.context.contentResolver, Settings.System.AIRPLANE_MODE_ON, if (enabled) 0 else 1)

            val intent = Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            intent.putExtra("state", !enabled)
            info.context.sendBroadcast(intent)

            return info.res.getString(R.string.output_airplane) + !enabled
        }
        return null
    }

    private fun isEnabled(context: Context): Boolean =
        Settings.System.getInt(context.contentResolver, Settings.System.AIRPLANE_MODE_ON, 0) != 0

    override fun priority(): Int = 2

    override fun argType(): IntArray = intArrayOf()

    override fun helpRes(): Int = R.string.help_airplane

    override fun onArgNotFound(pack: ExecutePack, indexNotFound: Int): String? = null

    override fun onNotArgEnough(pack: ExecutePack, nArgs: Int): String? = null

    override fun willWorkOn(api: Int): Boolean = api < Build.VERSION_CODES.JELLY_BEAN_MR1
}
