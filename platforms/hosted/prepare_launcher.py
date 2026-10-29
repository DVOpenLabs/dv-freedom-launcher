"""Stage our existing web presentation behind a VIDAA/Vizio companion adapter."""
from pathlib import Path
import json
from platforms.tizen.prepare_launcher import prepare as stage_tizen

SOURCE=Path(__file__).parent

def prepare(output, platform, media=None):
    if platform not in ('vidaa','vizio'):raise ValueError('Choose VIDAA or Vizio.')
    stage_tizen(output,media=media)
    folder=Path(output);name={'vidaa':'VIDAA','vizio':'Vizio'}[platform]
    # Reuse our maintained TV renderer, typography, D-pad settings, profiles and media lifecycle.
    # No third-party launcher or privileged platform implementation is copied.
    app=(folder/'app.js').read_text(encoding='utf-8')
    needle='native=!!(window.tizen&&window.tizen.application)'
    if needle not in app:raise ValueError('TV renderer entry changed; review hosted adaptation.')
    app=app.replace(needle,'native=true').replace('Samsung',name).replace('FreedomTizen','FreedomHosted').replace('DV-Freedom-Tizen','DV-Freedom-'+name)
    app=app.replace('No root, app removal, Home watcher, cloud account or network listener. Weather, portable Android/LG imports and automatic startup are not in this pilot.',
        'This hosted edition needs its paired local companion while in use. Only reviewed app launches are supported. No root, system changes, automatic Home return or cloud service. Weather and shared-profile import are not included.')
    (folder/'app.js').write_text(app,encoding='utf-8')
    core=(SOURCE/'core.js').read_text(encoding='utf-8').replace('__LABEL__',name).replace('__PLATFORM__',platform)
    (folder/'core.js').write_text(core,encoding='utf-8')
    html=(folder/'index.html').read_text(encoding='utf-8').replace("connect-src 'none'","connect-src 'self'").replace('Samsung',name)
    html=html.replace('<script src="app.js">','<script src="bridge.js"></script><script src="app.js">')
    (folder/'index.html').write_text(html,encoding='utf-8')
    (folder/'bridge.js').write_bytes((SOURCE/'bridge.js').read_bytes())
    (folder/'config.xml').unlink() # This is a hosted page, not a signed Samsung widget.
    return folder
