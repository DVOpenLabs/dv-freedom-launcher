"""Small source-reviewed directory, not an installer or an endorsement of every future release."""
def catalogue():
    def entry(name, category, summary, path, platforms, access):
        return dict(name=name, category=category, summary=summary, url=path, platforms=platforms, access=access)
    base='https://github.com/DVOpenLabs/dv-freedom-launcher/tree/main/addons/'
    return dict(schema=1, note='Optional. Nothing is downloaded or installed here. Check the linked project and release before installing. Android companions do not run on native LG or Samsung.', items=[
        entry('Weather', 'Daily essentials', 'Forecast, editable units and an opt-in local Home/screensaver display.', base+'weather', ['android','streamer'], 'Internet for weather; explicit package-and-certificate approval for launcher sharing.'),
        entry('Sleep timer', 'Daily essentials', 'Set a timer from the remote without adding a permanent Home shelf.', base+'sleep-timer', ['android','streamer'], 'Optional accessibility service for standby and overlay access for its warning. HDMI-CEC behavior varies.'),
        entry('Quiet hours', 'Daily essentials', 'A scheduled volume cap that never raises quieter audio.', base+'quiet-hours', ['android','streamer'], 'Runs an explicit foreground service while enabled; may resume after boot. Audio behavior varies.'),
        entry('World clock', 'Daily essentials', 'Keep track of another time zone.', base+'world-clock', ['android','streamer'], 'Local clock; see its guide for options.'),
        entry('Frigate Glance', 'Home', 'Optional camera glance for an existing Frigate installation.', base+'frigate-glance', ['android','streamer'], 'Your camera endpoint and credentials stay in the companion. Not enabled by default.'),
        entry('Photo screensaver', 'Home', 'Use your own pictures and silent clips for an ambient screen.', base+'photo-screensaver', ['android','streamer'], 'Your selected media. Optional weather uses internet and coarse location permission; it starts off.'),
        entry('Jellyfin', 'Your media', 'Browse your own media server with its TV app.', 'https://github.com/jellyfin/jellyfin-androidtv', ['android','streamer'], 'Separate app and media server. Not bundled with DV Freedom.'),
        entry('Moonlight', 'Your media', 'Stream games from your own PC.', 'https://github.com/moonlight-stream/moonlight-android', ['android','streamer'], 'Separate app and paired host PC. Network and controller support vary.'),
        entry('VLC', 'Your media', 'Play your own local or network media.', 'https://code.videolan.org/videolan/vlc-android', ['android','streamer'], 'Separate app; media and network access are chosen there.'),
        entry('Webmote · phone remote', 'External companion', 'Use a paired phone browser as a remote and keyboard. Separate upstream app; not bundled.', 'https://github.com/maxhayim/webmote', ['android','streamer'], 'Opt-in accessibility service and local network server. Android 13+ has wider D-pad support; Android 11/12 controls are limited in some apps.'),
        entry('Game Day · calendar reminders', 'External companion', 'External calendar-based countdown app. DV Freedom’s own family/Game Day panel is available in the optional Home & Calendar companion.', 'https://github.com/Gathin23/cleanyourtv/tree/main/addons/game-day', ['android','streamer'], 'Internet for calendar URLs you provide. Subscription links can be secrets; never share them in logs or screenshots. Feeds are fetched only by the optional companion after you add and enable them.'),
    ])
