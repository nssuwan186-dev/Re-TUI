package ohi.andre.consolelauncher.commands.main.raw

import android.content.Intent
import java.io.File
import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack
import ohi.andre.consolelauncher.commands.main.MainPack
import ohi.andre.consolelauncher.managers.FileManager
import ohi.andre.consolelauncher.managers.file.FileBackendManager
import ohi.andre.consolelauncher.managers.termux.TermuxBridgeManager
import ohi.andre.consolelauncher.tuils.Tuils

class share : CommandAbstraction {
    override fun exec(pack: ExecutePack): String {
        val info = pack as MainPack
        val path = info.getString()
        if (FileBackendManager.activeBackend(info.context) == FileBackendManager.Active.TERMUX) {
            val resolved = resolvePath(info.currentDirectory, path)
            if (resolved == null) {
                return info.res.getString(helpRes())
            }
            TermuxBridgeManager.dispatchShell(info.context, "share $resolved", tbridge.SHARE_FILE_SCRIPT, TermuxBridgeManager.TERMUX_HOME, resolved)
            return "Termux bridge sharing file: $resolved"
        }

        val f = resolveNative(info.currentDirectory, path)
        if (f == null || !f.exists()) {
            return info.res.getString(R.string.output_filenotfound)
        }
        if (f.isDirectory) {
            return info.res.getString(R.string.output_isdirectory)
        }

        val sharingIntent = Tuils.shareFile(pack.context, f)
        info.context.startActivity(Intent.createChooser(sharingIntent, info.res.getString(R.string.share_label)))

        return Tuils.EMPTYSTRING
    }

    override fun helpRes(): Int = R.string.help_share

    override fun argType(): IntArray = intArrayOf(CommandAbstraction.PLAIN_TEXT)

    override fun priority(): Int = 3

    override fun onNotArgEnough(pack: ExecutePack, nArgs: Int): String {
        val info = pack as MainPack
        return info.res.getString(helpRes())
    }

    override fun onArgNotFound(pack: ExecutePack, indexNotFound: Int): String {
        val info = pack as MainPack
        return info.res.getString(R.string.output_filenotfound)
    }

    private fun resolvePath(currentDirectory: File, path: String?): String? {
        if (path == null || path.trim().isEmpty()) {
            return null
        }
        val cleanPath = path.trim()
        val file = if (cleanPath.startsWith(File.separator)) File(cleanPath) else File(currentDirectory, cleanPath)
        return file.absolutePath
    }

    private fun resolveNative(currentDirectory: File, path: String?): File? {
        val dirInfo = FileManager.cd(currentDirectory, path ?: Tuils.EMPTYSTRING)
        return dirInfo?.file
    }
}
