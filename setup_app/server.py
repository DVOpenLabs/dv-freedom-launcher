"""Loopback-only GUI host with exact routes, session token and bounded JSON."""
import hmac
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
from pathlib import Path
import secrets
import threading
from .core import Session,SetupError

WEB=Path(__file__).with_name('web')
FONT=Path(__file__).resolve().parents[1]/'launcher/app/src/main/res/font/outfit.ttf'

def unique(pairs):
    value={}
    for key,item in pairs:
        if key in value:raise SetupError('Repeated field in request.')
        value[key]=item
    return value

def serve(session=None,port=0):
    session=session or Session();token=secrets.token_urlsafe(32)
    class Handler(BaseHTTPRequestHandler):
        def setup(self):super().setup();self.connection.settimeout(8)
        def log_message(self,*args):pass
        def send(self,data,mime='application/json; charset=utf-8',status=200):
            if not isinstance(data,bytes):data=json.dumps(data,ensure_ascii=False).encode()
            self.send_response(status);self.send_header('Content-Type',mime);self.send_header('Content-Length',str(len(data)))
            for key,value in {'Cache-Control':'no-store','X-Content-Type-Options':'nosniff','Referrer-Policy':'no-referrer','X-Frame-Options':'DENY',
              'Content-Security-Policy':"default-src 'self'; script-src 'self'; style-src 'self'; font-src 'self'; img-src 'self' blob:; connect-src 'self'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'"}.items():self.send_header(key,value)
            self.end_headers();self.wfile.write(data)
        def allowed(self,api=False,post=False):
            origin='http://127.0.0.1:'+str(self.server.server_port)
            if self.headers.get('Host')!=origin.removeprefix('http://') or self.headers.get('Origin') not in ([origin] if post else [None,origin]):
                self.send({'error':'This setup works only from its local window.'},status=403);return False
            if api and not hmac.compare_digest(self.headers.get('X-Setup-Token',''),token):
                self.send({'error':'This session expired. Reopen DV Freedom Setup.'},status=403);return False
            return True
        def do_GET(self):
            if not self.allowed(self.path.startswith('/api/')):return
            if self.path=='/api/state':self.send(session.public());return
            if self.path=='/platforms.js':
                from .capabilities import catalogue
                self.send(('window.DVPlatforms='+json.dumps(catalogue())+';').encode(),'text/javascript; charset=utf-8');return
            if self.path.startswith('/api/companion-package/'):
                try:
                    file=session.companion.package_file(self.path.rsplit('/',1)[-1])
                    self.send(file.read_bytes(),'application/zip')
                except (SetupError,OSError):self.send({'error':'Build a new package in this session.'},status=400)
                return
            assets={'/':(WEB/'index.html','text/html; charset=utf-8'),'/app.css':(WEB/'app.css','text/css; charset=utf-8'),
               '/app.js':(WEB/'app.js','text/javascript; charset=utf-8'),'/install.js':(WEB/'install.js','text/javascript; charset=utf-8'),'/native.js':(WEB/'native.js','text/javascript; charset=utf-8'),'/companion.js':(WEB/'companion.js','text/javascript; charset=utf-8'),'/scene.svg':(WEB/'scene.svg','image/svg+xml'),'/outfit.ttf':(FONT,'font/ttf')}
            if self.path not in assets:self.send({'error':'Page not found.'},status=404);return
            file,mime=assets[self.path]
            try:
                data=file.read_bytes()
                if self.path=='/':data=data.replace(b'__SETUP_TOKEN__',token.encode())
                self.send(data,mime)
            except OSError:self.send({'error':'A setup asset is missing. Reopen the packaged app.'},status=500)
        def do_POST(self):
            if not self.allowed(True,True):return
            try:
                length=int(self.headers.get('Content-Length','-1'))
                if not 0<=length<=8192:
                    self.close_connection=True;self.send({'error':'Request is too large.'},status=413);return
                if self.headers.get('Content-Type','').split(';')[0]!='application/json':self.send({'error':'JSON required.'},status=415);return
                value=json.loads(self.rfile.read(length).decode(),object_pairs_hook=unique,parse_constant=lambda _:(_ for _ in ()).throw(SetupError('Invalid number.')))
                with session.dispatch_lock:
                    if self.path=='/api/native-cancel':
                        if value!={}:raise SetupError('Unexpected stop options.')
                        session.native.cancelled.set();self.send({'requested':True});return
                    if session.native.lock.locked():raise SetupError('Wait for the current LG/Samsung step. Package installation cannot be interrupted halfway through.')
                    if self.path=='/api/cancel-install':
                        if value!={}:raise SetupError('Unexpected stop options.')
                        session.installer.cancelled.set();self.send({'requested':True});return
                    if session.installer.lock.locked():raise SetupError('Wait for the current installer step, or request Stop. Installation cannot be interrupted halfway through an Android package commit.')
                    if session.tools.lock.locked():raise SetupError('Pairing support is being prepared. Keep this app open until the download finishes.')
                    if session.job['status']=='running':raise SetupError('Wait for the current connection check to finish.')
                    if session.companion.server and not self.path.startswith('/api/companion-') and self.path!='/api/quit':raise SetupError('Stop the current hosted Home before starting another TV operation.')
                    if self.path.startswith('/api/companion-'):
                        actions={'info':lambda v:session.companion.info() if v=={} else (_ for _ in ()).throw(SetupError('Unexpected fields.')),
                            'connect':session.companion.connect,'trust':session.companion.trust_vizio,'pair':session.companion.pair,'capture':session.companion.capture,
                            'review':session.companion.review,'start':session.companion.start,'open-vizio':session.companion.open_vizio,
                            'roku':session.companion.roku,'stop':lambda v:session.companion.stop() if v=={} else (_ for _ in ()).throw(SetupError('Unexpected fields.')),
                            'disconnect':lambda v:session.companion.disconnect() or session.companion.public() if v=={} else (_ for _ in ()).throw(SetupError('Unexpected fields.'))}
                        action=actions.get(self.path.removeprefix('/api/companion-'))
                        if not action:raise SetupError('This companion action is unavailable.')
                        reply=action(value)
                    elif self.path=='/api/native-info':
                        if value!={}:raise SetupError('Unexpected tool check options.')
                        reply=session.native.info()
                    elif self.path=='/api/native-config':reply=session.native.configure(value)
                    elif self.path=='/api/native-tools':reply=session.native.prepare_tools(value)
                    elif self.path=='/api/native-connect':reply=session.native.connect(value)
                    elif self.path=='/api/native-prepare':reply=session.native.prepare(value)
                    elif self.path=='/api/native-install':reply=session.native.install(value)
                    elif self.path=='/api/native-check':reply=session.native.check(value)
                    elif self.path=='/api/native-launch':reply=session.native.check(value,launch=True)
                    elif self.path=='/api/preview':
                        if value!={}:raise SetupError('Unexpected preview options.')
                        reply=session.preview()
                    elif self.path=='/api/connect':reply=session.connect(value)
                    elif self.path=='/api/backup':
                        if value!={}:raise SetupError('Unexpected backup options.')
                        reply=session.snapshot()
                    elif self.path=='/api/plan':reply=session.plan(value)
                    elif self.path=='/api/review-install':reply=session.installer.review(value)
                    elif self.path=='/api/tools-info':
                        if value!={}:raise SetupError('Unexpected tools options.')
                        reply=session.tools.info()
                    elif self.path=='/api/tools-install':reply=session.tools.install(value)
                    elif self.path=='/api/install':reply=session.installer.start(value)
                    elif self.path=='/api/check-install':reply=session.installer.check(value)
                    elif self.path=='/api/continue-install':reply=session.installer.continue_preview(value)
                    elif self.path in ['/api/restore','/api/choose-home','/api/previous-home','/api/revert-preview','/api/home-redirect-settings']:
                        reply=session.installer.action({'/api/restore':'restore','/api/choose-home':'home','/api/previous-home':'previous','/api/revert-preview':'revert','/api/home-redirect-settings':'accessibility'}[self.path],value)
                    elif self.path=='/api/quit':
                        if value!={}:raise SetupError('Unexpected close options.')
                        if session.companion.server:raise SetupError('Hosted Home is running. Use Stop hosted Home first; closing this computer disconnects the TV page.')
                        session.companion.disconnect()
                        self.send({'closed':True});threading.Thread(target=self.server.shutdown,daemon=True).start();return
                    else:self.send({'error':'This action is not available. No TV changes were made.'},status=404);return
                    self.send(reply)
            except SetupError as error:self.send({'error':str(error)},status=400)
            except (ValueError,TypeError,UnicodeError):self.send({'error':'Check the form details and try again.'},status=400)
            except OSError:self.send({'error':'Could not save the local record. Check free space and folder access.'},status=400)
    http=ThreadingHTTPServer(('127.0.0.1',port),Handler);http.daemon_threads=True
    return http,session
