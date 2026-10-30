# Prayer Times for DV Freedom

An optional standalone foreground prayer-time panel, adapted from the upstream MIT add-on. Open
**Location & calculation method**, enter city/country and an Aladhan method ID, then Save & fetch.
It starts unconfigured and makes no request until you save a source. The original build-time location
resources are not used for network defaults in this edition.

The app shows five prayers and sunrise, Gregorian/Hijri dates and the next prayer’s countdown in the
selected city’s provider-reported timezone. It sends that city/country, date and method to Aladhan over
HTTPS; no account, GPS, telemetry, alarms, overlays or boot service. Requests stop on leaving, with
bounded timeout/response size; an in-flight request may finish but its stale UI result is discarded.
Use Disconnect to forget the source. Check the calculation method with your local mosque; method 3
is Muslim World League. These are advisory times, not guaranteed religious guidance.

Install through desktop Setup’s signed Add-on Centre after review. Installing does not add a Home
shortcut, change system time or configure a source. From source, use JDK17/SDK34:

```sh
../../launcher/gradlew -p . --no-daemon testReleaseUnitTest lintRelease assembleRelease
```

The module metadata is retained for compatible launchers; DV Freedom also opens it as an ordinary
optional app. Original authorship and MIT licensing remain in the source and root notices.
