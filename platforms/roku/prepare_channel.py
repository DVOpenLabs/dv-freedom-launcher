"""Build a self-contained Roku ambient app. No ECP, installation or device access."""
from pathlib import Path
import argparse
import hashlib
import json
import re
import zipfile

SOURCE = Path(__file__).with_name('channel')

def prepare(output, theme='midnight', clock='12h'):
    if theme not in ('midnight','ocean','forest') or clock not in ('12h','24h'):
        raise ValueError('Choose a supported background and clock.')
    output = Path(output)
    if output.exists() or output.is_symlink() or output.suffix != '.zip':
        raise ValueError('Choose a new .zip path. Existing packages are never overwritten.')
    output.parent.mkdir(parents=True, exist_ok=True)
    files = {str(p.relative_to(SOURCE)).replace('\\','/'):p.read_bytes() for p in SOURCE.rglob('*') if p.is_file()}
    expected={'manifest','source/main.brs','components/FreedomScene.xml','components/FreedomScene.brs'}
    if set(files)!=expected:raise ValueError('Unexpected Roku source files.')
    files['LICENSE']= (SOURCE.resolve().parents[2]/'LICENSE').read_bytes()
    files['DVOpenLabs.txt']=b'DV Freedom Ambient: Copyright (c) 2026 DVOpenLabs. MIT terms apply; see LICENSE. No network control, vendor code or media is bundled.\n'
    # Fixed enumerations, never interpolate user text into BrightScript.
    color={'midnight':'0x101C2CFF','ocean':'0x123A51FF','forest':'0x173B31FF'}[theme]
    files['components/FreedomScene.brs']=files['components/FreedomScene.brs'].replace(b'__COLOR__',color.encode()).replace(b'__CLOCK24__',str(clock=='24h').lower().encode())
    with zipfile.ZipFile(output,'x',compression=zipfile.ZIP_DEFLATED) as z:
        for name,data in sorted(files.items()):z.writestr(name,data)
    return {'platform':'roku','kind':'ambient','file':str(output),'bytes':output.stat().st_size,
        'sha256':hashlib.sha256(output.read_bytes()).hexdigest(),'installed':False,
        'limitations':['No app launching, HDMI switching, Home replacement or automatic startup.',
            'Manual sideload replaces the single development-app slot. Review that slot first.',
            'No credentials, network calls, ECP commands or personal media are bundled.']}

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('output');p.add_argument('--theme',default='midnight');p.add_argument('--clock',default='12h')
    a=p.parse_args();print(json.dumps(prepare(a.output,a.theme,a.clock),indent=2))
