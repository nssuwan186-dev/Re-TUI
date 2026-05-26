package ohi.andre.consolelauncher.managers.settings

import ohi.andre.consolelauncher.managers.xml.options.Notifications

object NotificationSettings {
    @JvmStatic
    fun showTerminal(): Boolean =
        LauncherSettings.getBoolean(Notifications.show_notifications) ||
            "enabled".equals(LauncherSettings.get(Notifications.show_notifications), ignoreCase = true)

    @JvmStatic
    fun printToOutput(): Boolean = LauncherSettings.getBoolean(Notifications.terminal_notifications)

    @JvmStatic
    fun format(): String? = LauncherSettings.get(Notifications.notification_format)

    @JvmStatic
    fun defaultColor(): Int = LauncherSettings.getColor(Notifications.notification_text_color)

    @JvmStatic
    fun defaultColorRaw(): String? = LauncherSettings.get(Notifications.notification_text_color)

    @JvmStatic
    fun appNotificationsEnabledByDefault(): Boolean = LauncherSettings.getBoolean(Notifications.app_notification_enabled_default)

    @JvmStatic
    fun clickOpensNotification(): Boolean = LauncherSettings.getBoolean(Notifications.click_notification)

    @JvmStatic
    fun longClickOpensNotificationActions(): Boolean = LauncherSettings.getBoolean(Notifications.long_click_notification)

    @JvmStatic
    fun showExcludeAppPopupAction(): Boolean = LauncherSettings.getBoolean(Notifications.notification_popup_exclude_app)

    @JvmStatic
    fun showExcludeNotificationPopupAction(): Boolean = LauncherSettings.getBoolean(Notifications.notification_popup_exclude_notification)

    @JvmStatic
    fun showReplyPopupAction(): Boolean = LauncherSettings.getBoolean(Notifications.notification_popup_reply)
}
