# Developer guide

DV Freedom is an independent CleanYourTV derivative. Start with the [project README](../README.md)
for current features, screenshots, platform limits and source setup.

| Guide | Purpose |
| --- | --- |
| [Android feature build](ANDROID-COMMUNITY.md) | Settings, Home experiences, rows, wallpaper playlists, profiles and shelves |
| [Add-on Centre](ADDONS.md) · [Native phone Setup](../mobile/README.md) | Optional packages, requirements and separate phone installation |
| [Profiles](ANDROID-PROFILES.md) | Local format, import/export and account boundaries |
| [Companions](ANDROID-COMPANION.md) · [AI drafts](AI-SETUP.md) | Explicit optional providers/permissions and local draft validation |
| [Streamer helper](STREAMER-HOME.md) | Optional Home-only redirect and recovery boundaries |
| [Setup GUI](../setup_app/README.md) · [all platforms](../setup_app/ALL-PLATFORMS.md) | Guided connection, review and installation limits |
| [Release identity](RELEASE-IDENTITY.md) | Package, signature, signed-bundle and recovery contracts |
| [Source notices](THIRD-PARTY.md) · [source register](sources.json) | Integrated source and dependency licences |
| [LG](../platforms/webos/README.md) · [Samsung](../platforms/tizen/README.md) | Platform-specific setup and runtime limits |

## Shared-profile exporter

Profiles describe launcher preferences, not streaming accounts. The public example uses placeholder
app IDs; resolve real IDs from an authorized inventory of the selected device.

```sh
python tools/product_profiles.py product/profiles.example.json --profile adult --platform android --output product/build/android.json
python tools/product_profiles.py product/profiles.example.json --profile child --platform webos --output product/build/webos.json
```

Use `--base <existing-config.json>` to preserve unrelated fields. This tool writes local JSON only;
it does not connect, install, change Home or apply a theme. It refuses existing output paths. Private
configuration belongs outside source control or in ignored `product/private/`.

See [CONTRIBUTING](../CONTRIBUTING.md) for checks and publication/privacy rules. Working notes and
device-specific test evidence are local and are not part of this documentation directory.
