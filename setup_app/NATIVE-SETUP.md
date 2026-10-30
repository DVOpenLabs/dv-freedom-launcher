# Native LG and Samsung guided setup — first-install pilot

The Windows GUI now has separate **LG webOS** and **Samsung Tizen** paths. It pairs/checks the selected
TV, packages this repository's native app, reviews the exact target/package, installs once after your
confirmation, verifies the app ID in the TV's package list and offers a separate launch button.
It does not select a new Home, install a helper, restart a TV or copy Android settings into a native TV.

**Development pilot: no real LG or Samsung hardware acceptance yet.** Vendor CLI packaging, browser
journeys and simulated failures have been tested. A supported-looking model is not a compatibility
promise. The native apps have fewer features than the Android edition. Keep the original Home and remote.

## LG: your first test

1. Open the updated **DV Freedom Setup** Windows app and choose **LG webOS TV**. If your apps run on
   a Google TV Streamer plugged into the LG, choose Streamer for that device instead. The native LG
   path installs an app on webOS itself; it does not touch the attached Streamer.
2. Choose **Check tools**. If needed, install Node.js LTS using the [official installer](https://nodejs.org/en/download),
   reopen Setup, tick the tool-download consent and choose **Prepare LG tools**. Setup prepares
   `@webos-tools/cli@3.2.6` and its dependencies locally from npm, with install scripts disabled. It does
   not change global CLI installations. Existing official CLI/Node paths can be selected under the
   expanded tool-path section. Downloads need internet; no CLI/Node executable is bundled.
3. On the LG, install **Developer Mode** from LG Apps. Sign in there with your own LG developer
   account. Enable Dev Mode Status and complete any LG-requested restart yourself. Reopen that app,
   turn **Key Server** on and leave it open. Setup does not collect your account/password or accept
   terms for you. See [LG's official steps](https://webostv.developer.lge.com/develop/getting-started/developer-mode-app).
4. In Setup, enter the full model code, installed webOS/firmware version, TV IP address and current
   six-character case-sensitive passphrase. Confirm the TV steps and local pairing/key storage.
   **Pair & check TV** uses port 9922 and user `prisoner`, in a separate local LG CLI home. It does not
   alter your other CLI device registrations. The code is passed through stdin, cleared from the form
   and omitted from setup records and command arguments. LG's own tool stores the pairing material
   privately so you can reconnect; keep the entire setup-data folder private.
5. Review the device report. Optionally select up to eight **starter apps**, in the desired order,
   from the exact IDs the developer list exposes. Some firmware may expose only development apps;
   the GUI does not guess missing retail IDs. You can start empty and add verified LG IDs on the TV.
6. Choose **Prepare app package**. This builds a local IPK from the included source. Check its target,
   size/hash and optional starter row. Tick the separate installation confirmation, then choose
   **Install reviewed app**. The GUI rechecks the TV, tools and installed apps before sending it.
7. After verification, choose **Open DV Freedom on the TV**. If you chose starter apps, the first
   launch offers **Use these apps**; Back/Start without these apps saves nothing. Existing Home data
   is never overwritten by the starter prompt. Native launch checks each app again through LG's API.
8. Test the real arrows/OK/Back, Magic Remote, Home, Input, settings, held volume, app video playback
   and profile persistence. Press Home for LG Home. These checks remain unchecked until you do them;
   an SDK launch reply cannot establish visibility or protected playback.

**Keep the LG Developer Mode session active.** Use EXTEND in the TV app before it expires. Expiration
or disabling Developer Mode can remove development apps. Setup does not enable automatic renewal,
change OLED care/idle settings, clear caches, install a Home helper or bypass firmware restrictions.
This is the documented [LG development workflow](https://webostv.developer.lge.com/develop/tools/cli-dev-guide),
not LG Store distribution or a permanent certified Home replacement.

## Samsung: certificate and guided installation

1. Choose **Samsung Tizen TV**. Install Tizen Studio with **Web CLI**, **TV Extension** and **Samsung
   Certificate Extension** using [Samsung's official SDK steps](https://developer.samsung.com/smarttv/develop/getting-started/setting-up-sdk/installing-tv-sdk.html).
   Select its local folder if Check tools cannot find it. The SDK is not downloaded/bundled by Setup.
2. Follow the [TV Developer Mode instructions](https://developer.samsung.com/smarttv/develop/getting-started/using-sdk/tv-device.html).
   Set the permitted computer address correctly; complete a vendor-required restart yourself. Create
   your own Samsung certificate profile for this TV, including its DUID where required, and choose
   **Permit to install applications** in Device Manager where required. Keep keys/accounts private.
3. Enter the exact model, software and local TV address; approve the local developer connection.
   Setup connects only to that address on port 26101. It checks the exact authorized SDB target,
   reported Tizen version (6.0+ declared minimum), capability identity and installed packages.
4. Enter the existing certificate **profile name**, as shown in Certificate Manager. Setup runs the
   SDK's `tizen package -t wgt -s PROFILE` against its own staged source. It does not read certificate
   passwords/keys or change global SDK profiles. A package must have the expected identity, privileges,
   background-off manifest and both signature files. Presence is not a cryptographic certificate
   attestation; the selected SDK signs it and the TV enforces certificate/DUID validity.
5. Review the exact TV/profile/package and confirm installation. Installation and later launch specify
   that one SDB target explicitly. Setup verifies `DVFreedom1` in its package list before offering
   **Open DV Freedom**. Test physically afterward; Samsung Home stays selected.

The fixed read-only `pkgcmd -l` query is not available on every retail Samsung firmware. An unfamiliar
or denied response blocks automatic installation before sending the package. No root/private-command
fallback is attempted. Use the [manual Samsung guide](../platforms/tizen/README.md) for a separate
model-specific SDK evaluation; browser/mock checks do not certify the user's TV. Command syntax follows
[Samsung's CLI guide](https://developer.samsung.com/smarttv/develop/getting-started/using-sdk/command-line-interface.html?device=htv).

## Interrupted setup, existing installs and private data

This version is deliberately **first-install only**. If the app is already installed, it stops before
packaging/overwriting it. Native profile backup/update/migration remains separate work. Open your
existing app manually; no uninstall/downgrade/reinstall fallback or automatic defaults reset is offered.

The review is single-use, expires after 15 minutes and is bound to the checked TV, tool files, app
inventory and package hash. A private record is saved before the package commit. A lost result is
marked **needs check**. Reopen Setup, choose the same platform and use **Check saved installation**;
it queries the same TV and never repeats the install. If absent, reconnect and prepare a fresh review.
The GUI blocks closing its server or starting Android/another native operation during a vendor job.
Closing the browser tab leaves that job/server running. Keep the computer/TV on during installation.

Local data is under `DVFreedomSetup/native` in your user's application-data folder: tool locations,
isolated LG developer connection/key, packages and installation records. Selected starter IDs are
included in your private IPK. Records are not full backups and do not contain the entered pairing code.
Do not upload the directory publicly: the vendor connection material and TV metadata are private.
Samsung uses its existing SDK certificate configuration outside this app; no universal signing key ships.
The installer has no arbitrary command/file-upload route, root/uninstall/reboot control or Home selector
on the native paths. OS security warnings and vendor terms are never bypassed/accepted for the user.
