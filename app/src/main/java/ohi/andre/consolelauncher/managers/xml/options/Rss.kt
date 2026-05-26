package ohi.andre.consolelauncher.managers.xml.options

import ohi.andre.consolelauncher.managers.RssManager
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsElement
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsSave

enum class Rss(
    private val defaultValue: String,
    private val info: String,
    private val type: String
) : XMLPrefsSave {
    rss_item_text_color("#f44336", "RSS item text color", XMLPrefsSave.COLOR),
    rss_default_format(
        "%[50][green]title ### %[100][teal]description (%pubDate)",
        "The default format",
        XMLPrefsSave.TEXT
    ),
    include_rss_default(
        "true",
        "If true, a filter will exclude an item if it matches. If false, a filter will include an item if it matches",
        XMLPrefsSave.BOOLEAN
    ),
    rss_hidden_tags("img", "A list of excluded tags (separated by comma)", XMLPrefsSave.TEXT),
    rss_time_format("%t0", "The time format used by RSS items", XMLPrefsSave.TEXT),
    show_rss_download("true", "If true, you will see a message when Re:T-UI downloads a feed", XMLPrefsSave.BOOLEAN),
    rss_download_format("RSS: %id --- Downloaded %sb bytes", "The message shown when an RSS feed is downloaded", XMLPrefsSave.TEXT),
    rss_download_message_text_color("aqua", "RSS download message text color", XMLPrefsSave.COLOR),
    click_rss("true", "If true, you will be able to click on an RSS item to open the associated webpage", XMLPrefsSave.BOOLEAN);

    override fun defaultValue(): String = defaultValue

    override fun type(): String = type

    override fun info(): String = info

    override fun parent(): XMLPrefsElement? = RssManager.instance

    override fun label(): String = name

    override fun invalidValues(): Array<String>? = null

    override fun getLowercaseString(): String = label()

    override fun getString(): String = label()
}
