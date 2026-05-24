package ohi.andre.consolelauncher.managers.callback

import android.content.Context
import android.content.SharedPreferences
import java.security.SecureRandom

object CallbackAuthManager {
    private const val PREFS = "retui_callback_auth"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_TOKEN = "token"
    private const val TOKEN_BYTES = 32

    @JvmStatic
    fun isEnabled(context: Context): Boolean {
        val prefs = prefs(context)
        return prefs.getBoolean(KEY_ENABLED, false) && !prefs.getString(KEY_TOKEN, "").isNullOrEmpty()
    }

    @JvmStatic
    fun getToken(context: Context): String = prefs(context).getString(KEY_TOKEN, "") ?: ""

    @JvmStatic
    fun getOrCreateToken(context: Context): String {
        val prefs = prefs(context)
        var token = prefs.getString(KEY_TOKEN, "")
        if (token == null || token.isEmpty()) {
            token = newToken()
            prefs.edit()
                .putString(KEY_TOKEN, token)
                .putBoolean(KEY_ENABLED, true)
                .apply()
        }
        return token
    }

    @JvmStatic
    fun rotateToken(context: Context): String {
        val token = newToken()
        prefs(context).edit()
            .putString(KEY_TOKEN, token)
            .putBoolean(KEY_ENABLED, true)
            .apply()
        return token
    }

    @JvmStatic
    fun setEnabled(context: Context, enabled: Boolean) {
        val editor = prefs(context).edit().putBoolean(KEY_ENABLED, enabled)
        if (enabled && getToken(context).isEmpty()) {
            editor.putString(KEY_TOKEN, newToken())
        }
        editor.apply()
    }

    @JvmStatic
    fun isAuthorized(context: Context, candidate: String?): Boolean {
        if (!isEnabled(context) || candidate == null) {
            return false
        }
        return constantTimeEquals(getToken(context), candidate)
    }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun newToken(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        SecureRandom().nextBytes(bytes)
        val builder = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            builder.append(String.format("%02x", b.toInt() and 0xff))
        }
        return builder.toString()
    }

    private fun constantTimeEquals(expected: String?, candidate: String?): Boolean {
        if (expected == null || candidate == null) {
            return false
        }
        var diff = expected.length xor candidate.length
        val length = kotlin.math.min(expected.length, candidate.length)
        for (i in 0 until length) {
            diff = diff or (expected[i].code xor candidate[i].code)
        }
        return diff == 0
    }
}
