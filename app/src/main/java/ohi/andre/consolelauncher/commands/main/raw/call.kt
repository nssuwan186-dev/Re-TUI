package ohi.andre.consolelauncher.commands.main.raw

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import ohi.andre.consolelauncher.LauncherActivity
import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack
import ohi.andre.consolelauncher.commands.main.MainPack
import ohi.andre.consolelauncher.tuils.Tuils

class call : CommandAbstraction {
    override fun exec(pack: ExecutePack): String {
        val info = pack as MainPack
        if (
            ContextCompat.checkSelfPermission(info.context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(info.context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                info.context as Activity,
                arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE),
                LauncherActivity.COMMAND_REQUEST_PERMISSION
            )
            return info.context.getString(R.string.output_waitingpermission)
        }

        val number = info.getString() ?: return pack.context.getString(R.string.invalid_number)

        val s = StringBuilder(Tuils.EMPTYSTRING)
        for (c in number.toCharArray()) {
            if (c == '#') {
                s.append(Uri.encode("#"))
            } else {
                s.append(c)
            }
        }

        val uri = Uri.parse("tel:$s") ?: return pack.context.getString(R.string.invalid_number)
        val intent = Intent(Intent.ACTION_CALL, uri)

        try {
            (pack.context as Activity).runOnUiThread { info.context.startActivity(intent) }
        } catch (e: SecurityException) {
            return info.res.getString(R.string.output_nopermissions)
        }

        return info.res.getString(R.string.calling) + " " + number
    }

    override fun helpRes(): Int = R.string.help_call

    override fun argType(): IntArray = intArrayOf(CommandAbstraction.CONTACTNUMBER)

    override fun priority(): Int = 5

    override fun onNotArgEnough(pack: ExecutePack, nArgs: Int): String = pack.context.getString(helpRes())

    override fun onArgNotFound(pack: ExecutePack, indexNotFound: Int): String {
        val info = pack as MainPack
        return info.res.getString(R.string.output_numbernotfound)
    }
}
