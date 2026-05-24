package ohi.andre.consolelauncher.commands.main.raw

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack
import ohi.andre.consolelauncher.commands.main.MainPack

class rate : CommandAbstraction {
    override fun exec(pack: ExecutePack): String {
        val info = pack as MainPack
        try {
            info.context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + info.context.packageName))
            )
        } catch (anfe: ActivityNotFoundException) {
            info.context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + info.context.packageName))
            )
        }

        return info.res.getString(R.string.output_rate)
    }

    override fun helpRes(): Int = R.string.help_rate

    override fun argType(): IntArray = intArrayOf()

    override fun priority(): Int = 3

    override fun onNotArgEnough(pack: ExecutePack, nArgs: Int): String? = null

    override fun onArgNotFound(pack: ExecutePack, indexNotFound: Int): String? = null
}
