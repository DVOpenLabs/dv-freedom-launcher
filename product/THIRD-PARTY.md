# Integrated component provenance

## Launcher lifecycle and optional tools

The MIT CleanYourTV [v3.2.0 source](https://github.com/Gathin23/cleanyourtv/tree/42fe7b09d61fb9175cb03b8c659cd63c9c1dd9a4)
supplies adapted idle-clock/date fixes, held-key ownership cleanup, RTL library alignment and ignored
remote actions ([#2304](https://github.com/Gathin23/cleanyourtv/pull/2304),
[#2330](https://github.com/Gathin23/cleanyourtv/pull/2330),
[#2359](https://github.com/Gathin23/cleanyourtv/pull/2359),
[#2180](https://github.com/Gathin23/cleanyourtv/pull/2180),
[#1764](https://github.com/Gathin23/cleanyourtv/pull/1764)). Bluetooth Connect includes the Android-version
permission guard from [#2178](https://github.com/Gathin23/cleanyourtv/pull/2178), with additional foreground
cleanup and system-settings fallback. The root MIT and original add-on author credits remain.

Profile browser exclusions, startup wait coordination, bounded package-scoped link settings and signed
tool packaging are DVOpenLabs implementations. TV Inspector, Bluetooth Connect, Digital Rain and News
retain their original source/author notices. News changes its default to no configured feed and adds
bounded XML/redirect safeguards. No additional runtime dependency, private device evidence or media is bundled.

## Platform adapters

The VIDAA/Vizio adapters and Roku ambient app are independent DVOpenLabs implementations. Protocol
origins and pinned revisions are in `allPlatformReferences` in [sources.json](sources.json).
CleanFrame Hisense and Zfinix's VIDAA launcher informed the hosted architecture and protocol boundary;
vizaio and vizaio-conjure document pairing/current-app and normal Conjure behavior. No launcher source,
vendor key/certificate, static password, credential generator, root service or privileged webview was
imported. Existing native LG/Samsung source and Home behavior are unchanged.

Hosted pages reuse our own maintained web renderer/policies and existing Outfit OFL notice. Paho
MQTT 2.1.0 is the only new runtime library, confined to desktop VIDAA support and redistributed under
its EDL 1.0 option with [full terms](licenses/Paho-EDL-1.0.txt) and [attribution](licenses/Paho-NOTICE.txt).
It is not included in the Android APK. BrighterScript 0.73.8 was used only for local Roku source
validation; no compiler, npm dependency or Roku SDK is bundled. The independent ambient package
contains root MIT and DVOpenLabs notices and uses system fonts.

## CleanYourTV v3.1.0 source adaptations

Selected MIT source from [Gathin23/cleanyourtv at 1684678064aa7bbb4881e6069e4cfbd4b5fc9df7](https://github.com/Gathin23/cleanyourtv/tree/1684678064aa7bbb4881e6069e4cfbd4b5fc9df7)
is adapted for input folding, icon-tile configuration, focus/locale/calendar/rotation fixes, translated
resources, tool safeguards, reference recipes and documentation. The original root MIT notice remains.
TV Inspector retains attribution to [shaharukhs](https://github.com/shaharukhs); Bluetooth Toggle to
[justin-newman](https://github.com/justin-newman). Both add-ons retain the root upstream MIT licence
and their author information, with DV Freedom build/default adaptations documented in their READMEs.
No additional third-party binary, screenshot, themed media, runtime library or font was imported.
SHOWCASE entries are not automatically bundled components.

The fork retains the root MIT licence and existing font notices. Additional source adaptations:

| Component | Origin and reviewed revision | Reuse |
|---|---|---|
| `LiveVideo.java` | [csermet/cleanyourtv](https://github.com/csermet/cleanyourtv/tree/18b03b2145c51c35fb255b1c6863ed609746ece6), `18b03b2145c51c35fb255b1c6863ed609746ece6` | Local TextureView/MediaPlayer lifecycle and still-fallback design; reduced to silent local packs, with additional callback guards |
| `ThemeArchive.java` | [DipsySu/cleanyourtv](https://github.com/DipsySu/cleanyourtv/tree/1c05b6b50cd8e5a1f9cff9f8914b1439f02a66f4), `1c05b6b50cd8e5a1f9cff9f8914b1439f02a66f4` | Adapted strict JSON parser and manifest concept; visual-only format, bounded staging/import and library are product integration work |
| `GlassView.java` double frame | [OrenDdp/cleanyourtv-header-grid](https://github.com/OrenDdp/cleanyourtv-header-grid/tree/703609369f2e5955e282b6cf687691e82801b7bf), `703609369f2e5955e282b6cf687691e82801b7bf` | Adapted black/white dual-frame drawing; no extra glow, fonts or layout renderer |
| `HdmiVisibility.java` | [midhatn/cleanyourtv](https://github.com/midhatn/cleanyourtv/tree/eeda31c7029737286e74016e7b24a5882ea4deef), `eeda31c7029737286e74016e7b24a5882ea4deef` | Bare-port state filter; product adds explicit settings and a one-visit Show all escape |
| `TvButtonSettings.java`, `TvButtonPress.java` | [lionlev21/cleanyourtv](https://github.com/lionlev21/cleanyourtv/tree/e412c5b2b4e0819598f5da96060c0d1bf8522526), `e412c5b2b4e0819598f5da96060c0d1bf8522526` | Adapted picker/press-gate concepts and app-query/sorting code; restricted native dialog, shared per-device deduplication, no new watchers/services or auto-grants |
| webOS recovery patch/source preparation | [Razdva122/clean-your-tv-lg-webos](https://github.com/Razdva122/clean-your-tv-lg-webos/tree/d5804f6a7ce5d4eb35c0093ca19ae011e5a6df7f), `d5804f6a7ce5d4eb35c0093ca19ae011e5a6df7f` | Small filter/relay exit-contract fix and tests; selected pinned source is extracted locally rather than bundling upstream executables |

The listed Android source snapshots carry the Gathin MIT notice, reproduced in
[Android-integrations-MIT.txt](licenses/Android-integrations-MIT.txt). The webOS source carries Gathin
and OmkarK97 MIT notices, retained in [LICENSE.upstream](../platforms/webos/LICENSE.upstream).
Source preparation also copies its original `THIRD_PARTY_NOTICES.md`; further integration must retain
applicable transitive licences. No additional fonts, themed artwork, Android libraries, webOS IPKs,
desktop Qt packages or Node dependencies were imported by this batch.

The new native carousel does not copy an alternate launcher's renderer; it extends the existing native
adapter. Design references are not represented as bundled components. Preserve this attribution and the licence
files when redistributing adapted source or builds.

The later Samsung manual-launch pilot and shared web policy/media layer are independently implemented
DVOpenLabs code, reusing our own LG policy code. No candidate Samsung project's code was copied. The
staged widget includes the existing Outfit font and its OFL notice, root MIT notice and a DVOpenLabs
MIT attribution. Its scene and icon are original project assets. Personal media is optional private
staging input and never bundled into public source.

## Temporary phone pairing

The optional phone-pairing screen uses [python-qrcode 8.2](https://github.com/lincolnloop/python-qrcode/tree/v8.2)
(BSD 3-Clause, including its original MIT notice) to render an address-only QR code locally.
Windows packaging includes its dependency [colorama 0.4.6](https://github.com/tartley/colorama/tree/0.4.6)
(BSD 3-Clause). Notices are retained in `product/licenses` and the frozen distribution. No remote QR service,
analytics or phone library is used. Pairing and draft/media transfer are independent DVOpenLabs code.


## Optional Home/calendar and phone remote

The two DV Freedom companions are independently implemented. The four curated existing essentials
retain their original packages, source and licenses.

The Home/calendar companion uses biweekly 0.6.8 (BSD 2-Clause), including its embedded google-rfc-2445
and codec notices; vinnie 2.0.2 (distributed BSD notices); OkHttp 4.12.0, Okio 3.6.0, Kotlin stdlib 1.9.10,
JetBrains annotations 13.0 and Commons Codec 1.23.0 (Apache 2.0). Jackson/XML/JSON calendar APIs are not
used and Jackson dependencies are excluded. Exact versions are registered in `sources.json`. Notices
from pinned dependency artifacts or official repositories are retained in `product/licenses` and
combined into the companion’s generated notices asset. Outfit and its OFL notice are shared through
the optional UI build task. The phone server/pairing protocol, Android control service, settings,
provider, cache and signed companion installer are DVOpenLabs code.

## Provider APIs and attribution

Immich and WebDAV integrations use public protocols; no Immich/Projectivy provider source was copied.
Google Gemini uses the documented generateContent REST API and requires a separately configured key/model.

The optional Discover panel uses TMDB metadata and images with the required non-endorsement notice in
About, and identifies JustWatch as its availability source. The TMDB primary-short mark is TMDB’s own
trademark, used only for attribution under its [logo guidelines](https://www.themoviedb.org/about/logos-attribution)
and [API FAQ](https://developer.themoviedb.org/docs/faq), not relicensed as DVOpenLabs artwork. The
unmodified geometry/approved gradient is sourced from the
attribution-only [TMPlayer mark](https://github.com/dracu-lah/TMPlayer/blob/c716e99dc88e11e2fce5c2f2e8b162e8b91050ba/ui/src/shared/kotlin/com/tmplayer/ui/about/TmdbLogo.kt).
Only the vector asset was converted to Android XML; its application/Compose implementation was not
copied. The mark remains smaller than DV Freedom’s panel title and implies no endorsement. Configured
provider/API acceptance and the user’s applicable service terms remain separate from local build tests.

## Ecosystem additions and separate phone Setup

Nepali Calendar is retained from the MIT CleanYourTV source at
`fe9ad3204b945d3aadfb6876ee6ea78c20a1fe73`, including its separate algorithm licence/notice. Other
optional add-ons keep their existing authorship. Headphones’ disconnect guard now defaults off;
Prayer Times uses an explicit source form and foreground-only bounded requests. No reference artwork
or commercial launcher implementation was copied. Discover filters, ambient framing, family policies,
volume/wake integration and native platform presentation are independently implemented DV code.
The local contributor checker takes the deterministic-check concept from the MIT maintainer-bot;
no bot source, provider credentials or automatic PR actions are included.

The **separate `mobile/` phone application is GPL-3.0-or-later**, with its licence and build/relink
instructions in [mobile/README.md](../mobile/README.md). It vendors selected LibADB Android 3.1.1 runtime
source at `c849886ebc6d48e7b46d967e78a6bb65c90c3b74`, preserving GPL/Apache/BSD/MIT notices and the LGPL-3
SPAKE2 dependency notice. Phone code is not linked into the MIT TV launcher/desktop. Conscrypt, Bouncy
Castle, AndroidX/JetBrains annotations, Kotlin and Outfit retain their own notices. A public phone
binary distribution needs corresponding source and dependency source/notices, not just the APK.

Museum art is requested only on demand from The Met Open Access API, restricted to records explicitly
marked public domain. CC0 credit accompanies cached works; no museum image is committed or bundled.

The optional independently implemented Camera app uses Media3 1.4.1, its AndroidX/Guava dependencies,
OkHttp/Okio and Kotlin. Exact runtime versions and attribution are retained in
[media3-NOTICE.txt](licenses/media3-NOTICE.txt), with Apache/other applicable licence texts copied into
the app’s generated notices. No Corner Player code, camera imagery, native decoder extension or
Frigate source is linked into the core launcher. It is MIT product code with separately reviewed access.

Reliability safeguards and preset/remote interactions also draw on failure cases discussed in the
MIT CleanYourTV community. Exact references and adaptation scopes are in [sources.json](sources.json).
The implementations retain DV’s permission, profile and recovery contracts. The provider helper’s
output handling follows Android’s [content command behavior](https://github.com/aosp-mirror/platform_frameworks_base/blob/android14-release/cmds/content/src/com/android/commands/content/Content.java); no AOSP source was copied.

Installed icon-pack support independently reads the documented appfilter data convention illustrated
by [Blueprint](https://github.com/jahirfiquitiva/Blueprint). No Blueprint code or icon artwork is copied
or bundled. Static icon bytes come only from a pack the user installed; its author’s artwork licence
still applies. Seasonal ambient graphics are original procedural DV Freedom code, with no third-party
media. The source register records the format and Android package-visibility references.
