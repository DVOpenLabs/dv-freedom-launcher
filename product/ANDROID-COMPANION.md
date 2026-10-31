# Everyday controls and local app editing

The optional companion controls extend the native launcher. It does not install another launcher,
web engine, notification listener, microphone service or TV-side HTTP server. No Android dependency or
uses-permission is added. Personal artwork remains outside the APK.

## Categorized Launcher settings

The settings page has seven sections: **Appearance**, **Apps & rows**, **Wallpaper**, **Profiles**,
**Remotes & inputs**, **Backup & restore** and **System**. Up/Down selects a category; Right or OK opens
its controls. Back returns to the parent, then closes settings. Contextual help and still previews
explain visual controls. Browsing saves nothing; changes retain their Apply/Cancel or Keep/Revert
contracts. See the [feature guide](ANDROID-COMMUNITY.md) for presets and compatibility controls.

Layout captions show current values, including theme values, and apps-per-row now offers 4 through 8
so an existing eight-app layout has a matching selected control. Numeric rows retain Left/Right
movement. Restoring theme defaults requires a confirmation, focused on Cancel; it preserves app order
and media files. The dialog explains the startup-app reset in Current Home. Opening TV settings remains
an explicit action and never applies a TV setting automatically.

This is an independent native implementation using the public
[Projectivy feature overview](https://github.com/spocky/miproja1) as
references. No Projectivy application code, proprietary assets or additional preference framework was
imported. English-device navigation was tested; RTL, large-font and physical-remote acceptance are
separate checks, not inferred from the layout compiling.

## Quick actions and finding an app

Open **Launcher settings â†’ Quick actions**, or hold OK on the header's Settings button and choose
**Quick actions**. The compact panel reuses the existing QuickSettingsActivity. It opens the TV's own
settings, display, network, Bluetooth/accessories and apps pages; it also offers **Find apps** and
Launcher settings. Opening a page does not apply a picture, sound, network or pairing setting.

**Find apps** opens the installed-app library without adding a new Home tile. Up from a card selects
the Find apps button. Search uses the native keyboard and matches every query word against the app's
installed name and package, ignoring case and accents. Clear search restores the full catalogue.
Nothing is sent to a search service or saved as search history. In this quick-action library, OK opens
an app; adding/removing a Home shortcut remains an explicit hold-menu action. The ordinary library
retains its existing OK/add/selection settings. Back closes the utility library and returns focus to
the header Settings button.

The Bluetooth shortcut tries exported, permission-accessible native Settings entry points. On the
tested Sony Android 12 build it opens Remotes & accessories directly. On the tested Google TV Streamer
Android 14 build those direct routes are unavailable; it opens TV Settings and explains where to go.
It does not scan, request Bluetooth grants, bypass startup verification, select a device or pair one.

## Recently opened apps, profiles and wallpaper dimming

**Launcher settings â†’ Apps & profiles â†’ Remember recently opened apps** is off by default. Enabling
it explicitly remembers up to six package IDs opened successfully through Home tiles or the app library.
**Quick actions â†’ Recently opened apps** shows newest first, using the current profile's app names and
omitting hidden, disabled, removed or unavailable apps. Reopening an app moves it to the front without
changing favorites. It opens the app, not a promised episode/playback position. Dedicated remote app
buttons, other launchers and viewing history are not tracked. There is no UsageStats permission,
notification access, timestamp log, service or network request.

History uses a small separate preferences file per launcher profile, so recording a launch does not
invalidate an unrelated desktop app draft. **Clear recently opened apps** clears that profile's list;
switching the feature off clears it and removes the optional flag. Failed saves/clears repair the
in-memory preference state. This history is independent of the pre-existing cinematic shelf's
`LastOpened` timestamps; it neither imports nor clears those older shelf records.

When the existing `local_profiles` capability is enabled, holding OK on the header Settings button
offers **Switch launcher profile** directly. Quick actions has the same shortcut. It uses the existing
profile library and chooser; import remains in Launcher settings. It switches launcher preferences only.
Profile import/setup and Google/streaming accounts are unchanged. The two owner trial configurations
currently leave this capability off; they do not gain a profile row or a fabricated user automatically.

**Launcher settings â†’ Wallpaper â†’ Wallpaper dimming** offers Follow configuration, 0%, 15%, 30%, 45%
and 60%. The existing still/video renderer applies it only to the background. Icons, labels, app layout,
wallpaper files, the screensaver and the TV's actual brightness setting are preserved. The choice is
scoped per profile; Follow configuration removes it, and explicit theme-default reset clears it.

These are independent implementations inspired by Projectivy's wallpaper controls, Home Deck's recent
apps and LiquiTvOs profile access. Quick actions can scroll to keep added entries reachable on shorter
screens. No new dependencies, permissions or Home routing services are introduced.

## Optional audio indicator

**Launcher settings â†’ Audio-playing indicator** is off by default and stored per launcher profile.
When enabled, it shows the generic text **Audio playing** if Android's `AudioManager.isMusicActive()`
reports active music. It checks every two seconds only while Home is resumed. Leaving Home removes the
callback and hides the label. The indicator has no focusable control and no extra service.

It cannot identify a song or source app, and is not a Cast, HDMI or playback-success indicator. There
is no notification, media-session, recording or usage-access permission. A wallpaper's own muted
soundtrack uses UI-sonification audio attributes at zero gain, mapped separately from music; no system
volume or audio focus is changed. This avoids reporting the silent background as music
([Android attribute mapping](https://source.android.com/docs/core/audio/attributes)). Device-specific
external audio paths may not be reflected by Android's music-active signal.

## Desktop app editor

Run from the repository root with the existing authorized ADB tools:

```sh
python tools/layout_editor.py --tv <tv-ip> --package com.gathin.tvhome --label "Living room"
```

Use `--repo <tools-checkout>` when the existing `tools/tv.py`, venv and authorized key live in a separate
checkout. Use the exact installed package; a preview build can have a different application ID.
`--demo` opens a disconnected example instead. `--port` selects a local port; `--no-browser` only prints
the URL. An optional `--wallpaper <local-still.jpg>` changes the computer's illustrative preview only.

The browser page can arrange 1â€“32 favorite apps, hide/unhide tiles and edit their displayed names.
Its picture is an app-order sketch, not a pixel-exact renderer of every theme, scaling or layout.
Theme/media files, Home selection, permissions, services, system settings and streaming-app data are
outside its write contract. In particular, it never calls the theme-push scripts that reset manual
launcher appearance choices.

The editor also offers **Top / Middle / Bottom** app position. This is a profile-scoped alignment
choice: it does not change icon dimensions, app order, wallpaper or TV display scaling. **Use theme
position** removes that override. Existing app-only clients and imported four-field app drafts preserve
the current position. Position is reviewed alongside app changes and included in guarded Undo; app-draft
exports continue to contain only app order, names and visibility.

The Home illustration includes clock/date, weather and header buttons. An authorized read captures
only allowlisted text from this launcher's visible Home, with a capture time; **Reload from TV** refreshes
it. It does not capture another app's screen or poll weather. Weather appears when the installed Home
has a weather header/companion; this does not add weather networking to the public launcher. If Home is
not visible, the page labels the clock as PC time and weather as unavailable. Inputs and Settings reflect
the current header configuration, and the depicted buttons do not open system settings. Tile artwork,
responsive sizing and complex themes remain illustrative; an optional wallpaper is a local still.

On the TV, open **Launcher settings â†’ Home layout â†’ App position**. The three choices start a 20-second
preview with **Keep / Revert**; Revert is focused first. Timeout, cancellation, leaving Home or process
death discards an unkept preview. The saved position stays unchanged on upgrade until explicitly chosen.
Theme-default reset removes the override. Header clearance is retained on crowded layouts; a layout
too tall for three distinct alignments remains scrollable rather than overlapping the clock.

1. Load the selected TV/profile and edit a local draft.
2. Preview the changes. The confirmation names the target and lists the changes.
3. Apply only the exact reviewed draft. A private JSON backup is written first.
4. Undo restores the previous app state and position, provided nothing relevant has changed since that Apply.

Backups default to ignored `product/private/layout-backups/`; use `--backups` to choose another local
directory. They include package names and personal app labels: do not commit or publicly upload them.
An exported draft contains only the four-field layout, not device credentials, media or system settings.

Import validates a JSON draft into the browser; it does not apply it. An assistant can prepare this
same format for review, but **there is no built-in AI provider, model SDK, cloud call or credit charge**.
The app catalogue is the source of valid installed IDs; never invent them. Profiles use the separate
[profile library](ANDROID-PROFILES.md); this editor edits the currently selected profile and does not
switch Netflix/Google accounts or create a parental-control boundary.

```json
{"schema":1,"favorites":["org.example.video"],"hidden":[],"labels":{"org.example.video":"Video"}}
```

### Preservation and conflict handling

The native provider serializes requests with the UI/profile picker, rejects edits during on-TV reorder
or a style preview, and fingerprints preferences, profile, theme configuration and native order. Stale
drafts and stale Undo are refused. Reload and review again; a timeout is not proof a write failed.
The native write is one atomic preference transaction. A failed commit repairs the prior in-memory
state. The original `order.txt`, base configuration and media are not rewritten by the editor.

A name/visibility-only change preserves native ordering and dock-fill behavior. Explicit desktop
favorite moves take precedence over old native ranks, while preserving the relative order of remaining
apps. A later saved on-TV order takes precedence again. Undo restores the prior override, rather than
reconstructing an approximate base layout. Cards, empty docks, arbitrary grid ordering and individual
component aliases are outside this first app-package-only editor.

The server binds only `127.0.0.1`; Host/Origin checks and a random session token protect API calls. It
serves fixed local assets under a restrictive CSP. The TV provider separately enforces the shell's
`WRITE_SECURE_SETTINGS` permission; ordinary apps cannot call it. The launcher itself does not receive
that permission. Do not expose the editor through port forwarding or a network bind.

See the SHOWCASE review, [source register](sources.json) and
[alpha limitations](../README.md#alpha-limitations). Remote keyboard behavior, native settings routes and playback still
need acceptance on each model; a successful app launch alone does not establish protected playback.

## Phone media inbox and Now Playing

The full Android product includes a fixed `.inbox` provider protected by Android's shell/system
`WRITE_SECURE_SETTINGS` boundary. It accepts only prepared, profile/revision-bound, checksummed JPEG,
PNG or MP4 transfers into private cache. It has no arbitrary file, command, profile-write or theme-
selection endpoint. Review and library import require separate actions; received media never starts
as Home wallpaper automatically. See [phone setup](../setup_app/README.md#use-your-phone).

**Apps & rows â†’ Now Playing** starts Off in each profile. Android Notification access is a separate,
broader permission; grant it only after reviewing that explanation. The listener service ignores
notification text, and Home/player-panel media-session callbacks close on pause or destruction. Only
apps visible in the current profile qualify. The panel offers only the actions reported by that app,
with no background polling, history, artwork download, credential access or cloud call. Turning the
profile display off does not revoke the system grant; revoke that in Android Notification access settings.

Content shelves now include at most 12 Watch Next/Preview Programs cards, reading at most 96 rows per
source per foreground visit. Titles are capped at the profileâ€™s choice of one, two or four per visible app, with twelve overall. Posters are optional
local app-provider resources (2 MB input, bounded decode), with an app banner fallback. HTTPS images are never fetched by Home; the optional Companions app can fetch bounded thumbnails
after its separate poster opt-in. All reads/callbacks are scoped to the visible surface; empty shelves collapse.


## Optional panels and TV phone remote (alpha 17)

**Launcher settings â†’ Apps & rows â†’ Add-on Center** opens installed companions. The desktop Setup
Add-on Center provides reviewed installation of six pinned offline APKs. It verifies an Ed25519-signed
catalogue, bytes, SHA-256, signing certificate, version and Android minimum; updates reject a changed
signer or downgrade. A fresh private device/Home record and previous APK are saved before an update.
This is not an Android private-data backup or a promise of automatic rollback: Android can reject old
versions. No default Home, accessibility, overlay, exact-alarm or runtime grant is changed by installation.

**DV Freedom Companions** (`com.dvopenlabs.freedom.companions`) is a separate INTERNET-enabled app.
It starts unconfigured. Configure Home Assistant in its private TV form, then choose up to eight lights,
switches, fans or scenes. Each action requires a Run confirmation. Security-device control and arbitrary
services are excluded. Read refreshes occur only while its panel is visible, at ten-second intervals;
closed panels have no polling service. Local HTTP is supported only for a user-entered trusted endpoint;
HTTPS certificate failures and redirects are not bypassed. No token is returned to Home or logged.

Its calendar form accepts separate family and sports HTTPS iCalendar subscriptions. Links may be
secrets; do not paste them into issues or screenshots. Fetches are limited to 1 MB, happen at most twice
daily when a panel/Home requests a refresh, and retain a dated cache when offline. Calendar expansion
includes daily-or-slower recurrence, exceptions, replacement instances, all-day and timezone handling;
subdaily recurrence is intentionally omitted. At most twelve events per feed within ninety days are
kept. Game Day is a countdown to calendar events, not a score service. Forget feeds removes their cache.
Enable companion sharing and **Apps & rows â†’ Calendar on Home** separately for the desired profile.
This visibility flag is private to that profile and excluded from AI/setup exports. Empty cards hide.

Network poster sharing is a distinct companion opt-in. Only public HTTPS hosts on port 443 are used,
with validated DNS addresses, no proxy/redirect, no credentials, a 2 MB response cap, 12 MP decode bound,
384Ã—216 thumbnail and 16 MB LRU cache. Failed URLs back off for five minutes, with at most 128 hashed
negative-cache keys, preventing failed-poster notifications from continually restarting requests. Home uses an app banner until the local thumbnail arrives.
Downloads may disclose the TVâ€™s IP and requested poster URL to the image host. They are not enabled by
installing the companion. Local posters continue to work without it. Credentials, feed links and raw
poster URLs are absent from diagnostic exports. Companion private data is excluded from Android backup
and transfer; dependency licenses are available in Artwork & privacy â†’ Open-source notices.

**DV Freedom Phone Remote** (`com.dvopenlabs.freedom.remote`) hosts its own local server on the TV.
Open it on TV, choose an owned LAN interface, and explicitly Start; enter its displayed address and
six-digit PIN in a phone browser. No PC, cloud service, discovery scan or firewall change is required.
Pairing expires in five minutes, one phone session in thirty minutes, and a new pairing revokes the old
phone. Five wrong attempts cause a one-minute lockout. Stop revokes all access, including its pairing window and already accepted/queued commands. No boot receiver or
automatic server startup is present. HTTP is unencrypted: trusted local networks only; never port-forward.
Host/Origin checks, same-site HttpOnly cookies, bounded workers/body/timeouts and an action allowlist
protect the local endpoint. It has no ADB, shell, file browser, arbitrary intent or screenshot endpoint.

Android Accessibility is a separate broad permission, explained before opening system settings. The
service ignores events and inspects the focused node only on a paired command. Keyboard replaces the
focused editable field and rejects password fields; no text or action history is saved. Android 13+
provides global D-pad actions. Android 11/12 depends on app-exposed focus and cannot promise navigation
in every app. Home/Back, media keys and volume are explicit phone actions; use the physical remote when
an action is unavailable. The foreground service runs only while the server is enabled. Neither
installing the app nor opening its pairing screen enables Accessibility.

## Alpha 18 optional panels

**Discover** is a separate panel in Companions. Configure your own TMDB v3 API key or v4 read token
and two-letter region before any catalogue request. Movies/TV and subscription-provider filters are
available; title search clears the provider filter. Up to 18 cards and eight reported services are
shown. Metadata uses a bounded 4 MB cache; failures can show saved availability, clearly marked.
A local watchlist retains up to 100 titles and 256 KB. It is independent of streaming accounts and
shared by users of this companion on the TV. No private viewing history or account scraping occurs.

Availability is supplied by JustWatch via TMDB, may be stale/incomplete and is advisory. An installed
known service offers its exported search action when available, otherwise Open app. There is no claim
that every title has a universal playback link. **About & data credits** includes the TMDB mark,
required notice and JustWatch attribution. Commercial API use needs its own appropriate provider terms.

**Personal photos** supports one explicitly selected Immich album or WebDAV folder. Credentials remain
in private, backup-excluded storage. Refresh is manual by default; optional ambient refresh is described below. There is no job/service. Immich requests only that
album and image previews; WebDAV uses Depth 1, disables DTD/external entities, accepts direct image
children only and never follows a supplied link to another origin. Redirects/proxies are disabled.
Up to 12 decoded/re-encoded local pictures and 16 MB remain cached, available through the same-signer
provider. Source replacement/Forget clears cached photos. Wallpaper Studio previews a selected cache
item and requires Add; no source refresh changes the active wallpaper. HTTPS certificates are verified;
local HTTP exposes credentials/content on the LAN. A request already in flight may take up to the
bounded ten-second timeout to finish after leaving, but its UI result is discarded. Real configured
Immich/WebDAV acceptance is pending; official API routes and local path-policy tests were checked.

**Phone setup** extends the existing paired Remote. Check its setup/uploads option before Start, and
separately allow phone setup sharing in the current launcher profile. Remote commands do not imply setup
rights. The paired phone can read app names/IDs and stage allowed appearance/favorite choices for two
minutes; native review/Apply still controls saving. Upload accepts one JPEG/PNG (8 MB) or short MP4
(48 MB), checks profile/revision, size/checksum and stages app-private media for native review. It cannot
read arbitrary files or overwrite/select the current wallpaper. Stop rejects queued/new work, revokes
pending drafts and cancels unfinished phone transfers. Ready media still requires native import review.
Neither opting into setup nor installing an update grants Accessibility or starts the server.

Optional Gemini drafting is described in [AI setup](AI-SETUP.md). No cloud credential is bundled and no
paid inference occurs without an explicit request review.

## Photo & Art ambient

In **Companions → Personal photos**, enter your own Immich/WebDAV address and credentials. The explicit
**Choose an Immich album** action requests album names; selecting one only fills the draft. Save, then
Refresh to cache that album. An optional exact Immich tag filters images within that album. Up to twelve
images/16 MB are retained. Date, place and people captions are private cache metadata and start hidden.
Forget clears credentials and cached photos. Source changes never select a Home wallpaper.

**Photo & Art appearance** offers fit/mat framing, portrait pairs, transitions, reduced motion and
optional captions, with Apply/Cancel. Preview uses cached images. Selecting the optional system
screensaver remains an Android choice; some Google TV devices do not expose its picker. Refresh every
30 minutes is separately opt-in and only runs while ambient is visible. Leaving releases its worker,
bitmaps and timer; it is not an unattended server sync.

**Museum art** offers a small curated list from The Met Open Access API. Select a work and explicitly
download it over HTTPS. Only API records marked public domain and the fixed museum image host are
accepted. Credits/CC0 attribution stay with the cached work. Art has a separate cache; opening the
list makes no network request. Your own photos and active Home wallpaper remain saved.

## Discover filters and links

**Browse filters** supports genre, minimum rating, popular/top-rated/recent ordering and pages 1–20.
Apply/Cancel protects the draft. Top-rated uses a minimum vote count. Text search is global, with its
limits shown rather than silently claiming the browse/provider filters apply to it.

A service card can store a manually entered title URL for that service and title. Only approved HTTPS
host/path forms are accepted: no credentials, query strings, fragments or arbitrary intents. Launch
stays package-scoped; unavailable links offer search/Open app. Availability is advisory, and a valid
link does not guarantee playback or subscription access. No streaming account is scraped.

Home Assistant favourites now also show sensors, binary sensors, climate temperatures and cover
positions read-only. Scene/light/switch/fan actions retain the existing confirmation and allowlist;
adding a read-only entity does not grant a new remote control or start a new connection.

These panels require Companions alpha 4. Native rendering, real provider accounts and device-specific
screensaver behaviour need acceptance on the selected TV; JVM, lint and build checks do not prove them.

Family/Game Day calendars merge duplicate titles at the same start time across the two subscriptions,
keep upcoming timed events in order, and show minute/hour or Tomorrow countdowns rather than labelling
every nearby event Today. All-day wording uses calendar dates across DST. This remains cached calendar
information, not a live-score service; reload the panel to update the countdown wording.

Home Assistant refreshes have one owner per visible connection generation. Repeated refreshes
coalesce into one follow-up; leaving or rebuilding the page discards stale results and queued actions.
Climate, security and sensor state remain informational. No real Home Assistant connection is implied
by unconfigured-panel tests.

## Seasonal ambient looks

Companions alpha 6 adds **Seasonal ambient looks**: Autumn glow, Winter light, Spring bloom and
Summer tide. These are original procedural scenes drawn on the TV, with no image pack, download,
location lookup or season guessing. Open the existing Companions hub; browse and Preview before Apply.

Choose On, a scene, an expiry date (7/30/90-day shortcuts) and optional gentle motion/clock drift.
Preview saves nothing. Cancel or Back discards the draft. Apply changes only the companion’s seasonal
choice. Reopening the editor preserves its saved dates; it does not silently extend the season.
After the inclusive local expiry day, ambient returns to the existing Photo & Art choice. Clock drift
and motifs stop when Android animations are disabled. Moving scenes redraw at most fifteen times per
second; static scenes refresh the clock once a minute. Work stops away from the ambient surface.

This setting starts off, does not change Home wallpaper, and does not select or take over Android’s
screensaver. The system picker remains a separate user choice. Preview saved Photo & Art appearance
explicitly bypasses seasonal mode. No screen mode guarantees protection from OLED image retention.
Camera remains an independent optional integration requiring a user-configured source.
