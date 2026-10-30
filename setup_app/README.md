# DV Freedom Setup — installer private alpha

One local Windows GUI for Android TV/Google TV, native LG/Samsung pilots, experimental hosted
VIDAA/Vizio editions and a Roku ambient-app builder. Each path explains its own requirements and
limits before proceeding; these are not equivalent full-Home replacements. See
[all-platform setup](ALL-PLATFORMS.md). The package includes Python, UI and a verified offline Android
APK. No AI account is required. The setup browser connects to loopback; an optional, separately
reviewed hosted Home listener binds one selected LAN address for its paired TV. Records stay local.

## What it does

1. Explains all setup paths, external Streamers, server requirements and limits in one device chooser.
2. Offers a clearly labelled example journey, or a real authorized ADB connection and inventory.
3. Previews four Home styles, app order, top/middle/bottom rows, three sizes and spacing levels.
4. Verifies the signed offline catalog, APK hash, package, version and installed signer. It rejects
   incompatible updates and saves a private recovery point before installation.
5. Installs only after explicit review, then previews the choices in a **new separate profile**.
   Keep or Revert on the TV within 45 seconds. Leaving the prompt or timeout reverts the preview.
6. Records progress durably and checks the actual TV before continuing an interrupted operation.
   It offers guarded launcher restore and a separate system Home chooser after Keep.

The optional Home-only redirect is explained separately. Setup can open Accessibility settings;
the person must select any helper there. It never grants a permission or selects Home silently.
Stop waits for the current safe boundary; it does not interrupt Android during a package commit.

TCP ADB uses this app's dedicated local key. Pairing-code mode can use existing Android Platform
Tools, or the app can offer Google's licence and download the stable Windows tools **only after
the user accepts**. Tools are not bundled. Download checks Google's HTTPS metadata/checksum and
extracts only the required allowlisted files. See [third-party notices](../THIRD_PARTY_NOTICES.md).

## Recovery limits

- Device records describe the TV; they are not full backups.
- An existing standalone DV Freedom installation gets an active-profile settings/app-order backup
  with wallpaper references, plus a copy of its installed APK. Accounts and media files are not copied.
- A new installation saves its initial launcher baseline before the first preview. Its original Home
  remains installed and selected. Returning to it does not require removing DV Freedom.
- Restore checks the same app/device, current revision and referenced files. It restores launcher
  choices, not system settings, streaming accounts or all profiles. Created profiles remain available.
- The saved APK is an artifact for diagnosis/recovery; setup does not automatically downgrade code,
  uninstall an app or bypass installation protections.
- Opening the previous Home does not change the default. If the optional redirect was manually
  enabled, turn it off in Accessibility before returning to the old Home. Preserve other assistive tools.
- Older pilot/inherited launcher identities are separate apps. Their private profiles and personal
  media are not automatically migrated into the new standalone identity.

Native LG/Samsung pairing, packaging, reviewed first installation, verification and separate launch are
implemented; hardware acceptance is pending. See [the native setup guide](NATIVE-SETUP.md). Native updates
stop before overwriting existing apps; they do not reuse Android backup/preview semantics. Debloating,
personal media import and AI-provider calls are not part of this installer.

## Run from source

The Android bundle is built from the current full product. VIDAA and Vizio now have separate experimental hosted
setup paths; Roku has an ambient-package builder with official manual-sideload instructions.
**Other smart TV** remains a manual privacy-guide path. Native LG/Samsung pilots are retained;
an Android box connected to any TV uses the Android/Streamer path. Exact capability limits are
listed before connection, not hidden behind a generic supported-platform label.

```sh
python -m pip install -r setup_app/requirements.txt
python -m setup_app.main
```

Without a valid `release_bundle` the preview and read-only checks still work, but installation is
unavailable. The server binds `127.0.0.1` on a random port; development options are `--port` and
`--no-browser`. No TV address/key is embedded in source. Local files live in the current user's
application-data directory under `DVFreedomSetup`; keep this directory private.

**Need a hand? â†’ Close setup app** stops the process. Closing only the browser tab leaves it running.
Active installation/download steps must finish or pause before closing. Reopen and reconnect to the
same TV to check a saved setup. The app never blindly retries an uncertain install result.

## Developer build

Use JDK 17, SDK 34/build-tools 34.0.0, and a project-local Python environment from
`requirements-build-lock.txt`. Read the deliberate [identity/signing plan](../product/RELEASE-IDENTITY.md).

```sh
python -m setup_app.build_release --key-dir <private-directory-outside-repository> --init-keys
python -m setup_app.build_release --key-dir <same-private-directory>
```

Use `--init-keys` only for the first build; it explicitly creates persistent release/catalog keys.
Do not recreate them for updates. The script runs Android tests/lint/build, SDK signature verification
and signs the bounded offline catalog. Keys and binaries must stay out of source control. Public
`trust.json` is committed; replacing its trust key is a deliberate release change.

Put the environment's Python on PATH, then run `setup_app/build-windows.ps1`. PyInstaller's
[one-file/windowed build](https://pyinstaller.org/en/stable/usage.html) bundles the verified APK/catalog,
UI/font/runtime and distribution licences. Only explicit verified release artifacts are bundled, never keys. Optional native phone APK/catalogue and companion packages are built separately.
Build on Windows. The Android APK is release-signed; the **Windows EXE is unsigned** and is a private
alpha, not a public release. Do not advise bypassing OS security warnings.

## Validation

```sh
python -m unittest discover -s setup_app/tests -v
python -m unittest discover -s product/tests -v
python -m setup_app.main --self-check
```

The frozen EXE also supports `--self-check`. Test backup, preview, restore and real remote/playback
behavior on a spare supported device before distributing a binary.
Connection guidance follows [Android's ADB documentation](https://developer.android.com/tools/adb).


## LG and Samsung choices

Choose **LG webOS TV** or **Samsung Tizen TV** for the native five-step wizard. It checks official tools,
walks through Developer Mode, pairs/checks the exact TV, builds/reviews its package, verifies a separately
confirmed first installation and offers explicit launch. Existing Home stays unchanged. LG can download
the pinned official CLI after consent; Samsung signing uses the owner's existing SDK/profile.
[Start here for tomorrow's LG test](NATIVE-SETUP.md). Neither native path sends an Android APK or enables
a Home helper, and neither has real hardware acceptance yet. A Streamer on either brand uses Android.

## Setup health and optional Extras

The help area includes **Setup health & support** and **Explore optional extras**. On narrow windows,
use **Help & extras** in the top bar. Health checks distinguish a trusted offline package from an
actual TV connection; preview mode never counts as device acceptance. The downloadable report uses
an explicit field allowlist and excludes addresses, device names, file paths, cities, app inventories,
keys and raw exception text. Private recovery records remain separate on this computer.

Extras is a small project directory, not an install API. It describes companion access and limits,
links to source guides and distinguishes separately built DV companions from external apps. Native
LG/Samsung filters explain that Android companions cannot run there. Nothing downloads, installs,
pairs, grants permission or changes Home when this view opens.

## Use your phone

Open **Use your phone** after connecting to a TV or choosing the example journey. Pick this computer's
home-network interface and explicitly start temporary sharing. App names/IDs are a separate unchecked
choice. Scan the address-only QR code, enter the six-digit code on the phone, then send an appearance
draft or media. Only one phone can pair; the code expires in five minutes and sharing in 15 minutes.
Use **Stop sharing** to revoke access immediately. Closing the dialog alone does not stop sharing.

This is HTTP on a trusted home network. Do not port-forward it or use it on public/shared networks. No
firewall rule or discovery scan is added. If the phone cannot connect, stop sharing and use the computer
journey; this alpha does not automatically change firewall/network policy. Ordinary Setup stays loopback-only.

Receiving a draft changes nothing on the TV. **Review received draft** stops sharing and opens the
computer's choices for normal review. JPEG/PNG files up to 8 MB and MP4 up to 48 MB stay in a private
computer inbox; remove them there when no longer needed. Stop sharing before **Review on TV**. This
verifies the installed standalone package and records a private native baseline, then transfers one
checksummed file to app-private TV cache. The TV separately checks image bounds and video compatibility
(up to 4K/60 seconds) and offers **Add to wallpaper library**. Import does not select it. Profiles,
wallpaper and layout are not replaced by receipt, review or import. Finish or discard one received TV
file before sending another; the review receipt expires after 30 minutes. Existing owner pilots use
compatible development tooling; this GUI does not silently migrate their package identity.

An Android update can clear the system's preferred Home. Setup rechecks the recorded choice before
installation. If Android leaves only its unselected chooser afterward, Setup restores only the exact
recorded, still-registered Home component. It never overwrites a different newly selected Home or
chooses DV Freedom by default for a fresh installation. An unexpected change stops for review.


## Add-on Center

Open **Add-on Center** in the sidebar (or Help & extras on a narrow screen). Connect to an Android TV,
then choose **Review installation** for one companion. Review its purpose, declared access, version,
size and private recovery path before choosing Install/Update. The offline signed bundle must verify;
missing, changed, differently signed or newer installed packages block the operation. No uninstall,
downgrade, Home takeover or optional grant fallback is attempted. **Open on TV to configure** is separate.
Weather, Sleep Timer, World Clock and Quiet Hours retain their source guides and attribution. Home &
Calendar and Phone Remote are independent DVOpenLabs companions; [their guide](../product/ANDROID-COMPANION.md#optional-panels-and-tv-phone-remote-alpha-17)
explains privacy, network and Android-version limits. Android companions cannot be installed on native
LG/Samsung/VIDAA/Vizio/Roku. Their platform paths in the same GUI remain separate.

Maintainers build the optional signed bundle with `python -m setup_app.build_addons --key-dir <existing-private-key-directory>`
after preparing JDK 17 / SDK 34. This uses the existing release/catalog keys and does not create new keys.
The Windows build includes only these reviewed artifacts, their signed catalogue and retained licenses.
APK/recovery data is ignored by Git; do not commit binaries, private connection files or signing material.

## Optional features

The signed alpha 20 launcher includes folders, Simple/All settings, Control Center, Wallpaper Studio
and offline setup help. Add-on Center’s **DV Freedom Companions** entry also explains Discover,
Immich/WebDAV photos and explicitly reviewed Gemini drafts. Bring your own credentials in the private
TV forms; installing or opening the panel never enables a connection. Phone Remote’s optional setup/
upload session additionally needs launcher-profile sharing. Both start off. Existing paired command
access and Android Accessibility do not implicitly grant setup rights.

The Add-on Center also offers **TV Inspector** (no permissions), **Bluetooth Connect** (optional Nearby
devices access), **Digital Rain** (no permissions) and **News/RSS** (Internet). News starts without any
feed selected. Bluetooth connects/disconnects only after an explicit action and only where supported
by firmware. Ambient installation does not select a system screensaver or replace your wallpaper.
The launcher can open installed tools from **System → Optional TV tools**.

Source publication is separate from public binary distribution. The locally rebuilt Windows alpha
continues to be unsigned; no new release is published automatically. See the [alpha limitations](../README.md#alpha-limitations).

## Native Android phone alternative

**Use a phone → Native Android phone Setup → Check native phone app** verifies the fixed signed phone
APK if included. Download and transfer it to your Android 10+ phone, review Android’s installation
prompt and follow its pairing-code wizard. This does not start a LAN server or install anything
automatically. TVs without pairing-code Wireless debugging, and native LG/Samsung, use desktop Setup.
The phone app is separately GPL-3.0-or-later; the TV/desktop remain MIT. Read its
[build, recovery and licence guide](../mobile/README.md).

After building the signed current launcher bundle, developers can build optional artifacts with the
same existing external keys:

```sh
python -m setup_app.build_addons --key-dir <same-private-directory>
python -m setup_app.build_phone --key-dir <same-private-directory>
```

The Add-on Centre contains 48 curated package/source/guide entries and 19 supported signed package
definitions. Only verified artifacts included in the bundle can be installed. New runtime/special
access and configured network sources remain separate choices. See [Add-ons](../product/ADDONS.md).
