"""Read-only capability/recovery decisions. Unknown firmware never authorizes a Home helper."""
import re

def assess(platform, *, version='', developer_seconds=None, capabilities=None):
    if platform not in ('webos','tizen'):raise ValueError('Choose webos or tizen')
    capabilities=capabilities or {}
    allowed={'inventory','launch','inputs','exit','manualAcceptance','recoveryShortcut'}
    if set(capabilities)-allowed or any(type(v)is not bool for v in capabilities.values()):raise ValueError('Invalid capability report')
    if version and not re.fullmatch(r'\d{1,2}(?:\.\d{1,4}){0,3}',version):raise ValueError('Invalid firmware version')
    if developer_seconds is not None and (type(developer_seconds)is not int or not 0<=developer_seconds<=31536000):raise ValueError('Invalid developer session time')
    major=int(version.split('.')[0]) if version else None
    warnings=[]
    if major is None:warnings.append('Firmware is unknown; retain the manual launcher path.')
    if developer_seconds is not None and developer_seconds<3600:warnings.append('Developer session is expired or near expiry. Renew it in the vendor app; do not toggle Developer Mode off.')
    if not capabilities.get('recoveryShortcut'):warnings.append('An independent vendor Home/recovery shortcut has not been verified.')
    manual_ready=all(capabilities.get(k,False) for k in ('launch','exit','manualAcceptance'))
    if not manual_ready:warnings.append('Complete manual app launch, exit, physical remote and streaming playback checks first.')
    if platform=='webos' and major is not None and major<4:warnings.append('Legacy webOS needs the compatibility package; this app cannot accelerate LG Home or Settings.')
    # Public third-party app APIs do not establish permission or support for system Home interception.
    return dict(platform=platform,firmwareKnown=major is not None,manualReady=manual_ready,
                inventory=capabilities.get('inventory',False),inputs=capabilities.get('inputs',False),
                helperEligible=False,homeTakeover=False,warnings=warnings,
                nextStep='Use the manual app. Helper installation remains a separate version-specific review and owner choice.')
