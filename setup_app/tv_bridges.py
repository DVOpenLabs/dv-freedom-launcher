"""Small, explicit LAN adapters. No discovery scan, generic command API or privileged surfaces."""
import hashlib
import http.client
import json
from pathlib import Path
import queue
import re
import socket
import ssl
import time
from .core import SetupError,address

MAX_REPLY=256*1024

def bounded_json(raw):
    if len(raw)>MAX_REPLY:raise SetupError('TV reply exceeded the size limit.')
    try:return json.loads(raw)
    except (ValueError,UnicodeError):raise SetupError('The TV returned an unfamiliar reply. Keep its original Home available.')

def local_file(value,label):
    if not isinstance(value,str):raise SetupError('Choose '+label+'.')
    p=Path(value)
    if not p.is_absolute() or p.is_symlink() or not p.is_file() or not 0<p.stat().st_size<=65536:raise SetupError('Choose a small, unlinked local '+label+' file.')
    return p

def tls_profile(value):
    """Explicit CA trust plus endpoint pin, rather than disabling certificate verification."""
    ca=local_file(value.get('ca'),'TV CA certificate')
    pin=value.get('fingerprint','')
    if not isinstance(pin,str):raise SetupError('The TV fingerprint must be a SHA-256 string.')
    pin=pin.lower().replace(':','')
    if not re.fullmatch('[a-f0-9]{64}',pin):raise SetupError('Enter the verified TV certificate SHA-256 fingerprint from your trusted setup.')
    ctx=ssl.SSLContext(ssl.PROTOCOL_TLS_CLIENT);ctx.check_hostname=False
    ctx.verify_mode=ssl.CERT_REQUIRED
    try:ctx.load_verify_locations(cafile=str(ca))
    except (OSError,ssl.SSLError):raise SetupError('The trusted TV certificate could not be loaded. Choose a valid public PEM certificate.')
    return ctx,pin

class PinnedHTTPS(http.client.HTTPSConnection):
    def __init__(self,host,port,context,pin):super().__init__(host,port,timeout=6,context=context);self.pin=pin
    def connect(self):
        super().connect()
        if hashlib.sha256(self.sock.getpeercert(binary_form=True)).hexdigest()!=self.pin:
            self.close();raise SetupError('TV certificate changed. Recheck its fingerprint through your trusted setup; no request was sent.')

class Vizio:
    def __init__(self,host,profile,requester=None):
        self.host,_=address(host,7345);self.profile=profile;self.token='';self.challenge=None
        self.ctx,self.pin=tls_profile(profile);self.requester=requester
        self.identity=profile['identity']
    def request(self,method,path,value=None):
        if path not in ('/pairing/start','/pairing/pair','/pairing/cancel','/app/current','/app/launch'):
            raise SetupError('This Vizio operation is not allowed.')
        body=json.dumps(value).encode() if value is not None else None
        if self.requester:return self.requester(method,path,value,self.token)
        conn=PinnedHTTPS(self.host,7345,self.ctx,self.pin)
        try:
            headers={'Content-Type':'application/json'}
            if self.token:headers['AUTH']=self.token
            conn.request(method,path,body=body,headers=headers);r=conn.getresponse();raw=r.read(MAX_REPLY+1)
            if r.status!=200:raise SetupError('Vizio declined the request. Check pairing and firmware support; no automatic retry was made.')
            result=bounded_json(raw)
            if not isinstance(result,dict) or not isinstance(result.get('STATUS'),dict) or result['STATUS'].get('RESULT')!='SUCCESS':raise SetupError('Vizio did not confirm success. Check the TV before retrying.')
            return result
        except (OSError,ssl.SSLError,http.client.HTTPException):raise SetupError('Could not reach the trusted Vizio endpoint. Check its certificate, address and network.')
        finally:conn.close()
    def begin(self):
        r=self.request('PUT','/pairing/start',{'DEVICE_ID':self.identity,'DEVICE_NAME':'DV Freedom Setup'})
        item=r.get('ITEM',{})
        if not isinstance(item,dict) or type(item.get('PAIRING_REQ_TOKEN')) is not int or type(item.get('CHALLENGE_TYPE')) is not int:raise SetupError('Unrecognized pairing challenge. Nothing was installed.')
        self.challenge=(item['PAIRING_REQ_TOKEN'],item['CHALLENGE_TYPE'],time.monotonic())
        return {'pairingRequired':True,'message':'Enter the four-digit PIN displayed by this TV.'}
    def pair(self,pin):
        if not isinstance(pin,str) or not re.fullmatch(r'\d{4}',pin):raise SetupError('Enter the four-digit TV PIN.')
        if not self.challenge or time.monotonic()-self.challenge[2]>90:raise SetupError('Pairing expired. Start a new challenge on the same TV.')
        token,kind,_=self.challenge;self.challenge=None
        r=self.request('PUT','/pairing/pair',{'DEVICE_ID':self.identity,'CHALLENGE_TYPE':kind,'PAIRING_REQ_TOKEN':token,'RESPONSE_VALUE':pin})
        item=r.get('ITEM',{});auth=item.get('AUTH_TOKEN') if isinstance(item,dict) else None
        if not isinstance(auth,str) or not 1<=len(auth)<=1024 or re.search(r'[\x00-\x1f\x7f]',auth):raise SetupError('No valid pairing token was returned.')
        self.token=auth
    def current(self):
        if not self.token:raise SetupError('Pair this Vizio first.')
        r=self.request('GET','/app/current');item=r.get('ITEM',{})
        if 'VALUE' in item:item=item['VALUE']
        if not isinstance(item,dict):raise SetupError('Open a normal streaming app on the TV, then capture it again.')
        app=item.get('APP_ID');namespace=item.get('NAME_SPACE')
        if not isinstance(app,(str,int)) or not re.fullmatch(r'\d{1,8}',str(app)) or type(namespace) is not int or not 0<=namespace<=100:raise SetupError('This firmware did not expose a usable app ID.')
        if str(app) in ('17','3001'):raise SetupError('Capture a normal streaming app, not a system or Conjure surface.')
        return {'APP_ID':str(app),'NAME_SPACE':namespace}
    def launch(self,payload):
        if not self.token:raise SetupError('Pair this Vizio first.')
        if set(payload)-{'APP_ID','NAME_SPACE','MESSAGE'} or str(payload.get('APP_ID'))=='3001':raise SetupError('Privileged Vizio surfaces are excluded.')
        self.request('PUT','/app/launch',{'VALUE':payload})
    def close(self):self.token='';self.challenge=None

class Vidaa:
    """Existing authorized MQTT profile only. Never extracts/bundles vendor secrets or bypasses pairing."""
    def __init__(self,host,profile,factory=None):
        self.host,_=address(host,36669);self.profile=profile;self.factory=factory;self.client=None;self.replies=queue.Queue(maxsize=32);self.ready=False
        required={'clientId','topicId','username','password','certificate','key','ca','fingerprint'}
        if set(profile)!=required:raise SetupError('Use a working VIDAA MQTT profile with clientId, topicId, username, password, certificate, key, ca and fingerprint.')
        for k in ('clientId','topicId','username','password'):
            if not isinstance(profile[k],str) or not 1<=len(profile[k])<=256 or re.search(r'[\x00-\x1f\x7f]',profile[k]):raise SetupError('Invalid private MQTT profile.')
        if re.search(r'[/+#]',profile['topicId']):raise SetupError('The VIDAA topic identity must not contain MQTT wildcards or path separators.')
        self.context,self.pin=tls_profile(profile)
        try:self.context.load_cert_chain(str(local_file(profile['certificate'],'client certificate')),str(local_file(profile['key'],'client key')),password='')
        except (OSError,ssl.SSLError):raise SetupError('The existing client certificate/key could not be loaded. Encrypted or mismatched client keys are not supported by this pilot.')
    def connect(self):
        try:
            if self.factory:client=self.factory()
            else:
                import paho.mqtt.client as mqtt
                client=mqtt.Client(mqtt.CallbackAPIVersion.VERSION2,client_id=self.profile['clientId'],reconnect_on_failure=False)
            client.username_pw_set(self.profile['username'],self.profile['password']);client.tls_set_context(self.context)
            client.connect_timeout=6
            endpoint=self.profile['topicId'];prefix='/remoteapp/mobile/'+endpoint+'/ui_service/'
            def opened(c,u,sock):
                if hashlib.sha256(sock.getpeercert(binary_form=True)).hexdigest()!=self.pin:
                    sock.close();raise SetupError('VIDAA certificate pin changed; no MQTT login was sent.')
            def connected(c,u,f,reason,properties=None):
                if reason==0:
                    # Subscribe only to needed replies, not all remote traffic or other clients.
                    c.subscribe([(prefix+'data/applist',0),(prefix+'state',0)]);self.ready=True
            def message(c,u,msg):
                if len(msg.payload)<=MAX_REPLY and msg.topic in (prefix+'data/applist',prefix+'state'):
                    try:self.replies.put_nowait((msg.topic,bounded_json(msg.payload)))
                    except (queue.Full,SetupError):pass
            client.on_socket_open=opened;client.on_connect=connected;client.on_message=message
            client.on_disconnect=lambda *args:setattr(self,'ready',False)
            self.client=client;client.connect(self.host,36669,keepalive=20)
            deadline=time.monotonic()+6
            while not self.ready and time.monotonic()<deadline:client.loop(timeout=.2)
            if not self.ready:raise SetupError('VIDAA did not authorize this existing MQTT profile. Pair using your trusted companion first; newer firmware may need refreshed credentials.')
            client.loop_start() # Keepalive only for this explicit connection; no automatic reconnect.
            return self.inventory()
        except ImportError:raise SetupError('The optional VIDAA MQTT library is missing. Use the packaged Setup app or install setup_app/requirements.txt in your source environment.')
        except (OSError,ssl.SSLError):self.close();raise SetupError('VIDAA connection failed. Check the existing paired profile, trusted certificates and firmware.')
    def request(self,action,payload=None,reply=None):
        if not self.ready:raise SetupError('Connect the authorized VIDAA profile first.')
        while not self.replies.empty():self.replies.get_nowait()
        topic='/remoteapp/tv/ui_service/'+self.profile['topicId']+'/actions/'+action
        sent=self.client.publish(topic,json.dumps(payload) if payload is not None else '',qos=0)
        sent.wait_for_publish(timeout=2)
        if not sent.is_published():raise SetupError('VIDAA send was not confirmed. Check the TV before retrying.')
        deadline=time.monotonic()+6
        while time.monotonic()<deadline:
            if not reply:return {'requested':True,'foregroundVerified':False}
            try:
                name,value=self.replies.get(timeout=.2)
                if name.endswith(reply):return value
            except queue.Empty:pass
        raise SetupError('VIDAA did not return the requested catalog. No launch or install was attempted.')
    def inventory(self):
        raw=self.request('applist',reply='/data/applist')
        if not isinstance(raw,list) or len(raw)>512:raise SetupError('VIDAA returned an invalid catalog.')
        return raw
    def launch(self,payload):return self.request('launchapp',payload)
    def close(self):
        self.ready=False
        if self.client:
            try:self.client.disconnect()
            except OSError:pass
            self.client.loop_stop()
            self.client=None
