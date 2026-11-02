# Cinema Studio

Cinema Studio is an optional Android feature in alpha 24. Open **Launcher settings → Appearance →
Cinema Studio**. **Control Center** also opens the saved collection, mood picker and Play screens.
Opening the editor does not change Home. Review the draft and choose **Apply** or **Cancel**.

## Eight coordinated worlds

Alpha 26 adds **Cinematic worlds** for the Clean, Glass and Cinema Home experiences. Pair each style
with a different imported wallpaper in **Configure eight cinematic worlds**, turn **Follow matching
wallpapers** on, then Apply the Cinema draft. The computer creator offers the same pairings under
**Find your feel → Cinematic worlds** after reading the connected TV.

| World | Home treatment | Library and gallery |
| --- | --- | --- |
| Harry Potter | Candle gold, serif clock, winged companion | Eight film books and a moonlit greenhouse |
| The Hobbit | Leaf green, warm serif, round-door companion | Three journey books and Shire symbols |
| Lord of the Rings | Elven silver, serif, winged ornament | Three film books and Middle-earth symbols |
| Ramayana | Lamp gold, serif, lotus | Story chapters and a symbolic courtyard, without figures |
| Iron Man | Arc cyan, technical typography, reactor | Armour archive and workshop symbols |
| Avengers | Beacon blue, technical typography | Film archive and mission-control symbols |
| Infinity War | Cosmic violet, orbital companion | Film archive and a six-stone gallery |
| Doomsday | Emerald, technical typography, rune | Film archive and citadel symbols |

These are coordinated interface styles, not bundled film artwork or a catalogue of available streams.
The default books, symbols and unassigned app emblems are original DV drawings. Each style supplies
transparent app marks, a matching selection indicator and selected-app name, header typography,
foreground atmosphere, a focus-following companion, and its library/gallery presentation.
Existing custom transparent app logos are tinted to the world's accent without changing their shape.
The header keeps Inputs, Apps, Launcher settings, Controls and configured weather available.

While a paired wallpaper is playing, its complete interface follows automatically; unrelated entries
keep the ordinary Home presentation. Wallpaper timing, order and the app layout are not rewritten.
Down from the app row opens the library; Down again opens the gallery. Left/Right select, OK opens
the selected action, and Up/Back return one level. Home retains the existing system Home route.
Film actions let you choose an installed service to browse or search; they do not claim direct title
playback, regional availability, a subscription or access to an upcoming film.

**Preview a saved cinematic world** temporarily selects its background for twenty seconds. Back cancels
from Home; expiry restores ordinary rotation. Preview never saves a wallpaper, changes the playlist
or consumes its remaining interval. The normal quiet-Home mode is awakened for an explicit preview.

Optional personal images can replace the library and gallery backgrounds or book covers. A companion
can use two transparent atlases: idle is **12 columns × 2 rows**, hop is **6 columns × 4 rows**; both use
24 frames and the existing image-size limits. Attach them through the world's native draft editor.
Keep your original media separately: exported Cinema JSON contains local image references only.
The renderer does not include or redistribute the Castle Home artwork, owl sprites or movie covers.
Those assets require their own rights and provenance when supplied privately by a user.

The animated background continues through the existing one-decoder controller. The small foreground
companion and atmosphere use a bounded Canvas layer at up to 20 updates per second, with no second
video decoder. They stop away from Home and use a static state under reduced motion. Libraries and
galleries use still scenery and selection effects; supplied creatures are not generated moving video.
Classic/category Home and non-Android editions do not acquire this presentation automatically.

## A coordinated dock

Clean, Glass and Cinema experiences support floating custom logos. Import a transparent PNG/WebP
under **App logos & taglines**, or choose **Elven gold**, **Moonlit silver** or **Warm ember** emblems.
The emblems are original DV illustrations with app names; they are not exact official logo files.
Imported logos take precedence. Apps without a matching custom logo or emblem keep their usual art.
Aspect ratio and transparency are retained. Selection keeps the existing visible focus outline.
Classic and category layouts retain their existing artwork presentation.

Taglines are user-written, profile-local text. Selected-app glow is optional. Text readability still
depends on the chosen wallpaper; use a calm dark background and the existing text/focus settings.

## Titles, books and characters

Add up to 48 film or series titles, select an installed service app and optionally add an HTTPS title
link. Add local poster, backdrop and transparent character images separately. Choose poster cards,
film books or character cut-outs. Selected cut-outs reveal colour; available backdrop images change
with focus. Missing art uses an original labelled placeholder. Nothing downloads automatically.

The **Lord of the Rings + Hobbit** starting point adds six film names to an unsaved draft, using the
service app you select, with book-style covers, gold emblems and mist. It replaces only that draft's
collection after confirmation. It includes no posters, actor likenesses, streaming links or availability
promise. Check the selected service, regional catalogue and links yourself. Personal theme art is
imported separately and is not part of the source distribution.

Only titles whose apps are available in the current profile appear in the collection or mood pool.
Links are scoped to the chosen package. Unsupported links fall back to opening that app; launching an
app is not a guarantee of title selection, an active subscription or protected video playback.

## Can't decide?

Create up to 12 moods and assign titles to them. **Anything** includes all available titles automatically.
On the large mood card, Left skips the mood; Right or OK picks a title. The conventional mood list is
also available. Picks avoid the previous title when another eligible title exists.

The picked title offers **Open**, **Pick again** and **Back**. Optional three-second automatic opening
starts off. Back cancels; leaving the screen cancels pending animation/countdown and app launches.
Reduced motion and disabled launcher animations use the short, static selection path.

## Scene atmosphere

Aurora, fireflies and mist are original bounded Canvas effects. The optional starlight companion follows
app focus in the Clean/Glass/Cinema dock. These are procedural effects; they do not animate faces or
turn a still picture into generated video. They stop when Home is paused and respect reduced motion.

Local scene artwork can be previewed behind a collection. **Use scene artwork on Home** is a separate
choice: while selected and the picture is available, it temporarily supersedes the saved wallpaper
playlist and its decoder. Turning it off resumes the existing playlist. The playlist, app order and
system settings remain saved. No scene is selected by an upgrade.

Received JPEG/PNG images have an **Add to Cinema artwork** action in the existing media review. This
uses a separate library and does not consume wallpaper theme slots or select the image. Local file
imports and received artwork require review; failed imports leave the saved Cinema setup unchanged.
Each received file opens its own fresh review, including when the previous review is still in the task.

## Create it on a computer

In desktop Setup, choose **Cinema Studio**. Its four steps cover style, dock, collection and review.
The initial TV scene and example app are an illustration, not a live device screenshot. Connect an
Android TV through the normal Setup journey, then choose **Read my connected TV** to load its apps
and saved Cinema draft. Read requires exactly one compatible standalone or earlier DV development
pilot installation. It refuses an ambiguous target and never migrates or changes a package identity.
Received Cinema artwork is listed by name; image bytes are not downloaded into the creator.
The authorized read also returns local wallpaper names/tokens, capacity and storage totals; it does not select them.

**Send draft for TV review** stages a profile/revision-bound draft for two minutes. The TV performs
its own validation and still requires Apply. A changed profile or Home invalidates a stale draft.
Existing edit locks and family approval remain in force. The computer does not get an arbitrary shell
or automatic permission-grant endpoint.

**Download setup JSON** works without a TV or AI service. Import it through Cinema Studio on the TV.
JSON includes local artwork hashes, not image bytes, streaming accounts or credentials. Import the
referenced art on the destination TV first; this is not a complete portable artwork backup. The browser
draft is temporary, so download it before closing/reloading the creator.

## Optional Play

Play contains separate, lazily opened activities in the launcher APK; there is no extra mandatory app,
background service or permission. It does not run on Home.

- **Chess:** remote-controlled board, legal moves, castling, en passant, promotion choices, checkmate
  and stalemate; three bounded computer strengths. Positions save locally after each move. Threefold
  history is session-local after a reload; this is a casual game, not a tournament arbiter or engine-strength claim.
- **Skyhop:** an original one-button game with local best score. OK lifts the light between towers;
  Back pauses. Leaving the activity stops updates.
- **Daily frame:** a local daily title puzzle from collection titles with artwork. It needs at least
  two illustrated titles. The picture reveals more after guesses; six guesses per daily title. It is
  not an online quiz catalogue or a media downloader.

## Resource and platform limits

The Cinema JSON is bounded to 60,000 characters, with strict fields, local image hashes and HTTPS links.
Images are read from at most 8 MiB / 20-megapixel sources, normalized to bounded alpha-preserving PNGs
and capped at 2 MiB each. The independent art library allows 96 files / 64 MiB. Cancelled drafts may
leave unselected local art; it is not automatically deleted because other profiles may reference it.
The bitmap cache is capped at 12 MiB and is cleared under memory pressure. Scene effects target a
maximum 30 updates per second while visible. These bounds are not frame-rate guarantees.

No new Android permission, network call, paid AI call, Home routing change or automatic package removal
is introduced. Feature configuration belongs to the selected profile and its saved setups. Referenced
art must still exist when restoring. Physical remote, standby and protected playback checks remain
separate from unit tests or injected D-pad checks.

This batch is Android-specific. An LG screen connected to a Google TV device uses the Android edition.
Native LG, Samsung, hosted and Roku editions do not gain this renderer or feature parity automatically.

To keep one complete world active, select only its wallpaper in **Wallpaper → Wallpaper playlist → Choose wallpapers**, keep the playlist on, and Apply. One selected entry stays pinned; selecting several resumes rotation. The saved interval and order are retained.

## Freedom Showcase Home and preview

In **Appearance → Cinema Studio**, choose **Try Cinematic Showcase · unsaved preview**. It follows
an already paired and selected world. This preview uses a compact icon header,
local silent video, the configured weather units, transparent app marks, a moving selection glow and
app-specific phrases. Existing custom taglines take precedence. The preview never saves a profile,
changes the selected Home, or changes the wallpaper playlist or app order.

Down from its app dock enters an immersive library: closed leather book spines turn forward to reveal
the selected local cover. Down again opens the world's gallery. Harry Potter's supplied greenhouse
uses a soft selection light over its existing creatures; the creatures themselves stay still. Left/Right
move selection, OK opens the available action, and Up/Back return one level. Home returns to the saved
launcher. Reopen the unsaved preview from Cinema Studio to try it again.

To make it your daily Home, choose **Appearance → Home experience → Freedom Showcase**. This starts
a 20-second trial with Keep/Revert; only Keep saves this profile’s Home experience. It requires an
enabled playlist containing exactly one paired world with its imported wallpaper. Multiple-wallpaper
rotation stays available in the other Home experiences; Showcase does not silently change that playlist.
If the selected world or file later becomes unavailable, Home falls back to Freedom Cinema while your
saved choices remain intact. Change Home experience from Showcase’s header or Launcher settings.
All eight world palettes are supported, using original local emblems where personal art is absent.
The dock respects Top/Middle/Bottom placement, icon sizes, spacing and app-name visibility; longer
app rows scroll horizontally. Input/Settings and configured app/TV buttons keep their existing
routes; local silent video stops away from Home. Launcher locks and family editing restrictions still
apply. No helper grant, Home-package migration or new device permission is required.

Personal castle, owl, greenhouse and cover art is imported separately. A Hobbit preview can reuse the
configured candlelit study from the Harry Potter world; absent artwork uses original DV drawings.
It does not create Hobbit character illustrations or add actor likenesses to the source distribution.
One video decoder is used while the preview is visible and released in the library or away from the
preview. Book animation runs only while a selection settles; reduced motion uses the static path.
Book fronts use a hinged perspective with binding shade, page edges and a shelf shadow. Dock lighting
and the gallery lantern ease toward the new selection, including when movement is interrupted by
another arrow press. These transitions stop when the screen is left. Covers must be imported and
assigned to each world separately; selecting a world does not download movie artwork.

Harry Potter and Hobbit also use this compact shelf when entered from regular Home, so returning
from a streaming app does not switch to a second, spread-out book layout. The selected book is
retained when the activity is recreated.

**OK** opens a saved exact movie link directly. Hold **OK** on a book for Movie options,
where **Open app · find movie in app** is distinguished from **Open saved movie link**.
Without a saved link, pressing OK opens these options.
To target an owned edition, choose **Add or change this movie's link**, choose its installed app,
paste that movie's HTTPS Share link, and explicitly save. Links belong to the current profile and
world/book, and can also be edited in Cinema Studio's Collections. Without a saved link the launcher
opens the app only. If an app rejects a saved link, an explicit fallback offers to open the app.
For the official YouTube TV app, a book without a saved link offers **Find this movie on YouTube**
and passes its full title to YouTube search. Select your purchased edition there; the launcher never
automatically chooses the first result or a Buy/Rent offer. A saved exact movie link takes precedence.
The destination app controls sign-in, purchase rights and playback; a working shortcut does not
establish ownership or guarantee automatic playback. The launcher does not inspect account libraries.
