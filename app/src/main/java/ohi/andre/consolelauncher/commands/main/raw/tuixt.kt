package ohi.andre.consolelauncher.commands.main.raw

import android.app.Activity
import android.content.Intent
import java.io.File
import ohi.andre.consolelauncher.LauncherActivity
import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack
import ohi.andre.consolelauncher.commands.main.MainPack
import ohi.andre.consolelauncher.commands.tuixt.WidgetEditorActivity
import ohi.andre.consolelauncher.tuils.Tuils
import java.io.IOException
import ohi.andre.consolelauncher.managers.FileManager

class tuixt : CommandAbstraction {
    override fun exec(pack: ExecutePack): String {
        val info = pack as MainPack
        val fileName = info.getString() ?: return onNotArgEnough(info, 0)

        val file = File(Tuils.getFolder(), fileName)
        if (!file.exists()) {
            return info.res.getString(R.string.output_filenotfound)
        }

        if (file.isDirectory) {
            return info.res.getString(R.string.output_isdirectory)
        }

        val intent = Intent(info.context, WidgetEditorActivity::class.java)
        intent.putExtra(WidgetEditorActivity.EXTRA_FILE_PATH, file.absolutePath)
        (info.context as Activity).startActivityForResult(intent, LauncherActivity.TUIXT_REQUEST)

        return Tuils.EMPTYSTRING
    }

    override fun argType(): IntArray = intArrayOf(CommandAbstraction.CONFIG_FILE)

    override fun priority(): Int = 3

    override fun helpRes(): Int = R.string.help_tuixt

    override fun onArgNotFound(pack: ExecutePack, indexNotFound: Int): String = pack.context.getString(R.string.help_tuixt)

    override fun onNotArgEnough(pack: ExecutePack, nArgs: Int): String {
        val info = pack as MainPack
        return info.res.getString(R.string.help_tuixt)
    }
}
