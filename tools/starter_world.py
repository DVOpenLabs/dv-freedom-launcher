#!/usr/bin/env python3
"""Generate an original, shareable still-world example. No downloads, AI calls or TV changes."""
import argparse
import hashlib
import json
from pathlib import Path
import struct
import zlib
from world_pack import build, SELF

def png(width,height,pixel):
    def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
    rows=bytearray()
    for y in range(height):
        rows.append(0)
        for x in range(width):rows.extend(pixel(x,y))
    return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',width,height,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(bytes(rows),9))+chunk(b'IEND',b'')

def create(folder,world='hobbit'):
    if world not in ('hobbit','hogwarts'):raise ValueError('Choose hobbit or hogwarts')
    folder=Path(folder);folder.mkdir(parents=True,exist_ok=False);(folder/'art').mkdir()
    def landscape(x,y):
        if y>580+(x%600-300)**2//1800:return (18,47,44)
        if y>480+(x%900-450)**2//2100:return (34,68,70)
        if y<540 and (x*37+y*101)%1771==0:return (233,216,167)
        return (12+y//28,22+y//24,47+y//18)
    still=png(1280,720,landscape);(folder/'wallpaper.jpg').write_bytes(still)
    def art(raw):
        digest=hashlib.sha256(raw).hexdigest();(folder/'art'/(digest+'.png')).write_bytes(raw);return digest
    covers=[]
    for i in range(3 if world=='hobbit' else 8):
        def cover(x,y,i=i):
            if x<18 or x>302 or y<18 or y>462:return (173,153,99)
            if (x-160)**2+(y-220)**2<3200:return (180+i*6,165,110)
            return (22+i*5,42+i*3,53+i*5)
        covers.append(art(png(320,480,cover)))
    manifest=dict(schema=1,id='original-'+world,name='Original twilight · '+world,version=1)
    scene=dict(schema=1,worldsEnabled=True,scene='fireflies',worlds=[dict(id=world,wallpaper=SELF,covers=covers)])
    for name,value in [('manifest.json',manifest),('config.json',{'wallpaper':True}),('cinema.json',scene)]:
        (folder/name).write_text(json.dumps(value,indent=2),encoding='utf8')
    terms=(Path(__file__).resolve().parents[1]/'LICENSE').read_text(encoding='utf8')
    (folder/'CREDITS.txt').write_text('Original procedural landscape and abstract cover placeholders by DVOpenLabs, under the MIT terms below. No movie art, logos, video or licensed film content. The world identifier chooses the existing launcher navigation; these are not official book covers.\n\n'+terms,encoding='utf8')
    return folder

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('folder',type=Path);p.add_argument('output',type=Path);p.add_argument('--world',choices=['hobbit','hogwarts'],default='hobbit');a=p.parse_args()
    print(json.dumps(build(create(a.folder,a.world),a.output),indent=2))
