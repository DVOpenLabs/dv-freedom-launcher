# DV Freedom Panchang

Optional offline Android TV calendar, adapted from the CleanYourTV Hindu Calendar add-on at
`7b7666ad05a3a62ba25768c51fbf06e9c4e67da5` (MIT, Gathin; see LICENSE). The astronomy,
regional calendar and upstream tests are retained. Build as the `:panchang` module with the launcher
Gradle wrapper; the release configuration uses the same signing convention as the other optional apps.

Choose English or Hindi, a listed city or a manual name, latitude, longitude and IANA time zone,
and one of five regional calendar conventions. Day and month views show tithi, nakshatra, sunrise
and sunset, with additional traditional readings. The devotional display is an optional foreground
view; Back closes it. It does not select or configure a system screensaver. Settings are stored locally.
The month and day views use the same location and time zone. At polar latitudes with no sunrise or
sunset, the app says so and uses local noon for the calendar reading. Positions are approximate and
regional practices may differ. In Month, **Observances** lists the dates on which Purnima,
Amavasya and Shukla or Krishna Ekadashi are present at sunrise (local noon where there is no
sunrise). Navigate to another regional month using Previous or Next before opening the list.
Add or remove your own festival or occasion using an ISO civil date and a title; the separately
labelled entries stay in private local app preferences. Verify dates against your regional calendar.
A tithi can span two sunrises or be absent from sunrise. These are astronomical markers, not
fasting dates. Major festivals are not predicted: their rules can depend on evening or night,
moonrise, regional tradition and other conditions. This is not a ceremony scheduling calendar.

There are no requested permissions, network access, analytics, overlays, receivers or services.
No location permission is used. Manual time zones must be valid IANA zone IDs; coordinates are
decimal degrees. Hindi translates interface labels; traditional limb names remain transliterated,
and regional month names retain their native scripts. Device and physical remote acceptance remain
pending.
