# Manual-launch webOS preview

This is the first independently written webOS runtime in the product fork. It is a **development
preview**, separate from the pinned helper recovery patch. It is not a certified LG Home replacement.
Nothing here enables Developer Mode, connects to a TV, installs software, roots it or intercepts Home.

Implemented: local profiles and avatar colours, Glass/Clean/Cinema presentations, top/middle/bottom
app position, explicit app mappings, shared-profile import review, AI-draft review into a new profile,
local atomic storage, D-pad/Magic Remote navigation, nested Back and a root exit confirmation, bounded foreground wallpaper timing
and a single optional muted video element. The source ships with a gradient and **no private media**.
No external libraries, hosted fonts, analytics or AI calls are required.

Android content shelves, layout PINs, weather, scene time windows and hardware input controls are
not ported yet. webOS source tests do not establish real-device video/standby/remote compatibility.

## Build locally

```sh
node platforms/webos/test-launcher.cjs
python platforms/webos/prepare_launcher.py /path/to/new-staging-folder
# Optional: --profiles /private/path/reviewed-shared-profiles.json
ares-package /path/to/new-staging-folder -o /path/to/output
```

The preparation tool refuses existing output directories, draws the required original 80×80 PNG icon
from code and stages source only. It does not invoke the installer. A packaged profile library appears
as a review option; it is not silently applied. Use the official
[webOS CLI](https://webostv.developer.lge.com/develop/tools/cli-introduction) for packaging.

After reviewing the package, an owner can use LG’s normal
[Developer Mode app](https://webostv.developer.lge.com/develop/getting-started/developer-mode-app).
That procedure requires an LG developer account and may require a TV restart. It is a separate,
explicit device step. Existing LG Home remains the recovery/exit route; Developer Mode sessions expire.

## Honest app capability handling

LG’s public [Application Manager](https://webostv.developer.lge.com/develop/references/application-manager)
documents `getAppLoadStatus` and `launch`. This adapter uses only those methods for reviewed app IDs.
It declares only `application.launcher` and `application.query`, the public [ACG groups](https://webostv.developer.lge.com/develop/guides/acg-guide) for those two methods on newer firmware. It does not request privileged `listLaunchPoints`, private input APIs or OSE-only permissions.
Android IDs are never guessed as LG IDs. A failed/missing/privileged-app launch is reported without
changing another setting. Development apps may be unable to launch privileged targets, including
some system apps; test the actual installed retail TV and firmware before claiming compatibility.

The shared profile file maps logical names to exact `webos` IDs. Missing mappings and collisions are
errors. Existing profile IDs cannot be overwritten by import. Storage corruption is reported and
preserved rather than silently resetting the user’s Home. AI drafts are allowlisted, reviewed and saved
as new profiles. No server or network listener is installed on the TV.

## Hardware acceptance still required

Record full model code, webOS/firmware and Developer Mode session state. Test the real remote and
Magic Remote focus, Back to LG Home, every mapped app, app playback, video still fallback, one
decoder, video/timer pause when hidden, relaunch, standby and a normal owner-approved restart.
Confirm no Home/input/settings/account changes. The B3 model-family name alone is insufficient.

The native `PalmServiceBridge` boundary follows LG’s [official SDK source](https://github.com/webOS-TV-app-samples/webOSTVJSLibrary/blob/main/webOSTV_js/lib/webOSTVjs-1.2.13/webOSTV.js). Back handling follows the [single-page app guide](https://webostv.developer.lge.com/develop/guides/back-button): local navigation uses Back first; root Back asks before exiting. There is no dedicated close button on Home.

## Package validation on 28 October 2026

LG CLI 3.2.6 validates the staged app with no problems and produces a 9,944-byte IPK without private
media. Forty local policy/storage/service checks and the browser's review/Back flow pass. These are
packaging/source results only; no LG installation, app launching or physical remote/video test is claimed.
