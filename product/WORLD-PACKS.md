# Portable offline world packs

A world pack brings a still wallpaper, optional silent MP4, scene settings, original/personal artwork,
preview and credits into one ZIP. It contains no app order, profile identity, credentials, movie URLs,
executable code or service account data. The launcher still works without packs.

## Import and review

1. Import the ZIP through **Wallpaper → Offline theme library**. This adds immutable library files;
   it does not choose a wallpaper or Home experience.
2. Open **Appearance → Complete theme packs** and choose the imported pack.
3. Review the Cinema Studio draft on the TV. Apply is separate from import. Existing movie links and
   personal taglines are retained. Existing custom app logos take precedence over pack defaults.
4. Choose the paired wallpaper and Home experience separately when ready. Adjust package mappings
   in Cinema Studio if the destination TV uses a different edition of an app.

Cancelling review leaves only unselected imported files. Artwork normalized during preparation also
remains in the local artwork store under its existing quota; cancelling does not select it.

## Build a pack

Prepare a new local folder with `manifest.json`, `config.json`, `cinema.json`, `CREDITS.txt` and a JPEG
or PNG still named `wallpaper.jpg`. Optional files are `preview.jpg`, `video.mp4` and `art/<sha256>.png`.
Artwork references must match their bytes. Include only artwork you may share; generated film-themed
or personal assets are not included in the source repository.

```sh
python tools/world_pack.py /path/to/pack-source /path/to/new-world.zip
```

The builder reads an explicit allowlist; unrelated files in the source folder are not exported.
Existing output files are refused. Limits remain 64 MiB compressed/extracted, 64 entries, 8 MiB per
wallpaper/preview, 48 MiB for video, 2 MiB per cinema PNG and the existing 128 MiB theme-library quota.
Android performs its existing image/video validation at import; the builder is not a media transcoder.

The world uses the portable self-reference `p-00000000-0000-0000-0000-000000000000`, remapped on import.
The Cinema Studio schema and supported world IDs are unchanged. Packs contain exactly one world and
exclude `titles` and personal `taglines`. `config.json` accepts only the existing visual theme fields.
The artwork store normalizes images, so imported hashes can differ and are remapped in the draft.

## Reproducible original example

```sh
python tools/starter_world.py /path/to/new-source /path/to/new-example.zip --world hobbit
```

`--world hogwarts` selects the other example navigation. Both generate original procedural twilight
landscapes and abstract cover placeholders locally, with MIT sharing terms. They contain no official
film posters, character art, video or downloaded media. These are format examples, not the owner's
private cinematic theme collection. Native LG/Samsung and Projectivy do not consume this Android pack
format; their runtimes and media imports remain separate.
