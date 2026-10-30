"""Explicit, temporary phone-to-computer setup. The phone has no TV-command or install endpoint."""
from __future__ import annotations
import copy
import hashlib
import hmac
from http.cookies import SimpleCookie, CookieError
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import ipaddress
import json
import os
from pathlib import Path
import re
import secrets
import socket
import struct
import threading
import time
from urllib.parse import unquote
from .core import SetupError, validate_plan

WEB = Path(__file__).with_name('web')
MAX_FILE = 48 * 1024 * 1024
MAX_IMAGE = 8 * 1024 * 1024
MAX_INBOX = 128 * 1024 * 1024
LIFETIME = 15 * 60
ID = re.compile(r'[0-9a-f]{32}')


def local_addresses():
    """Local interface information only. No discovery scan, external request or firewall changes."""
    result = set()
    try: rows=socket.getaddrinfo(socket.gethostname(), None, socket.AF_INET, socket.SOCK_STREAM)
    except OSError: return []
    for row in rows:
        address = ipaddress.ip_address(row[4][0])
        if address in ipaddress.ip_network('10.0.0.0/8') or address in ipaddress.ip_network('172.16.0.0/12') or address in ipaddress.ip_network('192.168.0.0/16'):
            result.add(str(address))
    return sorted(result)


def object_json(raw):
    def unique(pairs):
        value = {}
        for key, item in pairs:
            if key in value: raise SetupError('Repeated field.')
            value[key] = item
        return value
    value = json.loads(raw, object_pairs_hook=unique, parse_constant=lambda _: (_ for _ in ()).throw(SetupError('Invalid number.')))
    if not isinstance(value, dict): raise SetupError('Object required.')
    return value


def media_kind(header):
    if header.startswith(b'\x89PNG\r\n\x1a\n'): return 'png'
    if header.startswith(b'\xff\xd8\xff'): return 'jpg'
    if len(header) >= 12 and header[4:8] == b'ftyp': return 'mp4'
    raise SetupError('Choose a JPEG, PNG or MP4 file. Renaming a file does not convert it.')


def validate_image(path, kind):
    with path.open('rb') as source:
        if kind=='png':
            header=source.read(24)
            if len(header)!=24 or header[12:16]!=b'IHDR': raise SetupError('This PNG is incomplete.')
            width,height=struct.unpack('>II',header[16:24])
        else:
            source.read(2); width=height=0
            while source.tell()<MAX_IMAGE:
                if source.read(1)!=b'\xff': break
                marker=source.read(1)
                while marker==b'\xff': marker=source.read(1)
                if not marker or marker in (b'\xd9',b'\xda'): break
                raw=source.read(2)
                if len(raw)!=2: break
                length=int.from_bytes(raw,'big')
                if length<2: break
                if marker[0] in (0xc0,0xc1,0xc2,0xc3,0xc5,0xc6,0xc7,0xc9,0xca,0xcb,0xcd,0xce,0xcf):
                    size=source.read(5)
                    if len(size)==5: height,width=struct.unpack('>HH',size[1:5])
                    break
                source.seek(length-2,1)
    if width<1 or height<1 or width>16384 or height>16384 or width*height>32_000_000:
        raise SetupError('This image is invalid or over 32 megapixels. Export a smaller picture first.')


class LimitedServer(ThreadingHTTPServer):
    daemon_threads = True
    def __init__(self, *args):
        self.slots = threading.BoundedSemaphore(6)
        super().__init__(*args)
    def process_request(self, request, address):
        if not self.slots.acquire(False): self.shutdown_request(request); return
        try: super().process_request(request, address)
        except BaseException: self.slots.release(); raise
    def process_request_thread(self, request, address):
        try: super().process_request_thread(request, address)
        finally: self.slots.release()


class PhoneSetup:
    def __init__(self, session, addresses=local_addresses, clock=time.monotonic):
        self.session, self.addresses, self.clock = session, addresses, clock
        self.folder = session.root / 'phone-inbox'
        self.server = None
        self.guard = threading.RLock()
        self.upload_lock = threading.Lock()
        self.pin = self.token_hash = self.url = ''
        self.deadline = self.pair_deadline = self.locked_until = 0
        self.failures = 0
        self.share_apps = False
        self.base = self.pending = None
        self.binding = ''
        self.timer = None
        self.transfer = {'status':'idle'}

    def context_key(self):
        value = dict(mode=self.session.mode, connection=self.session.connection,
                     apps=(self.session.inventory or {}).get('apps', []))
        return hashlib.sha256(json.dumps(value, sort_keys=True).encode()).hexdigest()

    def files(self):
        result = []
        if not self.folder.is_dir(): return result
        for record in sorted(self.folder.glob('*.json'))[:32]:
            try:
                if not ID.fullmatch(record.stem) or record.stat().st_size > 4096: continue
                item = object_json(record.read_text(encoding='utf-8'))
                if set(item) != {'id','name','kind','bytes','sha256','binding'} or item['id'] != record.stem: continue
                if item['kind'] not in ('jpg','png','mp4') or type(item['bytes']) is not int or not 0<item['bytes']<=MAX_FILE or not re.fullmatch('[0-9a-f]{64}',item['sha256']): continue
                path = self.folder / (item['id'] + '.' + item['kind'])
                if path.is_file() and not path.is_symlink() and path.stat().st_size == item['bytes']:
                    result.append(item)
            except (OSError, ValueError, TypeError): pass
        return result

    def public(self):
        with self.guard:
            active = bool(self.server and self.clock() < self.deadline)
            return dict(active=active, addresses=[self.url.split('/')[2].split(':')[0]] if active else self.addresses(), url=self.url if active else '',
                        pin=self.pin if active and not self.token_hash else '', paired=active and bool(self.token_hash),
                        seconds=max(0, int(self.deadline-self.clock())) if active else 0,
                        pending=copy.deepcopy(self.pending),
                        transfer=copy.deepcopy(self.transfer),
                        files=[{k:v for k,v in item.items() if k != 'binding'} for item in self.files()],
                        note='Temporary HTTP on your trusted home network. Keep this computer open. Stop sharing revokes phone access; received files remain private on this computer.')

    def start(self, value):
        if not isinstance(value, dict) or set(value) != {'address','shareApps','choices','consent'} or value['consent'] is not True or type(value['shareApps']) is not bool:
            raise SetupError('Review phone sharing and choose Start explicitly.')
        if value['address'] not in self.addresses(): raise SetupError('Choose a current local network interface.')
        if not self.session.inventory: raise SetupError('Connect to a TV or start the example preview first.')
        base = validate_plan(value['choices'], self.session.inventory['apps'])
        with self.guard:
            if self.server: raise SetupError('Stop the current phone session before starting another.')
            self.base, self.binding = base, self.context_key()
            self.share_apps = value['shareApps']
            self.pin = f'{secrets.randbelow(1_000_000):06d}'
            self.token_hash = ''; self.pending = None; self.failures = 0; self.locked_until = 0
            self.deadline = self.clock()+LIFETIME; self.pair_deadline = self.clock()+300
            self.server = self._server(value['address'])
            self.url = f'http://{value["address"]}:{self.server.server_port}'
            threading.Thread(target=self.server.serve_forever, daemon=True).start()
            server = self.server
            def expire():
                with self.guard:
                    same = self.server is server
                if same: self.stop()
            self.timer = threading.Timer(LIFETIME, expire); self.timer.daemon = True; self.timer.start()
        return self.public()

    def stop(self):
        with self.guard:
            server, self.server = self.server, None
            self.token_hash = self.pin = ''; self.deadline = 0
            if self.timer: self.timer.cancel(); self.timer=None
        if server: server.shutdown(); server.server_close()
        return self.public()

    def authenticated(self, cookie):
        try:
            jar = SimpleCookie(); jar.load(cookie or '')
            token = jar['dv_phone'].value
            return self.clock() < self.deadline and bool(self.token_hash) and hmac.compare_digest(hashlib.sha256(token.encode()).hexdigest(), self.token_hash)
        except (KeyError, ValueError, CookieError): return False

    def pair(self, code):
        with self.guard:
            now = self.clock()
            if now >= self.pair_deadline or now >= self.deadline: raise SetupError('This code expired. Start a new session on the computer.')
            if self.token_hash: raise SetupError('A phone is already paired. Stop sharing on the computer to pair another.')
            if now < self.locked_until: raise SetupError('Too many attempts. Wait before trying again.')
            if not isinstance(code,str) or not re.fullmatch(r'[0-9]{6}', code) or not hmac.compare_digest(code, self.pin):
                self.failures += 1
                if self.failures >= 5: self.locked_until = now+min(600, 60*2**min(3,(self.failures-5)//5))
                raise SetupError('That code did not match. Check the code on the computer.')
            token = secrets.token_urlsafe(32)
            self.token_hash = hashlib.sha256(token.encode()).hexdigest(); self.pin = ''
            return token

    def draft(self, value):
        with self.guard:
            allowed = {'style','position','size','spacing','wallpaper'} | ({'apps'} if self.share_apps else set())
            if not isinstance(value, dict) or set(value)-allowed: raise SetupError('Unsupported phone setup choice.')
            if self.context_key() != self.binding: raise SetupError('The selected TV changed. Start a new phone session.')
            next_plan = validate_plan({**self.base, **value}, self.session.inventory['apps'])
            self.pending = dict(id=secrets.token_hex(16), choices=next_plan)
            return dict(received=True, reviewRequired=True)

    def claim(self, value):
        if not isinstance(value, dict) or set(value) != {'id'}: raise SetupError('Choose the received draft.')
        with self.guard:
            if not self.pending or value['id'] != self.pending['id'] or self.context_key() != self.binding:
                raise SetupError('This draft expired or the selected TV changed.')
            plan = validate_plan(self.pending['choices'], self.session.inventory['apps'])
            self.pending = None
        self.stop()
        return dict(choices=plan, applied=False)

    def file(self, id):
        if not isinstance(id,str) or not ID.fullmatch(id): raise SetupError('Invalid received file.')
        item = next((item for item in self.files() if item['id'] == id), None)
        if not item: raise SetupError('This received file is unavailable.')
        return item, self.folder/(id+'.'+item['kind'])

    def remove(self, value):
        if not isinstance(value,dict) or set(value) != {'id'}: raise SetupError('Choose a received file to remove.')
        with self.upload_lock:
            item,path = self.file(value['id'])
            path.unlink(); path.with_suffix('.json').unlink()
        return self.public()

    def send_to_tv(self,value):
        if not isinstance(value,dict) or set(value)!={'id','consent'} or value['consent'] is not True:raise SetupError('Review the file and explicitly choose TV review.')
        if self.server:raise SetupError('Stop phone sharing before opening TV review.')
        item,file=self.file(value['id'])
        if item['binding']!=self.context_key() or self.session.mode!='device':raise SetupError('Connect to the same TV used for this transfer. Example media cannot be sent to a real TV.')
        installer=self.session.installer
        if not installer.lock.acquire(False):raise SetupError('Wait for the current TV operation.')
        self.transfer={'status':'running','message':'Verifying the installed app and preparing a private media copy…'}
        def run():
            device=None
            try:
                release=installer.catalog.verify();device=installer._open()
                if not installer._installed(device,release):raise SetupError('Install the reviewed DV Freedom package before sending media.')
                native=installer._native(device)
                baseline=dict(schema=1,kind='media-review',device=device.fingerprint(),profile=native['profile'],revision=native['revision'],backup=native['backup'],sha256=item['sha256'])
                (self.folder/(item['id']+'.review.json')).write_text(json.dumps(baseline),encoding='utf-8')
                self.transfer['message']='Sending media. The TV will require a separate Add to library choice.'
                device.send_media(file,item,native)
                self.transfer={'status':'done','message':'Media is ready for review. On the TV, open Launcher settings → Wallpaper → Received from phone if the review did not open. Nothing was imported or selected automatically.'}
            except SetupError as error:self.transfer={'status':'error','message':str(error)}
            except Exception:self.transfer={'status':'error','message':'The TV stopped responding. Check for an existing media review before retrying. No wallpaper was selected automatically.'}
            finally:
                if device:
                    try:device.close()
                    except Exception:pass
                installer.lock.release()
        threading.Thread(target=run,daemon=True).start()
        return {'started':True}

    def receive(self, stream, length, name):
        if not isinstance(name,str) or len(name)>128 or not name.strip() or any(ord(c)<32 for c in name) or any(c in name for c in '/\\'):
            raise SetupError('Use a plain file name without folders.')
        if not 12 <= length <= MAX_FILE: raise SetupError('Choose an image up to 8 MB or an MP4 up to 48 MB.')
        if not self.upload_lock.acquire(False): raise SetupError('Another upload is running. Wait for it to finish.')
        self.folder.mkdir(parents=True, exist_ok=True, mode=0o700)
        id = secrets.token_hex(16); temporary = self.folder/(id+'.part')
        try:
            files = self.files()
            used=sum(p.stat().st_size for p in self.folder.iterdir() if p.is_file() and not p.is_symlink())
            if len(files) >= 16 or used+length > MAX_INBOX: raise SetupError('The private inbox is full. Remove received files on the computer first.')
            digest = hashlib.sha256(); remaining = length; started = self.clock(); kind = None
            with temporary.open('xb') as out:
                while remaining:
                    if self.clock()-started > 120 or self.clock() >= self.deadline or not self.token_hash: raise SetupError('Transfer expired. Start again from the computer.')
                    data = stream.read(min(65536, remaining))
                    if not data: raise SetupError('Upload was interrupted. Nothing was imported.')
                    if kind is None:
                        kind = media_kind(data)
                        if kind != 'mp4' and length > MAX_IMAGE: raise SetupError('Images must be 8 MB or smaller.')
                    digest.update(data); out.write(data); remaining -= len(data)
            if kind!='mp4': validate_image(temporary,kind)
            if self.clock()>=self.deadline or not self.token_hash: raise SetupError('Sharing stopped. Nothing was imported.')
            item = dict(id=id, name=name.strip(), kind=kind, bytes=length, sha256=digest.hexdigest(), binding=self.binding)
            os.replace(temporary, self.folder/(id+'.'+kind))
            (self.folder/(id+'.json')).write_text(json.dumps(item), encoding='utf-8')
            return {k:v for k,v in item.items() if k != 'binding'}
        finally:
            if temporary.exists(): temporary.unlink()
            self.upload_lock.release()

    def qr(self):
        if not self.server or self.clock() >= self.deadline: raise SetupError('Start phone sharing first.')
        import qrcode
        from qrcode.image.svg import SvgPathImage
        return qrcode.make(self.url, image_factory=SvgPathImage, border=4).to_string()

    def _server(self, address):
        owner = self
        class Handler(BaseHTTPRequestHandler):
            def setup(self): super().setup(); self.connection.settimeout(10)
            def log_message(self,*args): pass
            def send(self, data, mime='application/json; charset=utf-8', status=200, cookie=None):
                if not isinstance(data,bytes): data=json.dumps(data,ensure_ascii=False).encode()
                self.send_response(status)
                headers={'Content-Type':mime,'Content-Length':str(len(data)),'Cache-Control':'no-store',
                    'Referrer-Policy':'no-referrer','X-Content-Type-Options':'nosniff','X-Frame-Options':'DENY',
                    'Content-Security-Policy':"default-src 'self'; script-src 'self'; style-src 'self'; font-src 'self'; connect-src 'self'; img-src 'self'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'"}
                if cookie: headers['Set-Cookie']=cookie
                for key,value in headers.items(): self.send_header(key,value)
                self.end_headers(); self.wfile.write(data)
            def allowed(self, post=False, auth=False):
                origin=owner.url
                if len(self.headers.get_all('Host',[]))!=1 or self.headers.get('Host')!=origin.removeprefix('http://') or self.headers.get('Origin') not in ([origin] if post else [None,origin]):
                    self.send({'error':'Open the address shown on the computer.'},status=403); return False
                if owner.clock() >= owner.deadline: self.send({'error':'Session expired. Reopen sharing on the computer.'},status=410); return False
                if auth and not owner.authenticated(self.headers.get('Cookie')):
                    self.send({'error':'Pair with the code on the computer first.'},status=401); return False
                return True
            def do_GET(self):
                if not self.allowed(auth=self.path.startswith('/api/')): return
                if self.path=='/api/state':
                    choices=copy.deepcopy(owner.base)
                    if not owner.share_apps: choices.pop('apps',None)
                    self.send(dict(choices=choices, apps=copy.deepcopy(owner.session.inventory['apps']) if owner.share_apps else [],seconds=max(0,int(owner.deadline-owner.clock())))); return
                if self.path=='/outfit.ttf':
                    self.send((Path(__file__).resolve().parents[1]/'launcher/app/src/main/res/font/outfit.ttf').read_bytes(),'font/ttf');return
                assets={'/':('phone.html','text/html; charset=utf-8'),'/phone.css':('phone.css','text/css; charset=utf-8'),'/phone.js':('phone.js','text/javascript; charset=utf-8')}
                if self.path not in assets: self.send({'error':'Not found.'},status=404); return
                name,mime=assets[self.path]; self.send((WEB/name).read_bytes(),mime)
            def do_POST(self):
                if not self.allowed(True,self.path!='/api/pair'): return
                try:
                    if self.headers.get('Transfer-Encoding'): raise SetupError('Use a fixed-length request.')
                    if len(self.headers.get_all('Content-Length',[]))!=1: raise SetupError('One content length is required.')
                    length=int(self.headers.get('Content-Length','-1'))
                    if self.path=='/api/upload':
                        if self.headers.get('Content-Type')!='application/octet-stream': raise SetupError('Choose a file first.')
                        result=owner.receive(self.rfile,length,unquote(self.headers.get('X-File-Name','')))
                    else:
                        if not 0 <= length <= 8192: self.close_connection=True; raise SetupError('Request too large.')
                        if self.headers.get('Content-Type','').split(';')[0]!='application/json': raise SetupError('JSON required.')
                        value=object_json(self.rfile.read(length).decode('utf-8'))
                        if self.path=='/api/pair':
                            if set(value)!={'code'}: raise SetupError('Enter only the pairing code.')
                            token=owner.pair(value['code'])
                            self.send({'paired':True},cookie='dv_phone='+token+'; HttpOnly; SameSite=Strict; Path=/; Max-Age=900'); return
                        if self.path!='/api/draft': self.send({'error':'No TV actions are available from the phone.'},status=404); return
                        result=owner.draft(value)
                    self.send(result)
                except (SetupError,ValueError,TypeError,UnicodeError) as error:
                    self.close_connection=True
                    self.send({'error':str(error) if isinstance(error,SetupError) else 'Check the form and try again.'},status=400)
                except OSError:
                    self.close_connection=True
                    self.send({'error':'Transfer interrupted. Check the connection and free space.'},status=400)
        return LimitedServer((address,0),Handler)
