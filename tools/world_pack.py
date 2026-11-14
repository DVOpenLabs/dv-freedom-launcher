#!/usr/bin/env python3
"""Build a portable offline world pack. No TV access, downloads, movie links or account data."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import tempfile
import os
import zipfile
import sys

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT))
from setup_app.cinema import validate
from setup_app.phone import validate_image
from setup_app.core import SetupError
SELF='p-00000000-0000-0000-0000-000000000000'
MAX=64*1024*1024

def build(source,output):
    source=Path(source).resolve();output=Path(output)
    if output.exists():raise ValueError('Output exists; choose a new filename')
    def read(name,limit):
        p=source/name
        if any(q.is_symlink() for q in [p,*list(p.parents)[:len(Path(name).parts)-1]]) or not p.is_file() or p.stat().st_size>limit:raise ValueError('Missing, linked or oversized pack file: '+name)
        return p.read_bytes()
    manifest=json.loads(read('manifest.json',65536));cinema=json.loads(read('cinema.json',60000));visual=json.loads(read('config.json',65536));credits=read('CREDITS.txt',65536)
    if set(manifest)!={'schema','id','name','version'} or type(manifest['schema'])is not int or manifest['schema']!=1 or not isinstance(manifest['id'],str) or not re.fullmatch('[a-z0-9][a-z0-9-]{0,47}',manifest['id']) or type(manifest['version'])is not int or not 1<=manifest['version']<=2147483647 or not isinstance(manifest['name'],str) or not manifest['name'].strip() or len(manifest['name'])>60 or any(ord(c)<32 or 127<=ord(c)<=159 for c in manifest['name']):raise ValueError('Invalid manifest')
    if not credits.decode('utf8').strip():raise ValueError('Include artwork provenance and sharing terms in CREDITS.txt')
    # Reuse the existing strict Cinema schema. A portable pack cannot carry private service URLs.
    validate(cinema)
    if cinema.get('titles') or cinema.get('taglines') or len(cinema.get('worlds',[]))!=1 or cinema['worlds'][0].get('wallpaper')!=SELF:raise ValueError('Use one self-referencing world; omit movie links and personal taglines')
    allowed={'wallpaper','background_color','dock_row','dock_title','always_show_names','reduced_motion','ambient_glow','app_glow','focus_overshoot','dock_glass','single_row','dock_band_all','columns','rows_below_dock','tile_gap','dock_opacity','wallpaper_dim','wallpaper_rotation_minutes','home_style','glass_style'}
    if not isinstance(visual,dict) or set(visual)-allowed:raise ValueError('Only visual settings belong in config.json')
    ranges={'columns':(4,7),'rows_below_dock':(0,3),'tile_gap':(0,32),'dock_opacity':(30,100),'wallpaper_dim':(0,90),'wallpaper_rotation_minutes':(0,1440)}
    for key,value in visual.items():
        if key in ranges:
            low,high=ranges[key]
            if type(value)is not int or not low<=value<=high:raise ValueError('Invalid visual number: '+key)
        elif key=='background_color':
            if not isinstance(value,str) or not re.fullmatch('#[a-fA-F0-9]{6}',value):raise ValueError('Invalid color')
        elif key=='glass_style':
            if value not in ('glass','rim','halo','double'):raise ValueError('Invalid focus style')
        elif key=='home_style':
            if value not in ('minimal','tv_grid','cinematic','easy_view','carousel'):raise ValueError('Invalid Home style')
        elif type(value)is not bool:raise ValueError('Invalid visual switch: '+key)
    files={'manifest.json':json.dumps(manifest,ensure_ascii=False).encode(),'config.json':json.dumps(visual).encode(),'cinema.json':json.dumps(cinema,ensure_ascii=False).encode(),'CREDITS.txt':credits,'wallpaper.jpg':read('wallpaper.jpg',8*1024*1024)}
    if (source/'preview.jpg').exists():files['preview.jpg']=read('preview.jpg',8*1024*1024)
    if (source/'video.mp4').exists():files['video.mp4']=read('video.mp4',48*1024*1024)
    for name in ('wallpaper.jpg','preview.jpg'):
        if name not in files:continue
        kind='png' if files[name].startswith(b'\x89PNG\r\n\x1a\n') else 'jpg' if files[name].startswith(b'\xff\xd8') else ''
        if not kind:raise ValueError('Still fallback must be JPEG or PNG')
        validate_image(source/name,kind)
    hashes=set()
    def scan(value):
        if isinstance(value,str) and re.fullmatch('[a-f0-9]{64}',value):hashes.add(value)
        elif isinstance(value,dict):
            for v in value.values():scan(v)
        elif isinstance(value,list):
            for v in value:scan(v)
    scan(cinema)
    for digest in hashes:
        name='art/'+digest+'.png';raw=read(name,2*1024*1024)
        if hashlib.sha256(raw).hexdigest()!=digest or not raw.startswith(b'\x89PNG\r\n\x1a\n'):raise ValueError('Artwork hash or PNG mismatch')
        files[name]=raw
    if len(files)>64 or sum(map(len,files.values()))>MAX:raise ValueError('Pack exceeds the existing theme quota')
    output.parent.mkdir(parents=True,exist_ok=True);fd,temp=tempfile.mkstemp(prefix='.world-',dir=output.parent);os.close(fd)
    try:
        with zipfile.ZipFile(temp,'w',zipfile.ZIP_DEFLATED)as z:
            for name,raw in sorted(files.items()):z.writestr(name,raw)
        if Path(temp).stat().st_size>MAX:raise ValueError('Compressed pack exceeds theme quota')
        # Exclusive destination publication; never overwrite an existing pack.
        if os.name=='nt':os.rename(temp,output)  # Windows refuses an existing destination.
        else:os.link(temp,output)
    finally:Path(temp).unlink(missing_ok=True)
    return {'name':manifest['name'],'files':len(files),'bytes':output.stat().st_size,'sha256':hashlib.sha256(output.read_bytes()).hexdigest()}

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('source',type=Path);p.add_argument('output',type=Path);a=p.parse_args()
    try:print(json.dumps(build(a.source,a.output),indent=2))
    except (ValueError,OSError,KeyError,TypeError,SetupError)as error:p.exit(2,str(error)+'\n')
