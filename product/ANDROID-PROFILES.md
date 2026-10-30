# Android launcher profiles

On-TV profile creation and management are described in the [Android feature guide](ANDROID-COMMUNITY.md).
The feature is off by default; a test theme can enable `local_profiles: true`. No manifest permission,
new dependency, Android account access or device setting is added.

## User flow

Launcher settings always offers explicit creation of a profile copied from the current Home. Once enabled, its native D-pad chooser offers
**Current layout**, imported named profiles, and **Import profile library**. Import uses Android's
document picker, with a Cancel-first confirmation. Cancel leaves the library and selected profile alone.
The file uses the format in [profiles.example.json](profiles.example.json); its app IDs are placeholders,
so a real library needs actual installed Android app mappings. Keep the original configuration private.

Import is limited to 64 KiB, eight nesting levels and 16 profiles. Unknown fields, duplicate profile IDs,
missing Android mappings, invalid IDs/columns, duplicate mapped apps and favorites also marked hidden
are rejected. Only a valid library reaches the app-private AtomicFile write. Importing a replacement
library requires selecting Current layout first, so replacing its data does not silently change an
active profile. Saved per-profile appearance/order choices remain associated with their IDs.

Selecting a profile saves its ID and the Home revision in one SharedPreferences commit. A failed commit
attempts to restore the prior in-memory selection too, because Android can update memory before reporting
a disk-write failure. The app does not claim protection from a failing storage device.

## Isolation and migration

Current layout reads the original settings and order files. It is the initial selection and the fallback
when no valid selected profile is available. Profiles use their catalog favorites/hidden apps and column
count as defaults, with separate namespaced choices for columns, rows, opacity, animations, advanced effects,
selected wallpaper, Home style and its Undo. Existing native reordering, added/removed library shortcuts
and wallpaper rotation state also use a separate file or preference name for each profile.

Startup-app behavior and the revision counter remain global. A profile's Follow theme action does not
clear the global startup-app choice. No settings are copied from another person during a switch.
The original external theme still supplies wallpapers when no offline pack is selected. Alpha 2 adds
per-profile offline pack selection and a silent-video toggle. Library selection preserves manual
choices; an explicit external theme push retains the reference launcher's existing theme-reset behavior.
See [appearance integration](ANDROID-COMMUNITY.md) for the separate pack and media limits.

These are preference profiles, not parental controls. They do not lock apps, switch streaming accounts,
create Android users or isolate app data. Profiles can be created, renamed and deleted on the TV; offline library import remains available. English fallback labels are present; translations are not complete.

## Validation and limits

JVM checks cover profile mapping, malformed documents, isolated settings, single-editor revision writes,
failed-selection repair, file names, import size and nesting boundaries. The desktop exporter tests cover
the shared format and preservation of unrelated keys. Android release compilation and lint are required.

No emulator, TV file-picker import, real disk-failure injection, physical remote, playback
or restart result is inferred from the locally created profile tests. Device installation requires an explicit owner-authorized pilot.
Availability of Android's document picker on each TV also needs checking; TVs without a provider need
an alternative offline import path before this feature can be offered there.
