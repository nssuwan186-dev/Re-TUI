package ohi.andre.consolelauncher.managers.xml.options

import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsElement
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsSave

/**
 * Created by francescoandreuzzi on 24/09/2017.
 */
enum class Theme : XMLPrefsSave {
    input_color {
        override fun defaultValue(): String? {
            return "#ff00ff00"
        }

        override fun info(): String? {
            return "Input color"
        }
    },
    output_color {
        override fun defaultValue(): String? {
            return "#ffffffff"
        }

        override fun info(): String? {
            return "Output color"
        }
    },
    bg_color {
        override fun defaultValue(): String? {
            return "#00000000"
        }

        override fun info(): String? {
            return "Background color"
        }
    },
    device_color {
        override fun defaultValue(): String? {
            return "#ffff9800"
        }

        override fun info(): String? {
            return "Device label color"
        }
    },
    battery_text_high {
        override fun defaultValue(): String? {
            return "#4CAF50"
        }

        override fun info(): String? {
            return "Battery text color when the battery level is high"
        }
    },
    battery_text_medium {
        override fun defaultValue(): String? {
            return "#FFEB3B"
        }

        override fun info(): String? {
            return "Battery text color when the battery level is medium"
        }
    },
    battery_text_low {
        override fun defaultValue(): String? {
            return "#FF5722"
        }

        override fun info(): String? {
            return "Battery text color when the battery level is low"
        }
    },
    ascii_color {
        override fun defaultValue(): String? {
            return "#00FF00"
        }

        override fun info(): String? {
            return "Color of the ASCII art (requires show_ascii true in ui.xml)"
        }
    },
    time_color {
        override fun defaultValue(): String? {
            return "#03A9F4"
        }

        override fun info(): String? {
            return "Time label color"
        }
    },
    storage_color {
        override fun defaultValue(): String? {
            return "#9C27B0"
        }

        override fun info(): String? {
            return "Storage label color"
        }
    },
    ram_color {
        override fun defaultValue(): String? {
            return "#fff44336"
        }

        override fun info(): String? {
            return "RAM label color"
        }
    },
    network_info_color {
        override fun defaultValue(): String? {
            return "#FFCA28"
        }

        override fun info(): String? {
            return "The color of the network info label"
        }
    },
    toolbar_bg {
        override fun defaultValue(): String? {
            return "#00000000"
        }

        override fun info(): String? {
            return "Toolbar background color"
        }
    },
    toolbar_color {
        override fun defaultValue(): String? {
            return "#ffffff"
        }

        override fun info(): String? {
            return "Toolbar icons color"
        }
    },
    enter_color {
        override fun defaultValue(): String? {
            return "#ffffffff"
        }

        override fun info(): String? {
            return "Enter icon color"
        }
    },
    cursor_color {
        override fun defaultValue(): String? {
            return "#ffffff"
        }

        override fun info(): String? {
            return "The color of the cursor"
        }
    },
    overlay_color {
        override fun defaultValue(): String? {
            return "#80000000"
        }

        override fun info(): String? {
            return "The overlay that overlaps to the background (only when system_wallpaper is true)"
        }
    },
    alias_content_color {
        override fun defaultValue(): String? {
            return "#1DE9B6"
        }

        override fun info(): String? {
            return "Alias content color"
        }
    },
    statusbar_color {
        override fun defaultValue(): String? {
            return "#000000"
        }

        override fun info(): String? {
            return "Status Bar color (5.0+)"
        }
    },
    navigationbar_color {
        override fun defaultValue(): String? {
            return "#000000"
        }

        override fun info(): String? {
            return "Navigation Bar color (5.0+)"
        }
    },
    app_installed_color {
        override fun defaultValue(): String? {
            return "#FF7043"
        }

        override fun info(): String? {
            return "App installed message color"
        }
    },
    app_uninstalled_color {
        override fun defaultValue(): String? {
            return "#FF7043"
        }

        override fun info(): String? {
            return "App uninstalled message color"
        }
    },
    hint_color {
        override fun defaultValue(): String? {
            return "#4CAF50"
        }

        override fun info(): String? {
            return "Hint color"
        }
    },
    mark_color {
        override fun defaultValue(): String? {
            return "#CDDC39"
        }

        override fun info(): String? {
            return "The background color that will be used as marker"
        }
    },
    notes_color {
        override fun defaultValue(): String? {
            return "#8BC34A"
        }

        override fun info(): String? {
            return "The default color of your notes"
        }
    },
    notes_locked_color {
        override fun defaultValue(): String? {
            return "#3D5AFE"
        }

        override fun info(): String? {
            return "The color of your locked notes"
        }
    },
    link_color {
        override fun defaultValue(): String? {
            return "#0000EE"
        }

        override fun info(): String? {
            return "The color of the links"
        }
    },
    restart_message_color {
        override fun defaultValue(): String? {
            return Theme.output_color.defaultValue()
        }

        override fun info(): String? {
            return "The color of the restart message"
        }
    },
    weather_color {
        override fun defaultValue(): String? {
            return Theme.ram_color.defaultValue()
        }

        override fun info(): String? {
            return "The color of the weather label"
        }
    },
    unlock_counter_color {
        override fun defaultValue(): String? {
            return Theme.device_color.defaultValue()
        }

        override fun info(): String? {
            return "The color of the unlock counter"
        }
    },
    session_info_color {
        override fun defaultValue(): String? {
            return "#888888"
        }

        override fun info(): String? {
            return "The color of the session info"
        }
    },
    status_lines_bgrectcolor {
        override fun defaultValue(): String? {
            return "#00000000,#00000000,#00000000,#00000000,#00000000,#00000000,#00000000,#00000000,#00000000,#00000000"
        }

        override fun info(): String? {
            return "Deprecated: dashed_border_color controls status line borders; use status_lines_bg for fill"
        }

        override fun invalidValues(): Array<String?>? {
            return arrayOf<String?>("#ff000000,#ff000000,#ff000000,#ff000000,#ff000000,#ff000000,#ff000000,#ff000000,#ff000000,#ff000000")
        }
    },
    input_bgrectcolor {
        override fun defaultValue(): String? {
            return "#00000000"
        }

        override fun info(): String? {
            return "Deprecated: dashed_border_color controls the input border; use input_bg for fill"
        }

        override fun invalidValues(): Array<String?>? {
            return arrayOf<String?>("#ff000000")
        }
    },
    output_bgrectcolor {
        override fun defaultValue(): String? {
            return "#00000000"
        }

        override fun info(): String? {
            return "Deprecated: dashed_border_color controls the output border; use output_bg for fill"
        }

        override fun invalidValues(): Array<String?>? {
            return arrayOf<String?>("#ff000000")
        }
    },
    toolbar_bgrectcolor {
        override fun defaultValue(): String? {
            return "#00000000"
        }

        override fun info(): String? {
            return "Deprecated: dashed_border_color controls the toolbar border; use toolbar_bg for fill"
        }

        override fun invalidValues(): Array<String?>? {
            return arrayOf<String?>("#ff000000")
        }
    },
    suggestions_bgrectcolor {
        override fun defaultValue(): String? {
            return "#00000000"
        }

        override fun info(): String? {
            return "Deprecated: dashed_border_color controls the suggestions border; use suggestions_bg for fill"
        }

        override fun invalidValues(): Array<String?>? {
            return arrayOf<String?>("#ff000000")
        }
    },
    status_lines_bg {
        override fun defaultValue(): String? {
            return "#00000000,#00000000,#00000000,#00000000,#00000000,#00000000,#00000000,#00000000,#00000000,#00000000"
        }

        override fun info(): String? {
            return "The bg color of the nth line"
        }

        override fun invalidValues(): Array<String?>? {
            return arrayOf<String?>("#ff000000,#ff000000,#ff000000,#ff000000,#ff000000,#ff000000,#ff000000,#ff000000,#ff000000,#ff000000")
        }
    },
    input_bg {
        override fun defaultValue(): String? {
            return "#00000000"
        }

        override fun info(): String? {
            return "The background color of the input field"
        }

        override fun invalidValues(): Array<String?>? {
            return arrayOf<String?>("#ff000000")
        }
    },
    output_bg {
        override fun defaultValue(): String? {
            return "#00000000"
        }

        override fun info(): String? {
            return "The background color of the output field"
        }

        override fun invalidValues(): Array<String?>? {
            return arrayOf<String?>("#ff000000")
        }
    },
    suggestions_bg {
        override fun defaultValue(): String? {
            return "#00000000"
        }

        override fun info(): String? {
            return "The background color of the suggestions area"
        }

        override fun invalidValues(): Array<String?>? {
            return arrayOf<String?>("#ff000000")
        }
    },
    status_lines_shadow_color {
        override fun defaultValue(): String? {
            return "#00000000,#00000000,#00000000,#00000000,#00000000,#00000000,#00000000,#00000000,#00000000,#00000000"
        }

        override fun info(): String? {
            return "The outline color of the nth line"
        }

        override fun invalidValues(): Array<String?>? {
            return arrayOf<String?>("#ff000000,#ff000000,#ff000000,#ff000000,#ff000000,#ff000000,#ff000000,#ff000000,#ff000000,#ff000000")
        }
    },
    input_shadow_color {
        override fun defaultValue(): String? {
            return "#00000000"
        }

        override fun info(): String? {
            return "The outline color of the input field"
        }

        override fun invalidValues(): Array<String?>? {
            return arrayOf<String?>("#ff000000")
        }
    },
    output_shadow_color {
        override fun defaultValue(): String? {
            return "#00000000"
        }

        override fun info(): String? {
            return "The outline color of the output field"
        }

        override fun invalidValues(): Array<String?>? {
            return arrayOf<String?>("#ff000000")
        }
    },
    apps_drawer_color {
        override fun defaultValue(): String? {
            return "#A5D6A7"
        }

        override fun info(): String? {
            return "The color of the apps drawer"
        }
    },
    dashed_border_color {
        override fun defaultValue(): String? {
            return "#ffffffff"
        }

        override fun info(): String? {
            return "The shared border color for terminal chrome, headers, panels, and dashed rect outlines"
        }
    },
    module_button_bg_color {
        override fun defaultValue(): String? {
            return "#00000000"
        }

        override fun info(): String? {
            return "The background color of module buttons"
        }
    },
    module_name_text_color {
        override fun defaultValue(): String? {
            return "#ffffffff"
        }

        override fun info(): String? {
            return "The text color of module names"
        }
    },
    module_button_border_color {
        override fun defaultValue(): String? {
            return "#ffffffff"
        }

        override fun info(): String? {
            return "Deprecated: use dashed_border_color for terminal and module borders"
        }
    },
    window_terminal_bg {
        override fun defaultValue(): String? {
            return "#00000000"
        }

        override fun info(): String? {
            return "The background color of the terminal windows (music, apps drawer, etc.)"
        }
    };

    override fun parent(): XMLPrefsElement? {
        return XMLPrefsManager.XMLPrefsRoot.THEME
    }

    override fun label(): String? {
        return name
    }

    override fun type(): String? {
        return XMLPrefsSave.COLOR
    }

    override fun invalidValues(): Array<String?>? {
        return null
    }

    override fun getLowercaseString(): String? {
        return label()
    }

    override fun getString(): String? {
        return label()
    }
}
