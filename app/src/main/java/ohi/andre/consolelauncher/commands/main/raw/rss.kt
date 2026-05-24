package ohi.andre.consolelauncher.commands.main.raw

import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack
import ohi.andre.consolelauncher.commands.main.MainPack
import ohi.andre.consolelauncher.commands.main.specific.ParamCommand
import ohi.andre.consolelauncher.managers.RssManager
import ohi.andre.consolelauncher.managers.TimeManager
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager
import ohi.andre.consolelauncher.managers.xml.options.Rss
import ohi.andre.consolelauncher.tuils.Tuils
import org.w3c.dom.Element
import java.io.File
import java.util.Locale
import java.util.regex.Pattern
import org.w3c.dom.Node

/**
 * Created by francescoandreuzzi on 30/09/2017.
 */
class rss : ParamCommand() {
    private enum class Param : ohi.andre.consolelauncher.commands.main.Param {
        add {
            override fun exec(pack: ExecutePack): String? {
                val id = pack.getInt()
                val tm = pack.get() as Long
                val url = pack.getString()

                return (pack as MainPack).rssManager!!.add(id, tm, url)
            }

            override fun args(): IntArray? {
                return intArrayOf(
                    CommandAbstraction.INT,
                    CommandAbstraction.LONG,
                    CommandAbstraction.PLAIN_TEXT
                )
            }
        },
        rm {
            override fun exec(pack: ExecutePack): String? {
                val id = pack.getInt()

                return (pack as MainPack).rssManager!!.rm(id)
            }

            override fun args(): IntArray? {
                return intArrayOf(CommandAbstraction.INT)
            }
        },
        ls {
            override fun exec(pack: ExecutePack): String? {
                return (pack as MainPack).rssManager!!.list()
            }

            override fun args(): IntArray? {
                return IntArray(0)
            }
        },
        l {
            override fun exec(pack: ExecutePack): String? {
                val id = pack.getInt()
                return (pack as MainPack).rssManager!!.l(id)
            }

            override fun args(): IntArray? {
                return intArrayOf(CommandAbstraction.INT)
            }
        },
        show {
            override fun exec(pack: ExecutePack): String? {
                val id = pack.getInt()
                val show = pack.getBoolean()

                return (pack as MainPack).rssManager!!.setShow(id, show)
            }

            override fun args(): IntArray? {
                return intArrayOf(CommandAbstraction.INT, CommandAbstraction.BOOLEAN)
            }
        },
        update_time {
            override fun exec(pack: ExecutePack): String? {
                val id = pack.getInt()
                val tm = pack.get() as Long

                return (pack as MainPack).rssManager!!.setTime(id, tm)
            }

            override fun args(): IntArray? {
                return intArrayOf(CommandAbstraction.INT, CommandAbstraction.LONG)
            }
        },
        time_format {
            override fun exec(pack: ExecutePack): String? {
                return (pack as MainPack).rssManager!!.setTimeFormat(
                    pack.getInt(),
                    pack.getString()
                )
            }

            override fun args(): IntArray? {
                return intArrayOf(CommandAbstraction.INT, CommandAbstraction.PLAIN_TEXT)
            }
        },
        format {
            override fun args(): IntArray? {
                return intArrayOf(CommandAbstraction.INT, CommandAbstraction.PLAIN_TEXT)
            }

            override fun exec(pack: ExecutePack): String? {
                val id = pack.getInt()
                val s = pack.getString()

                return (pack as MainPack).rssManager!!.setFormat(id, s)
            }
        },
        color {
            override fun args(): IntArray? {
                return intArrayOf(CommandAbstraction.INT, CommandAbstraction.COLOR)
            }

            override fun exec(pack: ExecutePack): String? {
                val id = pack.getInt()
                val c = pack.getString()

                return (pack as MainPack).rssManager!!.setColor(id, c)
            }

            override fun onArgNotFound(pack: ExecutePack, index: Int): String? {
                if (index == 2) return pack.context.getString(R.string.output_invalidcolor)
                return super.onArgNotFound(pack, index)
            }
        },
        entry_tag {
            override fun args(): IntArray? {
                return intArrayOf(CommandAbstraction.INT, CommandAbstraction.PLAIN_TEXT)
            }

            override fun exec(pack: ExecutePack): String? {
                return (pack as MainPack).rssManager!!.setEntryTag(pack.getInt(), pack.getString())
            }
        },
        date_tag {
            override fun args(): IntArray? {
                return intArrayOf(CommandAbstraction.INT, CommandAbstraction.PLAIN_TEXT)
            }

            override fun exec(pack: ExecutePack): String? {
                return (pack as MainPack).rssManager!!.setDateTag(pack.getInt(), pack.getString())
            }
        },
        last_check {
            override fun args(): IntArray? {
                return intArrayOf(CommandAbstraction.INT)
            }

            override fun exec(pack: ExecutePack): String? {
                val n = XMLPrefsManager.findNode(
                    File(Tuils.getFolder(), RssManager.PATH),
                    RssManager.RSS_LABEL,
                    arrayOf<String?>(RssManager.ID_ATTRIBUTE),
                    arrayOf<String?>(pack.getInt().toString())
                )
                if (n == null) return pack.context.getString(R.string.id_notfound)

                val el = n as Element

                val value = if (el.hasAttribute(RssManager.LASTCHECKED_ATTRIBUTE)) el.getAttribute(
                    RssManager.LASTCHECKED_ATTRIBUTE
                ) else null
                if (value == null) return pack.context.getString(R.string.rss_never_checked)

                try {
                    return TimeManager.instance!!.replace(
                        XMLPrefsManager.get(Rss.rss_time_format), value.toLong(),
                        Int.Companion.MAX_VALUE
                    ).toString()
                } catch (e: Exception) {
                    Tuils.log(e)
                    return pack.context.getString(R.string.output_error)
                }
            }
        },
        frc {
            override fun args(): IntArray? {
                return intArrayOf(CommandAbstraction.INT)
            }

            override fun exec(pack: ExecutePack): String? {
                if (!(pack as MainPack).rssManager!!.updateRss(
                        pack.getInt(),
                        false,
                        true
                    )
                ) return pack.context.getString(R.string.id_notfound)
                return null
            }
        },
        info {
            override fun args(): IntArray? {
                return intArrayOf(CommandAbstraction.INT)
            }

            override fun exec(pack: ExecutePack): String? {
                val rss = (pack as MainPack).rssManager!!.findId(pack.getInt())
                if (rss == null) return pack.context.getString(R.string.id_notfound)

                return rss.toString()
            }
        },
        include_if_matches {
            override fun args(): IntArray? {
                return intArrayOf(CommandAbstraction.INT, CommandAbstraction.PLAIN_TEXT)
            }

            override fun exec(pack: ExecutePack): String? {
                val id = pack.getInt()
                val r = pack.getString()

                return (pack as MainPack).rssManager!!.setIncludeIfMatches(id, r)
            }
        },
        exclude_if_matches {
            override fun args(): IntArray? {
                return intArrayOf(CommandAbstraction.INT, CommandAbstraction.PLAIN_TEXT)
            }

            override fun exec(pack: ExecutePack): String? {
                val id = pack.getInt()
                val r = pack.getString()

                return (pack as MainPack).rssManager!!.setExcludeIfMatches(id, r)
            }
        },
        add_command {
            override fun args(): IntArray? {
                return intArrayOf(
                    CommandAbstraction.INT,
                    CommandAbstraction.NO_SPACE_STRING,
                    CommandAbstraction.NO_SPACE_STRING,
                    CommandAbstraction.PLAIN_TEXT
                )
            }

            override fun exec(pack: ExecutePack): String? {
                val id = pack.getInt()

                val on = pack.getString()
                val regex = pack.getString()
                val cmd = pack.getString()

                try {
                    Pattern.compile(regex)
                } catch (e: Exception) {
                    return e.toString()
                }

                return (pack as MainPack).rssManager!!.addRegexCommand(id, on, regex, cmd)
            }
        },
        rm_command {
            override fun args(): IntArray? {
                return intArrayOf(CommandAbstraction.INT)
            }

            override fun exec(pack: ExecutePack): String? {
                return (pack as MainPack).rssManager!!.rmRegexCommand(pack.getInt())
            }
        },
        wifi_only {
            override fun args(): IntArray? {
                return intArrayOf(CommandAbstraction.INT, CommandAbstraction.BOOLEAN)
            }

            override fun exec(pack: ExecutePack): String? {
                val id = pack.getInt()
                val w = pack.getBoolean()

                return (pack as MainPack).rssManager!!.setWifiOnly(id, w)
            }
        },
        add_format {
            override fun exec(pack: ExecutePack): String? {
                return (pack as MainPack).rssManager!!.addFormat(pack.getInt(), pack.getString())
            }

            override fun args(): IntArray? {
                return intArrayOf(CommandAbstraction.INT, CommandAbstraction.PLAIN_TEXT)
            }
        },
        rm_format {
            override fun exec(pack: ExecutePack): String? {
                return (pack as MainPack).rssManager!!.removeFormat(pack.getInt())
            }

            override fun args(): IntArray? {
                return intArrayOf(CommandAbstraction.INT)
            }
        },
        file {
            override fun exec(pack: ExecutePack): String? {
                pack.context.startActivity(
                    Tuils.openFile(
                        pack.context,
                        File(Tuils.getFolder(), RssManager.PATH)
                    )
                )
                return null
            }

            override fun args(): IntArray? {
                return IntArray(0)
            }
        };

        override fun label(): String? {
            return Tuils.MINUS + name
        }

        override fun onNotArgEnough(pack: ExecutePack, n: Int): String? {
            return pack.context.getString(R.string.help_rss)
        }

        override fun onArgNotFound(pack: ExecutePack, index: Int): String? {
            return pack.context.getString(R.string.invalid_integer)
        }

        companion object {
            fun get(p: String): Param? {
                var p = p
                p = p.lowercase(Locale.getDefault())
                val ps = entries.toTypedArray()
                for (p1 in ps) if (p.endsWith(p1.label()!!)) return p1
                return null
            }

            fun labels(): Array<String?> {
                val ps = entries.toTypedArray()
                val ss = arrayOfNulls<String>(ps.size)

                for (count in ps.indices) {
                    ss[count] = ps[count].label()
                }

                return ss
            }
        }
    }

    override fun paramForString(
        pack: MainPack,
        param: String
    ): ohi.andre.consolelauncher.commands.main.Param? {
        return Param.Companion.get(param)
    }

    override fun doThings(pack: ExecutePack): String? {
        return null
    }

    override fun priority(): Int {
        return 3
    }

    override fun helpRes(): Int {
        return R.string.help_rss
    }

    public override fun params(): Array<String?> {
        return Param.Companion.labels()
    }
}
