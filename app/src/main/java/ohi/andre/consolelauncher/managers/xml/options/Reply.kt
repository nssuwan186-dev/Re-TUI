package ohi.andre.consolelauncher.managers.xml.options

import android.os.Build
import ohi.andre.consolelauncher.managers.notifications.reply.ReplyManager
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsElement
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsSave

enum class Reply(
    private val defaultValue: String,
    private val info: String,
    private val type: String = XMLPrefsSave.BOOLEAN
) : XMLPrefsSave {
    reply_enabled("true", "If false, notification reply will be disabled");

    override fun defaultValue(): String = defaultValue

    override fun type(): String = type

    override fun info(): String = info

    override fun parent(): XMLPrefsElement? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) ReplyManager.getInstance() else null

    override fun label(): String = name

    override fun invalidValues(): Array<String>? = null

    override fun getLowercaseString(): String = label()

    override fun getString(): String = label()
}
