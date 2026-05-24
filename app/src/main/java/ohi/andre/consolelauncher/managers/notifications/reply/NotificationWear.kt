package ohi.andre.consolelauncher.managers.notifications.reply

import android.app.PendingIntent
import android.app.RemoteInput
import android.os.Bundle

class NotificationWear {
    @JvmField var app: BoundApp? = null
    @JvmField var pendingIntent: PendingIntent? = null
    @JvmField var remoteInputs: Array<RemoteInput>? = null
    @JvmField var bundle: Bundle? = null
    @JvmField var id = 0
    @JvmField var actionTitle: CharSequence? = null
    @JvmField var semanticAction = 0
    @JvmField var text: CharSequence? = null

    override fun equals(other: Any?): Boolean {
        return try {
            val wear = other as NotificationWear
            wear.app!!.packageName == app!!.packageName
        } catch (e: Exception) {
            false
        }
    }
}
