package ohi.andre.consolelauncher.commands.main.specific

import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack
import java.util.ArrayList

abstract class RedirectCommand : CommandAbstraction {
    @JvmField
    var beforeObjects: MutableList<Any?> = ArrayList()

    @JvmField
    var afterObjects: MutableList<Any?> = ArrayList()

    abstract fun onRedirect(pack: ExecutePack): String?
    abstract fun getHint(): Int
    abstract fun isWaitingPermission(): Boolean

    fun cleanup() {
        beforeObjects.clear()
        afterObjects.clear()
    }
}
