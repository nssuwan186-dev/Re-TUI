package ohi.andre.consolelauncher.commands.main

import ohi.andre.consolelauncher.commands.ExecutePack

interface Param {
    fun args(): IntArray?
    fun exec(pack: ExecutePack): String?
    fun label(): String?

    fun onNotArgEnough(pack: ExecutePack, n: Int): String?
    fun onArgNotFound(pack: ExecutePack, index: Int): String?
}
