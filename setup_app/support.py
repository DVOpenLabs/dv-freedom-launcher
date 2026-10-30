"""Shareable, allowlisted setup diagnostics. Never serialize the session or raw device records."""
import platform
from datetime import datetime, timezone


def report(session):
    release = session.installer.catalog.summary()
    inventory = session.inventory or {}
    api = inventory.get('api')
    checks = [
        dict(name='Trusted offline launcher', status='ready' if release.get('available') else 'needs-attention',
             next='Ready for installation review.' if release.get('available') else 'Use a signed DV Freedom Setup bundle. A source checkout alone cannot install the launcher.'),
        dict(name='TV connection', status='ready' if session.mode == 'device' and session.connection else 'not-checked',
             next='Connection details stay private.' if session.mode == 'device' and session.connection else 'Connect explicitly when ready. Preview mode does not test a TV.'),
        dict(name='Private device record', status='ready' if session.backup and session.mode == 'device' else 'not-checked',
             next='Keep the private device record and installation backup on this computer. They are not part of this report.'),
    ]
    # Every field below is selected deliberately. In particular, no exception text is shareable.
    return dict(schema=1, kind='dv-freedom-support', createdAt=datetime.now(timezone.utc).isoformat(),
                computerOS=platform.system(), mode='device' if session.mode == 'device' else 'preview',
                androidApi=api if type(api) is int and 26 <= api <= 100 and session.mode == 'device' else None,
                release=dict(available=bool(release.get('available')),
                             versionCode=release.get('versionCode') if release.get('available') else None),
                checks=checks, privacy='No addresses, names, paths, locations, app inventories, keys or raw logs. Review before sharing.')
