@file:Suppress("DEPRECATION")

package ohi.andre.consolelauncher.tuils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import ohi.andre.consolelauncher.BuildConfig
import ohi.andre.consolelauncher.MainManager
import android.os.Bundle

class PublicIOReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = when (intent.action) {
            ACTION_CMD -> {
                val remoteInput = RemoteInput.getResultsFromIntent(intent) ?: return
                val cmd = remoteInput.getString(PrivateIOReceiver.TEXT)
                val wasMusic = remoteInput.getBoolean(MainManager.MUSIC_SERVICE) ||
                    intent.getBooleanExtra(MainManager.MUSIC_SERVICE, false)

                intent.putExtra(MainManager.MUSIC_SERVICE, wasMusic)
                intent.putExtra(MainManager.CMD, cmd)
                intent.putExtra(MainManager.CMD_COUNT, MainManager.commandCount)
                MainManager.ACTION_EXEC
            }
            ACTION_OUTPUT -> PrivateIOReceiver.ACTION_OUTPUT
            else -> return
        }

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent.setAction(action))
    }

    companion object {
        @JvmField val ACTION_OUTPUT: String = BuildConfig.APPLICATION_ID + ".action_public_output"
        @JvmField val ACTION_CMD: String = BuildConfig.APPLICATION_ID + ".action_public_cmd"
    }
}
