package ohi.andre.consolelauncher.commands.tuixt.raw

import java.util.ArrayList
import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack
import ohi.andre.consolelauncher.commands.tuixt.TuixtPack
import ohi.andre.consolelauncher.tuils.Tuils
import java.util.Arrays
import java.util.Collections
import java.util.Comparator

class help : CommandAbstraction {
    override fun exec(pack: ExecutePack): String {
        val info = pack as TuixtPack
        val cmd = pack.get(CommandAbstraction::class.java)
        val res = cmd?.helpRes() ?: R.string.output_commandnotfound
        return info.resources.getString(res)
    }

    override fun argType(): IntArray = intArrayOf(CommandAbstraction.COMMAND)

    override fun priority(): Int = 5

    override fun helpRes(): Int = R.string.help_tuixt_help

    override fun onArgNotFound(pack: ExecutePack, indexNotFound: Int): String? = onNotArgEnough(pack, 0)

    override fun onNotArgEnough(pack: ExecutePack, nArgs: Int): String {
        val info = pack as TuixtPack
        val toPrint = ArrayList(info.commandGroup.commandNames.toList())

        toPrint.sortWith(Tuils::alphabeticCompare)

        Tuils.addPrefix(toPrint, Tuils.DOUBLE_SPACE)
        Tuils.addSeparator(toPrint, Tuils.TRIBLE_SPACE)
        Tuils.insertHeaders(toPrint, true)

        return Tuils.toPlanString(toPrint, Tuils.EMPTYSTRING)
    }
}
