# Re:T-UI Preset Marketplace

The website uses Cloudflare Pages Functions for preset uploads and browsing.

- D1 stores preset metadata and moderation status.
- Workers KV stores screenshots, preset packages, and optional wallpaper images.
- Public gallery reads only approved presets from `/api/presets`.
- Public asset downloads only work for approved presets.
- Uploads land in `pending` status through `/api/presets/submit`.
- Staff moderation is available at `/moderate.html` after setting `RETUI_ADMIN_TOKEN` as a Pages secret.

## Initial Setup

```sh
wrangler d1 execute retui-presets --remote --file cloudflare/d1/0001_preset_marketplace.sql
wrangler kv key put "presets/northern-violet/screenshot" --path docs/preset-assets/northern-violet/screenshot.png --namespace-id fcff455084a64eee86e2c2e1358cc80e --remote
wrangler kv key put "presets/northern-violet/preset" --path docs/preset-assets/northern-violet/northern-violet.retui-backup.zip --namespace-id fcff455084a64eee86e2c2e1358cc80e --remote
wrangler d1 execute retui-presets --remote --file cloudflare/d1/seed_northern_violet.sql
```

## Moderation Secret

Set a private token before using `/moderate.html`:

```sh
wrangler pages secret put RETUI_ADMIN_TOKEN --project-name re-tui
```

R2 is a better long-term asset store, but this project currently uses KV because R2 is not enabled on the Cloudflare account yet.
