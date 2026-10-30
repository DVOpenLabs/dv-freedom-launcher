"""User-facing optional-tool directory. A guide is never an installation authority."""
from copy import deepcopy

BASE = 'https://github.com/DVOpenLabs/dv-freedom-launcher/tree/main/addons/'

def entry(id, name, category, summary, access, *, source=None, mode='source', needs='Android TV / Google TV', background='Only while open', recovery='Close the app to return to Home. Installation does not select a screensaver or change Home.'):
    return dict(id=id, name=name, category=category, summary=summary, access=access,
                source=source or BASE+id, distribution=mode, requirements=needs,
                background=background, recovery=recovery, platforms=['android','streamer'])

_ITEMS = [
 entry('weather','Weather','Everyday','Forecast and optional Home/screensaver weather sharing.','Internet; approve sharing separately in Weather.',mode='signed'),
 entry('sleep-timer','Sleep timer','Everyday','A timer you can set with your remote.','Optional exact alarms, overlays and standby Accessibility.',mode='signed',background='An explicitly started timer'),
 entry('world-clock','World clock','Everyday','Local time zones and a clock screensaver.','No Android permissions.',mode='signed'),
 entry('quiet-hours','Quiet hours','Everyday','A scheduled volume cap that never raises quieter audio.','Foreground service and boot receiver; configure a schedule first.',mode='signed',background='An enabled schedule can resume after restart'),
 entry('companions','DV Freedom Companions','Home & media','Discover, watchlist, photos, Home Assistant, family calendars and reviewed AI setup drafts.','Internet; no connections or provider calls until configured.',source='https://github.com/DVOpenLabs/dv-freedom-launcher/tree/main/launcher/companions',mode='signed'),
 entry('phone-remote','Phone remote & keyboard','Setup & support','Pair a phone browser for remote commands and keyboard input.','Internet, foreground service and optional Accessibility. Server starts off.',source='https://github.com/DVOpenLabs/dv-freedom-launcher/tree/main/launcher/remote',mode='signed',background='Only when you explicitly start the local server'),
 entry('camera','DV Freedom Camera','Home & media','Silent in-app camera preview and an explicitly started corner window.','Internet; overlay, foreground playback and notification posting are separate choices. No boot or Accessibility.',source='https://github.com/DVOpenLabs/dv-freedom-launcher/tree/main/launcher/camera',mode='signed',needs='HTTPS HLS/MP4 or private-LAN RTSP camera; firmware/decoder support varies',background='Only explicitly started window; stops on screen-off or timeout, at most 15 minutes',recovery='Use Stop camera window or its Stop notification. Forget removes the configured source/credentials.'),
 entry('tv-inspector','TV Inspector','Setup & support','Remote keycodes, display modes and media capabilities.','No Android permissions. Diagnostic data stays on the TV.',mode='signed'),
 entry('bluetooth-connect','Bluetooth Connect','Audio','List paired speakers/headphones; firmware-dependent connect/disconnect.','Nearby devices permission is a separate choice. No automatic pairing.',mode='signed'),
 entry('digital-rain','Digital Rain','Ambient','A local animated scene and optional screensaver.','No Android permissions or network.',mode='signed'),
 entry('news-screensaver','News & RSS','Ambient','Your HTTPS RSS/Atom feeds and article QR codes.','Internet; starts without feeds. No fetch until configured and open.',mode='signed'),
 entry('headphones','Headphones','Audio','Audio route, reported battery and an optional disconnect pause guard.','Nearby devices; guard starts off. Fixed-volume outputs may not mute.',mode='signed',background='If enabled, a Bluetooth broadcast receiver handles disconnects; no permanent service'),
 entry('capacity-stats','Device capacity','Setup & support','Storage and memory information, without a cleaner or booster.','No Android permissions.',mode='signed'),
 entry('tv-hub','TV Hub','Home & media','A guide/player for your M3U/XMLTV, HDHomeRun or Dispatcharr sources.','Internet and your configured sources. Does not replace STB EMU portals.',mode='signed',needs='Your own compatible TV source; English interface'),
 entry('ad-blocking','Ad Blocking','Home & media','Pi-hole/AdGuard Home status and a five-minute pause action.','Internet and blocker credentials. Not YouTube ad removal.',needs='An existing Pi-hole or AdGuard Home server'),
 entry('airplay-status','AirPlay Status','Setup & support','Inspect receiver discovery and network status.','Internet and network state. This is not an AirPlay receiver.'),
 entry('bluetooth-toggle','Bluetooth Toggle','Audio','Open Bluetooth controls where firmware permits.','Bluetooth permissions. Android 13+ generally requires the system Accessories screen.'),
 entry('brightness','Brightness','Specialist','Optional software dimmer for supported devices.','Overlay/foreground/boot components; some OEM functions affect picture settings.',background='Optional dimming service',recovery='Turn the dimmer off in its app. Software dimming is not panel backlight control.'),
 entry('flock','Flock','Ambient','An offline flocking scene.','No Android permissions.'),
 entry('frigate-glance','Frigate Glance','Home & media','Camera and doorbell glance for your Frigate installation.','Internet, overlays, foreground service and optional Accessibility.',needs='Your Frigate server and credentials',background='Configured camera/doorbell monitoring only'),
 entry('game-day','Game Day','Calendars','Calendar-based sports countdowns; also available in DV Companions.','Internet and an explicitly supplied calendar link. Not live scores.',source='https://github.com/Gathin23/cleanyourtv/tree/main/addons/game-day',mode='guide'),
 entry('haunted','Haunted','Ambient','An optional seasonal scene.','No Android permissions.',source='https://github.com/Gathin23/cleanyourtv/tree/main/addons/haunted',mode='guide'),
 entry('jewish-calendar','Jewish calendar','Calendars','An offline Hebrew/Jewish calendar.','No Android permissions.'),
 entry('nepali-calendar','Nepali calendar','Calendars','An offline regional calendar.','No Android permissions.',source='https://github.com/Gathin23/cleanyourtv/tree/main/addons/nepali-calendar',mode='guide'),
 entry('photo-prints','Photo Prints','Ambient','Personal photo and date-aware presentation.','Your selected media; no manifest permissions.'),
 entry('photo-screensaver','Photo screensaver','Ambient','Personal photos and silent clips.','Internet and optional coarse location for its own weather. DV photo ambient does not need location.'),
 entry('prayer-times','Prayer times','Calendars','Configurable prayer schedules and reminders.','Version-dependent alarms, overlay and boot/wake access. Review the exact build.',background='Only configured reminders'),
 entry('remote-access','Remote Access','Specialist','Debugging status and manual recovery controls.','Network-state and secure-settings access. Never turn off debugging while someone is providing support.'),
 entry('scoreboard','App-use statistics','Specialist','Local app-use/watchtime statistics.','Usage access reveals app habits. Not a sports scoreboard.',source='https://github.com/Gathin23/cleanyourtv/tree/main/addons/scoreboard',mode='guide'),
 entry('smart-home','Smart Home','Home & media','Home Assistant controls; also available in DV Companions.','Internet and your Home Assistant address/token.',needs='Your Home Assistant server'),
 entry('sources','Sources','Everyday','Input shortcuts; DV already includes an Input picker.','No permissions. An external Streamer cannot list its television’s HDMI inputs.'),
 entry('startup-branding','Startup welcome','Ambient','An optional welcome screen.','No permissions. This does not replace a firmware boot logo.'),
 entry('tv-static','TV Static','Ambient','Optional retro static effect.','Overlays, foreground service and boot receiver. Review sound and wake behaviour.',background='Optional configured effect'),
 entry('wake-app','Wake app','Everyday','Open a selected app after the display wakes.','Overlay, foreground service and boot receiver. Different from once-per-boot startup.',source='https://github.com/Gathin23/cleanyourtv/tree/main/addons/wake-app',mode='guide',background='Only after explicitly enabling a wake rule'),
]

_EXTERNAL = [
 ('dvd','DVD Video','Ambient','A familiar offline screensaver.','No Android permissions.','TheWombRaider/dvd-tv'),
 ('pipes','3D Pipes','Ambient','An offline pipes screensaver.','No Android permissions.','TheWombRaider/pipes-tv'),
 ('weather-clock','Weather Clock','Ambient','An alternative forecast/clock screensaver.','Internet; its own weather/location-source choices. DV weather is already available.','matyivagyok/weatherclock-tv'),
 ('homelab','Homelab Companion','Home & media','Immich ambient photos and optional media-server controls.','Internet and server credentials; server actions/settings require separate review.','epochcoder/cleanyourtv/tree/main/companion'),
 ('corner-player','Corner Player','Home & media','Personal RTSP/HLS picture-in-picture streams.','Internet, overlay and foreground services. HDMI/decoder support varies.','maxhayim/corner-player'),
 ('kids-mode','Kids Mode','Family','A restricted app chooser with an optional Accessibility guard.','Home role and Accessibility. Not tamper-proof parental controls.','maxhayim/kids-mode'),
 ('quickvol','QuickVol TV','Audio','Editable remote-button volume presets.','Accessibility and secure-settings access; reference app can enable its key service on open.','thenewnano/quickvol-google-tv'),
 ('forslag','Forslag','Home & media','Provider-aware catalogue browsing.','Internet and provider/API keys. Availability and links vary by region.','modist/forslag'),
 ('offline-voice','NoGmsVoice','Specialist','Optional offline recognition for compatible speech-service clients.','Microphone, package visibility and large speech models. Not an AI assistant or universal voice-button integration.','MrPitonych/NoGmsVoice'),
 ('castfix','Cast Fix','Specialist','Recovery reference for particular broken casting configurations.','Stores an ADB key and changes debugging/stock components. Not needed for normal DV Home routing.','thayford/castfix'),
 ('coocaa-shim','Coocaa Home shim','Specialist','A device-specific Home compatibility workaround.','OEM Home/boot changes. Do not apply to unrelated TVs.','VincentYe123/coocaa-home-shim'),
 ('airplay-receiver','AirPlay receiver','Home & media','Apple-device mirroring/audio through a separate receiver.','Separate GPL-3 receiver; no FairPlay-protected streaming. Installer defaults need review.','nguyenduchoai/tv-vibehack/tree/feat/airplay-receiver/airplay'),
 ('aerial-bridge','Aerial Xiaomi bridge','Specialist','Home-launch compatibility for certain Xiaomi firmware.','Competes for the Home role. Requires Aerial and exact-model review.','hhhwl/cleanyourtv/tree/codex/aerial-startup-bridge'),
 ('art-frame','Art Frame','Ambient','Art mats, framing and credited artwork.','Internet, system wallpaper, overlay and service access in the reference app. Advanced 4K/OEM paths vary.','cgogoel/cleanyourtv/tree/art-frame/addons/art-frame'),
]
_ITEMS += [entry(id,name,category,summary,access,source='https://github.com/'+repo,mode='guide',needs='Separate project; review its current release, licence and device requirements',background='Check the linked guide before enabling services') for id,name,category,summary,access,repo in _EXTERNAL]

def items():
    result=deepcopy(_ITEMS)
    for item in result:
        if item['id'] in {'ad-blocking','flock','jewish-calendar','nepali-calendar','prayer-times'}:item['distribution']='signed'
        if item['id']=='nepali-calendar':item['source']=BASE+'nepali-calendar'
        if item['id']=='prayer-times':
            item['summary']='Prayer times from your chosen location, in a separate optional app.'
            item['access']='Internet for schedule data. This build has no alarm, overlay or boot scheduler.'
            item['background']='Only while visible'
    return result
