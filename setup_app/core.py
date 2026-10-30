"""Explicit read-only device operations. No install, shell or Home-change API."""
from __future__ import annotations
import copy
from datetime import datetime, timezone
import ipaddress
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import subprocess
import threading

APP_NAMES = {'com.netflix.ninja':'Netflix','com.google.android.youtube.tv':'YouTube',
 'com.amazon.amazonvideo.livingroom':'Prime Video','com.crunchyroll.crunchyroid':'Crunchyroll',
 'com.mvas.stb.emu.pro':'STB Emu Pro','com.mvas.stb.emu.free':'STB Emu',
 'com.disney.disneyplus':'Disney+','com.hbo.hbonow':'HBO Max','com.plexapp.android':'Plex'}
DEMO_APPS = [{'package':'example.'+x,'name':n} for x,n in [('video','Video'),('music','Music'),('cinema','Cinema'),('live','Live TV'),('library','Library'),('photos','Photos')]]
NETWORKS = [ipaddress.ip_network(s) for s in ['10.0.0.0/8','172.16.0.0/12','192.168.0.0/16']]
COMMANDS = {'model':'getprop ro.product.model', 'android':'getprop ro.build.version.release',
 'api':'getprop ro.build.version.sdk','space':'df -k /data',
 'home':'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME',
 'apps':'cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LEANBACK_LAUNCHER',
 'packages':'pm list packages','disabled':'pm list packages -d'}

class SetupError(ValueError): pass

def address(host, port):
    try:
        if not isinstance(host,str): raise ValueError()
        ip=ipaddress.ip_address(host)
        if ip.version != 4 or not any(ip in n for n in NETWORKS): raise ValueError()
        if type(port) is not int or not 1 <= port <= 65535: raise ValueError()
        if int(str(ip).split('.')[-1]) in (0,255): raise ValueError()
        return str(ip),port
    except (ValueError,TypeError):
        raise SetupError('Enter your TV’s local IPv4 address and a port from 1 to 65535. Find them in the TV’s network or debugging settings.')

def private_dir():
    root=Path(os.environ.get('LOCALAPPDATA',Path.home()/'.local/share'))/'DVFreedomSetup'
    root.mkdir(parents=True,exist_ok=True,mode=0o700)
    return root

class Device:
    """Existing adb-shell transport for TCP ADB; optional local adb for TLS pairing."""
    def __init__(self,host,port,root,paired=False):
        self.host,self.port=address(host,port);self.root=root;self.paired=paired;self.device=None
    def _adb(self,args,code=None):
        from .platform_tools import find_adb
        adb=find_adb(self.root)
        if not adb: raise SetupError('Pairing-code connections need Android Platform Tools on this computer. Use the official download linked in Help, or a TV’s supported USB/network debugging option.')
        try:
            r=subprocess.run([adb,*args],input=code,capture_output=True,text=True,timeout=30,
              creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
        except subprocess.SubprocessError: raise SetupError('Pairing timed out. Keep the pairing dialog open and try the new code and port shown there.')
        if r.returncode or 'failed' in (r.stdout+r.stderr).lower() or 'cannot' in (r.stdout+r.stderr).lower():
            raise SetupError('Could not pair or connect. Check the current port, keep both devices on the same network and approve the TV prompt.')
        return r.stdout
    def pair(self,port,code):
        address(self.host,port)
        if not isinstance(code,str) or not re.fullmatch(r'\d{6}',code): raise SetupError('Enter the six-digit code shown on the TV.')
        self._adb(['pair',f'{self.host}:{port}'],code+'\n')
    def connect(self):
        if self.paired:
            self._adb(['connect',f'{self.host}:{self.port}']);return
        try:
            from adb_shell.adb_device import AdbDeviceTcp
            from adb_shell.auth.keygen import keygen
            from adb_shell.auth.sign_pythonrsa import PythonRSASigner
            folder=self.root/'keys';folder.mkdir(mode=0o700,exist_ok=True);key=folder/'adbkey'
            if not key.exists():
                mask=os.umask(0o077)
                try:keygen(str(key))
                finally:os.umask(mask)
            key.chmod(0o600)
            signer=PythonRSASigner((key.with_suffix('.pub')).read_text(),key.read_text())
            self.device=AdbDeviceTcp(self.host,self.port,default_transport_timeout_s=6)
            if not self.device.connect(rsa_keys=[signer],auth_timeout_s=20): raise SetupError('TV authorization was not accepted. Choose Allow on the TV, then try again.')
        except ImportError: raise SetupError('This source checkout is missing its connection libraries. Use the packaged app, or install setup_app/requirements.txt for development.')
        except SetupError: raise
        except Exception: raise SetupError('Could not connect. Check the address, turn on the TV and debugging, then choose Allow on the TV. Pairing-code ports require the pairing option.')
    def read(self,key):
        if key not in COMMANDS: raise SetupError('Unsupported device check.')
        if self.paired:return self._adb(['-s',f'{self.host}:{self.port}','shell',COMMANDS[key]])[:131072]
        return self.device.shell(COMMANDS[key],transport_timeout_s=8,read_timeout_s=12)[:131072]
    def close(self):
        if self.device:self.device.close()

def inspect(device):
    values={k:device.read(k).strip() for k in ['model','android','api','space','home','apps','packages','disabled']}
    try: api=int(values['api'])
    except ValueError: raise SetupError('The device did not return a valid Android version. Reconnect to the TV rather than a different network device.')
    if api<26:raise SetupError('This launcher needs Android 8 or newer. Nothing was changed on your TV.')
    packages=set(re.findall(r'^package:([\w.]+)$',values['packages'],re.M))
    apps=sorted(set(re.findall(r'^([\w.]+)/[^\s]+$',values['apps'],re.M)))
    apps=[{'package':p,'name':APP_NAMES.get(p,p)} for p in apps if p in packages and p not in {'com.gathin.tvhome','com.dvopenlabs.cytv.usabilitytest','com.dvopenlabs.freedomlauncher'}]
    return {'model':values['model'][:120],'android':values['android'][:24],'api':api,'apps':apps[:256],
       'existingLauncher':bool(packages & {'com.gathin.tvhome','com.dvopenlabs.cytv.usabilitytest','com.dvopenlabs.freedomlauncher'}),
       'standaloneInstalled':'com.dvopenlabs.freedomlauncher' in packages,
       'legacyLauncher':bool(packages & {'com.gathin.tvhome','com.dvopenlabs.cytv.usabilitytest'}),
       'checks':{'android':True,'connection':True},'raw':values}

def validate_plan(value,apps):
    if not isinstance(value,dict) or set(value)!={'style','position','size','spacing','wallpaper','apps'}: raise SetupError('The setup plan is incomplete. Review your Home choices again.')
    enums={'style':['classic','clean','glass','cinema'],'position':['top','middle','bottom'],
        'size':['small','medium','large'],'spacing':['compact','comfortable','wide'],'wallpaper':['keep','coast','dusk','forest']}
    for key,choices in enums.items():
        if value[key] not in choices:raise SetupError('Choose a supported '+key+'.')
    selected=value['apps'];available={a['package'] for a in apps}
    if not isinstance(selected,list) or not 1<=len(selected)<=8 or any(not isinstance(p,str) for p in selected) or len(set(selected))!=len(selected) or not set(selected)<=available:
        raise SetupError('Choose one to eight available apps, without duplicates.')
    return copy.deepcopy(value)

class Session:
    def __init__(self,root=None,device_factory=Device):
        self.root=Path(root) if root else private_dir();self.factory=device_factory;self.lock=threading.Lock()
        self.mode='preview';self.inventory=None;self.backup=None;self.job={'status':'idle'}
        self.connection=None
        self.dispatch_lock=threading.Lock()
        from .installer import Installer
        self.installer=Installer(self)
        from .addons import AddonCenter
        self.addons=AddonCenter(self)
        from .platform_tools import PairingTools
        self.tools=PairingTools(self.root)
        from .native import NativeInstaller
        self.native=NativeInstaller(self.root)
        from .companion import CompanionSetup
        self.companion=CompanionSetup(self.root)
        from .phone import PhoneSetup
        self.phone=PhoneSetup(self)
    def public(self):
        info=copy.deepcopy(self.inventory)
        if info:info.pop('raw',None)
        install=self.installer.state()
        return {'mode':self.mode,'device':info,'backup':self.backup,'job':copy.deepcopy(self.job),'tools':self.tools.state(),**install,
          'native':self.native.public(),'companion':self.companion.public(),'installAvailable':install['release']['available'] and self.mode=='device' and bool(self.inventory),'releaseMessage':install['release'].get('message','Offline signed launcher ready. Default Home changes only through your TV’s chooser.')}
    def preview(self):
        self.mode='preview';self.backup=None;self.job={'status':'idle'};self.connection=None;self.installer.reviewed=None
        self.inventory={'model':'Example Google TV','android':'14','api':34,'apps':copy.deepcopy(DEMO_APPS),'existingLauncher':False,'checks':{'android':True,'connection':True}}
        return self.public()
    def connect(self,value):
        if not isinstance(value,dict) or set(value)-{'host','port','pairPort','code'} or not {'host','port'}<=set(value):raise SetupError('Enter the connection details shown on the TV.')
        host,port=address(value['host'],value['port'])
        paired='pairPort' in value or 'code' in value
        if paired:
            if not {'pairPort','code'}<=set(value):raise SetupError('Enter the pairing port and six-digit code.')
            address(host,value['pairPort'])
            if not isinstance(value['code'],str) or not re.fullmatch(r'\d{6}',value['code']):raise SetupError('Enter the six-digit code shown on the TV.')
        if not self.lock.acquire(blocking=False):raise SetupError('A connection check is already running. Wait for it to finish.')
        self.mode='device';self.inventory=None;self.backup=None;self.connection=None;self.installer.reviewed=None;self.job={'status':'running','message':'Waiting for the TV. Choose Allow if a prompt appears.'}
        def run():
            device=None
            try:
                device=self.factory(host,port,self.root,paired)
                if paired:device.pair(value['pairPort'],value['code'])
                device.connect();self.inventory=inspect(device);self.connection={'host':host,'port':port,'paired':paired};self.job={'status':'done','message':'TV details read. Nothing has been installed or changed.'}
            except SetupError as error:self.job={'status':'error','message':str(error)}
            except Exception:self.job={'status':'error','message':'The TV stopped responding. Keep it on, check debugging and reconnect. Nothing was installed.'}
            finally:
                if device:
                    try:device.close()
                    except Exception:pass
                self.lock.release()
        threading.Thread(target=run,daemon=True).start();return {'started':True}
    def snapshot(self):
        if self.job['status']=='running':raise SetupError('Wait for the connection check to finish.')
        if not self.inventory:raise SetupError('Connect to a TV first, or choose the preview journey.')
        if self.mode=='preview':self.backup={'preview':True,'message':'Example device record only. No file or TV backup was created.'};return self.public()
        self.root.mkdir(parents=True,exist_ok=True,mode=0o700)
        folder=self.root/'backups';folder.mkdir(exist_ok=True,mode=0o700)
        file=folder/(datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')+'-'+secrets.token_hex(4)+'.json')
        record={'schema':1,'kind':'device-record','capturedAt':datetime.now(timezone.utc).isoformat(),'device':self.inventory,
          'limits':'Read-only model, Android, storage, package lists and resolved Home record. Does not contain app private data, accounts, wallpaper media or a restorable launcher profile.'}
        fd=os.open(file,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
        with os.fdopen(fd,'w',encoding='utf-8') as out:json.dump(record,out,indent=2)
        self.backup={'preview':False,'path':str(file),'message':'Device record saved. This is not a full TV or launcher-data backup.'};return self.public()
    def plan(self,value):
        if not self.inventory:raise SetupError('Connect to a TV or start the preview journey first.')
        draft=validate_plan(value,self.inventory['apps'])
        return {'schema':1,'kind':'dv-freedom-setup-plan','mode':self.mode,'applied':False,'choices':draft,
          'note':'A setup plan, not a launcher import file. No install or TV settings were applied.'}
