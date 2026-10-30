# Google TV Streamer Home-only redirect

The optional `StockHomeRedirectService` returns Google TV Home to this launcher on firmware where
the system Home role remains with Google. Its component is available in Accessibility settings, but
installing the APK does not select or bind the service. An already selected service remains selected
on update. It is separate from the broader remote-key helpers and never enables them.

The system delivers only Google launcher window-state events. The implementation matches exactly
`com.google.android.apps.tv.launcherx.home.HomeActivity`, ignores Google's Settings and dispatch screens,
and opens this package's own MainActivity. On binding it also opens Home once after user unlock when
the display is interactive, so the same Home can return after reboot. It does not wake a sleeping device.
Same-tick duplicate attempts are coalesced; distinct rapid Home presses are retained. It requests no screen-content retrieval, remote-key filtering,
gestures, notification access, new network permission, periodic polling or boot receiver.

This is an alternative to another Home redirect, not a second watcher to enable alongside it. Deployment
must back up the existing accessibility service list, install and visually verify the new Home and media,
then replace only the previous custom Home redirect entry, preserving all unrelated accessibility services.
Google's launcher, setup wizard, Cast, remote and update components stay enabled. App preferences,
theme/media files, picture/sound/display/power settings and streaming accounts are not reset.

The user must explicitly choose this persistent behavior. Select the service through Accessibility
settings or the authorized ADB setup. Android 14 can refuse shell component-enable/disable commands for
release apps; component-state changes are not part of this deployment. Undo restores the exact saved
accessibility list and its enabled state, unbinding this helper, and reopens the previous Home.
Do not force-stop the new launcher after activation: Android may disable its accessibility binding.
Do not enable the broader InputKeyService/HomeKeyService/notification helper for this mode.

Tests must include Home from streaming apps, repeated Home, Settings staying open, decoder release,
standby/wake and reboot persistence on the real model. Injected keys are smoke tests; physical remote
and actual protected playback still need the owner's acceptance. UI Automator can take over accessibility
and rebind helpers, so capture focus and screenshots without it while testing this mode.

The initial target is the Google TV Streamer (`kirkwood`, Android 14). This is a fork feature, not an
upstream contribution or a general compatibility claim for every Google TV. Eight JVM policy tests
check exact matching and duplicate/rapid-press handling; native service binding and firmware acceptance are separate.
