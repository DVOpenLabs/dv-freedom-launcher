# Contributing to DV Freedom Launcher

This repository develops the independent launcher product. Bug fixes, coherent UI improvements,
Android features and separately tested platform adapters belong here. Start with the
[product overview](product/README.md) and the guide for the affected feature or platform.

Before adding a feature, explain the user problem, interaction, resource cost and platform limits.
Keep settings consistent: clear focus, readable text, understandable current values, and explicit
Apply/Cancel or timed Keep/Revert for previews. Preserve existing user choices during upgrades.

## Checks

For the full Android product, in `launcher/`:

```sh
./gradlew --no-daemon -PcommunityFeatures=true testReleaseUnitTest lintRelease assembleRelease
```

Use `gradlew.bat` on Windows. Shared Android changes also need the same command without the
feature property to validate the reference variant and its unchanged size gate. Run `clean` when switching source sets to discard stale incremental compiler outputs. Record the full
product's size separately. Use JDK 17 and Android SDK/build-tools 34. Keep the reference APK below
589,824 bytes; product features are measured separately.

For desktop product changes:

```sh
python -m unittest discover -s product/tests -v
```

For desktop setup changes also run `python -m unittest discover -s setup_app/tests -v`,
check the real GUI and follow [the setup guide](setup_app/README.md) for the frozen Windows smoke test.
Keep simulation visibly separate from real operations. Installer changes must preserve the reviewed
[identity, release, backup and recovery contracts](product/RELEASE-IDENTITY.md); no arbitrary shell API.

Run the platform-specific checks described by [the webOS guide](platforms/webos/LAUNCHER.md) for LG work.
Run Samsung checks in [the Tizen pilot guide](platforms/tizen/README.md) for Samsung work.
Native UI changes need real-device captures and D-pad checks. State separately what was tested with
injected keys and what the owner checked with the physical remote. Never imply app launch proves playback.

## Pull requests

Describe the concrete issue, resulting behavior, tests and remaining limits. Do not include APKs,
keys, personal media or device data. Public screenshots require review for location/account details.
Retain licences and update the source register when adapting another project's source.

Include user-facing documentation in the same feature batch: review the root README and affected
guides for current behavior, access paths, versions, prerequisites and known limitations. Keep detailed
validation records, plans, research comparisons and personal device results locally. Publish source,
tests, reproducible build instructions, required attribution and useful product documentation.
Review the complete staged diff and tracked-file list for private or generated material. When publication is authorized, verify that
the intended source and documentation commit is on GitHub before reporting it published. Source
publication and a downloadable binary release are separate deliverables.

No automatic debloating, installer takeover or system-setting changes belong in a UI improvement.
Inherited safety checks remain intact. Development here does not authorize an upstream PR or changes
to CleanYourTV.

The inherited model-list catalogue and press/table generators are retired. Generic private-list
safety/recovery, theme validators and their regression fixtures remain. Run
`python product/check_upstream.py` for the retained cross-platform implementation checks. Retired
catalogue and former root-README assertions have been removed. POSIX-only script tests require a
POSIX host; this runner is not the entire inherited test collection.
The reference build/size CI job remains in source, with Actions disabled; catalogue CI jobs were removed.

Before publishing, run `python tools/review_changes.py` on the staged diff. It reports deterministic
permission, privileged-command, workflow and privacy facts; inspect each review item yourself. Use
`--working-tree` for tracked local edits. It does not certify safety, upload code, call AI, activate a bot
or make merge decisions. See [contributor checks](product/CONTRIBUTOR-CHECKS.md).
