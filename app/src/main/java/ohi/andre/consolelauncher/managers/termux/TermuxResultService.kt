@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package ohi.andre.consolelauncher.managers.termux

import android.app.IntentService
import android.content.Intent
import androidx.annotation.Nullable

class TermuxResultService : IntentService("TermuxResultService") {
    override fun onHandleIntent(intent: Intent?) {
        TermuxResultReceiver.forwardResult(applicationContext, intent)
    }
}
