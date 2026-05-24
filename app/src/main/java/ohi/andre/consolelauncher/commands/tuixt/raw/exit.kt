package ohi.andre.consolelauncher.commands.tuixt.raw

import android.app.Activity
import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack
import ohi.andre.consolelauncher.commands.tuixt.TuixtPack

class exit : CommandAbstraction {
    override fun exec(pack: ExecutePack): String? {
        val info = pack as TuixtPack
        (info.context as Activity).finish()
        return null
    }

    override fun argType(): IntArray = intArrayOf()

    override fun priority(): Int = 0

    override fun helpRes(): Int = R.string.help_tuixt_exit

    override fun onArgNotFound(pack: ExecutePack, indexNotFound: Int): String? = null

    override fun onNotArgEnough(pack: ExecutePack, nArgs: Int): String? = null
}
