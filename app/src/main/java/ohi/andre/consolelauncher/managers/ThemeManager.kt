package ohi.andre.consolelauncher.managers

import android.content.Context
import android.graphics.Color
import ohi.andre.consolelauncher.BuildConfig
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.regex.Pattern
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.util.regex.Matcher
import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager
import ohi.andre.consolelauncher.tuils.Tuils
import ohi.andre.consolelauncher.tuils.interfaces.Reloadable
import okhttp3.OkHttpClient
import okhttp3.Response

/**
 * Created by francescoandreuzzi on 17/02/2018.
 */
class ThemeManager(client: OkHttpClient, context: Context, reloadable: Reloadable) {
    var client: OkHttpClient
    var context: Context
    var reloadable: Reloadable

    var parser: Pattern =
        Pattern.compile("(<SUGGESTIONS>.+<\\/SUGGESTIONS>).*(<THEME>.+<\\/THEME>)", Pattern.DOTALL)

    var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.getAction() == ACTION_APPLY) {
                val name: String? = intent.getStringExtra(NAME)
                if (name == null) return

                //                name needs to be the absolute path
                if (name.endsWith(".zip")) apply(File(name))
                else apply(name)
            } else if (intent.getAction() == ACTION_REVERT) {
                revert()
            } else if (intent.getAction() == ACTION_STANDARD) {
                standard()
            }
        }
    }

    fun dispose() {
        LocalBroadcastManager.getInstance(context.getApplicationContext())
            .unregisterReceiver(receiver)
    }

    fun apply(themeName: String) {
        object : Thread() {
            override fun run() {
                super.run()

                if (!Tuils.hasInternetAccess()) {
                    Tuils.sendOutput(Color.RED, context, R.string.no_internet)
                    return
                }

                val url =
                    "https://tui.tarunshankerpandey.com/show_data.php?data_type=xml&theme_id=" + themeName

                val builder = Request.Builder()
                    .url(url)
                    .get()

                try {
                    client.newCall(builder.build()).execute().use { response ->
                        if (response.isSuccessful) {
                            var string: String?
                            try {
                                val body = response.body
                                string = if (body != null) body.string() else Tuils.EMPTYSTRING
                            } catch (e: IOException) {
                                string = Tuils.EMPTYSTRING
                            }

                            if (string!!.length == 0) {
                                Tuils.sendOutput(context, R.string.theme_not_found)
                                return
                            }

                            val m = parser.matcher(string)
                            if (m.find()) {
                                val suggestions = m.group(1)
                                val theme = m.group(2)

                                applyTheme(theme, suggestions, true, themeName)
                            } else {
                                Tuils.sendOutput(context, R.string.theme_not_found)
                            }
                        }
                    }
                } catch (e: IOException) {
                    Tuils.sendOutput(context, e.toString())
                }
            }
        }.start()
    }

    fun apply(zip: File?) {
    }

    private fun applyTheme(theme: File?, suggestions: File?, keepOld: Boolean) {
        if (theme == null || suggestions == null) {
            Tuils.sendOutput(context, R.string.theme_unable)
            return
        }

        val oldTheme: File = File(Tuils.getFolder(), XMLPrefsManager.XMLPrefsRoot.THEME.path)
        val oldSuggestions: File =
            File(Tuils.getFolder(), XMLPrefsManager.XMLPrefsRoot.SUGGESTIONS.path)
        if (keepOld) {
            Tuils.insertOld(oldTheme)
            Tuils.insertOld(oldSuggestions)
        }

        theme.renameTo(oldTheme)
        suggestions.renameTo(oldSuggestions)

        reloadable.reload()
    }

    private fun applyTheme(
        theme: String?,
        suggestions: String?,
        keepOld: Boolean,
        themeName: String?
    ) {
        var theme = theme
        var suggestions = suggestions
        if (theme == null || suggestions == null) {
            Tuils.sendOutput(context, R.string.theme_unable)
            return
        }

        var colorMatcher = colorParser.matcher(theme)
        while (colorMatcher.find()) {
            theme = Pattern.compile(Pattern.quote(colorMatcher.group())).matcher(theme)
                .replaceAll(toHexColor(colorMatcher.group()))
        }

        colorMatcher = colorParser.matcher(suggestions)
        while (colorMatcher.find()) {
            suggestions = Pattern.compile(Pattern.quote(colorMatcher.group())).matcher(suggestions)
                .replaceAll(toHexColor(colorMatcher.group()))
        }

        val oldTheme: File = File(Tuils.getFolder(), XMLPrefsManager.XMLPrefsRoot.THEME.path)
        val oldSuggestions: File =
            File(Tuils.getFolder(), XMLPrefsManager.XMLPrefsRoot.SUGGESTIONS.path)
        if (keepOld) {
            Tuils.insertOld(oldTheme)
            Tuils.insertOld(oldSuggestions)
        }
        oldTheme.delete()
        oldSuggestions.delete()

        try {
            val themeStream = FileOutputStream(oldTheme)
            themeStream.write(theme.toByteArray())
            themeStream.flush()
            themeStream.close()

            val suggestionsStream = FileOutputStream(oldSuggestions)
            suggestionsStream.write(suggestions.toByteArray())
            suggestionsStream.flush()
            suggestionsStream.close()

            reloadable.addMessage(
                context.getString(R.string.theme_applied) + Tuils.SPACE + themeName,
                null
            )
            reloadable.reload()
        } catch (e: IOException) {
            Tuils.sendOutput(context, R.string.output_error)
        }
    }

    private fun revert() {
        applyTheme(
            Tuils.getOld(XMLPrefsManager.XMLPrefsRoot.THEME.path),
            Tuils.getOld(XMLPrefsManager.XMLPrefsRoot.SUGGESTIONS.path),
            false
        )
    }

    private fun standard() {
        val oldTheme: File = File(Tuils.getFolder(), XMLPrefsManager.XMLPrefsRoot.THEME.path)
        val oldSuggestions: File =
            File(Tuils.getFolder(), XMLPrefsManager.XMLPrefsRoot.SUGGESTIONS.path)
        Tuils.insertOld(oldTheme)
        Tuils.insertOld(oldSuggestions)

        oldTheme.delete()
        oldSuggestions.delete()

        reloadable.addMessage(
            context.getString(R.string.theme_applied) + Tuils.SPACE + "standard",
            null
        )
    }

    //    rgba(255,87,34,1)
    var colorParser: Pattern =
        Pattern.compile("rgba\\([\\s]*(\\d+),[\\s]*(\\d+),[\\s]*(\\d+),[\\s]*(\\d.*\\d*)[\\s]*\\)")

    init {
        this.client = client
        this.context = context
        this.reloadable = reloadable

        val filter: IntentFilter = IntentFilter()
        filter.addAction(ACTION_APPLY)
        filter.addAction(ACTION_REVERT)
        filter.addAction(ACTION_STANDARD)

        LocalBroadcastManager.getInstance(context.getApplicationContext())
            .registerReceiver(receiver, filter)
    }

    private fun toHexColor(color: String): String {
        val m = colorParser.matcher(color)
        if (m.find()) {
            val red = m.group(1).toInt()
            val green = m.group(2).toInt()
            val blue = m.group(3).toInt()
            val alpha = m.group(4).toFloat()

            var redHex = Integer.toHexString(red)
            if (redHex.length == 1) redHex = "0" + redHex

            var greenHex = Integer.toHexString(green)
            if (greenHex.length == 1) greenHex = "0" + greenHex

            var blueHex = Integer.toHexString(blue)
            if (blueHex.length == 1) blueHex = "0" + blueHex

            var alphaHex = Integer.toHexString(alpha.toInt())
            if (alphaHex.length == 1) alphaHex = "0" + alphaHex

            return "#" + (if (alpha == 1f) Tuils.EMPTYSTRING else alphaHex) + redHex + greenHex + blueHex
        } else return Tuils.EMPTYSTRING
    }

    companion object {
        var ACTION_APPLY: String = BuildConfig.APPLICATION_ID + ".theme_apply"
        var ACTION_REVERT: String = BuildConfig.APPLICATION_ID + ".theme_revert"
        var ACTION_STANDARD: String = BuildConfig.APPLICATION_ID + ".theme_standard"

        var NAME: String = "name"
    }
}
