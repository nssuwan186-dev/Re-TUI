package ohi.andre.consolelauncher.commands.main.raw

import android.content.Intent
import android.net.Uri
import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack
import ohi.andre.consolelauncher.commands.main.MainPack

class donate : CommandAbstraction {
    private val donateUrl = "https://github.com/DvilSpawn/Re-TUI"

    override fun exec(pack: ExecutePack): String {
        val info = pack as MainPack
        val donateIntent = Intent(Intent.ACTION_VIEW, Uri.parse(donateUrl))
        info.context.startActivity(donateIntent)
        return info.res.getString(R.string.output_rate)
    }

    override fun argType(): IntArray = intArrayOf()

    override fun priority(): Int = 3

    override fun helpRes(): Int = R.string.help_donate

    override fun onArgNotFound(pack: ExecutePack, indexNotFound: Int): String? = null

    override fun onNotArgEnough(pack: ExecutePack, nArgs: Int): String? = null
}
