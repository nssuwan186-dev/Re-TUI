package ohi.andre.consolelauncher.commands.main.raw;

import android.content.Intent;
import android.text.TextUtils;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.UIManager;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.managers.modules.ModuleManager;
import ohi.andre.consolelauncher.managers.modules.ModulePromptManager;

public class module implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) {
        Object arg = pack.get(Object.class, 0);
        String input = arg == null ? "" : arg.toString().trim();
        if (input.length() == 0 || "-ls".equalsIgnoreCase(input)) {
            return listModules(pack);
        }

        String[] parts = input.split("\\s+");
        String option = parts[0].toLowerCase();

        if ("-show".equals(option) || "-open".equals(option)) {
            if (parts.length < 2) return pack.context.getString(R.string.help_module);
            String module = ModuleManager.normalize(parts[1]);
            if (!ModuleManager.isKnown(pack.context, module)) {
                return "Unknown module: " + parts[1];
            }
            send(pack, "show", module);
            return "Module opened: " + module;
        }

        if ("-close".equals(option)) {
            send(pack, "close", null);
            return "Module closed.";
        }

        if ("-prompt".equals(option)) {
            if (parts.length < 3) return pack.context.getString(R.string.help_module);
            String module = ModuleManager.normalize(parts[1]);
            String action = parts[2].toLowerCase();
            if (!ModuleManager.REMINDER.equals(module)) {
                return "No native prompt session for module: " + module;
            }
            send(pack, "show", ModuleManager.REMINDER);
            if ("add".equals(action) || "-add".equals(action)) {
                ModulePromptManager.startReminderAdd(pack.context);
                return "Reminder prompt started.";
            }
            if ("edit".equals(action) || "-edit".equals(action)) {
                ModulePromptManager.startReminderEdit(pack.context);
                return "Reminder edit prompt started.";
            }
            if ("remove".equals(action) || "rm".equals(action) || "-rm".equals(action)) {
                ModulePromptManager.startReminderRemove(pack.context);
                return "Reminder remove prompt started.";
            }
            return pack.context.getString(R.string.output_invalid_param) + " " + parts[2];
        }

        if ("-hide".equals(option)) {
            if (parts.length < 2) return pack.context.getString(R.string.help_module);
            ModuleManager.hideFromDock(pack.context, parts[1]);
            send(pack, "rebuild", null);
            return "Module hidden from dock: " + ModuleManager.normalize(parts[1]);
        }

        if ("-add".equals(option)) {
            List<String> args = ohi.andre.consolelauncher.tuils.Tuils.splitArgs(input);
            if (args.size() < 3) return pack.context.getString(R.string.help_module);
            String module = ModuleManager.normalize(args.get(1));
            String path = args.get(2);
            ModuleManager.setScriptModule(pack.context, module, path);
            ModuleManager.addToDock(pack.context, Arrays.asList(module));
            send(pack, "rebuild", null);
            if (ModuleManager.isLauncherSource(ModuleManager.getModuleSource(pack.context, module))) {
                send(pack, "refresh", module);
            }
            return "Module added: " + module
                    + "\nSource: " + ModuleManager.getModuleSource(pack.context, module)
                    + "\nRun module -refresh " + module + " to update it.";
        }

        if ("-refresh".equals(option)) {
            if (parts.length < 2) return pack.context.getString(R.string.help_module);
            String module = ModuleManager.normalize(parts[1]);
            if (!ModuleManager.isKnown(pack.context, module)) {
                return "Unknown module: " + parts[1];
            }
            if (TextUtils.isEmpty(ModuleManager.getModuleSource(pack.context, module))) {
                return "Module has no source: " + module;
            }
            send(pack, "refresh", module);
            return "Module refresh dispatched: " + module;
        }

        if ("-rm".equals(option) || "-remove".equals(option)) {
            if (parts.length < 2) return pack.context.getString(R.string.help_module);
            String module = ModuleManager.normalize(parts[1]);
            if (!ModuleManager.isKnown(pack.context, module)) {
                return "Unknown module: " + parts[1];
            }
            if (ModuleManager.getBuiltIns().contains(module)) {
                return "Built-in modules cannot be removed. Use module -hide " + module + " instead.";
            }
            ModuleManager.removeScriptModule(pack.context, module);
            send(pack, "rebuild", null);
            return "Module removed from registry: " + module;
        }

        if ("-dock".equals(option)) {
            if (parts.length < 3) return pack.context.getString(R.string.help_module);
            String mode = parts[1].toLowerCase();
            String verb;
            if ("add".equals(mode) || "-add".equals(mode)) {
                verb = "added";
            } else if ("remove".equals(mode) || "-remove".equals(mode) || "rm".equals(mode) || "-rm".equals(mode)) {
                verb = "removed";
            } else {
                return pack.context.getString(R.string.output_invalid_param) + " " + parts[1]
                        + "\nUse module -dock add [name] or module -dock remove [name].";
            }

            List<String> modules = new ArrayList<>(Arrays.asList(parts).subList(2, parts.length));
            if ("added".equals(verb)) {
                ModuleManager.addToDock(pack.context, modules);
            } else {
                ModuleManager.removeFromDock(pack.context, modules);
            }
            send(pack, "rebuild", null);
            return "Module dock " + verb + ": " + formatDock(pack);
        }

        return pack.context.getString(R.string.output_invalid_param) + " " + parts[0];
    }

    private String listModules(ExecutePack pack) {
        return "Modules: " + TextUtils.join(", ", ModuleManager.listAll(pack.context))
                + "\nDock: " + formatDock(pack)
                + "\nUse module -add [name] termux:/path/script.sh, module -refresh [name], module -show [name], events -access, module -prompt reminder add|edit|remove, module -hide [name], module -dock add [name], module -dock remove [name], module -rm [name], module -close.";
    }

    private String formatDock(ExecutePack pack) {
        List<String> dock = ModuleManager.getDock(pack.context);
        return dock.isEmpty() ? "<empty>" : TextUtils.join(", ", dock);
    }

    private void send(ExecutePack pack, String command, String module) {
        Intent intent = new Intent(UIManager.ACTION_MODULE_COMMAND);
        intent.putExtra(UIManager.EXTRA_MODULE_COMMAND, command);
        if (module != null) {
            intent.putExtra(UIManager.EXTRA_MODULE_NAME, module);
        }
        LocalBroadcastManager.getInstance(pack.context.getApplicationContext()).sendBroadcast(intent);
    }

    @Override
    public int[] argType() {
        return new int[] {CommandAbstraction.PLAIN_TEXT};
    }

    @Override
    public int priority() {
        return 3;
    }

    @Override
    public int helpRes() {
        return R.string.help_module;
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int index) {
        return pack.context.getString(R.string.help_module);
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        return listModules(pack);
    }
}
