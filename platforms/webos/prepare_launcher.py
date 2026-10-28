#!/usr/bin/env python3
"""Stage the manual-launch webOS app in a NEW directory. Never connects, installs or changes Home."""
import argparse,json,shutil,struct,zlib
from pathlib import Path
SOURCE=Path(__file__).parent/'launcher'
def icon():
    # Original code-drawn F mark. No external artwork or binary payload in source.
    def pixel(x,y):
        foreground=20<=x<29 and 17<=y<64 or 20<=x<61 and 17<=y<26 or 20<=x<52 and 35<=y<44
        return bytes((199,231,255,255) if foreground else (17,29,48,255))
    raw=b''.join(b'\0'+b''.join(pixel(x,y) for x in range(80)) for y in range(80))
    def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
    return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',80,80,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw,9))+chunk(b'IEND',b'')
def prepare(output,profiles=None):
    if profiles:
        import sys
        sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'tools'))
        import product_profiles
        doc=product_profiles.validate(json.loads(Path(profiles).read_text(encoding='utf-8-sig')))
        for p in doc['profiles']:product_profiles.export_profile(doc,p['id'],'webos')
    output=Path(output).resolve();output.mkdir(parents=True,exist_ok=False)
    for name in ('appinfo.json','index.html','style.css','core.js','app.js','media.js','bootstrap.js'):shutil.copyfile(SOURCE/name,output/name)
    (output/'icon.png').write_bytes(icon())
    if profiles:(output/'bootstrap.js').write_text('window.FREEDOM_BOOTSTRAP='+json.dumps(doc,ensure_ascii=True,separators=(',',':'))+';\n',encoding='utf-8')
    return output
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('output',type=Path);p.add_argument('--profiles',type=Path);a=p.parse_args()
    try:print(prepare(a.output,a.profiles))
    except (ValueError,OSError) as error:p.exit(1,str(error)+'\n')
