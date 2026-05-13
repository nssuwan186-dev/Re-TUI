package ohi.andre.consolelauncher.commands.main.raw;

import android.app.Activity;
import android.content.Intent;

import java.io.File;
import java.io.IOException;

import ohi.andre.consolelauncher.LauncherActivity;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.tuixt.WidgetEditorActivity;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.managers.FileManager;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 18/01/2017.
 */

public class tuixt implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) {
        MainPack info = (MainPack) pack;
        String fileName = info.getString();
        if (fileName == null) {
            return onNotArgEnough(info, 0);
        }

        File file = new File(Tuils.getFolder(), fileName);
        if(!file.exists()) {
            return info.res.getString(R.string.output_filenotfound);
        }

        if(file.isDirectory()) {
            return info.res.getString(R.string.output_isdirectory);
        }

        Intent intent = new Intent(info.context, WidgetEditorActivity.class);
        intent.putExtra(WidgetEditorActivity.EXTRA_FILE_PATH, file.getAbsolutePath());
        ((Activity) info.context).startActivityForResult(intent, LauncherActivity.TUIXT_REQUEST);

        return Tuils.EMPTYSTRING;
    }

    @Override
    public int[] argType() {
        return new int[] {CommandAbstraction.CONFIG_FILE};
    }

    @Override
    public int priority() {
        return 3;
    }

    @Override
    public int helpRes() {
        return R.string.help_tuixt;
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int index) {
        return pack.context.getString(R.string.help_tuixt);
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        MainPack info = (MainPack) pack;
        return info.res.getString(R.string.help_tuixt);
    }
}
