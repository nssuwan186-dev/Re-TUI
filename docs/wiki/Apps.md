# Apps Command

The `apps` command manages app visibility, app groups, app details, and app drawer organization.

Typing an app name at the prompt is still the fastest way to launch it. Use `apps` when you want to manage how apps appear in Re:T-UI.

## Common Commands

- `apps -ls` opens the app drawer, or lists visible apps when the drawer UI is unavailable
- `apps -lsh` lists hidden apps
- `apps -l <app>` shows package, activity, permissions, and app metadata
- `apps -hide <app>` hides an app from normal suggestions and drawer views
- `apps -show <app>` restores a hidden app
- `apps -st <app>` opens Android's app settings page
- `apps -ps <app>` opens the app's Play Store page
- `apps -frc <app>` force launches a known app, including hidden apps
- `apps -file` opens `apps.xml`
- `apps -reset <app>` resets one app's launch count
- `apps -tutorial` opens the public Apps command guide

## Hidden Apps

Hidden apps are not uninstalled. They are removed from normal app suggestions and drawer views.

Use:

```text
apps -hide YouTube
apps -lsh
apps -show YouTube
```

If you still want to launch a hidden app without restoring it, use:

```text
apps -frc YouTube
```

## App Groups

Groups drive focused app drawer tabs.

```text
apps -mkgp work
apps -addtogp work Slack
apps -addtogp work Calendar
apps -lsgp work
```

Group commands:

- `apps -mkgp <group>` creates a group
- `apps -rmgp <group>` removes a group
- `apps -addtogp <group> <app>` adds an app to a group
- `apps -rmfromgp <group> <app>` removes an app from a group
- `apps -lsgp` lists groups
- `apps -lsgp <group>` lists apps in one group
- `apps -gp_bg_color <group> <color>` sets group background color
- `apps -gp_fore_color <group> <color>` sets group text color

Group names should not contain spaces.

## Default App Slots

Default app slots are stored in `apps.xml`.

```text
apps -default_app 1 Phone
apps -default_app 2 most_used
apps -default_app 3 null
```

Use `most_used` when you want usage counts to drive the slot. Use `null` to clear one.

## Troubleshooting

If an app disappeared, run:

```text
apps -lsh
```

If suggestions feel stale after installing or removing apps, run:

```text
refresh
```

If you need Android's app permission or battery screen, run:

```text
apps -st <app>
```
