package ohi.andre.consolelauncher.managers.xml.options

import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsElement
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsSave

enum class Toolbar(
    private val defaultValue: String,
    private val info: String,
    private val type: String
) : XMLPrefsSave {
    show_toolbar("true", "If false, the toolbar is hidden", XMLPrefsSave.BOOLEAN),
    hide_toolbar_no_input("false", "If true, the toolbar will be hidden when the input field is empty", XMLPrefsSave.BOOLEAN),
    shortcut_button_1_enabled("false", "If true, the first custom toolbar button is shown", XMLPrefsSave.BOOLEAN),
    shortcut_button_1_command("", "Command, alias, or app name executed by the first custom toolbar button", XMLPrefsSave.TEXT),
    shortcut_button_1_icon("star", "Icon key for the first custom toolbar button", XMLPrefsSave.TEXT),
    shortcut_button_2_enabled("false", "If true, the second custom toolbar button is shown", XMLPrefsSave.BOOLEAN),
    shortcut_button_2_command("", "Command, alias, or app name executed by the second custom toolbar button", XMLPrefsSave.TEXT),
    shortcut_button_2_icon("star", "Icon key for the second custom toolbar button", XMLPrefsSave.TEXT);

    override fun defaultValue(): String = defaultValue

    override fun info(): String = info

    override fun type(): String = type

    override fun parent(): XMLPrefsElement = XMLPrefsManager.XMLPrefsRoot.TOOLBAR

    override fun label(): String = name

    override fun invalidValues(): Array<String>? = null

    override fun getLowercaseString(): String = label()

    override fun getString(): String = label()
}
