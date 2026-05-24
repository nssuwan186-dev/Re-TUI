package ohi.andre.consolelauncher.commands.main.raw

import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack

class landscape : CommandAbstraction {
    override fun exec(pack: ExecutePack): String? = orientation.apply(pack, orientation.MODE_LANDSCAPE)

    override fun argType(): IntArray = intArrayOf()

    override fun priority(): Int = 4

    override fun helpRes(): Int = R.string.help_landscape

    override fun onArgNotFound(pack: ExecutePack, indexNotFound: Int): String? = null

    override fun onNotArgEnough(pack: ExecutePack, nArgs: Int): String? = null
}
