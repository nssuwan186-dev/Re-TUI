# Cyberpunk Visual Pass Idea Board

This board is reference-only. The app implementation should use original Android drawables and should not ship CDPR, Reddit, or daemon-dotfiles assets as production UI.

## Local References

| File | What To Validate |
| --- | --- |
| `references/cyberpunk-site-hero-buttons.png` | Reusable stretch button: left notch, angled lower-right corner, uppercase centered text, thin outline variants. |
| `references/cyberpunk-site-button-strip.png` | Dynamic-width CTA buttons where the middle stretches and the edge geometry stays fixed. |
| `references/breach-protocol-panel.png` | Terminal HUD chrome: thin square rails, tabbed headers, lime/cyan contrast, dense information panels. |
| `references/cyberdeck-mobile-layout.png` | Mobile launcher direction: dark red deck surface, segmented action buttons, terminal/input bar, side rails, sparse HUD markings. |
| `references/emulator-visual-pass.png` | Current Re:T-UI implementation check: angular dynamic buttons, HUD backdrop, output/input/tool/suggestion chrome. |
| `references/emulator-toolbar-frame.png` | Toolbar refinement check: focus-bracket icon buttons and capped terminal panel angle. |
| `references/emulator-cyberdeck-behavior-toggle.png` | Behavior-toggle check: cyberdeck drawables are gated by `enable_cyberdeck_mode`, with the existing theme palette left intact. |
| `references/emulator-cyberdeck-wallpaper-auto.png` | Wallpaper-auto check: generated palette recolors the same cyberdeck drawables through active theme colors. |

## External References

- Cyberpunk homepage: https://www.cyberpunk.net/in/en/
- Cyberpunk button CSS pattern: `.cp-btn` plus color/fill modifiers using SVG `border-image` slices.
- Reddit thread: https://www.reddit.com/r/Cyberpunk/comments/1s2vpu2/turn_your_phone_into_a_cyberdeck_with_this/
- daemon-dotfiles: https://github.com/MathisP75/daemon-dotfiles

## License Notes

- `MathisP75/daemon-dotfiles` is GPL-3.0, not MIT. Treat it as inspiration unless the project explicitly accepts GPL obligations for copied assets.
- CDPR/Cyberpunk web assets are not app assets. Recreate the geometry in app-native drawables.
- Reference screenshots in this folder are for validation only.

## Surface Rules

- `Behavior.enable_cyberdeck_mode=true` is a visual mode, not another theme pile.
- The mode intentionally ignores rounded-corner and dashed-border user settings.
- The mode does not set a palette; drawables use the active theme colors, including wallpaper-derived colors from `wallpaper -auto`.
- Text, command behavior, visibility settings, tray behavior, modules, suggestions, and user content remain normal launcher behavior.
- Buttons and panels must stretch from measured Android view bounds; no baked-in text in assets.

## Geometry Targets

- Sharp rectangular panels with angled lower-right corners.
- Small left-edge notch on buttons and compact chips.
- Thin rails and micro-lines on large panels only.
- Separate filled, outline, and selected states through colors, not separate duplicated art.
- Subtle background grid/rails behind content, with low contrast so terminal text remains readable.
