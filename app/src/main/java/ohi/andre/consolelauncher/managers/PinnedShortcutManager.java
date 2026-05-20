package ohi.andre.consolelauncher.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.os.Build;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import ohi.andre.consolelauncher.tuils.Tuils;

public final class PinnedShortcutManager {

    public static final String HANDLE_PREFIX = "@";

    private static final String PREFS = "pinned_shortcuts";
    private static final String KEY_PREFIX = "shortcut.";

    private PinnedShortcutManager() {
    }

    public static String normalizeHandle(String handle) {
        if(handle == null) return Tuils.EMPTYSTRING;
        handle = handle.trim();
        if(handle.startsWith(HANDLE_PREFIX)) {
            handle = handle.substring(HANDLE_PREFIX.length());
        }
        String lower = handle.toLowerCase(Locale.US);
        StringBuilder builder = new StringBuilder();
        for(int count = 0; count < lower.length(); count++) {
            char c = lower.charAt(count);
            if((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-') {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    public static String defaultHandle(CharSequence label) {
        String value = label == null ? Tuils.EMPTYSTRING : label.toString().trim().toLowerCase(Locale.US);
        StringBuilder builder = new StringBuilder();
        boolean separatorPending = false;
        for(int count = 0; count < value.length(); count++) {
            char c = value.charAt(count);
            if((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                if(separatorPending && builder.length() > 0) {
                    builder.append('-');
                }
                builder.append(c);
                separatorPending = false;
            } else if(builder.length() > 0) {
                separatorPending = true;
            }
        }
        String handle = normalizeHandle(builder.toString());
        return handle.length() == 0 ? "shortcut" : handle;
    }

    public static void save(Context context, String handle, ShortcutInfo info, CharSequence label) throws Exception {
        if(context == null || info == null) return;
        String normalized = normalizeHandle(handle);
        if(normalized.length() == 0) return;

        Record record = new Record();
        record.handle = normalized;
        record.packageName = info.getPackage();
        record.shortcutId = info.getId();
        record.profileSerial = profileSerial(context, info.getUserHandle());
        record.label = label == null ? safeLabel(info) : label.toString();

        prefs(context).edit()
                .putString(key(normalized), record.toJson().toString())
                .apply();
    }

    public static Record find(Context context, String handle) {
        if(context == null) return null;
        String normalized = normalizeHandle(handle);
        if(normalized.length() == 0) return null;
        String raw = prefs(context).getString(key(normalized), null);
        return Record.fromJson(raw);
    }

    public static List<Record> list(Context context) {
        ArrayList<Record> records = new ArrayList<>();
        if(context == null) return records;
        for(Object value : prefs(context).getAll().values()) {
            if(!(value instanceof String)) continue;
            Record record = Record.fromJson((String) value);
            if(record != null) records.add(record);
        }
        Collections.sort(records, (a, b) -> a.handle.compareTo(b.handle));
        return records;
    }

    public static String start(Context context, String handle) {
        Record record = find(context, handle);
        if(record == null) return "Pinned shortcut not found: " + handle;
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return "Pinned shortcuts require Android 7.1+";

        LauncherApps apps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        if(apps == null) return "Shortcut service unavailable";

        ShortcutInfo info = resolve(context, apps, record);
        if(info == null) return "Pinned shortcut unavailable: @" + record.handle;

        try {
            apps.startShortcut(info, null, null);
            return null;
        } catch (Exception e) {
            Tuils.log(e);
            return "Unable to launch @" + record.handle + ": " + e.getMessage();
        }
    }

    private static ShortcutInfo resolve(Context context, LauncherApps apps, Record record) {
        List<UserHandle> profiles;
        try {
            profiles = apps.getProfiles();
        } catch (Exception e) {
            profiles = Collections.singletonList(Process.myUserHandle());
        }

        for(UserHandle profile : profiles) {
            if(profileSerial(context, profile) != record.profileSerial) continue;
            ShortcutInfo info = findShortcut(apps, record, profile);
            if(info != null) return info;
        }

        for(UserHandle profile : profiles) {
            ShortcutInfo info = findShortcut(apps, record, profile);
            if(info != null) return info;
        }

        return null;
    }

    private static ShortcutInfo findShortcut(LauncherApps apps, Record record, UserHandle profile) {
        try {
            LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery();
            query.setPackage(record.packageName);
            query.setShortcutIds(Collections.singletonList(record.shortcutId));
            query.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                    | LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC
                    | LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED);
            List<ShortcutInfo> shortcuts = apps.getShortcuts(query, profile);
            if(shortcuts == null) return null;
            for(ShortcutInfo info : shortcuts) {
                if(record.shortcutId.equals(info.getId())) return info;
            }
        } catch (Exception e) {
            Tuils.log(e);
        }
        return null;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String key(String handle) {
        return KEY_PREFIX + handle;
    }

    private static long profileSerial(Context context, UserHandle profile) {
        if(profile == null) return 0L;
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        if(userManager != null) {
            try {
                long serial = userManager.getSerialNumberForUser(profile);
                if(serial >= 0L) return serial;
            } catch (Throwable e) {
            }
        }
        return profile.equals(Process.myUserHandle()) ? 0L : Integer.toUnsignedLong(profile.hashCode());
    }

    private static String safeLabel(ShortcutInfo info) {
        CharSequence shortLabel = info.getShortLabel();
        if(shortLabel != null && shortLabel.length() > 0) return shortLabel.toString();
        return info.getId();
    }

    public static final class Record {
        public String handle;
        public String packageName;
        public String shortcutId;
        public long profileSerial;
        public String label;

        JSONObject toJson() throws Exception {
            JSONObject object = new JSONObject();
            object.put("handle", handle);
            object.put("package", packageName);
            object.put("shortcutId", shortcutId);
            object.put("profileSerial", profileSerial);
            object.put("label", label);
            return object;
        }

        static Record fromJson(String raw) {
            if(raw == null || raw.trim().length() == 0) return null;
            try {
                JSONObject object = new JSONObject(raw);
                Record record = new Record();
                record.handle = object.optString("handle", Tuils.EMPTYSTRING);
                record.packageName = object.optString("package", Tuils.EMPTYSTRING);
                record.shortcutId = object.optString("shortcutId", Tuils.EMPTYSTRING);
                record.profileSerial = object.optLong("profileSerial", 0L);
                record.label = object.optString("label", record.shortcutId);
                if(record.handle.length() == 0 || record.packageName.length() == 0 || record.shortcutId.length() == 0) {
                    return null;
                }
                return record;
            } catch (Exception e) {
                return null;
            }
        }
    }
}
