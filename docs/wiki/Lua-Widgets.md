# Lua Modules

Lua modules are Re:TUI's in-app scripting surface. They are meant for users who want reactive launcher panels without setting up Termux files. Termux remains the shell automation surface; Lua is the launcher-native UI surface.

Lua modules live under Re:TUI's local `widgets/<id>/` storage folder for compatibility and are included in personal backup/restore. A saved Lua module is registered as a `lua:<id>` module, so it shares the same module dock as built-in modules and Termux-backed modules. The document name is the user-facing module title; the id is the generated slug used for commands and storage.

Lua files can also be suggestion scripts with `-- type = "suggest"`. These are installed locally like Lua modules, but they do not appear in the module dock. They contribute command chips while the user types.

## Commands

```text
module -ls
module -new lua counter
module -edit counter
module -rename counter better_counter
module -show counter
module -refresh counter
module -check counter
module -info counter
module -approve counter
module -copy-error counter
module -disable counter
module -enable counter
module -export counter
module -toggle counter
module -expand counter
module -collapse counter
```

Changing the document name in the editor updates the module title. If the name changes enough to produce a different slug, Re:TUI moves the local Lua module folder and updates module dock references. You can do the same from the terminal with `module -rename`.

`module -new lua <name>` opens the editor with a starter script. Users can paste Lua from a marketplace, Reddit, GitHub, or a friend, then save/run. The older `widget -add <name>` and `widget -new <name>` commands remain compatibility aliases.

`module -export <id>` copies a shareable JSON package to the clipboard. Re:TUI does not auto-install clipboard packages; pasted Lua should go through the editor so the user sees and names what they are adding.

`module -check <id>` loads and renders the script once, returning Lua errors without opening it as the active module. `module -info <id>` shows metadata parsed from the script header.

Scripts that use sensitive Re:TUI Lua capabilities must declare them before they run:

```lua
-- permissions = "network,clipboard,local-files,active-tick,vibrate,notifications,apps,intents,shortcuts"
```

These are script permissions, not Android manifest permissions. Re:TUI does not expand launcher permissions for Lua. The supported script permissions are `network`, `clipboard`, `vibrate`, `local-files`, `active-tick`, `notifications`, `apps`, `intents`, and `shortcuts`. `module -check <id>` reports missing or unsupported metadata without executing blocked scripts. `module -approve <id>` stores consent for the current script hash and permission set; changing the script or adding a capability requires approval again.

Use `-- retui = "1"` to declare the Re:TUI Lua API version a script targets. Missing metadata defaults to API `1` for compatibility.

When a script fails at runtime, the Lua module panel adds recovery chips for `edit`, `check`, `copy error`, and `disable`. `module -disable <id>` parks a script without deleting it; `module -enable <id>` makes it runnable again after edits. `module -copy-error <id>` copies the last saved Lua error for sharing/debugging. Script execution is guarded by a short runtime timeout so accidental infinite loops fail as Lua errors instead of hanging indefinitely.

## Script Shape

```lua
-- name = "Counter"
-- type = "module"

local prefs = require "prefs"

function on_load()
    if prefs.count == nil then prefs.count = 0 end
end

function on_resume()
    ui:set_title("Counter")
    ui:show_text("Count: " .. prefs.count)
    ui:show_buttons({"+1", "Reset"})
end

function on_click(index)
    if index == 1 then prefs.count = prefs.count + 1 end
    if index == 2 then prefs.count = 0 end
    on_resume()
end
```

## Lifecycle

- `on_load()` runs once when the Lua engine loads the script.
- `on_resume()` runs when the Lua module is rendered, including when the module is shown.
- `on_alarm()` runs on the first render, then no more than once every 30 minutes unless the user runs `module -refresh` or taps the module title to force a refresh.
- `on_tick(n)` runs only while the Lua module is the active open module and only after the script opts in with `ui:set_tick_interval(seconds)`. Intervals are clamped between 1 and 60 seconds.
- `on_click(index)` runs when a module button suggestion is tapped.
- `on_action(value)` runs when a parameterized action suggestion is tapped. `on_command(value)` and `on_submit(value)` are fallback names for the same event.
- `on_dialog_action(index)` runs when a script-owned choice list is answered. `-1` means cancel.
- `on_network_result(body, code, headers)` runs after `http:get/post/put/delete`.
- `on_network_result_id(body, code, headers)` runs when a request is made with an id, for example `http:get(url, "id")`.
- `on_network_error(error)` and `on_network_error_id(error)` run on network failure.
- `on_suggest(query)` runs for `-- type = "suggest"` scripts while the user types at the root command prompt.

## UI API

- `ui:set_title(text)`
- `ui:default_title()`
- `ui:clear()`
- `ui:render(layout_table)`
- `ui:layout(layout_table)`
- `ui:show_text(text)`
- `ui:add_line(text)`
- `ui:show_lines(lines)`
- `ui:show_table(rows)`
- `ui:show_kv(table)`
- `ui:show_buttons(labels)`
- `ui:button(label[, action])`
- `ui:add_button(label)`
- `ui:show_action(label, value)`
- `ui:show_command(label, command)`
- `ui:show_module(module, label)`
- `ui:command_button(label, command)`
- `ui:module_button(label, module)`
- `ui:app_button(label, app_query)`
- `ui:intent_button(label, intent_spec)`
- `ui:shortcut_button(label, shortcut_id, app)`
- `ui:show_radio_dialog(title, items, selected_index)`
- `ui:show_list_dialog(title, items, selected_index)`
- `ui:show_progress_bar(label, current, max, width)` renders a bounded Unicode bar using `░▒▓█`; `width` is optional and capped.
- `ui:set_progress(percent)`
- `ui:set_tick_interval(seconds)`
- `ui:set_tick(seconds)`
- `ui:disable_tick()`
- `ui:show_toast(text)`
- `ui:set_expandable(true_or_false)`
- `ui:is_folded()`
- `ui:is_expanded()`
- `ui:expand()`
- `ui:collapse()`
- `ui:toggle()`

Button labels render as native module buttons and matching dock suggestion chips. Tapping either dispatches `module -click <id> <index>`.
Action labels become dock suggestion chips that dispatch `module -action <id> <value>`, which lets Lua modules receive text or other small parameters without parsing the whole command line.
Dialog/list items become suggestion chips that dispatch `module -dialog <id> <index>`, keeping choices in the Re:TUI suggestion surface instead of opening a separate Android modal.
Command labels render as native module buttons and matching chips that execute the command directly.

The active Lua module no longer needs a default refresh chip. Opening a module renders it, and tapping the module title forces a refresh. Manual `module -refresh <id>` still works.

## Native Layout and Buttons

`ui:render(table)` lets a Lua module describe a small native panel instead of only returning text. The renderer currently supports:

- `text` objects: `{ type = "text", text = "..." }`
- `row` objects with `children`
- `column` / `container` objects with `children`
- `progress` objects: `{ type = "progress", label = "Done", value = 2, max = 5, width = 8 }`
- `button`, `command`, and `module` objects that run launcher commands
- `divider` and `spacer`
- `pre`, `ascii`, and `code` objects for explicitly monospace blocks

Example:

```lua
-- permissions = "apps,intents"

function render()
    ui:set_title("Control Pad")
    ui:render({
        { type = "text", text = "Launcher-native controls" },
        { type = "row", children = {
            { type = "text", text = "Module state" },
            { type = "progress", label = "Done", value = 2, max = 5, width = 8 },
        }},
        { type = "pre", text = "CPU  [####....] 50%" },
    })
    ui:button("+1")
    ui:command_button("Modules", "module -ls")
    ui:app_button("Settings", "Settings")
    ui:intent_button("Open Web", { view = "https://example.com" })
end

function on_resume()
    render()
end

function on_click(index)
    -- Handle ui:button callbacks here.
    render()
end
```

Plain Lua text follows the active launcher font so custom modules stay visually consistent with the rest of the launcher. Renderer-owned structural elements such as `progress` and `divider`, plus explicit `pre`, `ascii`, and `code` layout objects, use monospace when alignment matters.

Example parameterized todo module:

```lua
-- name = "Quick Todo"
-- type = "module"
-- retui = "1"

function ensure()
    if prefs.items == nil then prefs.items = "Inbox zero" end
end

function lines()
    ensure()
    local out = {}
    for item in string.gmatch(prefs.items, "([^\n]+)") do
        table.insert(out, item)
    end
    return out
end

function render()
    ui:set_title("Quick Todo")
    ui:show_lines(lines())
    ui:show_action("Add create good modules", "create good modules")
    ui:show_action("Add review release", "review release")
end

function on_resume()
    render()
end

function on_action(text)
    ensure()
    prefs.items = text .. "\n" .. prefs.items
    render()
end
```

Expandable state is stored per Lua module. A script can render compact and expanded modes:

```lua
function on_resume()
    ui:set_expandable(true)
    if ui:is_expanded() then
        ui:show_kv({ mode = "expanded", time = os.date("%H:%M:%S") })
    else
        ui:show_text("Standard mode")
    end
end
```

Ticking Lua modules update only while open:

```lua
function on_resume()
    ui:set_tick_interval(1)
    ui:show_text(os.date("%H:%M:%S"))
end

function on_tick(n)
    ui:set_tick_interval(1)
    ui:show_text("Tick " .. n .. "\n" .. os.date("%H:%M:%S"))
end
```

## Persistent Settings

`prefs` is a per-module persistent Lua table:

```lua
local prefs = require "prefs"

if prefs.city == nil then prefs.city = "Bengaluru" end
prefs.refreshes = (prefs.refreshes or 0) + 1
```

Prefs are stored in `widgets/<id>/prefs.json`. Values survive launcher restarts and backup/restore. Use prefs for settings, not large data.

Helper methods are available when you prefer explicit access:

- `prefs:get(key, fallback)`
- `prefs:set(key, value)`
- `prefs:has(key)`
- `prefs:unset(key)`
- `prefs:number(key, fallback)`
- `prefs:bool(key, fallback)`
- `prefs:inc(key, amount)`

## Local Files

`files` stores small module-owned text files under `widgets/<id>/files/`.

```lua
files:write("count.txt", "42")
local count = files:read("count.txt")
files:append("log.txt", "opened\n")
local exists = files:exists("log.txt")
local names = files:list()
files:delete("count.txt")
```

File names are local to the Lua module and cannot include path separators.

## JSON

```lua
local json = require "json"
local data = json.decode('{"ok":true}')
local text = json.encode({ ok = true, count = 3 })
```

## Standard Libraries

Small Re:TUI libraries are available through `require`:

- `date`: `now()`, `seconds()`, `format(pattern, seconds)`, `parts()`
- `fmt`: `upper(text)`, `lower(text)`, `title(text)`, `percent(value, max)`, `progress_bar(value, max, width)`, `bytes(value)`, `round(value)`, `fixed(value, places)`, `pad_left(text, width)`, `pad_right(text, width)`
- `strings`: `trim(text)`, `contains(text, needle)`, `starts_with(text, prefix)`, `ends_with(text, suffix)`, `replace(text, old, new)`, `split(text, delimiter)`, `join(table, delimiter)`
- `colors`: current launcher color values such as `accent`, `primary_text`, `button`
- `debug`: `log(text)`, `toast(text)`, `show()`, `clear()`

Example:

```lua
local fmt = require "fmt"
local date = require "date"

function on_resume()
    ui:show_kv({
        time = date.format("%H:%M:%S"),
        memory = fmt.bytes(123456789),
    })
end
```

## Suggestion Scripts

Suggestion scripts let Lua add command chips without becoming dock modules.

```lua
-- name = "Quick Config"
-- type = "suggest"

local strings = require "strings"
local fmt = require "fmt"

function on_suggest(query)
    local q = strings.trim(fmt.lower(query))
    if strings.starts_with(q, "cfg") then
        suggest:command("Config list", "config -ls")
        suggest:command("Edit behavior", "config -file behavior.xml")
    end
end
```

Suggestion API:

- `suggest:command(label, command)`
- `suggest:module(module, label)`
- `suggest:text(text)`

## HTTP

HTTP calls are async. They return through lifecycle callbacks:

```lua
local json = require "json"

function on_alarm()
    ui:show_text("Loading...")
    http:get("https://api.ipify.org?format=json", "ip")
end

function on_network_result_ip(body, code)
    local data = json.decode(body)
    ui:show_text(code == 200 and data.ip or "Request failed")
end
```

Supported calls:

- `http:get(url, id)`
- `http:delete(url, id)`
- `http:post(url, body, media_type, id)`
- `http:put(url, body, media_type, id)`
- `http:set_headers({"Header: value"})`

## System Helpers

- `system:open_browser(url)`
- `system:open_url(url)`
- `system:to_clipboard(text)`
- `system:copy_to_clipboard(text)`
- `system:clipboard()`
- `system:vibrate(milliseconds)`
- `system:lang()`
- `system:tz()`
- `system:tz_offset()`
- `system:battery_info()`
- `system:network_state()`
- `system:app_version()`
- `system:app_version_code()`
- `system:widget_id()`
- `system:widget_name()`

## Launcher, App, Intent, and Shortcut Helpers

`launcher:state()` returns a table with launcher-safe values such as `widget_id`, `widget_name`, `app_version`, `app_version_code`, `language`, `timezone`, `battery`, and `network`. The `widget_*` names are retained for compatibility with existing Lua packages.

`launcher:vars()` returns a compact API discovery table for scripts that want to show the available helper groups.

Launcher button helpers:

- `launcher:command_button(label, command)`
- `launcher:module_button(label, module)`

App helpers require `apps`:

- `apps:list(limit)` returns launchable app entries with `name`, `label`, `package`, `class`, `component`, and `command`.
- `apps:find(query)` finds a launchable app by label, package, or component.
- `apps:launch_command(query)` returns an explicit Re:TUI `intent -activity -n ...` command.
- `apps:button(label, query)` adds an app launch button.

Intent helpers require `intents`:

- `intents:view(url)`
- `intents:activity(component)`
- `intents:command(spec)`
- `intents:button(label, spec)`

Shortcut helpers require `shortcuts`:

- `shortcuts:use_command(shortcut_id, app)`
- `shortcuts:button(label, shortcut_id, app)`

## Clock Helpers

- `clock:timer()` returns `{ running, remaining_ms, total_ms, elapsed_ms }`.
- `clock:stopwatch()` returns `{ running, elapsed_ms }`.
- `clock:pomodoro()` returns `{ running, remaining_ms, total_ms, elapsed_ms, task, type, cycle }`.
- `clock:format_duration(milliseconds)` returns the launcher timer format.
- `clock:parse_duration(text)` parses values such as `30s`, `5m`, or `1h` into milliseconds.

## Launcher Helpers

- `aio:self_name()`
- `aio:widget_name()`
- `aio:retui_version()`
- `aio:show_toast(text)`
- `aio:colors()`

## Security Model

The runtime uses a small Re:TUI-owned Lua environment instead of LuaJ's full JSE globals. Scripts can use safe Lua basics plus Re:TUI APIs, not arbitrary Java, shell execution, package loading, or filesystem loaders such as `dofile` / `loadfile`.

Sensitive APIs are enforced when the API is called, not just by scanning the script text:

- `http:*` requires `network`
- `system:clipboard`, `system:to_clipboard`, and `system:copy_to_clipboard` require `clipboard`
- `system:vibrate` requires `vibrate`
- `files:*` requires `local-files`
- `ui:set_tick_interval` / `ui:set_tick` requires `active-tick`
- `reminders:*` requires `notifications`
- `apps:*` and `ui:app_button` require `apps`
- `intents:*` and `ui:intent_button` require `intents`
- `shortcuts:*` and `ui:shortcut_button` require `shortcuts`

Declare these with `-- permissions = "network,clipboard"` and approve with `module -approve <id>`. This does not add Android manifest permissions.

Network responses and module-local files are size-limited to protect launcher memory and app storage. Shell automation belongs in Termux modules.

## Current Limits

Android AppWidgetHost bridging, arbitrary Android view classes, arbitrary Java/Kotlin execution, and global filesystem or shell access are not part of the Lua module surface. Use Termux modules when a workflow needs Linux tools, long-running background work, or shell automation.
