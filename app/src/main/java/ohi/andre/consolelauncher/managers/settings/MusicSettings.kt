package ohi.andre.consolelauncher.managers.settings

import android.text.TextUtils
import ohi.andre.consolelauncher.managers.xml.options.Behavior

object MusicSettings {
    @JvmStatic
    fun enabled(): Boolean = LauncherSettings.getBoolean(Behavior.enable_music)

    @JvmStatic
    fun showWidget(): Boolean = LauncherSettings.getBoolean(Behavior.show_music_widget)

    @JvmStatic
    fun autoShowWidget(): Boolean = LauncherSettings.getBoolean(Behavior.auto_show_music_widget)

    @JvmStatic
    fun preferredPackage(): String? = LauncherSettings.get(Behavior.preferred_music_app)

    @JvmStatic
    fun acceptsPackage(packageName: String?): Boolean {
        val preferredPackage = preferredPackage()
        return TextUtils.isEmpty(preferredPackage) || preferredPackage == packageName
    }
}
