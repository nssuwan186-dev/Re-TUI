package ohi.andre.consolelauncher.commands.main.raw

import java.util.Locale
import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack
import ohi.andre.consolelauncher.commands.main.MainPack
import ohi.andre.consolelauncher.commands.main.specific.RedirectCommand
import ohi.andre.consolelauncher.managers.ClockManager
import ohi.andre.consolelauncher.tuils.Tuils

class timer : CommandAbstraction {
    override fun argType(): IntArray = intArrayOf(CommandAbstraction.PLAIN_TEXT)

    override fun exec(pack: ExecutePack): String {
        val arg = pack.get(Any::class.java, 0)
        val input = arg?.toString()
        return execute(pack, input)
    }

    private fun execute(pack: ExecutePack, input: String?): String {
        if (input == null) {
            return pack.context.getString(R.string.help_timer)
        }

        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return pack.context.getString(R.string.help_timer)
        }

        val clockManager = ClockManager.getInstance(pack.context)

        if (trimmed.startsWith("-")) {
            val split = trimmed.split("\\s+".toRegex(), limit = 2).toTypedArray()
            val option = split[0].lowercase(Locale.getDefault())

            if (option == "-stop") {
                return clockManager.stopTimer()
            }
            if (option == "-status") {
                return clockManager.timerStatus
            }
            if (option == "-add") {
                if (split.size < 2) {
                    return pack.context.getString(R.string.help_timer)
                }
                val duration = ClockManager.parseDurationMillis(split[1])
                if (clockManager.isTimerRunning) {
                    return clockManager.addToTimer(duration)
                }
                return clockManager.startTimer(duration)
            }

            return pack.context.getString(R.string.output_invalid_param) + " " + split[0]
        }

        val duration = ClockManager.parseDurationMillis(trimmed)
        if (clockManager.isTimerRunning) {
            if (pack is MainPack && duration > 0L) {
                pack.redirectator!!.prepareRedirection(TimerAddConfirmation(duration))
                return "Timer already running. Do you want to add " + ClockManager.formatDuration(duration) + " to it? (Yes/No)"
            }
            return "A timer is already running. Use timer -add [duration], timer -status, or timer -stop."
        }
        return clockManager.startTimer(duration)
    }

    override fun onArgNotFound(pack: ExecutePack, indexNotFound: Int): String =
        pack.context.getString(R.string.help_timer)

    override fun onNotArgEnough(pack: ExecutePack, nArgs: Int): String =
        pack.context.getString(R.string.help_timer)

    override fun priority(): Int = 3

    override fun helpRes(): Int = R.string.help_timer

    private class TimerAddConfirmation(private val duration: Long) : RedirectCommand() {
        override fun onRedirect(pack: ExecutePack): String {
            val mainPack = pack as MainPack
            var answer = Tuils.EMPTYSTRING
            if (afterObjects.isNotEmpty() && afterObjects[0] != null) {
                answer = afterObjects[0].toString().trim()
            }

            mainPack.redirectator!!.cleanup()
            return if ("yes".equals(answer, ignoreCase = true) || "y".equals(answer, ignoreCase = true)) {
                ClockManager.getInstance(pack.context).addToTimer(duration)
            } else {
                "Timer unchanged."
            }
        }

        override fun getHint(): Int = R.string.hint_wallpaper_auto_confirm

        override fun isWaitingPermission(): Boolean = false

        override fun argType(): IntArray = IntArray(0)

        override fun priority(): Int = 0

        override fun helpRes(): Int = R.string.help_timer

        override fun onArgNotFound(pack: ExecutePack, indexNotFound: Int): String? = null

        override fun onNotArgEnough(pack: ExecutePack, nArgs: Int): String? = null

        override fun exec(pack: ExecutePack): String? = null
    }
}
