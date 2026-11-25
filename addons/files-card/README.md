# Files Card

Optional Android app adapted from [CleanYourTV](https://github.com/Gathin23/cleanyourtv/tree/d11bd68c9ac45b2fb38480941586d17930137e9d/addons/files-card). MIT; see LICENSE.upstream.

Build from this directory with `./gradlew testReleaseUnitTest lintRelease assembleRelease` (or `gradlew.bat` on Windows) (JDK 17, SDK 34). It is packaged separately from the launcher. Install through the reviewed Add-on Centre; installing does not select Home, enable a schedule or select a screensaver.

## Permissions

No network or storage permissions. Files opens an existing supported file manager; its permissions remain separate.
