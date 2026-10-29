# DV Freedom Launcher — Samsung manual-launch pilot 0.1.0

The Samsung edition is implemented as an independent Tizen web app. It uses our shared web policies,
with a separate Samsung public-API adapter. **Source/browser/mock tested; no Samsung TV has been
accepted yet.** Tizen 6.0 is the declared minimum, not a compatibility guarantee.

This app opens from Samsung's Apps screen. **Samsung Home stays available and selected.** It does not
root the TV, intercept Home, start at power-on, disable apps, change picture/sound/network settings or
create SmartThings rules. No Samsung account, token or API key is read by the app. Samsung's official
signing/developer tools require the TV owner's own account and certificate separately.

## Included

- Clean, Glass and Cinema presentations; top/middle/bottom favorites.
- Small/Medium/Large icons, independent spacing and theme-aware app-name color. Plain labels have no
  outline/border; a row fade supports contrast. Strong dual-color focus marks the selected app.
- Installed-app discovery and launch requests through public Tizen APIs. App IDs are checked again
  before launch. Inventory errors, missing apps, denied access, timeout and cancellation are handled.
- Eight favorites, Library/search, draft ordering and settings with Apply/Cancel. Browsing does not save.
- Local profiles, avatar colors, per-profile appearance/order and revision-guarded local persistence.
  Invalid/corrupt state is kept for recovery, never silently reset. Profiles do not switch accounts.
- Original local starter scenery and optional packaged personal still/MP4 scenes. Video is forced muted
  at zero volume; a single player releases its source outside Home, with a still fallback. Wallpaper
  changes after 15 minutes of foreground time; hidden time does not consume the interval.
- Quiet idle, keyboard/D-pad navigation, modal focus containment, predictable Cancel/Back and explicit
  exit. Home, held volume and input/settings keys remain Samsung's responsibility.

Browser example data uses a separate storage namespace and cannot contact a TV. A launch success
callback means a request was accepted, not that foreground display or protected playback was verified.
The first 100 visible apps are listed; if the TV reports more, the limitation is shown.

Not in this pilot: automatic Home return/startup, weather, Android TV content shelves, layout PINs,
portable Android/LG profile import or an unattended Samsung installer. The Windows setup GUI now has a [guided first-install path](../../setup_app/NATIVE-SETUP.md)
using the owner’s local SDK and existing certificate profile; native updates/backups remain pending. Samsung profiles are
native local state; existing portable-v1 readers are unchanged.

## Preview and stage

From the repository root, with Python 3.10+:

```sh
python platforms/tizen/prepare_launcher.py product/build/samsung-pilot --archive product/build/DV-Freedom-Samsung-unsigned.wgt
python -m http.server 9496 --bind 127.0.0.1 --directory product/build/samsung-pilot
```

Open [the local preview](http://127.0.0.1:9496/) and choose **Try the example Home**. It is a simulation,
with example apps and local browser-only preferences. The native package ignores browser simulation
when Samsung APIs are present. A real TV reports its actual apps; no example apps are injected there.

Use a new output directory/archive for each build; the tool refuses to overwrite existing data.
It stages only allowlisted application files, the shared policies/media controller, Outfit, original
scene/icon and licence notices. It does not connect to a TV, sign, install, restart or run any helper.
The generated `.wgt` is an **unsigned source archive, not an installable release**. Binaries stay out of Git.

### Optional personal wallpapers

Add `--media /private/path/scenes` to the stage command. Use a dedicated flat folder containing one to
eight lowercase scenes: `scene.png` or `scene.jpg`, optionally paired with `scene.mp4`. Every MP4 needs
its same-name still fallback. No directory links or extra files are accepted. Stills are limited to
8 MiB/8192-pixel PNG dimensions, MP4s to 100 MiB, and the collection to 256 MiB. Header checks are not
codec certification. Use a TV-compatible H.264 MP4 and test decoding on that exact model.

The app cannot upload files or fetch wallpaper URLs. Personal art/media never belongs in the public
repository. Select **Background → My local library** after packaging it. Motion can be disabled;
reduced-motion preference also keeps the still. Audio tracks are not removed from user files, but the
app cannot play them audibly. Browser player tests are not Samsung hardware decoder acceptance.

## Samsung signing and device testing

Read Samsung's [TV device guide](https://developer.samsung.com/smarttv/develop/getting-started/using-sdk/tv-device.html),
[certificate guide](https://developer.samsung.com/smarttv/develop/getting-started/setting-up-sdk/creating-certificates.html)
and [CLI guide](https://developer.samsung.com/smarttv/develop/getting-started/using-sdk/command-line-interface.html).

1. Record the full model, installed Tizen/firmware and region. Use a consenting spare/test TV first.
   Inspect `config.xml`: only `application.info` and `application.launch`, with background support off.
   There is no network privilege, service or startup metadata. The in-page CSP disallows connections.
2. The owner enables Developer Mode and pairs official Tizen tools, including any restart Samsung
   requires. This tool does not do those steps automatically. Use SDB/Tizen tools, not Android ADB.
3. In Samsung Certificate Manager, create/select your own Samsung profile and include this TV's DUID
   where required. Keep its author key for future updates; do not share keys, passwords or DUID in issues.
4. Review the staged directory, then build/sign with the official CLI and your own profile. A typical
   command is `tizen package -t wgt -s YOUR_PROFILE -- product/build/samsung-pilot`. Follow your installed
   SDK's output/target options. Do not assume the unsigned ZIP or a certificate for another TV will install.
5. On that explicitly selected TV, install/run the signed package using Samsung's documented tools.
   The candidate package is `DVFreedom1`, application `DVFreedom1.Launcher`, version `0.1.0`. It is
   distinct from Android/LG builds. Stop if another app already uses this identity or signing differs.
   No uninstall fallback or firmware/security bypass is provided.
6. Open it from Apps and check the items below. A public certificate may not launch some privileged
   system apps, even if other installed apps work. Keep Samsung Home and the original remote available.

No signing certificate, Samsung SDK validation or TV install was performed during development. No
universal pre-signed package is promised. Preserve personal settings and prior package/signing data
before any future update. Native app data is local storage, not a full-TV backup.

## Checks

```sh
node platforms/tizen/test-launcher.cjs
node platforms/webos/test-launcher.cjs
python -m unittest discover -s platforms/tizen/tests -v
python -m unittest discover -s product/tests -v
```

The tests exercise mock Tizen errors/cancellation, stale replies, app revalidation, profile isolation,
settings drafts, storage corruption, bounded package/media admission and foreground-only silent media.
They do not run Samsung software or claim real remote/playback acceptance. The symbolic-link test may
be skipped where Windows denies creation; no OS privilege is changed to make it run.

Hardware acceptance still needs real arrows/OK/Back, text entry, Home/Source/held volume, app inventory,
actual Netflix/other protected playback, returning manually from Apps, video failure/reduced motion,
one decoder, memory/CPU, standby and an owner-approved restart. Test Apply/Cancel and persistence with
isolated test profiles; retain original TV/Home settings. Do not infer Samsung behavior from LG/Android.

Report non-sensitive results using the [TV compatibility form](https://github.com/DVOpenLabs/dv-freedom-launcher/issues/new?template=compatibility.yml).
Read the [source review](../../product/SAMSUNG-REVIEW.md) before considering any later return helper or
cloud startup. No Garage Home, Vetro or Skylight code, service, media or font is bundled in this pilot.
