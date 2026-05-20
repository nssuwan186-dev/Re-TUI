package ohi.andre.consolelauncher.commands.main.raw;

import android.app.Activity;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.util.List;
import java.util.Locale;

import ohi.andre.consolelauncher.LauncherActivity;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.UIManager;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.managers.NotesManager;
import ohi.andre.consolelauncher.managers.modules.ModuleManager;
import ohi.andre.consolelauncher.commands.tuixt.NotesEditorActivity;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 12/02/2018.
 */

public class notes implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) {
        String input = pack.getString();
        if(input == null || input.trim().length() == 0) {
            return openEditor(pack);
        }

        input = input.trim();
        List<String> parts = Tuils.splitArgs(input);
        if(parts.size() == 0) {
            return openEditor(pack);
        }

        String option = parts.get(0).toLowerCase(Locale.US);
        String rest = input.length() > parts.get(0).length()
                ? input.substring(parts.get(0).length()).trim()
                : Tuils.EMPTYSTRING;

        if("-add".equals(option)) {
            if(rest.length() == 0) return pack.context.getString(R.string.help_notes);
            send(pack, NotesManager.ACTION_ADD, rest, false);
            refreshNotesModule(pack);
            return null;
        }
        if("-rm".equals(option) || "-remove".equals(option)) {
            if(parts.size() < 2) return pack.context.getString(R.string.help_notes);
            send(pack, NotesManager.ACTION_RM, parts.get(1), false);
            refreshNotesModule(pack);
            return null;
        }
        if("-ls".equals(option) || "-list".equals(option)) {
            send(pack, NotesManager.ACTION_LS, null, false);
            return null;
        }
        if("-clear".equals(option)) {
            send(pack, NotesManager.ACTION_CLEAR, null, false);
            refreshNotesModule(pack);
            return null;
        }
        if("-lock".equals(option)) {
            if(parts.size() < 3) return pack.context.getString(R.string.help_notes);
            send(pack, NotesManager.ACTION_LOCK, parts.get(1), Boolean.parseBoolean(parts.get(2)));
            refreshNotesModule(pack);
            return null;
        }
        if("-cp".equals(option) || "-copy".equals(option)) {
            if(parts.size() < 2) return pack.context.getString(R.string.help_notes);
            send(pack, NotesManager.ACTION_CP, parts.get(1), false);
            return null;
        }
        if("-file".equals(option)) {
            pack.context.startActivity(Tuils.openFile(pack.context, new File(Tuils.getFolder(), NotesManager.PATH)));
            return null;
        }

        return pack.context.getString(R.string.help_notes);
    }

    private String openEditor(ExecutePack pack) {
        Intent intent = new Intent(pack.context, NotesEditorActivity.class);
        if(pack.context instanceof Activity) {
            ((Activity) pack.context).startActivityForResult(intent, LauncherActivity.TUIXT_REQUEST);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            pack.context.startActivity(intent);
        }
        return Tuils.EMPTYSTRING;
    }

    private void send(ExecutePack pack, String action, String text, boolean lock) {
        Intent intent = new Intent(action);
        intent.putExtra(NotesManager.BROADCAST_COUNT, NotesManager.broadcastCount);
        if(text != null) {
            intent.putExtra(NotesManager.TEXT, text);
        }
        intent.putExtra(NotesManager.LOCK, lock);
        LocalBroadcastManager.getInstance(pack.context.getApplicationContext()).sendBroadcast(intent);
    }

    private void refreshNotesModule(ExecutePack pack) {
        Intent intent = new Intent(UIManager.ACTION_MODULE_COMMAND);
        intent.putExtra(UIManager.EXTRA_MODULE_COMMAND, "update");
        intent.putExtra(UIManager.EXTRA_MODULE_NAME, ModuleManager.NOTES);
        LocalBroadcastManager.getInstance(pack.context.getApplicationContext()).sendBroadcast(intent);
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
        return R.string.help_notes;
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int index) {
        return pack.context.getString(R.string.help_notes);
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        return openEditor(pack);
    }
}
