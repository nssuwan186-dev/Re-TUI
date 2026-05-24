package ohi.andre.consolelauncher.tuils.interfaces

import ohi.andre.consolelauncher.commands.main.specific.RedirectCommand

interface Redirectator {
    fun prepareRedirection(cmd: RedirectCommand)
    fun cleanup()
}
