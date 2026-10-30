# DV Freedom Camera — optional alpha

A separate Android TV app for a camera you own. Save an HTTPS HLS/MP4 address or a literal private-LAN
RTSP address and limited viewer credentials, then explicitly start an in-app preview. Saving/opening
makes no connection. RTSP-over-TCP is unencrypted on your trusted LAN; do not port-forward it. HTTPS
validates certificates, rejects redirects and refuses foreign-origin HLS segments before sending
credentials. Self-signed HTTPS and cross-host playlists need server-side configuration; no certificate
bypass is included. There is no camera discovery, recording, audio, microphone or doorbell monitoring.

For a corner window, separately review Android draw-over-other-apps and (Android 13+) notification
posting access, then select **Show silent corner window**. It is non-touchable/non-focusable, stops on
screen-off, Stop notification/app action, task removal or its selected 1/5/15-minute timeout. It never
starts at boot or uses Accessibility. A foreground media service exists only for that explicit window.
In-app preview stops on leaving and has the same timeout. Forget stops the window and clears this
app’s source/credentials. Other apps, Home, wallpapers and system settings are not changed.

| Permission | Purpose / default |
| --- | --- |
| Internet | Only the explicitly started configured stream; off on open |
| Draw over other apps | Optional corner window; separate system choice |
| Foreground service + media playback | Keep an explicitly started video window alive until Stop/timeout |
| Post notifications | Own status/Stop control; does not read notifications; separate system choice |

The camera window may be unavailable over HDMI/DRM apps, on restricted firmware or when a second
video decoder is unavailable. Stream compatibility, authentic credentials, Stop/screen-off and
resource behaviour need physical acceptance with your server. Local policy tests/builds are not
camera, HDMI or streaming-playback certification. No camera/access is configured by installation.

Build with JDK17/SDK34:

```sh
./launcher/gradlew -p launcher --no-daemon :camera:testReleaseUnitTest :camera:lintRelease :camera:assembleRelease
```

Use desktop Setup’s signed Add-on Centre to review a packaged artifact. This code is independently
implemented MIT DV code using Media3 1.4.1 (Apache-2.0), OkHttp and their retained dependency notices.
No Corner Player implementation or camera imagery is copied. See root source/licence registers.
For Frigate doorbell/event integration use the separate guided project entry; this app is a personal
stream viewer, not that event-monitoring service.
