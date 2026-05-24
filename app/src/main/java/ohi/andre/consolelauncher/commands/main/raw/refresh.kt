package ohi.andre.consolelauncher.commands.main.raw

import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack
import ohi.andre.consolelauncher.commands.main.MainPack

class refresh : CommandAbstraction {
    override fun exec(pack: ExecutePack): String {
        val info = pack as MainPack
        info.appsManager.fill()
        info.aliasManager.reload()
        info.player?.refresh()
        info.contacts.refreshContacts(info.context)
        info.rssManager!!.refresh()

        return info.res.getString(R.string.output_refresh)
    }

    override fun helpRes(): Int = R.string.help_refresh

    override fun argType(): IntArray = intArrayOf()

    override fun priority(): Int = 3

    override fun onNotArgEnough(pack: ExecutePack, nArgs: Int): String? = null

    override fun onArgNotFound(pack: ExecutePack, indexNotFound: Int): String? = null
}
