# DV Freedom Ambient for Roku

This independent BrightScript/SceneGraph pilot is an ambient clock/background app. It is **not a
launcher** and does not issue ECP requests, launch other apps, control HDMI or override Home.

Use the same DV Freedom Setup GUI, choose **Roku / Roku TV**, choose one of three backgrounds and a
clock format, then download the generated ZIP. Follow [the installation and slot warning](../../setup_app/ALL-PLATFORMS.md#roku-ambient-app).
The GUI does not connect to the TV, accept developer terms, reboot, upload or replace the current
development app. Hardware acceptance and Store certification remain pending.

Build only from source:

```sh
python -m platforms.roku.prepare_channel /new/path/freedom-ambient.zip --theme ocean --clock 24h
```

Existing output is never overwritten. The package includes MIT notices. System fonts are used; no
third-party artwork or font is bundled. OK cycles backgrounds; Back/Home remain with Roku. A small
clock shift each minute does not disable the TV's own idle/OLED protection.

The generated source was validated with BrighterScript **0.73.8** (development-only npm tool, not
bundled or required by the setup runtime). This checks syntax/project structure, not actual rendering,
remote behavior, screen protection, standby, startup or hardware compatibility.
