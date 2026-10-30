# One setup app, distinct platform capabilities

The welcome screen, help, comparisons and review vocabulary are shared. Each system has its own
connection/packaging implementation. Selecting a brand is not permission to change Home, firmware,
network, accounts, Developer Mode or picture/sound settings. No TV was contacted in this development batch.

| Device running the apps | Setup path | Current boundary |
| --- | --- | --- |
| Android TV / Google TV / Streamer | Signed offline APK, recovery and reviewed installation | Existing native alpha; physical acceptance is device/build specific |
| LG webOS | Developer Mode, official CLI, reviewed IPK installation | Manual-launch pilot; automatic Home helper is separate work |
| Samsung Tizen | Official SDK, owner certificate, reviewed signed WGT installation | Manual-launch pilot; no bundled root service, return helper or SmartThings rules |
| Hisense VIDAA | Existing authorized MQTT profile, app catalog, reviewed local hosted page | Experimental; private credentials/certificates and a compatible TV debug installer required; no fresh authentication implementation |
| Vizio SmartCast | Trusted TV certificate, PIN pairing, captured app IDs, normal Conjure route | Experimental; firmware may refuse Conjure; no privileged webview, root, system mutation or automatic Home return |
| Roku | Generate and download DV Freedom Ambient ZIP, then official manual sideload | Clock/background app only; not a launcher, ECP controller or Home replacement |
| Other systems | Manual privacy guide | No installer claim |

Use the device running the apps: a Google TV Streamer attached to an LG panel follows the Android
path, while apps running on the LG itself use webOS. LG/Samsung details are in [native setup](NATIVE-SETUP.md).

## VIDAA and Vizio hosted Home

1. Choose the exact platform and read **What to expect**. The current computer must remain awake
   while serving Home. This is not a Windows service; no firewall rule is created. A NAS/headless
   source installation needs its own local browser/secure access to Setup's loopback GUI.
2. Supply the TV's literal private IPv4 address and the trusted connection material described in
   [the hosted adapter guide](../platforms/hosted/README.md). Setup never downloads vendor secrets,
   extracts a phone app, disables TLS validation or silently trusts a changed certificate.
3. VIDAA reads apps from an existing authenticated profile. Vizio requests its own TV pairing PIN,
   retaining the token only in memory. Incorrect/expired PIN attempts require a fresh challenge;
   use **Start a new pairing challenge**. Its per-attempt identity does not reuse another integration's ID.
4. For Vizio, open each regular app on the TV, name it and choose **Capture current TV app**. Setup
   reads the actual app ID/namespace. It does not guess a global installed-app catalog. System and
   privileged surfaces are excluded. For VIDAA, choose from the returned valid catalog.
5. Select one to eight apps in order; choose style, row position, icon size and spacing. Enter this
   computer's LAN address and an unused port. Optionally choose a folder of your own stills/videos.
   Library video needs a matching JPG/PNG fallback and is always muted. The existing eight-scene,
   256 MiB media bound applies; no personal media is bundled in public source.
6. Review the exact TV, server, apps and settings. **Start reviewed hosted Home** opens the listener
   on that one local address. Only the selected TV can use the page. Launch actions need its random
   session capability and exact captured app IDs. The capability is in the private URL fragment,
   never a query string, credential log, setup record or persistent browser storage.
7. On compatible VIDAA firmware, use `hisense://debug` in the TV browser to add a web tile using the
   exact link, including its fragment. This is a user-operated vendor installer, not a protocol
   sideload performed by Setup. Stop if the installer is absent or declines the tile.
   On Vizio, separately confirm **Open hosted Home on Vizio**; this invokes only normal Conjure app
   `17`, namespace `4`, with the reviewed page. There is no app-3001/internal API fallback.
8. Check the real display, remote, original Home, app video playback, media lifecycle, input/volume
   and standby/wake. The on-screen checklist records your session observations only. A launch
   acknowledgement does not prove foreground display, decoding or compatibility.

**Stop hosted Home** closes the listener and revokes its link. Disconnect releases the TV connection
and forgets the in-memory pairing token. No TV tile is deleted, and no original Home is removed.
The GUI refuses to quit or start a different TV operation while a hosted Home is running; stop it
first. Reloading the browser recovers Stop controls without repeating a launch or pairing request.
Use the TV's original Home if the companion becomes unavailable. No automatic relaunch watcher is included.

The local HTTP page is restricted to the selected TV IP, with Host/Origin checks, a bounded JSON API,
no CORS grant, no arbitrary file/shell/URL route and an exact app-launch allowlist. The URL capability
is sensitive: do not share it. HTTP is not encryption; use a trusted private LAN. This is not a public
website, router-port-forwarding service, multi-user authorization system or internet deployment.

## Roku ambient app

Choose a background and 12/24-hour clock, acknowledge its limited scope, then build/download the ZIP.
Setup includes the independent BrightScript/SceneGraph source and notices. No TV is contacted.

Follow [Roku's developer instructions](https://developer.roku.com/dev/docs/developer-setup) yourself.
Developer Mode terms, credentials and any restart stay in Roku's own flow. **Only one development app
can be sideloaded at a time**; manually uploading this ZIP replaces that slot. Preserve an existing
development app first. Setup never logs into the installer, uploads, uninstalls or overwrites it.

The package displays the local clock and three quiet backgrounds. OK cycles colors; Back/Home leave
the app. The clock moves slightly each minute; TV screen protection is not disabled. There are no
streaming-app launches, HDMI controls, network requests, automatic startup or Home interception.
Roku [discontinues in-app ECP commands](https://developer.roku.com/dev/docs/external-control-api);
we do not add a relay to disguise them. Source compilation is not Roku certification or hardware acceptance.

## Validation and follow-up

The local suite covers fake bridge operations, actual loopback Host/Origin/token/route checks, unknown
apps, explicit launch consent, expired/single-use reviews, source staging, package hashes, media byte
ranges and shutdown. The generated Roku source is checked with pinned BrighterScript 0.73.8 without
deployment. Android code/package identity is unchanged by this batch. Native LG and Samsung retain
their source/packaging tests. Each platform guide states its hardware and runtime limitations.

Real VIDAA/Vizio pairing, native firmware capabilities, streaming playback, browser codecs, prolonged
standby and Roku display behavior are pending. All-platform support does not imply Android feature
parity: weather, streaming content shelves, portable profile mappings and automatic Home return need
separate platform-specific work. The existing shared-profile v1 schema is unchanged.
