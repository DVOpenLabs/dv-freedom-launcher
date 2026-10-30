# Weather

The weather for one city, full screen on the TV: now (temperature, feels-like, conditions), the US air quality
index as a coloured pill, and the next three days. For the [Clean Your TV](../../README.md) launcher it declares a
[module card](../../docs/modules.md), so once you answer **Show card** its tile reads "Weather" with a
sky-blue glow; on any other Android TV home it is an ordinary app.

![The weather page](https://raw.githubusercontent.com/Sumyabazar1/cleanyourtv/pr-assets/addon-weather/addon-3-weather.png)

- **The city** is chosen once: type its name with the remote's keyboard (or voice), pick it from the matches,
  and it is kept. **Change city** at the bottom chooses another.
- **Data** comes from [Open-Meteo](https://open-meteo.com/), which needs no account or key: the weather from
  DWD's ICON model (Open-Meteo's default blend called light snow over Ulaanbaatar "overcast"; ICON matched
  what was falling), and the air quality from the Copernicus (CAMS) model, shown on the US AQI scale.
- **Snow, not rain, near freezing.** The models type precipitation by the air temperature and call snow
  "light rain" at 1 °C (ICON and GFS did, in a Ulaanbaatar blizzard). The app decides by the wet-bulb
  temperature instead, which counts how dry the air is: at 1 °C or below, rain and drizzle are shown as snow.
- It reads Open-Meteo only while the page is on screen, every 30 minutes, or 5 minutes after a failure.
- Degrees Celsius by default (Fahrenheit is an option below); English or French words, with the TV's language; day names follow the TV's language.

## Options

Set in `app/src/main/res/values/options.xml` before building. With the defaults the page is as above.

| Option | Default | What it does |
|---|---|---|
| `forecast_days` | `3` | Days in the forecast row after today, 3 to 7. Above 3 the day cards share the row, with short day names, so a week fits on screen. |
| `show_humidity` | `false` | The relative humidity now, after "Feels like". |
| `show_wind` | `false` | The wind speed now in km/h, after "Feels like" (mph with `temperature_unit` `f`). |
| `temperature_unit` | `c` | `f` shows degrees Fahrenheit for the temperature now, feels-like and the days' highs and lows, converted from the reading before rounding. |
| `show_both_units` | `false` | The temperature now in the other unit too, at the start of the "Feels like" line ("22°C · Feels like 70°"). |

![A week, with humidity and wind](https://raw.githubusercontent.com/majnioui/cleanyourtv-contrib/pr-assets/addon-weather-week/weather-options.png)

## Permissions

| Permission | Why |
|---|---|
| `INTERNET` | Open-Meteo: the place search, the weather and the air quality. |

That is all. What is sent: the city name you type in the search, then that place's latitude and longitude. No
account, no telemetry, no ads, no background service, no location permission. The chosen city stays in the
app's own storage (backups are off).

## Build and install

JDK 17 and Android SDK platform 34 with build-tools 34.0.0, as for the launcher:

```bash
cd addons/weather && ./gradlew assembleRelease
TV_HOST=<tv ip> ../../tools/venv/bin/python -I ../../tools/tv.py --push app/build/outputs/apk/release/app-release.apk /data/local/tmp/addon.apk
TV_HOST=<tv ip> ../../tools/venv/bin/python -I ../../tools/tv.py 'pm install -r /data/local/tmp/addon.apk; rm -f /data/local/tmp/addon.apk'
```

Press Home: the launcher asks once whether to show the card. Change the answer later by holding OK on
Settings, then Module cards.

The release build is signed with the debug key so it installs over ADB like the launcher. Sign your own builds
with your own key if you hand them out; the launcher pins its Show answer to the signing key.

## Tested on

Hisense SmartTV 4K FFM (lushan, MediaTek MT9676), Google TV 12 / Android 12: city search with the TV keyboard,
the weather page, the card prompt, and the card shown in the launcher's dock.

The options: Xiaomi TV (MiTV-MOOR2), Google TV 11, with `forecast_days` 7, `show_humidity` and `show_wind` on.

## Maintainer

[@Sumyabazar1](https://github.com/Sumyabazar1)

## DV Freedom 1.1: runtime settings and local sharing

**Weather settings** now offers Celsius, Fahrenheit or both units, three/seven forecast days,
humidity and wind. These override the build defaults only after Apply; Cancel preserves them.
Existing city and build defaults survive an update.

For an optional Home/screensaver reading:

1. Build/install the companion separately with a signer compatible with any existing installation.
2. Choose a city in Weather. No Android location permission is needed for this city search.
3. In Weather settings → Share with a launcher, select the compatible Home app you trust and review
   its package and certificate before allowing it. Only one selected launcher is trusted at a time.
4. In DV Freedom → Apps & rows → Weather display, choose the units for Home and the screensaver
   separately. Both start Off; enabling them grants no Android system permission.

The read-only `io.github.sumyabazar1.weather.snapshot/current` provider shares schema, Celsius
reading, city name, translated conditions and update timestamp. The caller's installed package and
current SHA-256 signing certificate must match the approval. Coordinates, calendar links and keys
are never exported by this provider. Network requests to Open-Meteo stay inside Weather.

Only an approved local query can trigger a background refresh: one worker, at most every 30 minutes,
with a five-minute failure backoff. There is no periodic service or boot receiver. The launcher queries
once a minute only while its Home/screensaver is active, cancels on exit and has no INTERNET permission.
Cached readings older than an hour are labelled; over six hours old or implausibly future readings
are hidden. Weather → Stop sharing revokes the package/certificate approval. Home and screensaver
choices are profile-local; sharing authorization stays device-local and is not included in AI drafts.

This bridge has source/unit/lint/build checks. The inherited device tests above do not establish
acceptance of the new sharing protocol, physical remote focus or screenshots on every Home style.
