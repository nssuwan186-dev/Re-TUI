package ohi.andre.consolelauncher.commands.main.raw;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;

public class install implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) {
        MainPack info = (MainPack) pack;
        String appName = (String) pack.args[0];

        if (appName == null || appName.isEmpty()) {
            return info.res.getString(helpRes());
        }

        search.playstoreSearch(appName, info.context);
        return "install is deprecated. Use search -ps " + appName + ".";
    }

    @Override
    public int helpRes() {
        return R.string.help_install;
    }

    @Override
    public int[] argType() {
        return new int[]{CommandAbstraction.PLAIN_TEXT};
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        MainPack info = (MainPack) pack;
        return info.res.getString(helpRes());
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int index) {
        return null;
    }

}
