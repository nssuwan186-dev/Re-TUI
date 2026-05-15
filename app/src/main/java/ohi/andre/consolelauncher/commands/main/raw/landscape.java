package ohi.andre.consolelauncher.commands.main.raw;

import android.app.Activity;
import android.content.pm.ActivityInfo;

import ohi.andre.consolelauncher.LauncherActivity;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.managers.settings.LauncherSettings;
import ohi.andre.consolelauncher.managers.xml.options.Behavior;

public class landscape implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) {
        MainPack info = (MainPack) pack;
        LauncherSettings.set(info.context, Behavior.orientation, "0");

        if (info.context instanceof LauncherActivity) {
            ((LauncherActivity) info.context).applyOrientationPreference();
        } else if (info.context instanceof Activity) {
            ((Activity) info.context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        return "Landscape mode enabled. Use config -set orientation 1 for portrait or config -set orientation 2 for auto-rotate.";
    }

    @Override
    public int[] argType() {
        return new int[0];
    }

    @Override
    public int priority() {
        return 4;
    }

    @Override
    public int helpRes() {
        return R.string.help_landscape;
    }

    @Override
    public String onArgNotFound(ExecutePack info, int index) {
        return null;
    }

    @Override
    public String onNotArgEnough(ExecutePack info, int nArgs) {
        return null;
    }
}
