package ohi.andre.consolelauncher.commands.main.specific

import ohi.andre.consolelauncher.commands.CommandAbstraction

interface PermanentSuggestionCommand : CommandAbstraction {
    fun permanentSuggestions(): Array<String>?
}
