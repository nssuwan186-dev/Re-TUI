@file:Suppress("DEPRECATION")

package ohi.andre.consolelauncher.commands.main.raw

import android.content.Context
import android.os.Vibrator
import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack
import ohi.andre.consolelauncher.commands.main.MainPack
import ohi.andre.consolelauncher.tuils.Tuils

class vibrate : CommandAbstraction {
    override fun exec(pack: ExecutePack): String? {
        var text = pack.getString()
        val context = (pack as MainPack).context

        var separator = Tuils.firstNonDigit(text)

        if (separator.code == 0) {
            val ms: Int
            try {
                ms = text?.toInt() ?: throw NumberFormatException("null")
                (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(ms.toLong())
                return null
            } catch (e: NumberFormatException) {
                return context.getString(R.string.invalid_integer)
            } catch (e: Exception) {
                return e.toString()
            }
        } else {
            if (separator == ' ') {
                val s2 = Tuils.firstNonDigit(Tuils.removeSpaces(text))
                if (s2.code != 0) {
                    text = Tuils.removeSpaces(text)
                    separator = s2
                }
            }

            val split = text!!.split((separator.toString() + Tuils.EMPTYSTRING).toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val pattern = LongArray(split.size)

            for (c in split.indices) {
                try {
                    pattern[c] = split[c].toLong()
                } catch (e: Exception) {
                    pattern[c] = 0
                }
            }

            (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(pattern, -1)
        }

        return null
    }

    override fun argType(): IntArray = intArrayOf(CommandAbstraction.PLAIN_TEXT)

    override fun priority(): Int = 2

    override fun helpRes(): Int = R.string.help_vibrate

    override fun onArgNotFound(pack: ExecutePack, indexNotFound: Int): String? = null

    override fun onNotArgEnough(pack: ExecutePack, nArgs: Int): String = (pack as MainPack).context.getString(helpRes())
}
