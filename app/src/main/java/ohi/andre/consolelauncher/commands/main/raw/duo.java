package ohi.andre.consolelauncher.commands.main.raw;

import android.content.res.Configuration;

import java.util.Locale;

import ohi.andre.consolelauncher.LauncherActivity;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.UIManager;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.CommandTuils;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.managers.settings.LauncherSettings;
import ohi.andre.consolelauncher.managers.xml.options.Behavior;

public class duo implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) {
        if (!LauncherSettings.getBoolean(Behavior.duo_mode)) {
            return "Duo command is disabled. Enable it with: config -set duo_mode true";
        }

        UIManager ui = null;
        if (pack.context instanceof LauncherActivity) {
            ui = ((LauncherActivity) pack.context).getUIManager();
        }
        if (ui == null) {
            return "Duo layout is only available from the launcher screen.";
        }

        String input = pack.getString();
        String mode = input == null ? "status" : input.trim().toLowerCase(Locale.US);
        if (mode.length() == 0) {
            mode = "status";
        }

        if ("status".equals(mode) || "-status".equals(mode)) {
            return status(ui);
        }

        if ("off".equals(mode) || "-off".equals(mode) || "0".equals(mode)) {
            ui.setDuoLayoutMode(UIManager.DUO_LAYOUT_OFF);
            return "Duo layout off. Normal landscape split restored.";
        }

        if ("left".equals(mode) || "-left".equals(mode)) {
            ui.setDuoLayoutMode(UIManager.DUO_LAYOUT_LEFT);
            return appliedMessage(pack, "left");
        }

        if ("right".equals(mode) || "-right".equals(mode)) {
            ui.setDuoLayoutMode(UIManager.DUO_LAYOUT_RIGHT);
            return appliedMessage(pack, "right");
        }

        if ("on".equals(mode) || "-on".equals(mode) || "1".equals(mode)) {
            String side = ui.enableLastDuoSide();
            return appliedMessage(pack, side);
        }

        if ("toggle".equals(mode) || "-toggle".equals(mode)) {
            if (UIManager.DUO_LAYOUT_OFF.equals(ui.getDuoLayoutMode())) {
                String side = ui.enableLastDuoSide();
                return appliedMessage(pack, side);
            }
            ui.setDuoLayoutMode(UIManager.DUO_LAYOUT_OFF);
            return "Duo layout off. Normal landscape split restored.";
        }

        return "Unknown duo option: " + input + "\nUsage: " + CommandTuils.DUO_USAGE;
    }

    private String appliedMessage(ExecutePack pack, String side) {
        boolean landscape = pack.context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        String message = "Duo layout active on the " + side + " side.";
        if (!landscape) {
            message += " Rotate to landscape to see it.";
        }
        return message;
    }

    private String status(UIManager ui) {
        return "Duo command: enabled\nDuo layout: " + ui.getDuoLayoutMode()
                + "\nUsage: " + CommandTuils.DUO_USAGE;
    }

    @Override
    public int[] argType() {
        return new int[] {CommandAbstraction.PLAIN_TEXT};
    }

    @Override
    public int priority() {
        return 4;
    }

    @Override
    public int helpRes() {
        return R.string.help_duo;
    }

    @Override
    public String onArgNotFound(ExecutePack info, int index) {
        return info.context.getString(R.string.help_duo);
    }

    @Override
    public String onNotArgEnough(ExecutePack info, int nArgs) {
        if (!LauncherSettings.getBoolean(Behavior.duo_mode)) {
            return "Duo command is disabled. Enable it with: config -set duo_mode true";
        }
        if (info.context instanceof LauncherActivity) {
            UIManager ui = ((LauncherActivity) info.context).getUIManager();
            if (ui != null) {
                return status(ui);
            }
        }
        return info.context.getString(R.string.help_duo);
    }
}
