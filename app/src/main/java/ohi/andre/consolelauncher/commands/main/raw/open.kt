package ohi.andre.consolelauncher.commands.main.raw

import android.content.Intent
import java.io.File
import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack
import ohi.andre.consolelauncher.commands.main.MainPack
import ohi.andre.consolelauncher.managers.FileManager
import ohi.andre.consolelauncher.managers.TerminalManager
import ohi.andre.consolelauncher.managers.file.FileBackendManager
import ohi.andre.consolelauncher.managers.termux.TermuxBridgeManager
import ohi.andre.consolelauncher.tuils.Tuils

open class `open` : CommandAbstraction {
    override fun exec(pack: ExecutePack): String? {
        val info = pack as MainPack
        val path = info.getString()
        if (FileBackendManager.activeBackend(info.context) == FileBackendManager.Active.TERMUX) {
            val resolved = resolvePath(info.currentDirectory, path)
                ?: return info.res.getString(helpRes())
            TermuxBridgeManager.dispatchShell(
                info.context,
                "open $resolved",
                tbridge.OPEN_FILE_SCRIPT,
                TermuxBridgeManager.TERMUX_HOME,
                resolved
            )
            return "Termux bridge opening file: $resolved"
        }

        val file = resolve(info.currentDirectory, path)
        if (file == null || !file.exists()) {
            return info.res.getString(R.string.output_filenotfound)
        }
        if (file.isDirectory) {
            return info.res.getString(R.string.output_isdirectory)
        }

        val view = Tuils.openFile(info.context, file)
        info.context.startActivity(Intent.createChooser(view, "Open with"))

        Tuils.sendOutput(info.context, "Opening: " + file.name, TerminalManager.CATEGORY_OUTPUT)
        return null
    }

    private fun resolvePath(currentDirectory: File, path: String?): String? {
        if (path == null || path.trim().isEmpty()) {
            return null
        }
        val cleanPath = path.trim()
        val file = if (cleanPath.startsWith(File.separator)) File(cleanPath) else File(currentDirectory, cleanPath)
        return file.absolutePath
    }

    private fun resolve(currentDirectory: File, path: String?): File? {
        if (path == null || path.trim().isEmpty()) {
            return null
        }

        val cleanPath = path.trim()
        val file = if (cleanPath.startsWith(File.separator)) File(cleanPath) else File(currentDirectory, cleanPath)
        if (file.exists()) {
            return file
        }

        val dirInfo = FileManager.cd(currentDirectory, cleanPath)
        if (dirInfo != null && dirInfo.notFound == null) {
            return dirInfo.file
        }
        return file
    }

    override fun helpRes(): Int = R.string.help_open

    override fun argType(): IntArray = intArrayOf(CommandAbstraction.PLAIN_TEXT)

    override fun priority(): Int = 4

    override fun onNotArgEnough(pack: ExecutePack, nArgs: Int): String {
        val info = pack as MainPack
        return info.res.getString(helpRes())
    }

    override fun onArgNotFound(pack: ExecutePack, indexNotFound: Int): String {
        val info = pack as MainPack
        return info.res.getString(R.string.output_filenotfound)
    }
}
