# Standalone identity and signing

This guide documents package identity, signing and installation/recovery contracts.

## New installations

The full community product defaults to `com.dvopenlabs.freedomlauncher`, version code 21,
`3.2-freedom-alpha20`, minimum Android 8/API 26. The inherited reference variant keeps its identity,
version and size gate. An existing installation requires the same application ID and compatible signer to preserve its data.

`-PfreedomRelease=true` requires the standalone community identity and externally supplied PKCS12
signing material. The developer release script uses a persistent RSA-3072 key and verifies the APK
with SDK apksigner. The public certificate SHA-256 is:

`bef06b873225c7d8c63f617325db018d6757d2695f48ac26d9dc2f1525d24dad`

An Ed25519-signed offline catalog binds package, version, minimum SDK, file size/hash, APK certificate
and private-alpha channel. `setup_app/trust.json` pins the public verification key. Private keys,
passwords, binaries and recovery files remain outside Git; losing signing keys breaks normal updates.
The Windows executable has no Authenticode signature yet. This is not a signed public distribution.

## Installation and recovery policy

- Review binds the TV fingerprint, active native profile/revision, exact choices and signed artifact.
- Only the fixed standalone package can be installed. A changed signer, downgrade, stale review,
  ambiguous original Home or device mismatch fails closed. No uninstall/reinstall fallback exists.
- Before an update, save active-profile settings, wallpaper references and installed APK locally.
  A fresh install saves its initial native baseline before preview. No account or media-file backup
  is implied. No all-profile backup or automatic APK downgrade is offered.
- Preview creates its own profile and journal atomically. Keep/Revert has a 45-second native timeout,
  independent of the desktop connection; leaving the prompt reverts. Recovery checks actual state.
- Default Home stays selected until the person chooses otherwise on the TV. Accessibility is an
  optional, separately explained manual choice; setup opens settings without selecting a service.
- Restore is guarded by same-device/app/revision and existing media references. Old Home remains
  available. No factory reset, reboot, debloat, system preference write or permission grant is involved.

## Updating existing installations

Check the installed package and signing certificate before updating. A different package creates a
separate app; a different signer cannot update existing private app data. Use explicit export/import
when migrating between identities, and keep the original Home available until the new setup is accepted.
Test preference changes in a separate local profile and retain a private recovery point.

Fresh standalone install/update acceptance on a spare supported device, physical remote and protected
playback, novice usability, Windows signing/distribution and native LG acceptance remain release gates.
Source publication does not imply that a signed production binary is available.


The Add-on Center uses a separate signed `addons/catalog.json` under the same pinned catalogue
key. It allowlists ten exact package/file identities, authenticates APK bytes, signer, version, minimum
Android and declared permissions, and reviews each install separately. Own Home/calendar and phone
panels are independent packages with no HOME or app-launcher intent filters; the existing launcher
opens them through explicit same-signer components. Existing essentials retain their package identities;
a different installed signer blocks an update. Companion installation grants no optional access or
Home routing. Recovery records retain the previous APK/Home binding, not Android private app data;
automatic downgrade/uninstall fallback is not provided. These generated bundles remain ignored by Git.

The existing identities and signers are retained. The optional Companions package is code 3 /
`0.1-alpha3`; Phone Remote is code 4 / `0.1-alpha4`. Weather/Sleep Timer/World Clock/Quiet Hours retain
their earlier package/version contracts. New phone setup is an app-owned, same-signer provider opt-in;
it adds no Internet permission to the launcher or automatic Accessibility/system grant.
