@file:Suppress("DEPRECATION")

package ohi.andre.consolelauncher.commands.main.raw

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.io.File
import ohi.andre.consolelauncher.MainManager
import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.UIManager
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack
import ohi.andre.consolelauncher.commands.main.MainPack
import ohi.andre.consolelauncher.managers.FileManager
import ohi.andre.consolelauncher.managers.file.FileBackendManager
import ohi.andre.consolelauncher.managers.termux.TermuxBridgeManager

class cd : CommandAbstraction {
    override fun exec(pack: ExecutePack): String {
        val info = pack as MainPack
        if (FileBackendManager.activeBackend(info.context) == FileBackendManager.Active.TERMUX) {
            val path = info.getString()
            if (path == null || path.trim().isEmpty()) {
                return info.res.getString(helpRes())
            }
            val resolved = resolve(info.currentDirectory, path)
            TermuxBridgeManager.dispatchShell(info.context, "cd $resolved", tbridge.CD_SCRIPT, TermuxBridgeManager.TERMUX_HOME, resolved)
            return "Termux bridge checking directory: $resolved"
        }

        val dirInfo = FileManager.cd(info.currentDirectory, info.getString())
        val folder = dirInfo?.file
        if (folder == null || !folder.exists()) {
            return info.res.getString(R.string.output_filenotfound)
        }
        if (!folder.isDirectory) {
            return "This is a file"
        }

        info.currentDirectory = folder
        if (MainManager.interactive != null) {
            MainManager.interactive.addCommand("cd '" + folder.absolutePath.replace("'", "'\\''") + "'")
        }
        LocalBroadcastManager.getInstance(info.context.applicationContext)
            .sendBroadcast(Intent(UIManager.ACTION_UPDATE_HINT))
        return folder.absolutePath
    }

    override fun helpRes(): Int = R.string.help_cd

    override fun argType(): IntArray = intArrayOf(CommandAbstraction.PLAIN_TEXT)

    override fun priority(): Int = 5

    override fun onNotArgEnough(pack: ExecutePack, nArgs: Int): String = pack.context.getString(helpRes())

    override fun onArgNotFound(pack: ExecutePack, indexNotFound: Int): String = (pack as MainPack).res.getString(R.string.output_filenotfound)

    private fun resolve(currentDirectory: File, path: String): String {
        val cleanPath = path.trim()
        val file = if (cleanPath.startsWith(File.separator)) File(cleanPath) else File(currentDirectory, cleanPath)
        return file.absolutePath
    }
}
