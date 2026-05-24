package ohi.andre.consolelauncher.commands.main.raw

import java.io.File
import java.util.Arrays
import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.commands.CommandAbstraction
import ohi.andre.consolelauncher.commands.ExecutePack
import ohi.andre.consolelauncher.commands.main.MainPack
import ohi.andre.consolelauncher.managers.FileManager
import ohi.andre.consolelauncher.managers.file.FileBackendManager
import ohi.andre.consolelauncher.managers.termux.TermuxBridgeManager
import ohi.andre.consolelauncher.tuils.Tuils

class ls : CommandAbstraction {
    override fun exec(pack: ExecutePack): String {
        val info = pack as MainPack
        if (FileBackendManager.activeBackend(info.context) == FileBackendManager.Active.TERMUX) {
            val currentArgs = info.args
            val path = if (currentArgs != null && currentArgs.isNotEmpty()) info.getString() else null
            val resolved = resolve(info.currentDirectory, path)
            TermuxBridgeManager.dispatchShell(info.context, "ls $resolved", tbridge.LIST_ALL_SCRIPT, TermuxBridgeManager.TERMUX_HOME, resolved)
            return "Termux bridge listing: $resolved"
        }

        val currentArgs = info.args
        val file = if (currentArgs != null && currentArgs.isNotEmpty()) {
            resolveNative(info.currentDirectory, info.getString())
        } else {
            info.currentDirectory
        }
        if (file == null || !file.exists()) {
            return info.res.getString(R.string.output_filenotfound)
        }
        if (file.isFile) {
            return file.name
        }

        val names = file.list()
        if (names == null || names.isEmpty()) {
            return "[]"
        }
        Arrays.sort(names, String.CASE_INSENSITIVE_ORDER)
        for (i in names.indices) {
            val child = File(file, names[i])
            if (child.isDirectory) {
                names[i] = names[i] + File.separator
            }
        }
        return Tuils.toPlanString(names, Tuils.NEWLINE)
    }

    override fun helpRes(): Int = R.string.help_ls

    override fun argType(): IntArray = intArrayOf(CommandAbstraction.PLAIN_TEXT)

    override fun priority(): Int = 4

    override fun onArgNotFound(pack: ExecutePack, indexNotFound: Int): String = (pack as MainPack).res.getString(R.string.output_filenotfound)

    override fun onNotArgEnough(pack: ExecutePack, nArgs: Int): String = exec(pack)

    private fun resolve(currentDirectory: File, path: String?): String {
        if (path == null || path.trim().isEmpty()) {
            return currentDirectory.absolutePath
        }
        val cleanPath = path.trim()
        val file = if (cleanPath.startsWith(File.separator)) File(cleanPath) else File(currentDirectory, cleanPath)
        return file.absolutePath
    }

    private fun resolveNative(currentDirectory: File, path: String?): File? {
        val dirInfo = FileManager.cd(currentDirectory, path)
        return dirInfo?.file
    }
}
