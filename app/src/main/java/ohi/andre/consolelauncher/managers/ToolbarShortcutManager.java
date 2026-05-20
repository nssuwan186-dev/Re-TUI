package ohi.andre.consolelauncher.managers;

import android.content.Context;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.managers.settings.LauncherSettings;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsSave;
import ohi.andre.consolelauncher.managers.xml.options.Toolbar;

public final class ToolbarShortcutManager {

    public static final int MAX_SLOTS = 2;
    public static final String DEFAULT_ICON = "star";

    private static final IconChoice[] ICONS = {
            new IconChoice("star", "Star", R.drawable.ic_toolbar_star_24),
            new IconChoice("bell", "Bell", R.drawable.ic_toolbar_bell_24),
            new IconChoice("chat", "Chat", R.drawable.ic_toolbar_chat_24),
            new IconChoice("music", "Music", R.drawable.ic_toolbar_music_24),
            new IconChoice("timer", "Timer", R.drawable.timer_24),
            new IconChoice("note", "Note", R.drawable.ic_toolbar_note_24),
            new IconChoice("search", "Search", R.drawable.ic_toolbar_search_24),
            new IconChoice("refresh", "Refresh", R.drawable.ic_toolbar_refresh_24),
            new IconChoice("home", "Home", R.drawable.ic_toolbar_home_24),
            new IconChoice("terminal", "Terminal", R.drawable.ic_toolbar_terminal_24),
            new IconChoice("lock", "Lock", R.drawable.ic_tuixt_lock_24),
            new IconChoice("apps", "Apps", R.drawable.ic_menu)
    };

    private ToolbarShortcutManager() {}

    public static List<IconChoice> icons() {
        return Arrays.asList(ICONS);
    }

    public static Slot slot(int slot) {
        validateSlot(slot);
        String command = clean(LauncherSettings.get(commandOption(slot)));
        String icon = normalizeIcon(LauncherSettings.get(iconOption(slot)));
        boolean enabled = LauncherSettings.getBoolean(enabledOption(slot)) && command.length() > 0;
        return new Slot(slot, enabled, command, icon, iconRes(icon), iconLabel(icon));
    }

    public static void saveSlot(Context context, int slot, boolean enabled, String command, String icon) {
        validateSlot(slot);
        String cleanCommand = clean(command);
        String cleanIcon = normalizeIcon(icon);
        LauncherSettings.set(context, enabledOption(slot), Boolean.toString(enabled && cleanCommand.length() > 0));
        LauncherSettings.set(context, commandOption(slot), cleanCommand);
        LauncherSettings.set(context, iconOption(slot), cleanIcon);
    }

    public static void clearSlot(Context context, int slot) {
        saveSlot(context, slot, false, "", DEFAULT_ICON);
    }

    public static int iconRes(String icon) {
        String normalized = normalizeIcon(icon);
        for (IconChoice choice : ICONS) {
            if (choice.key.equals(normalized)) {
                return choice.drawableRes;
            }
        }
        return R.drawable.ic_toolbar_star_24;
    }

    public static String iconLabel(String icon) {
        String normalized = normalizeIcon(icon);
        for (IconChoice choice : ICONS) {
            if (choice.key.equals(normalized)) {
                return choice.label;
            }
        }
        return "Star";
    }

    public static String normalizeIcon(String icon) {
        String value = clean(icon).toLowerCase(Locale.US);
        if (value.length() == 0) {
            return DEFAULT_ICON;
        }
        for (IconChoice choice : ICONS) {
            if (choice.key.equals(value)) {
                return value;
            }
        }
        return DEFAULT_ICON;
    }

    private static XMLPrefsSave enabledOption(int slot) {
        return slot == 1 ? Toolbar.shortcut_button_1_enabled : Toolbar.shortcut_button_2_enabled;
    }

    private static XMLPrefsSave commandOption(int slot) {
        return slot == 1 ? Toolbar.shortcut_button_1_command : Toolbar.shortcut_button_2_command;
    }

    private static XMLPrefsSave iconOption(int slot) {
        return slot == 1 ? Toolbar.shortcut_button_1_icon : Toolbar.shortcut_button_2_icon;
    }

    private static void validateSlot(int slot) {
        if (slot < 1 || slot > MAX_SLOTS) {
            throw new IllegalArgumentException("Invalid toolbar shortcut slot: " + slot);
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class IconChoice {
        public final String key;
        public final String label;
        public final int drawableRes;

        private IconChoice(String key, String label, int drawableRes) {
            this.key = key;
            this.label = label;
            this.drawableRes = drawableRes;
        }
    }

    public static final class Slot {
        public final int index;
        public final boolean enabled;
        public final String command;
        public final String icon;
        public final int iconRes;
        public final String iconLabel;

        private Slot(int index, boolean enabled, String command, String icon, int iconRes, String iconLabel) {
            this.index = index;
            this.enabled = enabled;
            this.command = command;
            this.icon = icon;
            this.iconRes = iconRes;
            this.iconLabel = iconLabel;
        }
    }
}
