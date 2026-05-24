@file:Suppress("DEPRECATION")

package ohi.andre.consolelauncher.commands.main.raw

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack
import ohi.andre.consolelauncher.commands.main.MainPack

class bluetooth : CommandAbstraction {
    @SuppressLint("MissingPermission")
    override fun exec(pack: ExecutePack): String {
        val info = pack as MainPack
        val adapter = BluetoothAdapter.getDefaultAdapter()
            ?: return info.context.getString(R.string.output_bluetooth_unavailable)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            info.context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            return "Opening Bluetooth settings. Android no longer allows third-party launchers to toggle Bluetooth directly."
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(info.context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return info.context.getString(R.string.output_waitingpermission)
            }
        }

        return if (adapter.isEnabled) {
            adapter.disable()
            info.context.getString(R.string.output_bluetooth) + " false"
        } else {
            adapter.enable()
            info.context.getString(R.string.output_bluetooth) + " true"
        }
    }

    override fun helpRes(): Int = R.string.help_bluetooth

    override fun argType(): IntArray = intArrayOf()

    override fun priority(): Int = 2

    override fun onNotArgEnough(pack: ExecutePack, nArgs: Int): String? = null

    override fun onArgNotFound(pack: ExecutePack, indexNotFound: Int): String? = null
}
