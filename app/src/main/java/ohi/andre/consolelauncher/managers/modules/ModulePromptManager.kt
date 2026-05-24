package ohi.andre.consolelauncher.managers.modules

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.text.TextUtils
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import ohi.andre.consolelauncher.UIManager
import ohi.andre.consolelauncher.managers.TerminalManager
import ohi.andre.consolelauncher.managers.modules.ModuleManager.ModuleSuggestion
import ohi.andre.consolelauncher.managers.modules.ModuleManager.ModuleSuggestion.Companion.command
import ohi.andre.consolelauncher.managers.modules.ReminderManager.Reminder
import ohi.andre.consolelauncher.managers.modules.ReminderManager.add
import ohi.andre.consolelauncher.managers.modules.ReminderManager.formatList
import ohi.andre.consolelauncher.managers.modules.ReminderManager.formatWhen
import ohi.andre.consolelauncher.managers.modules.ReminderManager.get
import ohi.andre.consolelauncher.managers.modules.ReminderManager.parseDateTime
import ohi.andre.consolelauncher.managers.modules.ReminderManager.remove
import ohi.andre.consolelauncher.managers.modules.ReminderManager.save
import ohi.andre.consolelauncher.managers.notifications.reply.ReplyManager
import ohi.andre.consolelauncher.tuils.Tuils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.ArrayList

object ModulePromptManager {
    private const val PREFS = "retui_module_prompt"
    private const val KEY_ACTIVE = "active"
    private const val KEY_MODULE = "module"
    private const val KEY_FLOW = "flow"
    private const val KEY_STEP = "step"
    private const val KEY_TITLE = "title"
    private const val KEY_DATE = "date"
    private const val KEY_TIME = "time"
    private const val KEY_EDIT_ID = "edit_id"
    private const val KEY_PACKAGE = "package"
    private const val KEY_APP_NAME = "app_name"

    fun isActive(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_ACTIVE, false)
    }

    fun isNotificationReplyActive(context: Context): Boolean {
        val prefs = prefs(context)
        return prefs.getBoolean(KEY_ACTIVE, false)
                && ModuleManager.NOTIFICATIONS == prefs.getString(KEY_MODULE, "")
                && "reply" == prefs.getString(KEY_FLOW, "")
    }

    fun getNotificationReplyPackage(context: Context): String {
        return prefs(context).getString(KEY_PACKAGE, "")!!
    }

    fun startReminderAdd(context: Context) {
        prefs(context).edit()
            .clear()
            .putBoolean(KEY_ACTIVE, true)
            .putString(KEY_MODULE, ModuleManager.REMINDER)
            .putString(KEY_FLOW, "add")
            .putString(KEY_STEP, "title")
            .apply()
        prompt(context, "What do you want to be reminded about?")
    }

    fun startReminderEdit(context: Context) {
        prefs(context).edit()
            .clear()
            .putBoolean(KEY_ACTIVE, true)
            .putString(KEY_MODULE, ModuleManager.REMINDER)
            .putString(KEY_FLOW, "edit_select")
            .putString(KEY_STEP, "select")
            .apply()
        prompt(context, "Which reminder do you want to edit?\n" + formatList(context))
    }

    fun startReminderRemove(context: Context) {
        prefs(context).edit()
            .clear()
            .putBoolean(KEY_ACTIVE, true)
            .putString(KEY_MODULE, ModuleManager.REMINDER)
            .putString(KEY_FLOW, "remove")
            .putString(KEY_STEP, "select")
            .apply()
        prompt(context, "Which reminder do you want to remove?\n" + formatList(context))
    }

    fun startNotificationReply(context: Context, pkg: String, appName: String?) {
        if (TextUtils.isEmpty(pkg)) {
            Tuils.sendOutput(context, "No notification selected.")
            return
        }
        val label: String = (if (android.text.TextUtils.isEmpty(appName)) pkg else appName)!!
        Log.i("RetuiReplyDebug", "module reply prompt started pkg=" + pkg + " label=" + label)
        prefs(context).edit()
            .clear()
            .putBoolean(KEY_ACTIVE, true)
            .putString(KEY_MODULE, ModuleManager.NOTIFICATIONS)
            .putString(KEY_FLOW, "reply")
            .putString(KEY_STEP, "text")
            .putString(KEY_PACKAGE, pkg)
            .putString(KEY_APP_NAME, label)
            .apply()
        prompt(context, "Reply to " + label + ":")
    }

    fun handleInput(context: Context, input: String?): Boolean {
        if (!isActive(context)) return false
        val prefs = prefs(context)
        val module: String = prefs.getString(KEY_MODULE, "")!!
        if (ModuleManager.NOTIFICATIONS == module) {
            return handleNotificationInput(context, prefs, input)
        }
        if (ModuleManager.REMINDER != module) return false

        val value = if (input == null) "" else input.trim { it <= ' ' }
        if ("cancel".equals(value, ignoreCase = true)) {
            clear(context, "Module prompt cancelled.")
            return true
        }

        val flow: String = prefs.getString(KEY_FLOW, "")!!
        val step: String = prefs.getString(KEY_STEP, "")!!

        if ("add" == flow) {
            return handleAdd(context, prefs, step, value)
        }
        if ("edit_select" == flow || "edit" == flow) {
            return handleEdit(context, prefs, flow, step, value)
        }
        if ("remove" == flow) {
            return handleRemove(context, prefs, step, value)
        }

        clear(context, "Unknown module prompt.")
        return true
    }

    fun getSuggestions(context: Context): MutableList<ModuleSuggestion?> {
        val suggestions = ArrayList<ModuleSuggestion?>()
        if (!isActive(context)) return suggestions
        val step: String = prefs(context).getString(KEY_STEP, "")!!
        if ("confirm" == step) {
            suggestions.add(command("save", "save"))
            suggestions.add(command("edit", "edit"))
            suggestions.add(command("cancel", "cancel"))
        } else {
            suggestions.add(command("cancel", "cancel"))
        }
        return suggestions
    }

    private fun handleNotificationInput(
        context: Context,
        prefs: SharedPreferences,
        input: String?
    ): Boolean {
        val value = if (input == null) "" else input.trim { it <= ' ' }
        if ("cancel".equals(value, ignoreCase = true)) {
            clear(context, "Notification reply cancelled.")
            return true
        }
        if (TextUtils.isEmpty(value)) {
            prompt(context, "Reply text cannot be empty. Type a reply or cancel.")
            return true
        }

        val intent = Intent(ReplyManager.ACTION)
        intent.putExtra(ReplyManager.ID, prefs.getString(KEY_PACKAGE, ""))
        intent.putExtra(ReplyManager.WHAT, value)
        Log.i(
            "RetuiReplyDebug", ("module reply input captured pkg="
                    + prefs.getString(KEY_PACKAGE, "")
                    + " text=" + value)
        )
        LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(intent)
        clear(
            context,
            "Reply sent to " + prefs.getString(KEY_APP_NAME, prefs.getString(KEY_PACKAGE, "")) + "."
        )
        return true
    }

    private fun handleAdd(
        context: Context,
        prefs: SharedPreferences,
        step: String?,
        value: String?
    ): Boolean {
        if ("title" == step) {
            if (TextUtils.isEmpty(value)) {
                prompt(
                    context,
                    "Reminder text cannot be empty. What do you want to be reminded about?"
                )
                return true
            }
            prefs.edit().putString(KEY_TITLE, value).putString(KEY_STEP, "date").apply()
            prompt(context, "What date?\nAccepted: 10/05/2026 or 2026-05-10")
            return true
        }
        if ("date" == step) {
            prefs.edit().putString(KEY_DATE, value).putString(KEY_STEP, "time").apply()
            prompt(context, "What time?\nAccepted: 11:30PM or 23:30")
            return true
        }
        if ("time" == step) {
            prefs.edit().putString(KEY_TIME, value).putString(KEY_STEP, "confirm").apply()
            confirm(context, prefs)
            return true
        }
        if ("confirm" == step) {
            if ("edit".equals(value, ignoreCase = true)) {
                prefs.edit().putString(KEY_STEP, "title").apply()
                prompt(context, "What do you want to be reminded about?")
            } else if ("save".equals(value, ignoreCase = true) || "yes".equals(
                    value,
                    ignoreCase = true
                ) || "confirm".equals(value, ignoreCase = true)
            ) {
                saveNewReminder(context, prefs)
            } else {
                prompt(context, "Type save, edit, or cancel.")
            }
            return true
        }
        return true
    }

    private fun handleEdit(
        context: Context,
        prefs: SharedPreferences,
        flow: String?,
        step: String?,
        value: String?
    ): Boolean {
        if ("edit_select" == flow) {
            val reminder = get(context, value)
            if (reminder == null) {
                prompt(
                    context,
                    "Reminder not found. Enter a list number or type cancel.\n" + formatList(context)
                )
                return true
            }
            prefs.edit()
                .putString(KEY_FLOW, "edit")
                .putString(KEY_STEP, "title")
                .putString(KEY_EDIT_ID, reminder.id)
                .putString(KEY_TITLE, reminder.title)
                .putString(
                    KEY_DATE,
                    SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(reminder.atMillis))
                )
                .putString(
                    KEY_TIME,
                    SimpleDateFormat("h:mma", Locale.US).format(Date(reminder.atMillis))
                )
                .apply()
            prompt(
                context,
                "Current reminder: " + reminder.title + "\nNew text? Press enter to keep."
            )
            return true
        }

        if ("title" == step) {
            val editor = prefs.edit().putString(KEY_STEP, "date")
            if (!TextUtils.isEmpty(value)) editor.putString(KEY_TITLE, value)
            editor.apply()
            prompt(
                context,
                "New date? Press enter to keep.\nCurrent: " + prefs.getString(KEY_DATE, "")
            )
            return true
        }
        if ("date" == step) {
            val editor = prefs.edit().putString(KEY_STEP, "time")
            if (!TextUtils.isEmpty(value)) editor.putString(KEY_DATE, value)
            editor.apply()
            prompt(
                context,
                "New time? Press enter to keep.\nCurrent: " + prefs.getString(KEY_TIME, "")
            )
            return true
        }
        if ("time" == step) {
            val editor = prefs.edit().putString(KEY_STEP, "confirm")
            if (!TextUtils.isEmpty(value)) editor.putString(KEY_TIME, value)
            editor.apply()
            confirm(context, prefs)
            return true
        }
        if ("confirm" == step) {
            if ("edit".equals(value, ignoreCase = true)) {
                prefs.edit().putString(KEY_STEP, "title").apply()
                prompt(context, "New text? Press enter to keep.")
            } else if ("save".equals(value, ignoreCase = true) || "yes".equals(
                    value,
                    ignoreCase = true
                ) || "confirm".equals(value, ignoreCase = true)
            ) {
                saveEditedReminder(context, prefs)
            } else {
                prompt(context, "Type save, edit, or cancel.")
            }
        }
        return true
    }

    private fun handleRemove(
        context: Context,
        prefs: SharedPreferences,
        step: String?,
        value: String?
    ): Boolean {
        if ("select" == step) {
            val reminder = get(context, value)
            if (reminder == null) {
                prompt(
                    context,
                    "Reminder not found. Enter a list number or type cancel.\n" + formatList(context)
                )
                return true
            }
            prefs.edit().putString(KEY_EDIT_ID, reminder.id).putString(KEY_STEP, "confirm").apply()
            prompt(
                context,
                "Remove this reminder?\n" + reminder.title + "\n" + formatWhen(reminder.atMillis) + "\nType save to remove, or cancel."
            )
            return true
        }
        if ("confirm" == step) {
            if ("save".equals(value, ignoreCase = true) || "yes".equals(
                    value,
                    ignoreCase = true
                ) || "confirm".equals(value, ignoreCase = true)
            ) {
                val id: String = prefs.getString(KEY_EDIT_ID, "")!!
                remove(context, id)
                clear(context, "Reminder removed.")
                refreshReminder(context)
            } else {
                prompt(context, "Type save to remove, or cancel.")
            }
        }
        return true
    }

    private fun saveNewReminder(context: Context, prefs: SharedPreferences) {
        val at = parseDateTime(prefs.getString(KEY_DATE, ""), prefs.getString(KEY_TIME, ""))
        if (at == null) {
            prefs.edit().putString(KEY_STEP, "date").apply()
            prompt(context, "I could not parse that date/time. What date?")
            return
        }
        if (at <= System.currentTimeMillis()) {
            prefs.edit().putString(KEY_STEP, "date").apply()
            prompt(context, "That reminder time is in the past. What date?")
            return
        }
        val reminder = add(context, prefs.getString(KEY_TITLE, ""), at)
        clear(context, "Reminder saved:\n" + reminder.title + "\n" + formatWhen(reminder.atMillis))
        refreshReminder(context)
    }

    private fun saveEditedReminder(context: Context, prefs: SharedPreferences) {
        val at = parseDateTime(prefs.getString(KEY_DATE, ""), prefs.getString(KEY_TIME, ""))
        if (at == null) {
            prefs.edit().putString(KEY_STEP, "date").apply()
            prompt(context, "I could not parse that date/time. What date?")
            return
        }
        if (at <= System.currentTimeMillis()) {
            prefs.edit().putString(KEY_STEP, "date").apply()
            prompt(context, "That reminder time is in the past. What date?")
            return
        }
        val reminder = Reminder(
            prefs.getString(KEY_EDIT_ID, "")!!,
            prefs.getString(KEY_TITLE, ""),
            at
        )
        save(context, reminder)
        clear(
            context,
            "Reminder updated:\n" + reminder.title + "\n" + formatWhen(reminder.atMillis)
        )
        refreshReminder(context)
    }

    private fun confirm(context: Context, prefs: SharedPreferences) {
        val at = parseDateTime(prefs.getString(KEY_DATE, ""), prefs.getString(KEY_TIME, ""))
        val `when` = if (at == null) prefs.getString(KEY_DATE, "") + " " + prefs.getString(
            KEY_TIME,
            ""
        ) else formatWhen(at)
        prompt(
            context, ("Reminder:\n"
                    + prefs.getString(KEY_TITLE, "") + "\n"
                    + `when` + "\n"
                    + "Type save, edit, or cancel.")
        )
    }

    private fun prompt(context: Context, message: String?) {
        Tuils.sendOutput(context, message, TerminalManager.CATEGORY_OUTPUT)
        refreshReminder(context)
    }

    private fun clear(context: Context, message: String?) {
        prefs(context).edit().clear().apply()
        Tuils.sendOutput(context, message, TerminalManager.CATEGORY_OUTPUT)
    }

    private fun refreshReminder(context: Context) {
        val update = Intent(UIManager.ACTION_MODULE_COMMAND)
        update.putExtra(UIManager.EXTRA_MODULE_COMMAND, "update")
        update.putExtra(UIManager.EXTRA_MODULE_NAME, ModuleManager.REMINDER)
        LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(update)
    }

    private fun prefs(context: Context): SharedPreferences {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }
}
