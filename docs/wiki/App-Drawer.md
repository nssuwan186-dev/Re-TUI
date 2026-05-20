# App Drawer

Re:T-UI includes a terminal-style app drawer instead of a generic Android list.

## Design

The drawer behaves like an overlay window on the desktop:

- full terminal frame
- attached header label
- group tabs on the left
- alphabet tabs on the right
- launcher wallpaper still visible outside the drawer

## Groups

App groups are driven by the existing app command system.

That means command-line organization and drawer organization are connected.

Examples:

- `apps -mkgp favourite`
- `apps -addtogp favourite <app>`
- `apps -ls`

Those groups can then appear as left-side tabs in the drawer.

## Launching vs Managing

Typing an app name at the prompt is the fast path for launching.

Use the `apps` command for management:

- `apps -l <app>` for details
- `apps -hide <app>` to keep it out of the drawer
- `apps -show <app>` to restore it
- `apps -st <app>` for Android app settings
- `apps -ps <app>` for the Play Store page
- `apps -addtogp <group> <app>` for deliberate grouping

For the full command list, see [Apps Command](./Apps.md).

## Alphabet Navigation

The right side tabs are used for fast alphabetical jumps.

As you scroll, the current alphabet section is highlighted.

## Hidden Apps

The drawer respects app visibility rules.

If an app is hidden through app commands, it should not appear in the drawer.

## Why It Matters

This is one of the best examples of what Re:T-UI is trying to do well:

- command logic underneath
- strong terminal presentation on top
- no need to choose between power and readability
