package ohi.andre.consolelauncher.commands.main.raw

import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack
import ohi.andre.consolelauncher.commands.main.MainPack

class pwd : CommandAbstraction {
    override fun exec(pack: ExecutePack): String = (pack as MainPack).currentDirectory.absolutePath

    override fun helpRes(): Int = R.string.help_pwd

    override fun argType(): IntArray = intArrayOf()

    override fun priority(): Int = 4

    override fun onArgNotFound(pack: ExecutePack, indexNotFound: Int): String = pack.context.getString(helpRes())

    override fun onNotArgEnough(pack: ExecutePack, nArgs: Int): String = pack.context.getString(helpRes())
}
