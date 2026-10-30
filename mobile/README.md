# DV Freedom Setup for Android phones

A separate native setup app for Android 10+ phones and Android TVs with **pairing-code Wireless
debugging**. The phone and TV must be on the same trusted private network. Legacy USB/TCP-only ADB
devices and native LG/Samsung use desktop Setup. This source build is an alpha; hardware acceptance
is separate from JVM/build checks. It does not install automatically on your phone or TV. This first phone edition installs/previews the
launcher only; optional package installation stays in desktop Setup’s Add-on Centre.

## Guided setup

1. On the TV, enable Developer options and Wireless debugging. Open its pairing-code dialog.
2. Enter the TV address, **connection port**, **pairing port** and current six-digit code. Review the
   debugging authorization, then Pair & connect. This phone keeps its own encrypted ADB identity.
3. Check the connected TV and peer fingerprint. Confirm the TV before any write. A changed saved
   certificate is refused; pairing again with a fresh code on the TV allows a new identity review.
4. Choose up to eight installed apps, their order, Home style, row position, icon size and spacing.
   Review the signed release, permissions and recovery limits. Installation needs both an unchecked
   consent and confirmation using the phone's secure device lock.
5. Use the actual TV remote to Keep or Revert the separate profile preview within 45 seconds.
   Timeout restores the previous profile. The original default Home remains selected. Making the
   launcher daily Home, or enabling the Streamer redirect, are separate on-TV choices.

Reopen, reconnect and choose **Check saved operation** after an interruption. Checking does not
reinstall or replay a preview. A verified installation without a preview can offer a separately
approved preview. An unchanged original installation can be freshly reviewed. Unexpected APK or
device identities stop for manual recovery.

The recovery point includes the original Home, active launcher profile/preferences, app order,
media references and an existing APK. It is not an account backup, full private-data backup, copy
of media or automatic code downgrade. Restoration replaces launcher preferences only, under a new
phone-lock confirmation. Check real remote buttons and streaming playback yourself.

## Privacy and transport

- Paired TLS only. Both application network policy and ADB protocol reject cleartext setup.
  No general-purpose shell, debloating, uninstall, accessibility grant or background discovery UI.
- Per-TV RSA identities are software keys encrypted at rest by an Android Keystore AES key. Hardware
  backing depends on the phone. This is not a claim that the RSA key never enters process memory.
- The first ADB connection identity is reviewed on the phone after pairing; subsequent connections
  pin that certificate. This ADB trust mechanism is separate from normal HTTPS system trust.
- APK bytes are authenticated by the same signed offline release contract as desktop Setup. Wrong
  signers, downgrades, stale TV/Home/profile state and changed release bytes are refused.
- No telemetry, AI provider, external catalogue or paid calls. Nothing connects on launch. Private
  identities and recovery records are excluded from Android backup. The shareable support report is
  a small allowlist; users review it and choose the recipient themselves.

## Build

Use JDK 17 and Android SDK/build-tools 34. First build the signed desktop launcher bundle using its
existing release keys; do not create replacement keys for an update. The phone build copies the
verified bundle into generated assets; no APK or key is committed.

```sh
./launcher/gradlew -p mobile --no-daemon :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease
```

Use `launcher/gradlew.bat` on Windows. The local release variant uses the developer debug signature
until signed by the distribution build. Do not confuse that phone-app signature with the verified
TV APK's release signer. Generated phone APKs are in `mobile/app/build/outputs/apk/release`.
No binary release or phone/device certification is implied by source publication.

## Licensing and corresponding source

This independent **phone app** is GPL-3.0-or-later; see [LICENSE](LICENSE). The MIT TV launcher,
desktop Setup and platform adapters retain their own licences. Phone-app code is not linked into
the TV launcher or desktop installer.

The `adb` library retains upstream SPDX notices and all licence texts. It derives from
[LibADB Android 3.1.1](https://github.com/MuntashirAkon/libadb-android/tree/c849886ebc6d48e7b46d967e78a6bb65c90c3b74)
(GPL-3.0-or-later OR Apache-2.0, with BSD-3-Clause/MIT parts). Changes remove shared SSL-context
caching across identities, require a supplied peer verifier, reject non-TLS ADB, bound socket
connection/pairing timeouts, and suppress peer/debug traces. Its sample app and upstream build
scripts are excluded.

Dependencies are SPAKE2-Java Android 2.2.1 (LGPL-3.0, including CC0-derived Ed25519 code), Conscrypt
2.5.3 (Apache-2.0 and bundled BoringSSL/Netty/Harmony notices), Bouncy Castle 1.81 (MIT), AndroidX annotation 1.9.1, Kotlin
stdlib 1.9.24/annotations 13.0 and Outfit (OFL). SPAKE2's licence is retained under `adb/licenses`.
The bundled Conscrypt artifact identifies BoringSSL revision `49f0329110a1d93a5febc2bceceedc655d995420`; its licence is retained too.
Dependency sources are available from their official repositories/source artifacts at these versions;
the declared Gradle dependencies and this source provide the build/relink instructions. A public
binary distribution must also provide the corresponding dependency source and required notices;
do not ship just an APK and treat a README as a substitute for that source obligation.
