"""Reviewed hosted pilots and Roku ambient packaging in the same local Setup application."""
import copy
import hashlib
import hmac
from http.server import BaseHTTPRequestHandler,ThreadingHTTPServer
import ipaddress
import json
import mimetypes
from pathlib import Path
import re
import secrets
import ssl
import threading
import time
from .core import SetupError,address
from .native import atomic,digest,fields,text
from .tv_bridges import Vizio,Vidaa,local_file

MAX_BODY=4096

def profile_file(path):
    p=local_file(path,'private connection profile')
    try:value=json.loads(p.read_text(encoding='utf-8'))
    except (ValueError,UnicodeError):raise SetupError('The private connection profile must be valid JSON.')
    if not isinstance(value,dict):raise SetupError('The private connection profile must be an object.')
    return value

def app_entry(platform,payload,label=None):
    if platform=='vidaa':
        if not isinstance(payload,dict) or not isinstance(payload.get('name'),str):raise SetupError('Invalid VIDAA app.')
        allowed={'appId','name','url','urlType','storeType'};payload={k:v for k,v in payload.items() if k in allowed}
        if not isinstance(payload.get('url'),str) or len(payload['url'])>2048 or not payload['url']:raise SetupError('Invalid VIDAA app address.')
        for k in ('urlType','storeType'):
            if k in payload and (type(payload[k]) is not int or not 0<=payload[k]<=65535):raise SetupError('Invalid VIDAA app type.')
        if 'appId' in payload and not isinstance(payload['appId'],(str,int)):raise SetupError('Invalid VIDAA app ID.')
        name=text(payload['name'],'app name',64)
    else:
        fields(payload,{'APP_ID','NAME_SPACE'})
        if not isinstance(payload['APP_ID'],str) or not re.fullmatch(r'\d{1,8}',payload['APP_ID']) or payload['APP_ID'] in ('17','3001') or type(payload['NAME_SPACE']) is not int or not 0<=payload['NAME_SPACE']<=100:raise SetupError('Capture a regular Vizio app first.')
        name=text(label,'a name for this captured app',64)
    encoded=json.dumps(payload,sort_keys=True,separators=(',',':'),ensure_ascii=True).encode()
    if len(encoded)>4096:raise SetupError('App record is too large.')
    return {'id':'app_'+hashlib.sha256(encoded).hexdigest()[:24],'label':name,'payload':copy.deepcopy(payload)}

def choices(value):
    fields(value,{'style','position','size','spacing','wallpaper'})
    for key,allowed in {'style':['clean','glass','cinema'],'position':['top','middle','bottom'],'size':['small','medium','large'],'spacing':['compact','comfortable','wide'],'wallpaper':['coast','dusk','forest','library']}.items():
        if value[key] not in allowed:raise SetupError('Choose a supported '+key+'.')
    return {'experience':value['style'],'position':value['position'],'iconSize':value['size'],'spacing':value['spacing'],
        'wallpaper':value['wallpaper'],'columns':8,'text':'auto','textSize':100,'names':'all','strongFocus':True,'idleSeconds':60,'motion':'on'}

class HostedServer:
    def __init__(self,folder,host,port,tv_host,apps,settings,bridge):
        self.folder=Path(folder);self.host,self.port=address(host,port);self.tv_host=tv_host
        self.token=secrets.token_urlsafe(32);self.bridge=bridge;self.lock=threading.Lock();self.closed=False;self.apps={a['id']:a for a in apps}
        self.starter={'apps':[{'id':a['id'],'label':a['label']} for a in apps],'settings':settings}
        self.assets={'/'+p.relative_to(self.folder).as_posix():p for p in self.folder.rglob('*') if p.is_file()}
        self.assets['/']=self.folder/'index.html';self.http=None;self.thread=None
    def start(self):
        owner=self;origin='http://'+self.host+':'+str(self.port)
        class Handler(BaseHTTPRequestHandler):
            def setup(self):super().setup();self.connection.settimeout(8)
            def log_message(self,*args):pass
            def send(self,value,status=200,mime='application/json; charset=utf-8'):
                raw=value if isinstance(value,bytes) else json.dumps(value,ensure_ascii=False).encode()
                self.send_response(status);self.headers_out(mime,len(raw));self.end_headers()
                if self.command!='HEAD':self.wfile.write(raw)
            def headers_out(self,mime,length):
                for k,v in {'Content-Type':mime,'Content-Length':str(length),'Cache-Control':'no-store','X-Content-Type-Options':'nosniff','Referrer-Policy':'no-referrer','X-Frame-Options':'DENY','Content-Security-Policy':"default-src 'self'; connect-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; font-src 'self'; img-src 'self' data:; media-src 'self'; object-src 'none'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'"}.items():self.send_header(k,v)
            def allowed(self,api=False):
                if owner.closed:self.send({'error':'This hosted session has stopped.'},410);return False
                if self.client_address[0] not in (owner.tv_host,'127.0.0.1') or self.headers.get('Host')!=origin[7:] or self.headers.get('Origin') not in (None,origin):self.send({'error':'Only the paired TV can use this companion.'},403);return False
                if api and not hmac.compare_digest(self.headers.get('X-Freedom-Session',''),owner.token):self.send({'error':'Reopen the current private TV link from Setup.'},403);return False
                return True
            def do_GET(self):
                if not self.allowed(self.path.startswith('/api/')):return
                if self.path=='/api/catalog':self.send({'apps':owner.starter['apps'],'limited':False,'starter':owner.starter});return
                if self.path=='/bootstrap.js':self.send(('window.FREEDOM_BOOTSTRAP='+json.dumps(owner.starter,ensure_ascii=True)+';').encode(),mime='text/javascript; charset=utf-8');return
                file=owner.assets.get(self.path)
                if not file:self.send({'error':'Page not found.'},404);return
                mime=mimetypes.guess_type(file.name)[0] or 'application/octet-stream'
                size=file.stat().st_size;start,end=0,size-1;status=200
                if self.headers.get('Range'):
                    match=re.fullmatch(r'bytes=(\d+)-(\d*)',self.headers['Range'])
                    if not match:self.send({'error':'Use a single byte range.'},416);return
                    start=int(match[1]);end=int(match[2]) if match[2] else size-1
                    if not 0<=start<=end<size:self.send({'error':'Invalid media range.'},416);return
                    status=206
                self.send_response(status);self.headers_out(mime,end-start+1);self.send_header('Accept-Ranges','bytes')
                if status==206:self.send_header('Content-Range',f'bytes {start}-{end}/{size}')
                self.end_headers()
                if self.command=='HEAD':return
                try:
                    with file.open('rb') as stream:
                        stream.seek(start);remaining=end-start+1
                        while remaining:
                            chunk=stream.read(min(65536,remaining))
                            if not chunk:break
                            self.wfile.write(chunk);remaining-=len(chunk)
                except (BrokenPipeError,ConnectionResetError):pass
            def do_HEAD(self):self.do_GET()
            def do_POST(self):
                if not self.allowed(True):return
                try:
                    if self.path!='/api/launch':self.send({'error':'This action is unavailable.'},404);return
                    if self.headers.get('Content-Type')!='application/json':raise SetupError('JSON required.')
                    count=int(self.headers.get('Content-Length','-1'))
                    if not 0<count<=MAX_BODY:raise SetupError('Request too large.')
                    value=json.loads(self.rfile.read(count));fields(value,{'id'})
                    item=owner.apps.get(value['id'])
                    if not item:raise SetupError('Select an app from the reviewed catalog.')
                    if not owner.lock.acquire(False):raise SetupError('A launch is already pending. Check the TV before retrying.')
                    try:
                        if owner.closed:raise SetupError('The hosted session has stopped.')
                        owner.bridge.launch(copy.deepcopy(item['payload']))
                    finally:owner.lock.release()
                    self.send({'requested':True,'foregroundVerified':False})
                except (SetupError,ValueError,TypeError,KeyError):self.send({'error':'Launch was not confirmed. Check the TV and companion before retrying.'},400)
        self.http=ThreadingHTTPServer((self.host,self.port),Handler);self.http.daemon_threads=True
        self.thread=threading.Thread(target=self.http.serve_forever,daemon=True);self.thread.start()
        return origin+'/#'+self.token
    def close(self):
        self.closed=True;self.token=''
        if self.http:self.http.shutdown();self.http.server_close();self.http=None
        if self.thread:self.thread.join(timeout=3);self.thread=None
        with self.lock:pass # Finish an in-flight bounded launch before disconnecting its transport.

class CompanionSetup:
    def __init__(self,root,bridge_factory=None,server_factory=None):
        self.root=Path(root)/'companion';self.bridge_factory=bridge_factory or {'vidaa':Vidaa,'vizio':Vizio}
        self.server_factory=server_factory or HostedServer;self.lock=threading.RLock();self.bridge=None;self.platform=None;self.host=None;self.apps=[];self.reviewed=None;self.server=None;self.package=None;self.package_id=None
    def public(self):
        with self.lock:return {'platform':self.platform,'tvHost':self.host,'connected':bool(self.bridge),'apps':[{'id':a['id'],'label':a['label']} for a in self.apps],
            'serverRunning':self.server is not None,'package':copy.deepcopy(self.package),'hardwareAccepted':False}
    def info(self):
        from .capabilities import catalogue
        return {'platforms':catalogue(),'companion':self.public()}
    def trust_vizio(self,value):
        fields(value,{'certificate','confirmed'})
        if value['confirmed'] is not True:raise SetupError('Confirm that this is the selected TV’s trusted public certificate.')
        cert=local_file(value['certificate'],'trusted public TV certificate')
        try:
            raw=cert.read_text(encoding='ascii')
            if raw.count('-----BEGIN CERTIFICATE-----')!=1 or 'PRIVATE KEY' in raw:raise ValueError()
            from cryptography import x509
            x509.load_pem_x509_certificate(raw.encode('ascii'))
            der=ssl.PEM_cert_to_DER_cert(raw)
        except (ValueError,UnicodeError):raise SetupError('Choose one public PEM TV certificate, not a private key or CA bundle.')
        pin=hashlib.sha256(der).hexdigest();out=self.root/'trust'/('vizio-'+secrets.token_hex(8)+'.json')
        atomic(out,{'ca':str(cert),'fingerprint':pin})
        return {'profile':str(out),'fingerprint':pin,'connected':False}
    def connect(self,value):
        fields(value,{'platform','host','profile','consent'})
        if value['platform'] not in ('vidaa','vizio') or value['consent'] is not True:raise SetupError('Confirm the private connection and pairing requirements first.')
        with self.lock:
            if self.server:raise SetupError('Stop the hosted Home before changing its paired TV.')
            self.disconnect();platform=value['platform'];host,_=address(value['host'],36669 if platform=='vidaa' else 7345)
            profile=profile_file(value['profile'])
            if platform=='vizio':
                if set(profile)!={'ca','fingerprint'}:raise SetupError('The Vizio trust profile needs only ca and fingerprint.')
                # Unique for this setup attempt; never invalidate another integration's identity.
                profile['identity']='dv-freedom-'+secrets.token_hex(12)
            bridge=self.bridge_factory[platform](host,profile)
            try:
                if platform=='vizio':result=bridge.begin()
                else:
                    rows=bridge.connect();catalog=[]
                    for row in rows:
                        try:entry=app_entry(platform,row)
                        except SetupError:continue
                        if not any(a['id']==entry['id'] for a in catalog):catalog.append(entry)
                    self.apps=catalog[:100];result={'pairingRequired':False,'apps':len(self.apps)}
                self.bridge=bridge;self.platform=platform;self.host=host;self.reviewed=None;return {**result,**self.public()}
            except Exception:bridge.close();raise
    def pair(self,value):
        fields(value,{'pin'})
        with self.lock:
            if self.platform!='vizio' or not self.bridge:raise SetupError('Start a Vizio pairing challenge first.')
            self.bridge.pair(value['pin']);return self.public()
    def capture(self,value):
        fields(value,{'label'})
        with self.lock:
            if self.server:raise SetupError('Stop hosted Home before changing its app catalog.')
            if self.platform!='vizio' or not self.bridge:raise SetupError('Pair a Vizio first.')
            row=app_entry('vizio',self.bridge.current(),value['label'])
            if len(self.apps)>=100:raise SetupError('The catalog already has 100 apps.')
            self.apps=[a for a in self.apps if a['id']!=row['id']]+[row];self.reviewed=None;return self.public()
    def review(self,value):
        fields(value,{'host','port','apps','choices','media'})
        with self.lock:
            if self.server:raise SetupError('Stop the current hosted Home before preparing another.')
            if not self.bridge:raise SetupError('Connect this platform first.')
            if self.platform=='vizio' and not self.bridge.token:raise SetupError('Finish TV PIN pairing first.')
            bind,port=address(value['host'],value['port'])
            if bind==self.host:raise SetupError('The server address must belong to this computer, not the TV.')
            ids=value['apps']
            if not isinstance(ids,list) or not 1<=len(ids)<=8 or any(not isinstance(i,str) for i in ids) or len(set(ids))!=len(ids) or any(not any(a['id']==i for a in self.apps) for i in ids):raise SetupError('Choose one to eight distinct captured TV apps.')
            settings=choices(value['choices']);media=value['media']
            if not isinstance(media,str) or len(media)>1024:raise SetupError('Choose a local media folder or leave it blank.')
            if settings['wallpaper']=='library' and not media:raise SetupError('Library background needs a folder containing stills and optional silent video.')
            from platforms.tizen.prepare_launcher import media_files
            _,media_paths=media_files(media or None)
            media_hashes={p.name:digest(p) for p in media_paths}
            revision=secrets.token_urlsafe(24);data=copy.deepcopy(value)
            self.reviewed={'id':revision,'data':data,'settings':settings,'mediaHashes':media_hashes,'expires':time.monotonic()+900,
                'apps':[copy.deepcopy(next(a for a in self.apps if a['id']==i)) for i in ids]}
            return {'id':revision,'platform':self.platform,'server':f'{bind}:{port}','apps':[a['label'] for a in self.reviewed['apps']],
                'requirements':['This computer must stay on. No system service or firewall change is made.',
                    'Only the selected TV can reach the page; its session link authorizes the reviewed app launches.',
                    'Original TV Home, firmware, inputs and accounts are unchanged. Home return is manual.',
                    'Vizio Conjure and VIDAA debug installation depend on exact firmware.'],
                'settings':copy.deepcopy(settings),'expiresInSeconds':900}
    def start(self,value):
        fields(value,{'id','confirmed'})
        with self.lock:
            r=self.reviewed
            if value['confirmed'] is not True or not r or not hmac.compare_digest(str(value['id']),r['id']) or time.monotonic()>r['expires']:raise SetupError('Prepare a fresh review and confirm its exact TV, apps and server address.')
            self.reviewed=None # Single use, consumed even if bind/staging fails.
            from platforms.hosted.prepare_launcher import prepare
            folder=self.root/('hosted-'+secrets.token_hex(8));d=r['data']
            from platforms.tizen.prepare_launcher import media_files
            _,media_paths=media_files(d['media'] or None)
            if {p.name:digest(p) for p in media_paths}!=r['mediaHashes']:raise SetupError('Wallpaper files changed after review. Prepare a new review before starting.')
            prepare(folder,self.platform,media=d['media'] or None)
            core=folder/'core.js';source=core.read_text(encoding='utf-8');source=source.replace('dv-freedom.'+self.platform+'.v1','dv-freedom.'+self.platform+'.'+hashlib.sha256((self.host+self.bridge.pin).encode()).hexdigest()[:16]+'.v1');core.write_text(source,encoding='utf-8')
            html=folder/'index.html';html.write_text(html.read_text(encoding='utf-8').replace('<script src="bridge.js">','<script src="bootstrap.js"></script><script src="bridge.js">'),encoding='utf-8')
            server=self.server_factory(folder,d['host'],d['port'],self.host,r['apps'],r['settings'],self.bridge)
            try:url=server.start()
            except OSError:server.close();raise SetupError('Could not bind that computer address/port. Check its LAN address and choose an unused port. No firewall change was made.')
            self.server=server
            atomic(folder/'setup-record.json',{'schema':1,'platform':self.platform,'tv':self.host,'server':d['host'],'port':d['port'],'apps':[{'id':a['id'],'label':a['label']} for a in r['apps']],'hardwareAccepted':False})
            server.assets.pop('/setup-record.json',None) if hasattr(server,'assets') else None
            return {'url':url,'running':True,'platform':self.platform,'installed':False}
    def open_vizio(self,value):
        fields(value,{'confirmed'})
        with self.lock:
            if value['confirmed'] is not True or self.platform!='vizio' or not self.server:raise SetupError('Start and review the hosted Vizio page first.')
            # Fixed normal Conjure route only. Never app 3001 or internal/privileged APIs.
            url=f'http://{self.server.host}:{self.server.port}/#'+self.server.token
            self.bridge.launch({'APP_ID':'17','NAME_SPACE':4,'MESSAGE':url});return {'requested':True,'foregroundVerified':False}
    def roku(self,value):
        fields(value,{'theme','clock','confirmed'})
        if value['confirmed'] is not True:raise SetupError('Confirm this is an ambient app, not a Home replacement.')
        with self.lock:
            from platforms.roku.prepare_channel import prepare
            self.package_id=secrets.token_hex(16);out=self.root/'packages'/(self.package_id+'.zip')
            info=prepare(out,value['theme'],value['clock']);info.pop('file');info['download']='/api/companion-package/'+self.package_id
            self.package=info;return copy.deepcopy(info)
    def package_file(self,key):
        if not self.package or key!=self.package_id:raise SetupError('Build a package in this session first.')
        p=self.root/'packages'/(key+'.zip')
        if digest(p)!=self.package['sha256']:raise SetupError('Package changed. Build a new one.')
        return p
    def stop(self):
        with self.lock:
            if self.server:self.server.close();self.server=None
            self.reviewed=None;return self.public()
    def disconnect(self):
        self.stop()
        if self.bridge:self.bridge.close()
        self.bridge=None;self.platform=None;self.host=None;self.apps=[]
