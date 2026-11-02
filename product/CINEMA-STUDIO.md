# Cinema Studio

Cinema Studio is an optional Android feature in alpha 24. Open **Launcher settings → Appearance →
Cinema Studio**. **Control Center** also opens the saved collection, mood picker and Play screens.
Opening the editor does not change Home. Review the draft and choose **Apply** or **Cancel**.

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
