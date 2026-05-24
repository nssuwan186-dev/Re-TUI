@file:Suppress("DEPRECATION")

package ohi.andre.consolelauncher.commands.main.raw

import android.content.Intent
import java.io.File
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import ohi.andre.consolelauncher.MainManager
import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.UIManager
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack
import ohi.andre.consolelauncher.commands.main.MainPack
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager
import ohi.andre.consolelauncher.managers.xml.options.Behavior
import ohi.andre.consolelauncher.tuils.StoppableThread

class ctrlc : CommandAbstraction {
    override fun exec(pack: ExecutePack): String? {
        object : StoppableThread() {
            override fun run() {
                super.run()

                MainManager.interactive.kill()
                MainManager.interactive.close()

                MainManager.interactive = (pack as MainPack).shellHolder!!.build()

                pack.currentDirectory = XMLPrefsManager.get(File::class.java, Behavior.home_path)
                    ?: ohi.andre.consolelauncher.tuils.Tuils.getFolder()
                LocalBroadcastManager.getInstance(pack.context.applicationContext).sendBroadcast(Intent(UIManager.ACTION_UPDATE_HINT))
            }
        }.start()

        LocalBroadcastManager.getInstance(pack.context.applicationContext).sendBroadcast(Intent(UIManager.ACTION_NOROOT))

        return null
    }

    override fun argType(): IntArray = intArrayOf()

    override fun priority(): Int = 3

    override fun helpRes(): Int = R.string.help_ctrlc

    override fun onArgNotFound(pack: ExecutePack, indexNotFound: Int): String? = null

    override fun onNotArgEnough(pack: ExecutePack, nArgs: Int): String? = null
}
