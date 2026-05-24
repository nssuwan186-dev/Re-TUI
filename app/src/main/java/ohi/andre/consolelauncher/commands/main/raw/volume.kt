package ohi.andre.consolelauncher.commands.main.raw

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import java.util.Locale
import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack
import ohi.andre.consolelauncher.commands.main.MainPack
import ohi.andre.consolelauncher.commands.main.specific.ParamCommand
import ohi.andre.consolelauncher.tuils.Tuils

class volume : ParamCommand() {
    private enum class Param : ohi.andre.consolelauncher.commands.main.Param {
        set {
            override fun args(): IntArray = intArrayOf(CommandAbstraction.INT, CommandAbstraction.INT)

            override fun exec(pack: ExecutePack): String? {
                if (!ensureNotificationPolicyAccess(pack)) {
                    return pack.context.getString(R.string.output_waitingpermission)
                }

                val type = pack.getInt()
                var volume = pack.getInt()

                if (volume < 0) {
                    volume = 0
                } else if (volume > 100) {
                    volume = 100
                }

                val manager = pack.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val maxIndex = manager.getStreamMaxVolume(type)

                volume = volume * maxIndex / 100

                manager.setStreamVolume(type, volume, 0)

                return null
            }
        },
        profile {
            override fun args(): IntArray = intArrayOf(CommandAbstraction.INT)

            override fun exec(pack: ExecutePack): String? {
                if (!ensureNotificationPolicyAccess(pack)) {
                    return pack.context.getString(R.string.output_waitingpermission)
                }

                val manager = pack.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                manager.ringerMode = pack.getInt()

                return null
            }
        },
        get {
            private val labels = arrayOf("Voice call", "System", "Ring", "Media", "Alarm", "Notifications")

            private fun appendInfo(builder: StringBuilder, manager: AudioManager, stream: Int) {
                builder.append(labels[stream])
                    .append(":")
                    .append(Tuils.SPACE)
                    .append(manager.getStreamVolume(stream) * 100 / manager.getStreamMaxVolume(stream))
                    .append("%")
                    .append(Tuils.NEWLINE)
            }

            override fun args(): IntArray = intArrayOf(CommandAbstraction.INT)

            override fun exec(pack: ExecutePack): String {
                val manager = pack.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

                val stream = pack.getInt()

                val builder = StringBuilder()
                appendInfo(builder, manager, stream)

                return builder.toString().trim()
            }

            override fun onNotArgEnough(pack: ExecutePack, n: Int): String {
                val manager = pack.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

                val builder = StringBuilder()
                for (c in labels.indices) {
                    appendInfo(builder, manager, c)
                }

                return builder.toString().trim()
            }
        };

        override fun label(): String = Tuils.MINUS + name

        override fun onNotArgEnough(pack: ExecutePack, n: Int): String =
            pack.context.getString(R.string.help_volume)

        override fun onArgNotFound(pack: ExecutePack, index: Int): String =
            pack.context.getString(R.string.invalid_integer)

        companion object {
            private fun ensureNotificationPolicyAccess(pack: ExecutePack): Boolean {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val notificationManager = pack.context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    if (!notificationManager.isNotificationPolicyAccessGranted) {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                        pack.context.startActivity(intent)
                        return false
                    }
                }
                return true
            }

            fun get(p: String): Param? {
                val value = p.lowercase(Locale.getDefault())
                for (p1 in entries) {
                    if (value.endsWith(p1.label())) return p1
                }
                return null
            }

            fun labels(): Array<String> {
                val ps = entries
                val ss = Array(ps.size) { "" }

                for (count in ps.indices) {
                    ss[count] = ps[count].label()
                }

                return ss
            }
        }
    }

    override fun params(): Array<String> = Param.labels()

    override fun paramForString(pack: MainPack, param: String): ohi.andre.consolelauncher.commands.main.Param? =
        Param.get(param)

    override fun priority(): Int = 3

    override fun helpRes(): Int = R.string.help_volume

    override fun doThings(pack: ExecutePack): String? = null
}
