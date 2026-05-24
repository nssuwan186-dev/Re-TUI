package ohi.andre.consolelauncher.managers.xml.options

import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsElement
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsSave

enum class Cmd(
    private val defaultValue: String,
    private val info: String,
    private val type: String = XMLPrefsSave.TEXT
) : XMLPrefsSave {
    default_search(
        "-gg",
        "The param that will be used if you type \"search apples\" instead of \"search -param apples\""
    );

    override fun defaultValue(): String = defaultValue

    override fun type(): String = type

    override fun info(): String = info

    override fun parent(): XMLPrefsElement = XMLPrefsManager.XMLPrefsRoot.CMD

    override fun label(): String = name

    override fun invalidValues(): Array<String>? = null

    override fun getLowercaseString(): String = label()

    override fun getString(): String = label()
}
