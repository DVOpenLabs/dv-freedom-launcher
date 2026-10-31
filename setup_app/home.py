"""Classify a recorded Home before using it as a recovery destination."""
import re
from .core import SetupError

_COMPONENT = re.compile(r'[A-Za-z][\w.]*/[A-Za-z.][\w.$]*')
_PLACEHOLDERS = frozenset({
    'android', 'com.google.android.tungsten.setupwraith',
    'com.android.settings', 'com.android.tv.settings', 'com.android.provision',
})

def stable_home(component):
    if not isinstance(component, str) or not _COMPONENT.fullmatch(component):
        return False
    package, activity = component.split('/', 1)
    if package in _PLACEHOLDERS:
        return False
    return not any(word in component.lower() for word in (
        'factory', 'hkcfac', 'resolveractivity', 'chooseractivity', 'fallbackhome',
    )) and activity.rsplit('.', 1)[-1].lower() != 'recoveryactivity'

def require_stable_home(component):
    if not stable_home(component):
        raise SetupError('The recorded Home is a recovery, setup or unavailable Home screen. '
                         'Select a working original Home in TV settings and review setup again. '
                         'No new operation was started.')
    return component
