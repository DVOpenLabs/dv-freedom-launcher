# Manual-launch webOS preview

This is the first independently written webOS runtime in the product fork. It is a **development
preview**, separate from the pinned helper recovery patch. It is not a certified LG Home replacement.
Nothing here enables Developer Mode, connects to a TV, installs software, roots it or intercepts Home.

Implemented: local profiles and avatar colours, Glass/Clean/Cinema presentations, top/middle/bottom
app position, Small/Medium/Large icons, compact/comfortable/wide spacing, explicit app mappings, shared-profile import review, AI-draft review into a new profile,
local atomic storage, D-pad/Magic Remote navigation, nested Back and a root exit confirmation, bounded foreground wallpaper timing
and a single optional muted video element. The source ships with a gradient and **no private media**.
No external libraries, hosted fonts, analytics or AI calls are required. Appearance & layout choices
use a 20-second Keep/Revert preview: browsing writes nothing, Back/timeout/hidden state reverts, and
a changed stored revision blocks Keep. Existing profiles without size/spacing fields remain readable
without a migration write. Native LG weather and Android companion installation are not implied.

Android content shelves, layout PINs, weather and scene time windows are not ported yet. The Inputs
menu can offer reviewed SDK-reported HDMI app targets; it does not enumerate every physical port or
set a power-on source. webOS source tests do not establish real-device video/standby/remote compatibility.

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
For a default/newer package it declares only `application.launcher` and `application.query`, the public [ACG groups](https://webostv.developer.lge.com/develop/guides/acg-guide) for those two methods on newer firmware. It does not request privileged `listLaunchPoints`, private input APIs or OSE-only permissions.
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


## Shared-policy extraction

The portable core is now in `platforms/shared-web/core.js`; the webOS wrapper retains the LG public
Bridge, original `dv-freedom.webos.v1` key and v1 mappings. Staging copies the shared asset into the
package and rewrites its script path. Existing 40 policy/storage/service checks remain the regression
baseline. This source refactor does not update an installed LG app or establish new hardware acceptance.


## Guided first-install pilot — 29 October 2026

The [Windows GUI](../../setup_app/NATIVE-SETUP.md) now prepares/checks LG tools, explains Developer Mode,
pairs locally, builds/reviews an IPK and verifies a separately confirmed first installation. It retains
LG Home. There is no helper, root, automatic reboot or existing-app overwrite. The current official CLI
package is 11,562 bytes without private media/starter data; hardware acceptance remains pending.
Forty-five source policy/storage/service/starter assertions pass; packaging does not prove LG behavior.

Optional starter apps come only from the checked TV's developer catalog. A private setup package can
include them for a first-run review: **Use these apps** saves an initial empty Home; Back saves nothing.
The prompt cannot overwrite existing profiles/preferences. The TV may expose a limited developer app
list; unreported app IDs are not guessed. Native launch still checks actual installation separately.

## Picture framing and family view

Appearance/Background offers Fill, Fit and Matte for bundled media. The saved default stays Fill;
browsing does not save a choice. Favourite-only family view simplifies this profile’s app chooser.
It does not provide Android’s parent PIN or Accessibility guard: profile switching, vendor Home and
system settings remain available. These are native-runtime source/browser features; physical TV
rendering, remote navigation and decoding require separate acceptance.

## SDK-specific legacy trial

The first webOS 1.x hardware trial blocked package CSS and scripts because its WebKit treated
local file URLs as separate origins. The SDK-specific packaging repair now loads the layout and
code at the TV's full viewport. Physical remote, streaming-app launch and standby acceptance
remain pending. A manually opened app does not replace or accelerate LG's own Home or Settings.

Guided Setup now stages the exact `sdkVersion` read from the paired TV. Manual staging can pass
`--sdk-version <reported-version>` to `prepare_launcher.py`; omit it for the default modern path.
On webOS 4.0 and above, the bridge checks `getAppLoadStatus` before `launch`. A verified pre-4.0
package uses `launch` directly for an ID in its reviewed local catalogue because that status API is
unavailable. This path requests only `application.launcher`. Security/privileged-app denials remain
errors; there is no permission-error fallback, root, input relay or Home takeover.

Staging retains the SDK capability declaration when adding starter apps or shared-profile data.
Unknown/malformed versions are refused rather than guessed. The source includes prefixed flex,
scale and visibility handling and RGBA colour fallbacks for older WebKit. No new runtime library,
font download or network capability is added. Hardware rendering, Magic Remote, app-launch policy,
protected playback and standby/wake must be checked on the actual model. Browser/source checks
cannot certify the legacy engine or grant Android feature parity.

For verified webOS 1.x, staging embeds the package's reviewed CSS and five scripts into the local
HTML. Its CSP1 policy permits these inline assets while denying external resources, connections,
frames, media and `eval`; it does not broadly allow `file:` script loading. Generated profile and
starter JSON escapes HTML delimiters. This compatibility edition uses the plain bundled background
and has no live-media path. Modern/default packages retain their external-file `'self'` policy.
After adding private bootstrap data, tooling must call `finalize` again before packaging so that
the legacy HTML includes the reviewed data. Guided Setup does this automatically.

## Inputs and readable app names

The header's **Inputs** menu opens only HDMI targets reported by the pinned SDK's public
`Launcher.listRunningApp` API during Setup. A currently running HDMI source may be the only target
reported. The menu never guesses other port IDs; if none are available, use the LG remote's Input
button. Selecting a listed source uses the public `launch` method and can still be denied by firmware.
This operation changes the current source only. Power-on defaults, CEC and vendor startup settings
stay under LG's normal settings; fixed-HDMI startup is not verified for older webOS.

Manual staging accepts `--inputs /private/path/reviewed-inputs.json`, an array of exact `id`/`label`
pairs from the SDK. Keep that device-specific file private. Setup includes its input shortcuts in the
installation review, separately from installed apps and starter favourites.

App names wrap within their tiles. The runtime measures the required caption height and expands
the minimum tile height while retaining the saved icon size and spacing. Native legacy layout
measurements show unclipped starter captions; physical remote/input/playback checks remain separate.
