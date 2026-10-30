# Optional apps and integrations

Open **Setup → Add-on Centre** to browse by category, search features or filter Setup packages,
installed apps, missing runtime access and separate-project guides. Native LG/Samsung cannot run
Android packages; their setup paths stay separate.

Browsing does not contact your TV. **Check connected TV** explicitly reads installed versions and
runtime permission states. Special access such as overlays, Accessibility and feature configuration
still needs to be checked on the TV. A status check is not a signature verification or a permission grant.

## Install a Setup package

Choose **Review installation**. Setup checks the selected TV, Android version, installed signing key,
version and stable Home. Review the access, recovery limits and exact package; confirm the unchecked
installation choice before installing. The signed offline catalogue and APK must both verify.
Newer installed versions and different signers are refused, without uninstalling anything.

Installation preserves Home, wallpaper and app order. It does not grant runtime permissions, select
a system screensaver, enable Accessibility or configure network connections. An update saves the
previous APK for manual support, not an automatic downgrade or full private-data backup.

After a fresh status check, **Open on TV** verifies the installed APK matches the bundle before
launching it. **App access on TV** opens Android's application settings; make access choices there.
Check status again after changing access. Detailed recovery records stay on your computer.

## Included package definitions

The build supports Weather, Sleep Timer, World Clock, Quiet Hours, DV Freedom Companions,
Phone Remote, TV Inspector, Bluetooth Connect, Digital Rain, News/RSS, Headphones, Device Capacity
TV Hub, Ad Blocking, Flock, Jewish Calendar, Nepali Calendar Prayer Times and DV Freedom Camera. A package is installable only when the matching signed artifact is included in your Setup
bundle. A source checkout without that artifact shows **Build not included**.

- **Headphones:** reported battery and current audio output; optional disconnect pause/mute guard.
  Nearby devices access is separate and the guard starts off. Fixed-volume/ARC outputs may only pause;
  this is not a guarantee that sound can never reach the room. Firmware must deliver Bluetooth events.
- **Device Capacity:** storage/memory diagnostics. It does not clean RAM or promise faster playback.
- **TV Hub:** your configured M3U/XMLTV, HDHomeRun or Dispatcharr sources. No channels are bundled.
  STB EMU portals, Xtream login and USB tuners are not supported. Check guide and playback with your
  own source; the current interface is English.

## Guides and source projects

Entries labelled **Guide / separate project** or source/build guides are not installers. Their links
open outside Setup; review the exact release, licence, permissions and compatibility before using
them. Source availability does not establish real-TV acceptance. In particular:

- AirPlay Status is discovery/diagnostics, not a receiver. A separate AirPlay receiver cannot play
  FairPlay-protected Netflix/Apple TV+/Disney+ streams and has separate GPL obligations.
- Cast Fix and OEM Home bridges change debugging or Home components. They are specialist recovery
  references, not recommended installation steps for a working DV Home.
- Kids Mode is not tamper-proof parental control; QuickVol can enable a key service on open in its
  reference build. Read those guides before opening or granting access.
- External offline speech recognition is not a universal voice-button or AI assistant integration.

Connections, server credentials, subscription links and private media belong only in the selected
app's configuration. Never include them in public issue reports or screenshots.

## Regional and local extras

Flock is offline ambient; Jewish Calendar and Nepali Calendar are offline date tools. Select their
screensavers yourself where Android supports it. They do not change your Home header or system clock.
Prayer Times starts unconfigured: choose city/country and calculation method before a foreground
Aladhan request. It uses that city’s reported timezone and has no GPS, alarms, overlays or boot service.
Confirm religious calculation choices locally. Ad Blocking connects to your existing Pi-hole/AdGuard
Home server; it is not a YouTube ad remover, VPN or system-wide blocker installed by DV Freedom.

## Camera / Frigate and AirPlay

**DV Freedom Camera** is an optional signed silent stream viewer with in-app preview and a separately
authorized corner window. It starts unconfigured, accepts HTTPS HLS/MP4 or private-LAN RTSP, has a
Stop control and 1/5/15-minute timeout, and never starts at boot. It does not monitor doorbell events.
Read its [access, server and decoder limits](../launcher/camera/README.md) before enabling a window.

For **Frigate Glance**, first confirm your Frigate server is reachable on your private network and
create a limited dedicated account. Read the linked project’s supported version and camera/doorbell
configuration. Build/review its APK separately, then open its configuration before enabling any
monitoring. Verify in-app viewing first. Overlay, foreground notifications and any optional
Accessibility grant are separate decisions; never expose the camera service through router port
forwarding. Test Stop, leaving the app, screen-off and reconnect behaviour on your firmware. Use its
own Disconnect/Stop controls to end monitoring. DV Setup links this project; it does not install it,
copy credentials, enable its services or certify its decoder/doorbell support.

For the **AirPlay receiver**, use the linked project’s source/release and GPL-3 source requirements.
Review its installer commands before use; do not blindly run a remote shell script or grant battery/
background exemptions. Start on the same trusted LAN, open the receiver explicitly, and test ordinary
photo/audio mirroring from your own Apple device. Check Stop and discovery after closing. Keep Apple
clients on a network you trust; no public router exposure. FairPlay-protected streaming is unsupported,
and native LG/Samsung AirPlay is a different vendor feature. AirPlay Status only diagnoses discovery.
Existing Chromecast and HDMI routes remain available; no casting fix or OEM shim is enabled by Setup.
