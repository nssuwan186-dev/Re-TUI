package ohi.andre.consolelauncher.tuils.interfaces

import ohi.andre.consolelauncher.commands.main.specific.RedirectCommand

interface OnRedirectionListener {
    fun onRedirectionRequest(cmd: RedirectCommand)
    fun onRedirectionEnd(cmd: RedirectCommand)
    fun onRedirection(name: String, value: String)
}
