-- name = "Habit Tracker"
-- type = "widget"
-- retui = "1"
-- permissions = "notifications"
-- description = "Habitctl-style removable Lua module with config, reminders, and glyph progress"

local json = require "json"
local fmt = require "fmt"
local reminders = require "reminders"

local DEFAULT_HABITS = [[Meditate | daily | 08:30
Walk | daily | 18:00
Journal | daily | 21:30]]

local function trim(value)
    return tostring(value or ""):match("^%s*(.-)%s*$")
end

local function clamp(value, low, high)
    local n = tonumber(value) or low
    if n < low then return low end
    if n > high then return high end
    return n
end

local function today()
    return os.date("%Y-%m-%d")
end

local function habit_id(name, index)
    local clean = trim(name):lower():gsub("[^%w]+", "_"):gsub("^_+", ""):gsub("_+$", "")
    if clean == "" then clean = "habit_" .. tostring(index) end
    return clean
end

local function habit_lines()
    local text = prefs.habits_text
    if text == nil or trim(text) == "" then
        text = DEFAULT_HABITS
        prefs.habits_text = text
    end
    return text
end

local function parse_habits()
    local habits = {}
    for line in (habit_lines() .. "\n"):gmatch("(.-)\n") do
        local clean = trim(line)
        if clean ~= "" and clean:sub(1, 1) ~= "#" then
            local parts = {}
            for part in (clean .. "|"):gmatch("(.-)|") do
                table.insert(parts, trim(part))
            end
            local name = parts[1] or ""
            if name ~= "" then
                local mode = (parts[2] or "daily"):lower()
                if mode ~= "once" then mode = "daily" end
                table.insert(habits, {
                    id = habit_id(name, #habits + 1),
                    name = name,
                    mode = mode,
                    at = parts[3] or "",
                })
            end
        end
    end
    return habits
end

local function load_log()
    local data = json.decode(prefs.log_json or "{}")
    if type(data) ~= "table" then data = {} end
    return data
end

local function save_log(log)
    prefs.log_json = json.encode(log)
end

local function day_log(log)
    local key = today()
    log[key] = log[key] or {}
    return log[key]
end

local function is_due(habit)
    if habit.mode == "daily" then return true end
    return habit.at:sub(1, 10) == today()
end

local function sync_reminders()
    reminders:cancel_prefix("habit_")
    if prefs.reminders_enabled == false then return end
    for index, habit in ipairs(parse_habits()) do
        if habit.at ~= "" then
            local id = "habit_" .. tostring(index) .. "_" .. habit.id
            local title = "Habit: " .. habit.name
            if habit.mode == "once" then
                reminders:once(id, title, habit.at)
            else
                reminders:daily(id, title, habit.at)
            end
        end
    end
end

local function render()
    local habits = parse_habits()
    local log = load_log()
    local statuses = day_log(log)
    local done = 0
    local due = 0
    local width = clamp(prefs.bar_width, 6, 32)

    ui:set_title("Habits")
    ui:set_expandable(true)

    for _, habit in ipairs(habits) do
        if is_due(habit) then
            due = due + 1
            if statuses[habit.id] == "done" then done = done + 1 end
        end
    end

    if #habits == 0 then
        ui:add_line("No habits configured.")
        return
    end

    if due == 0 then
        ui:add_line("No habits due today.")
    else
        ui:add_line("Today: " .. fmt.progress_bar(done, due, width) .. " " .. fmt.percent(done, due))
    end

    local limit = #habits
    if ui:is_folded() and limit > 5 then limit = 5 end

    for index = 1, limit do
        local habit = habits[index]
        local status = statuses[habit.id] or ""
        local mark = " "
        if status == "done" then mark = "x" end
        if status == "skip" then mark = "-" end
        if not is_due(habit) then mark = "." end

        local suffix = ""
        if habit.at ~= "" then suffix = " @" .. habit.at end
        ui:add_line(tostring(index) .. ". [" .. mark .. "] " .. habit.name .. suffix)
        if is_due(habit) then
            ui:show_action("done " .. tostring(index), "done:" .. tostring(index))
            ui:show_action("skip " .. tostring(index), "skip:" .. tostring(index))
        end
    end

    if ui:is_folded() and #habits > limit then
        ui:add_line("... " .. tostring(#habits - limit) .. " more")
    end
    ui:show_action("clear today", "clear")
end

function on_load()
    if prefs.habits_text == nil then prefs.habits_text = DEFAULT_HABITS end
    if prefs.bar_width == nil then prefs.bar_width = 14 end
    if prefs.reminders_enabled == nil then prefs.reminders_enabled = true end
    sync_reminders()
end

function on_resume()
    render()
end

function on_action(value)
    local log = load_log()
    local statuses = day_log(log)
    if value == "clear" then
        log[today()] = {}
        save_log(log)
        render()
        return
    end

    local action, raw_index = tostring(value or ""):match("^([^:]+):(%d+)$")
    local index = tonumber(raw_index or "")
    local habit = parse_habits()[index or 0]
    if habit and is_due(habit) then
        if action == "done" then statuses[habit.id] = "done" end
        if action == "skip" then statuses[habit.id] = "skip" end
        save_log(log)
    end
    render()
end

function on_config()
    ui:show_config({
        title = "Habit Tracker",
        fields = {
            {
                id = "habits_text",
                label = "Habits: name | daily | HH:MM, or name | once | yyyy-mm-dd HH:MM",
                type = "textarea",
                value = habit_lines(),
                lines = 9,
                required = true,
            },
            {
                id = "reminders_enabled",
                label = "Notifications",
                type = "toggle",
                value = prefs.reminders_enabled ~= false,
            },
            {
                id = "bar_width",
                label = "Progress width",
                type = "number",
                value = prefs.bar_width or 14,
                min = 6,
                max = 32,
            },
        },
        buttons = {
            { id = "cancel", label = "Cancel" },
            { id = "save", label = "Save", primary = true },
        },
    })
end

function on_config_submit(action, values)
    if action == "save" then
        prefs.habits_text = values.habits_text or DEFAULT_HABITS
        prefs.reminders_enabled = values.reminders_enabled == true
        prefs.bar_width = clamp(values.bar_width, 6, 32)
        sync_reminders()
    end
    render()
end
