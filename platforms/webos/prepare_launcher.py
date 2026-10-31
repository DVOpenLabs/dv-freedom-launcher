#!/usr/bin/env python3
"""Stage the manual-launch webOS app in a NEW directory. Never connects, installs or changes Home."""
import argparse,json,re,shutil,struct,zlib
from pathlib import Path
SOURCE=Path(__file__).parent/'launcher'

def script_json(value):
    return json.dumps(value,ensure_ascii=True).replace('<','\\u003c').replace('>','\\u003e').replace('&','\\u0026')

def finalize(output,sdk_version=None):
    """WebKit 537 treats package file URLs as separate origins. Keep legacy code local."""
    output=Path(output)
    html=(SOURCE/'index.html').read_text(encoding='utf-8').replace('../../shared-web/core.js','shared-core.js')
    if sdk_version is not None and int(sdk_version.split('.')[0])==1:
        # CSP1 has no script hashes/nonces. Inline only the reviewed package sources;
        # do not allow file: scripts, remote code, eval, frames, media or connections.
        policy="default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src 'none'; media-src 'none'; connect-src 'none'; object-src 'none'; frame-src 'none'"
        html=re.sub(r'(?<=http-equiv="Content-Security-Policy" content=")[^"]*',lambda _:policy,html)
        css=(output/'style.css').read_text(encoding='utf-8')
        if '</style' in css.lower():raise ValueError('Unexpected style terminator in bundled CSS')
        html=html.replace('<link rel="stylesheet" href="style.css">','<style>'+css+'</style>')
        for name in ('media.js','bootstrap.js','shared-core.js','core.js','app.js'):
            code=(output/name).read_text(encoding='utf-8')
            # Generated JSON is escaped before writing bootstrap.js; fail closed
            # if an unexpected source could terminate its HTML script element.
            if '</script' in code.lower() or '<!--' in code:raise ValueError('Unexpected markup in bundled JavaScript')
            html=html.replace('<script src="'+name+'"></script>','<script>'+code+'</script>')
    (output/'index.html').write_text(html,encoding='utf-8')
def icon():
    # Original code-drawn F mark. No external artwork or binary payload in source.
    def pixel(x,y):
        foreground=20<=x<29 and 17<=y<64 or 20<=x<61 and 17<=y<26 or 20<=x<52 and 35<=y<44
        return bytes((199,231,255,255) if foreground else (17,29,48,255))
    raw=b''.join(b'\0'+b''.join(pixel(x,y) for x in range(80)) for y in range(80))
    def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
    return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',80,80,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw,9))+chunk(b'IEND',b'')
def prepare(output,profiles=None,sdk_version=None,inputs=None):
    if sdk_version is not None and (not isinstance(sdk_version,str) or not re.fullmatch(r'\d{1,2}\.\d{1,2}(?:\.\d{1,3})?',sdk_version)):
        raise ValueError('Use the sdkVersion reported by the paired TV')
    inputs=[] if inputs is None else inputs
    if not isinstance(inputs,list) or len(inputs)>16:raise ValueError('Use reviewed SDK input targets')
    seen=set()
    for entry in inputs:
        if not isinstance(entry,dict) or set(entry)!={'id','label'} or not isinstance(entry['id'],str) or not re.fullmatch(r'com\.webos\.app\.hdmi[1-9][0-9]?',entry['id']) or entry['id'] in seen or not isinstance(entry['label'],str) or not entry['label'].strip() or len(entry['label'])>64:
            raise ValueError('Use reviewed SDK input targets')
        seen.add(entry['id'])
    if profiles:
        import sys
        sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'tools'))
        import product_profiles
        doc=product_profiles.validate(json.loads(Path(profiles).read_text(encoding='utf-8-sig')))
        for p in doc['profiles']:product_profiles.export_profile(doc,p['id'],'webos')
    output=Path(output).resolve();output.mkdir(parents=True,exist_ok=False)
    for name in ('appinfo.json','index.html','style.css','core.js','app.js','media.js','bootstrap.js'):shutil.copyfile(SOURCE/name,output/name)
    shutil.copyfile(SOURCE.parents[1]/'shared-web/core.js',output/'shared-core.js')
    (output/'icon.png').write_bytes(icon())
    if profiles:(output/'bootstrap.js').write_text('window.FREEDOM_BOOTSTRAP='+script_json(doc)+';\n',encoding='utf-8')
    if inputs:
        with (output/'bootstrap.js').open('a',encoding='utf-8') as stream:stream.write('window.FREEDOM_INPUTS='+script_json(inputs)+';\n')
    if sdk_version is not None:
        with (output/'bootstrap.js').open('a',encoding='utf-8') as stream:stream.write('window.FREEDOM_PLATFORM='+script_json({'sdkVersion':sdk_version})+';\n')
        if int(sdk_version.split('.')[0])<4:
            info=json.loads((output/'appinfo.json').read_text(encoding='utf-8'));info['requiredACG']=['application.launcher']
            (output/'appinfo.json').write_text(json.dumps(info,indent=2)+'\n',encoding='utf-8')
    finalize(output,sdk_version)
    return output
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('output',type=Path);p.add_argument('--profiles',type=Path);p.add_argument('--sdk-version');p.add_argument('--inputs',type=Path,help='Private reviewed SDK-reported HDMI targets JSON');a=p.parse_args()
    try:print(prepare(a.output,a.profiles,a.sdk_version,json.loads(a.inputs.read_text(encoding='utf-8')) if a.inputs else None))
    except (ValueError,OSError) as error:p.exit(1,str(error)+'\n')
