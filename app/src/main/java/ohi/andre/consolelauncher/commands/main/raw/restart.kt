package ohi.andre.consolelauncher.commands.main.raw

import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack
import ohi.andre.consolelauncher.commands.main.MainPack
import ohi.andre.consolelauncher.tuils.interfaces.Reloadable

class restart : CommandAbstraction {
    override fun exec(pack: ExecutePack): String {
        val info = pack as MainPack
        (info.context as Reloadable).reload()
        return pack.context.getString(R.string.restarting)
    }

    override fun argType(): IntArray = intArrayOf()

    override fun priority(): Int = 4

    override fun helpRes(): Int = R.string.help_restart

    override fun onArgNotFound(pack: ExecutePack, indexNotFound: Int): String? = null

    override fun onNotArgEnough(pack: ExecutePack, nArgs: Int): String? = null
}
