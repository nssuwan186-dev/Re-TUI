package ohi.andre.consolelauncher.commands.main.raw

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack
import ohi.andre.consolelauncher.commands.main.MainPack

class beep : CommandAbstraction {
    override fun exec(pack: ExecutePack): String? {
        return try {
            val toneG = ToneGenerator(AudioManager.STREAM_ALARM, 50)
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1000)
            Handler(Looper.getMainLooper()).postDelayed({ toneG.release() }, 1100L)
            null
        } catch (e: Exception) {
            e.toString()
        }
    }

    override fun argType(): IntArray = intArrayOf()

    override fun priority(): Int = 2

    override fun helpRes(): Int = R.string.help_beep

    override fun onArgNotFound(pack: ExecutePack, indexNotFound: Int): String? = null

    override fun onNotArgEnough(pack: ExecutePack, nArgs: Int): String = (pack as MainPack).context.getString(helpRes())
}
