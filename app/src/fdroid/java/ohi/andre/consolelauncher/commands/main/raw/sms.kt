package ohi.andre.consolelauncher.commands.main.raw

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import ohi.andre.consolelauncher.LauncherActivity
import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack
import ohi.andre.consolelauncher.commands.main.MainPack
import ohi.andre.consolelauncher.commands.main.specific.RedirectCommand

class sms : RedirectCommand() {
    @Throws(Exception::class)
    override fun exec(pack: ExecutePack): String? {
        val info = pack as MainPack
        if (ContextCompat.checkSelfPermission(info.context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                info.context as Activity,
                arrayOf(Manifest.permission.READ_CONTACTS),
                LauncherActivity.COMMAND_REQUEST_PERMISSION
            )
            return info.context.getString(R.string.output_waitingpermission)
        }

        beforeObjects.add(pack.getString())

        return if (afterObjects.size == 0) {
            info.redirectator!!.prepareRedirection(this)
            null
        } else {
            onRedirect(info)
        }
    }

    override fun argType(): IntArray = intArrayOf(CommandAbstraction.CONTACTNUMBER)

    override fun priority(): Int = 3

    override fun helpRes(): Int = R.string.help_sms

    override fun onNotArgEnough(info: ExecutePack, nArgs: Int): String =
        info.context.getString(helpRes())

    override fun onArgNotFound(pack: ExecutePack, indexNotFound: Int): String {
        val info = pack as MainPack
        return info.res.getString(R.string.output_numbernotfound)
    }

    override fun onRedirect(pack: ExecutePack): String {
        val info = pack as MainPack

        val number = beforeObjects[0] as String
        val message = afterObjects[0] as String
        if (message.isEmpty()) {
            info.redirectator!!.cleanup()
            return info.res.getString(R.string.output_smsnotsent)
        }

        if (ContextCompat.checkSelfPermission(info.context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                info.context as Activity,
                arrayOf(Manifest.permission.SEND_SMS),
                LauncherActivity.COMMAND_REQUEST_PERMISSION
            )
            return info.context.getString(R.string.output_waitingpermission)
        }

        info.redirectator!!.cleanup()

        return try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(number, null, message, null, null)

            cleanup()
            info.res.getString(R.string.output_smssent)
        } catch (ex: Exception) {
            cleanup()
            ex.toString()
        }
    }

    override fun getHint(): Int = R.string.sms_hint

    override fun isWaitingPermission(): Boolean = beforeObjects.size == 1 && afterObjects.size == 1
}
