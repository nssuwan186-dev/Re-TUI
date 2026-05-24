@file:Suppress("OVERRIDE_DEPRECATION")

package ohi.andre.consolelauncher.tuils.stuff

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class PolicyReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence = ""

    override fun onDisabled(context: Context, intent: Intent) {
    }

    override fun onPasswordChanged(context: Context, intent: Intent) {
    }

    override fun onPasswordFailed(context: Context, intent: Intent) {
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent) {
    }
}
