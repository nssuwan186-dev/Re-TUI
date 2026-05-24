@file:Suppress("DEPRECATION")

package ohi.andre.consolelauncher.managers.callback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.util.Locale
import ohi.andre.consolelauncher.UIManager
import ohi.andre.consolelauncher.managers.modules.ModuleManager
import ohi.andre.consolelauncher.tuils.Tuils

class RetuiCallbackReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || ACTION_CALLBACK != intent.action) {
            return
        }

        val token = intent.getStringExtra(EXTRA_TOKEN)
        if (!CallbackAuthManager.isAuthorized(context, token)) {
            Log.w(TAG, "Rejected callback without valid token")
            return
        }

        val action = lower(intent.getStringExtra(EXTRA_ACTION))
        if ("output" == action) {
            output(context, "[callback] " + safe(intent.getStringExtra(EXTRA_TEXT)))
        } else if ("notify" == action) {
            val title = safe(intent.getStringExtra(EXTRA_TITLE))
            val text = safe(intent.getStringExtra(EXTRA_TEXT))
            output(context, "[callback notify] " + joinTitle(title, text))
        } else if ("module_set" == action || "module" == action) {
            val module = safe(intent.getStringExtra(EXTRA_MODULE))
            val text = safe(intent.getStringExtra(EXTRA_TEXT))
            ModuleManager.setScriptText(context, module, text)
            val update = Intent(UIManager.ACTION_MODULE_COMMAND)
            update.putExtra(UIManager.EXTRA_MODULE_COMMAND, "update")
            update.putExtra(UIManager.EXTRA_MODULE_NAME, ModuleManager.normalize(module))
            LocalBroadcastManager.getInstance(context.applicationContext).sendBroadcast(update)
            output(context, "[callback module] " + joinTitle(module, text))
        } else {
            Log.w(TAG, "Unsupported callback action: $action")
            output(context, "[callback] unsupported action: " + safe(action))
        }
    }

    companion object {
        const val ACTION_CALLBACK: String = "com.dvil.tui_renewed.RETUI_CALLBACK"
        const val EXTRA_TOKEN: String = "token"
        const val EXTRA_ACTION: String = "action"
        const val EXTRA_TEXT: String = "text"
        const val EXTRA_TITLE: String = "title"
        const val EXTRA_MODULE: String = "module"

        private const val TAG = "TUI-Callback"

        private fun output(context: Context, text: String) {
            Tuils.sendOutput(context.applicationContext, text)
        }

        private fun joinTitle(title: String, text: String): String {
            if (title.isEmpty()) {
                return text
            }
            if (text.isEmpty()) {
                return title
            }
            return "$title: $text"
        }

        private fun safe(value: String?): String = value?.trim() ?: ""

        private fun lower(value: String?): String = value?.trim()?.lowercase(Locale.getDefault()) ?: ""
    }
}
