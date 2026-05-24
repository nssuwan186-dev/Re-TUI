package ohi.andre.consolelauncher.commands.tuixt

import android.content.Context
import android.content.res.Resources
import android.widget.EditText
import java.io.File
import ohi.andre.consolelauncher.commands.CommandGroup
import ohi.andre.consolelauncher.commands.ExecutePack

class TuixtPack(
    group: CommandGroup,
    file: File,
    context: Context,
    @JvmField var editText: EditText
) : ExecutePack(group) {
    @JvmField var editFile: File = file
    @JvmField var resources: Resources = context.resources

    init {
        this.context = context
    }
}
