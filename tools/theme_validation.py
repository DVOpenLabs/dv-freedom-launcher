"""Local, side-effect-free validation shared by legacy theme tooling.

Keep the byte limit aligned with Config.read(). No device access occurs here.
"""
import datetime
import json
import math
import re
from pathlib import Path

MAX_CONFIG_BYTES = 65536
PACKAGE = re.compile(r"[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+", re.ASCII)
SETTINGS_ACTIONS = frozenset("android.settings." + name for name in (
    "SETTINGS", "WIFI_SETTINGS", "DISPLAY_SETTINGS", "MANAGE_APPLICATIONS_SETTINGS"))


def config_bytes(data):
    raw = (json.dumps(data, indent=2, ensure_ascii=False, allow_nan=False) + "\n").encode("utf-8")
    if len(raw) > MAX_CONFIG_BYTES:
        raise ValueError(f"config.json would be {len(raw)} bytes; the launcher limit is {MAX_CONFIG_BYTES} bytes. Nothing was written or pushed.")
    return raw


def validate_value(key, value, folder=None):
    """Reject entries the runtime would discard or interpret as another key/input."""
    def refuse(why):
        raise ValueError(f"{key}: {why}. Nothing was written or pushed.")

    if key.endswith("_keycodes") or key in ("settings_keys", "wifi_keys", "input_keys", "app_library_keys"):
        if not isinstance(value, list):
            refuse("expected a list of keycodes")
        for code in value:
            if isinstance(code, bool) or not isinstance(code, (int, float)) or not 1 <= code <= 2147483647 or (isinstance(code, float) and not math.isfinite(code)) or code != int(code):
                refuse("keycodes must be whole numbers from 1 to 2147483647")
    if key == "app_keys":
        for entry in value:
            if not isinstance(entry, str):
                refuse("each binding must be a string")
            binding = entry.strip()
            for prefix in ("hold:", "front:"):
                if binding.startswith(prefix):
                    binding = binding[len(prefix):].strip()
                    break
            code, sep, target = binding.partition(":")
            if code.strip() == "scan":
                code, sep, target = target.partition(":")
                valid_code = bool(re.fullmatch(r"[0-9]{1,6}", code.strip())) and int(code) > 0
            else:
                valid_code = (bool(re.fullmatch(r"[0-9]{1,6}", code.strip())) and int(code) > 0) or (bool(re.fullmatch(r"KEYCODE_[A-Z0-9_]+", code.strip())) and code.strip() != "KEYCODE_UNKNOWN")
            target = target.strip()
            if not sep or not valid_code or not target:
                refuse("binding needs a valid keycode/scancode and target")
            if target in ("back", "home", "recents", "mute", "sleep", "screensaver", "settings", "wifi", "library", "ignore", "inputs"):
                continue
            if target.startswith("action:"):
                if target[7:] not in SETTINGS_ACTIONS:
                    refuse("action target must be a supported Android Settings action")
            elif target.startswith("input:"):
                if not target[6:].strip():
                    refuse("input target is empty")
            elif target.startswith("guide:"):
                if not PACKAGE.fullmatch(target[6:].strip()):
                    refuse("guide needs a package name")
            else:
                package, slash, activity = target.partition("/")
                if not PACKAGE.fullmatch(package.strip()) or (slash and not re.fullmatch(r"\.?[A-Za-z_$][A-Za-z0-9_.$]*", activity.strip(), re.ASCII)):
                    refuse("target needs a package or activity name")
    if key in ("app_labels", "app_banners", "launch_actions"):
        for entry in value:
            if not isinstance(entry, str):
                refuse("entries must be package=value strings")
            package, sep, target = entry.partition("=")
            if not sep or not PACKAGE.fullmatch(package.strip()) or not target.strip():
                refuse("entries must have a package and non-empty value")
            if key == "app_banners":
                name = target.strip()
                resources = Path(__file__).resolve().parents[1] / "launcher/app/src/main/res"
                if not name.startswith("banner_") or not any(p.stem == name for p in resources.glob("drawable*/banner_*")):
                    refuse("banner must name a bundled banner drawable")
    if key == "festival_until":
        date = value.strip("".join(map(chr, range(33))))
        if date:
            if not re.fullmatch(r"[0-9]{4}-[0-9]{2}-[0-9]{2}", date):
                refuse("date must use YYYY-MM-DD")
            try:
                datetime.date.fromisoformat(date)
            except ValueError:
                refuse("date is not a calendar day")
    if key == "wallpaper_moods":
        for mood, pictures in value.items():
            if mood not in ("morning", "evening", "night") or not isinstance(pictures, list) or not pictures:
                refuse("moods must be morning, evening or night with non-empty picture lists")
            for picture in pictures:
                if not isinstance(picture, str) or picture.startswith(".") or ".." in picture or not re.fullmatch(r"[A-Za-z0-9_.-]+\.(jpg|jpeg|png|webp)", picture, re.IGNORECASE):
                    refuse("mood contains an unsupported picture name")
                if folder is not None and not (Path(folder) / "wallpapers" / picture).is_file():
                    refuse("mood picture is absent from wallpapers/")
    if key in ("input_sources", "source_panel_extra"):
        entries = value if key == "input_sources" else ([value] if value.strip() else [])
        seen = set()
        for entry in entries:
            if not isinstance(entry, str):
                refuse("input entries must be strings")
            name, sep, number = entry.partition("=")
            name, number = name.strip(), number.strip()
            if not sep or not name or "/" in name or name in seen:
                refuse("input mapping needs a unique name=value pair")
            seen.add(name)
            if key == "input_sources" and number == "-":
                continue
            if not re.fullmatch(r"[+-]?[0-9]+", number) or not -2147483648 <= int(number) <= 2147483647:
                refuse("input value must be a Java integer")
    return value
