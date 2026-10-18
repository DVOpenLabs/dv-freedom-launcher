# CHiQ "AI PONT" (DVB93CQ, Changhong, codename samseong)

Android TV 11 (API 30), firmware RTO2.250508.001, 1.3 GB RAM, MStar/MediaTek `m7322`. The vendor
layer is built by Homwee (`com.homwee.*`), automatic content recognition and FAST channels by Anoki
(`tv.anoki.acr.controller`, `com.cltv.fast`). Live TV package: `com.mediatek.wwtv.tvcenter`. Stock
launcher: `com.google.android.tvlauncher`.

`debloat.txt` works with `tools/disable.sh` (`pm disable-user --user 0`) and `tools/restore.sh`.

| Group | Packages |
|---|---|
| Telemetry, ads, recommendations | `tv.anoki.acr.controller`, `com.cltv.fast`, `com.homwee.aipont`, `com.google.android.tvrecommendations`, `android.autoinstalls.config.homwee` |
| Vendor apps | `com.homwee.store`, `com.homwee.help`, `com.homwee.upgrade` (vendor app updater, Play Store still updates apps) |
| Preloaded apps | `com.google.android.feedback`, `com.google.android.play.games`, `com.google.android.youtube.tvmusic`, `com.google.android.videos`, `com.google.android.backdrop` (Ambient screensaver), `com.google.android.syncadapters.calendar` |
| Optional, comment out what you use | `com.google.android.katniss` (Assistant, mic button), `com.google.android.apps.mediashell` (Chromecast), `com.homwee.multiscreenshare`, `com.homwee.mmp.screenshare` (Miracast/AirPlay), `com.homwee.alexacontrol` |
| Stock launcher | `com.google.android.tvlauncher` |

## Keep

- `com.google.android.leanbacklauncher.partnercustomizer`. It looks like launcher promo config, but it
  provides the `tvlauncher.widget` content provider. Without it `com.mediatek.tv.service` crash-loops
  (`SecurityException: Failed to find provider tvlauncher.widget`) after the next reboot, and the Input
  key and other remote hotkeys stop working. Nothing shows up until the TV reboots, so test with a reboot.
- `com.homwee.dmt.upgrade` (firmware updates), `com.homwee.synckey` (remote key service),
  `com.mediatek.*` (Live TV, inputs, remote pairing), `com.homwee.factory`.

## Notes

- The Live TV app reopens the last source. If that was an HDMI port with nothing on it, Live TV shows a
  black screen.
- HDMI and Live TV screenshots come out black because the video layer is protected.
- The HbbTV browser (`teatro_tv_browser`, about 30 MB) is a native vendor process started by
  `app_man`, not a package. HbbTV can be switched off in the Live TV menu under HbbTV settings.
- The Wi-Fi pill shows the network name only when Location is on (Android 11 restriction).
- The power button only suspends the TV. Restart it now and then (long press power, Restart).
