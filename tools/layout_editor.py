#!/usr/bin/env python3
"""Local app-layout editor. Standard-library server, explicit TV target, preview/apply/guarded Undo.

python tools/layout_editor.py --demo
python tools/layout_editor.py --tv <tv-ip> --package com.gathin.tvhome
App position is an optional, profile-scoped choice. No TV theme files, media, permissions, services or system settings are written by this tool.
"""
from __future__ import annotations
import argparse
import copy
from datetime import datetime, timezone
import hashlib
import hmac
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import os
from pathlib import Path
import re
import secrets
import shlex
import subprocess
import sys
import threading
import webbrowser

ROOT=Path(__file__).resolve().parents[1]
WEB=Path(__file__).with_name('layout_editor_web')
PACKAGE=re.compile(r'[A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)+')
MAX_BYTES=32768

def unique_object(pairs):
    result={}
    for key,value in pairs:
        if key in result:raise ValueError('Repeated JSON field')
        result[key]=value
    return result

def loads(text):
    if len(text.encode('utf-8'))>65536:raise ValueError('Request is too large')
    return json.loads(text,object_pairs_hook=unique_object,parse_constant=lambda _: (_ for _ in ()).throw(ValueError('Invalid number')))

def encode(value):return json.dumps(value,ensure_ascii=False,separators=(',',':'),sort_keys=True)

def validate_layout(value):
    if not isinstance(value,dict) or set(value)!={'schema','favorites','hidden','labels'} or type(value['schema']) is not int or value['schema']!=1:
        raise ValueError('Unsupported layout format')
    for name,maximum in [('favorites',32),('hidden',256)]:
        items=value[name]
        if not isinstance(items,list) or len(items)>maximum or (name=='favorites' and not items):raise ValueError('Choose 1–32 favorites')
        if any(not isinstance(p,str) or len(p)>200 or not PACKAGE.fullmatch(p) for p in items):raise ValueError('Invalid app package')
        if len(set(items))!=len(items):raise ValueError('An app is listed more than once')
    if set(value['favorites'])&set(value['hidden']):raise ValueError('A favorite cannot also be hidden')
    labels=value['labels']
    if not isinstance(labels,dict) or len(labels)>64:raise ValueError('Too many custom app names')
    for package,label in labels.items():
        if not PACKAGE.fullmatch(package) or len(package)>200:raise ValueError('Invalid app package')
        if not isinstance(label,str) or not label.strip() or len(label.encode('utf-16-le'))//2>60 or any(ord(c)<32 or 127<=ord(c)<160 for c in label):
            raise ValueError('App names must be 1–60 characters without control characters')
    if len(encode(value).encode('utf-8'))>MAX_BYTES:raise ValueError('Layout is too large')
    return copy.deepcopy(value)

def changes(before,after):
    result=[]
    if before['favorites']!=after['favorites']:result.append('Update favorite apps and their order')
    if set(before['hidden'])!=set(after['hidden']):result.append('Update which app tiles are hidden')
    keys=set(before['labels'])|set(after['labels'])
    count=sum(before['labels'].get(k)!=after['labels'].get(k) for k in keys)
    if count:result.append(f'Change {count} app name'+('s' if count!=1 else ''))
    return result

class NativeBridge:
    def __init__(self,host,package,repo=ROOT,runner=subprocess.run):
        if not re.fullmatch(r'[A-Za-z0-9_.:\[\]-]{1,253}',host) or host.startswith('-'):raise ValueError('Invalid TV address')
        if not PACKAGE.fullmatch(package):raise ValueError('Invalid launcher package')
        self.host,self.package,self.repo,self.runner=host,package,Path(repo),runner
    def call(self,method,request=None):
        if method not in {'snapshot','apply','undo'}:raise ValueError('Unknown editor action')
        command='content call --uri '+shlex.quote('content://'+self.package+'.theme')+' --method product_layout_'+method
        if request is not None:
            raw=encode(request)
            if len(raw.encode('utf-8'))>MAX_BYTES:raise ValueError('Editor request is too large')
            command+=' --arg '+shlex.quote(raw)
        env=os.environ.copy();env['TV_HOST']=self.host;env['PYTHONIOENCODING']='utf-8'
        candidate=self.repo/'tools/venv'/('Scripts/python.exe' if os.name=='nt' else 'bin/python')
        python=str(candidate) if candidate.is_file() else sys.executable
        result=self.runner([python,str(self.repo/'tools/tv.py'),command],env=env,capture_output=True,text=True,
            encoding='utf-8',errors='replace',timeout=35,creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
        output=result.stdout
        marker=output.find('json=')
        if result.returncode or marker<0:
            detail=(result.stderr or '')+output
            if 'Unlock launcher editing' in detail:raise ValueError('Unlock layout editing on the TV before using the desktop editor.')
            for phrase in ['The launcher changed','Undo is no longer current','Finish the on-TV layout edit','Favorite app is unavailable','Editor request is too large','Could not save layout']:
                if phrase in detail:raise ValueError(phrase+'. Reload the TV layout before trying again.')
            raise ValueError('Could not read the editor bridge. Check the TV address, authorized debugging and that this fork\'s editor build is installed.')
        state,_=json.JSONDecoder(object_pairs_hook=unique_object).raw_decode(output[marker+5:])
        if state.get('schema')!=1:raise ValueError('Unsupported TV editor bridge')
        validate_layout(state['layout'])
        return state

class DemoBridge:
    def __init__(self):
        names=['STB EMU','YouTube','Netflix','Crunchyroll','CNN','Fandango','Play Store','Settings']
        packages=['org.example.app'+str(i) for i in range(8)]
        self.state={'schema':1,'profile':'','layout':{'schema':1,'favorites':packages,'hidden':[],'labels':{}},
            'apps':[{'package':p,'label':n} for p,n in zip(packages,names)],'canUndo':False,'position':None,
            'header':{'available':False,'effectivePosition':'top','inputs':True,'settings':'Launcher settings'}}
        self.counter=0;self.old=None;self.old_position=None;self._revision()
    def _revision(self):self.state['revision']=hashlib.sha256((encode(self.state['layout'])+str(self.state['position'])+str(self.counter)).encode()).hexdigest()
    def call(self,method,request=None):
        if method=='snapshot':return copy.deepcopy(self.state)
        if method not in {'apply','undo'}:raise ValueError('Unknown editor action')
        if request['revision']!=self.state['revision'] or request['profile']!=self.state['profile']:raise ValueError('The launcher changed. Reload and review your draft again.')
        if method=='apply':
            self.old=copy.deepcopy(self.state['layout']);self.old_position=self.state['position']
            self.state['layout']=validate_layout(request['layout']);self.state['canUndo']=True
            if 'position' in request:self.state['position']=request['position']
        else:
            if not self.state['canUndo']:raise ValueError('Nothing to undo')
            self.state['layout']=self.old;self.state['position']=self.old_position;self.state['canUndo']=False
        self.counter+=1;self._revision();return copy.deepcopy(self.state)

class Editor:
    def __init__(self,bridge,label,backups):
        self.bridge,self.label,self.backups=bridge,label,Path(backups)
        self.lock=threading.RLock();self.reviewed=None
    def state(self):
        state=self.bridge.call('snapshot');state['target']=self.label;return state
    def request(self,value):
        if not isinstance(value,dict) or set(value) not in ({'revision','profile','layout'},{'revision','profile','layout','position'}):raise ValueError('Unsupported editor request')
        if not isinstance(value['revision'],str) or not re.fullmatch(r'[a-f0-9]{64}',value['revision']):raise ValueError('Reload the TV first')
        if not isinstance(value['profile'],str) or not re.fullmatch(r'(?:[a-z][a-z0-9_-]{0,63})?',value['profile']):raise ValueError('Invalid profile')
        if 'position' in value and value['position'] not in (None,'top','middle','bottom'):raise ValueError('Unknown app position')
        value=copy.deepcopy(value);value['layout']=validate_layout(value['layout']);return value
    def preview(self,value):
        with self.lock:
            value=self.request(value);current=self.state()
            if value['revision']!=current['revision'] or value['profile']!=current['profile']:raise ValueError('The launcher changed. Reload and review your draft again.')
            summary=changes(current['layout'],value['layout'])
            if 'position' in value and value['position']!=current.get('position'):
                summary.append('Move apps to '+value['position'] if value['position'] else 'Restore the theme app position')
            available={app['package'] for app in current['apps']}
            if any(package not in available for package in value['layout']['favorites']):raise ValueError('A favorite app is unavailable. Remove it from the draft first.')
            self.reviewed=hashlib.sha256(encode(value).encode()).hexdigest() if summary else None
            return {'target':self.label,'changes':summary,'canApply':bool(summary)}
    def _backup(self,state):
        self.backups.mkdir(parents=True,exist_ok=True)
        path=self.backups/(datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')+'-'+secrets.token_hex(4)+'.json')
        with path.open('x',encoding='utf-8') as stream:
            json.dump({'schema':1,'capturedAt':datetime.now(timezone.utc).isoformat(),'target':self.label,'snapshot':state},stream,ensure_ascii=False,indent=2)
        return path
    def apply(self,value):
        with self.lock:
            value=self.request(value)
            if self.reviewed!=hashlib.sha256(encode(value).encode()).hexdigest():raise ValueError('Preview these exact changes first')
            before=self.state()
            if value['revision']!=before['revision'] or value['profile']!=before['profile']:raise ValueError('The launcher changed. Reload and review your draft again.')
            backup=self._backup(before)
            state=self.bridge.call('apply',value);self.reviewed=None;state['target']=self.label
            return {'state':state,'backup':str(backup)}
    def undo(self,value):
        with self.lock:
            value=self.request(value);before=self.state()
            if not before['canUndo'] or before['revision']!=value['revision'] or before['profile']!=value['profile']:raise ValueError('Undo is no longer current. Reload first.')
            backup=self._backup(before);state=self.bridge.call('undo',value);self.reviewed=None;state['target']=self.label
            return {'state':state,'backup':str(backup)}

def server(editor,port=0,wallpaper=None):
    token=secrets.token_urlsafe(32)
    class Handler(BaseHTTPRequestHandler):
        def setup(self):
            super().setup();self.connection.settimeout(5)
        def log_message(self,*args):pass
        def headers_for(self,status,mime,size):
            self.send_response(status);self.send_header('Content-Type',mime);self.send_header('Content-Length',str(size))
            self.send_header('Cache-Control','no-store');self.send_header('X-Content-Type-Options','nosniff')
            self.send_header('X-Frame-Options','DENY');self.send_header('Referrer-Policy','no-referrer')
            self.send_header('Content-Security-Policy',"default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'")
            self.end_headers()
        def send_json(self,value,status=200):
            data=encode(value).encode();self.headers_for(status,'application/json; charset=utf-8',len(data));self.wfile.write(data)
        def allowed(self,api=False):
            origin='http://127.0.0.1:'+str(self.server.server_port)
            if self.headers.get('Host')!='127.0.0.1:'+str(self.server.server_port):self.send_json({'error':'Invalid host'},403);return False
            if self.headers.get('Origin') not in (None,origin):self.send_json({'error':'Invalid origin'},403);return False
            if api and not hmac.compare_digest(self.headers.get('X-Editor-Token',''),token):self.send_json({'error':'Reload the editor page'},403);return False
            return True
        def do_GET(self):
            if not self.allowed(self.path.startswith('/api/')):return
            try:
                if self.path=='/api/state':self.send_json(editor.state());return
                files={'/':('index.html','text/html; charset=utf-8'),'/editor.css':('editor.css','text/css; charset=utf-8'),'/editor.js':('editor.js','text/javascript; charset=utf-8')}
                if self.path=='/wallpaper' and wallpaper:
                    file=Path(wallpaper);data=file.read_bytes()
                    if len(data)>8*1024*1024 or file.suffix.lower() not in ('.png','.jpg','.jpeg','.webp'):raise ValueError('Unsupported preview image')
                    mime='image/png' if file.suffix.lower()=='.png' else 'image/webp' if file.suffix.lower()=='.webp' else 'image/jpeg'
                elif self.path in files:
                    name,mime=files[self.path];data=(WEB/name).read_bytes()
                    if name=='index.html':data=data.replace(b'__EDITOR_TOKEN__',token.encode()).replace(b'__HAS_WALLPAPER__',b'true' if wallpaper else b'false')
                else:self.send_json({'error':'Not found'},404);return
                self.headers_for(200,mime,len(data));self.wfile.write(data)
            except Exception as error:self.send_json({'error':str(error)},400)
        def do_POST(self):
            if not self.allowed(True):return
            try:
                length=int(self.headers.get('Content-Length','-1'))
                if not 0<=length<=65536:
                    # Drain only a bounded body. Closing with an unread 64 KiB body resets the socket
                    # on Windows before the browser can receive the useful 413 response.
                    if 0<length<=65537:self.rfile.read(length)
                    self.close_connection=True;self.send_json({'error':'Request too large'},413);return
                if self.headers.get('Content-Type','').split(';')[0]!='application/json':self.send_json({'error':'JSON required'},415);return
                value=loads(self.rfile.read(length).decode('utf-8'))
                actions={'/api/preview':editor.preview,'/api/apply':editor.apply,'/api/undo':editor.undo,
                    '/api/import':lambda document:{'layout':validate_layout(document)}}
                if self.path not in actions:self.send_json({'error':'Not found'},404);return
                self.send_json(actions[self.path](value))
            except Exception as error:self.send_json({'error':str(error)},400)
    http=ThreadingHTTPServer(('127.0.0.1',port),Handler);http.daemon_threads=True
    return http,token

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    target=parser.add_mutually_exclusive_group(required=True);target.add_argument('--demo',action='store_true');target.add_argument('--tv')
    parser.add_argument('--package',default='com.gathin.tvhome');parser.add_argument('--label',default='CleanYourTV')
    parser.add_argument('--repo',type=Path,default=ROOT);parser.add_argument('--port',type=int,default=0)
    parser.add_argument('--backups',type=Path,default=ROOT/'product/private/layout-backups')
    parser.add_argument('--wallpaper',type=Path);parser.add_argument('--no-browser',action='store_true')
    args=parser.parse_args();bridge=DemoBridge() if args.demo else NativeBridge(args.tv,args.package,args.repo)
    editor=Editor(bridge,'Demo · no TV connected' if args.demo else args.label,args.backups)
    http,_=server(editor,args.port,args.wallpaper)
    url='http://127.0.0.1:'+str(http.server_port)
    print(url,flush=True)
    if not args.no_browser:webbrowser.open(url)
    try:http.serve_forever()
    except KeyboardInterrupt:pass
    finally:http.server_close()

if __name__=='__main__':main()
