# Credits and third-party notices

DV Freedom Launcher is derived from Gathin's CleanYourTV and its contributors. The original MIT
notice remains in [LICENSE](LICENSE), with source history preserved. DVOpenLabs additions use the
same MIT terms.

Additional adaptations and reviewed source revisions are recorded in
[product/THIRD-PARTY.md](product/THIRD-PARTY.md) and [product/sources.json](product/sources.json).
Retain their linked licence files when redistributing source or builds, including
[Android integration notices](product/licenses/Android-integrations-MIT.txt),
[webOS upstream notices](platforms/webos/LICENSE.upstream) and the
[Outfit SIL Open Font License](launcher/FONT-LICENSE-OFL.txt).

Projectivy and other research-only candidates are references, not bundled proprietary source.
Device-specific fan artwork and personal videos are not included or licensed by this project.

## Desktop setup app

The desktop setup uses adb-shell, rsa and cryptography with their transitive runtime dependencies;
these are separate from the Android APK. The Windows build collects their distribution metadata and
licence files, the Python runtime licence, PyInstaller notices and the Outfit OFL notice. Source
requirements and the exact dated build environment are in `setup_app/`. The starter scene in
`setup_app/web/scene.svg` is original DVOpenLabs SVG artwork, under this repository's MIT terms.
No platform-tools executable is bundled. Optional pairing uses an existing local `adb`, or an explicit
user-consented download from Google's official HTTPS repository after showing Google's actual licence.
The app verifies the archive against Google's HTTPS-delivered metadata checksum (currently SHA-1),
records extracted-file SHA-256 hashes and extracts only allowlisted Windows files. It does not accept
terms on the user's behalf. The alpha's download checks use mock archives; no real tool download or
licence acceptance was performed during this development pass.
