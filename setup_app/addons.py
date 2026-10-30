"""Reviewed, signed offline companions. No arbitrary package, URL or permission commands."""
import base64
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import re
import secrets
import shlex
import time
from .core import SetupError
from .release import canonical, digest, apk_certificate, MAX_APK
from .transport import InstallerDevice

PACKAGES={
 'weather':'io.github.sumyabazar1.weather',
 'sleep-timer':'io.github.luyzfernando08.sleeptimer',
 'world-clock':'io.github.savioronezero.worldclock',
 'quiet-hours':'io.github.maxhayim.quiethours',
 'companions':'com.dvopenlabs.freedom.companions',
 'phone-remote':'com.dvopenlabs.freedom.remote',
 'camera':'com.dvopenlabs.freedom.camera',
 'tv-inspector':'io.github.shaharukhs.tvinspector',
 'bluetooth-connect':'com.gathin.btconnect',
 'digital-rain':'io.github.dennisoleary.digitalrain',
 'news-screensaver':'com.gathin.newsdream',
 'headphones':'io.github.maxhayim.headphones',
 'capacity-stats':'com.lionlev.tv.capacity',
 'tv-hub':'io.github.maxhayim.tvhub',
 'ad-blocking':'io.github.maxhayim.adblocking',
 'flock':'io.github.cleanyourtv.flock',
 'jewish-calendar':'com.lionlev.tv.jewishcalendar',
 'nepali-calendar':'com.oneyuv.tv.nepalicalendar',
 'prayer-times':'com.majnioui.tv.prayer',
}

class AddonCatalog:
    def __init__(self,folder=None,trust=None):
        self.folder=Path(folder) if folder else Path(__file__).with_name('release_bundle')/'addons'
        self.trust=Path(trust) if trust else Path(__file__).with_name('trust.json')
    def verify(self,id):
        try:
            from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PublicKey
            if id not in PACKAGES:raise ValueError()
            raw=(self.folder/'catalog.json').read_bytes()
            if len(raw)>32768:raise ValueError()
            envelope=json.loads(raw);body=envelope['addons'];key=json.loads(self.trust.read_text(encoding='utf-8'))
            Ed25519PublicKey.from_public_bytes(base64.b64decode(key['publicKey'],validate=True)).verify(base64.b64decode(envelope['signature'],validate=True),canonical(body))
            if set(body)!={'schema','items'} or body['schema']!=1 or not isinstance(body['items'],list) or len(body['items'])!=len(PACKAGES):raise ValueError()
            ids=[item['id'] for item in body['items']]
            if len(set(ids))!=len(PACKAGES) or set(ids)!=set(PACKAGES):raise ValueError()
            item=next(x for x in body['items'] if x['id']==id)
            if set(item)!={'id','package','file','versionCode','versionName','minSdk','bytes','sha256','certificate','permissions'}:raise ValueError()
            if item['package']!=PACKAGES[id] or item['file']!=id+'.apk' or type(item['versionCode']) is not int or item['versionCode']<1 or type(item['minSdk']) is not int or not 26<=item['minSdk']<=34:raise ValueError()
            if type(item['bytes']) is not int or not 0<item['bytes']<=MAX_APK or not isinstance(item['versionName'],str) or len(item['versionName'])>80:raise ValueError()
            if not isinstance(item['permissions'],list) or len(item['permissions'])>24 or any(not isinstance(p,str) or not re.fullmatch(r'[A-Za-z][A-Za-z0-9_.]{1,160}',p) for p in item['permissions']):raise ValueError()
            if any(not isinstance(item[k],str) or not re.fullmatch('[0-9a-f]{64}',item[k]) for k in ['sha256','certificate']):raise ValueError()
            path=self.folder/item['file']
            if path.stat().st_size!=item['bytes'] or digest(path)!=item['sha256'] or apk_certificate(path)!=item['certificate']:raise ValueError()
            return {**item,'path':path}
        except Exception:raise SetupError('This companion is missing or failed signed-bundle verification. Rebuild or obtain a trusted Setup package. Nothing was installed.')
    def summary(self,id):
        try:return {'available':True,**{k:v for k,v in self.verify(id).items() if k!='path'}}
        except SetupError as error:return {'available':False,'message':str(error)}

class AddonDevice(InstallerDevice):
    def status(self,package):
        if package not in PACKAGES.values():raise SetupError('Unsupported companion package.')
        info=self.metadata(package)
        if not info:return {'installed':False,'versionCode':None,'access':'not-installed'}
        raw=self._shell('dumpsys package '+package)
        # Only runtime grants can be reported reliably here. Special access remains an on-TV choice.
        missing=sorted(set(re.findall(r'^\s*(android\.permission\.[A-Z_]+): granted=false',raw,re.M)))
        return {'installed':True,'versionCode':info['versionCode'],'access':'needs-access' if missing else 'check-on-tv','missingPermissions':missing}
    def settings(self,package):
        if package not in PACKAGES.values():raise SetupError('Unsupported companion package.')
        if not self.metadata(package):raise SetupError('Install this companion before opening its settings.')
        self._shell('am start -a android.settings.APPLICATION_DETAILS_SETTINGS -d package:'+package)
    def metadata(self,package):
        if package not in PACKAGES.values():raise SetupError('Unsupported companion package.')
        paths=self._shell('pm path '+package).strip().splitlines()
        if not paths:return None
        if len(paths)!=1 or not paths[0].startswith('package:'):raise SetupError('Split companion installations require manual review.')
        path=paths[0][8:]
        if not re.fullmatch(r'/data/app/[A-Za-z0-9_./+=~-]+/base\.apk',path) or '..' in path:raise SetupError('Unsupported companion APK path.')
        version=re.search(r'\bversionCode=(\d+)',self._shell('dumpsys package '+package))
        if not version:raise SetupError('Could not read companion version.')
        return {'path':path,'versionCode':int(version[1])}
    def open_addon(self,package):
        if package not in PACKAGES.values():raise SetupError('Unsupported companion.')
        if package in {PACKAGES['companions'],PACKAGES['phone-remote']}:
            self._shell('am start -n '+package+'/'+package+('.HubActivity' if package==PACKAGES['companions'] else '.RemoteActivity'));return
        lines=self._shell('cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.LEANBACK_LAUNCHER -p '+package).strip().splitlines()
        component=lines[-1] if lines else ''
        if not component.startswith(package+'/') or not re.fullmatch(r'[A-Za-z][\w.]*/[A-Za-z.][\w.$]*',component):raise SetupError('No compatible TV activity was found.')
        self._shell('am start -n '+shlex.quote(component))

class AddonCenter:
    def __init__(self,session,catalog=None,factory=AddonDevice):
        self.session=session;self.catalog=catalog or AddonCatalog();self.factory=factory;self.reviewed=None;self.checked={};self.checked_connection=None
    def catalogue(self):
        from .addon_directory import items
        connected=self.session.mode=='device' and bool(self.session.connection)
        checked=self.checked if connected and self.checked_connection==self.session.connection else {}
        result=[]
        api=(self.session.inventory or {}).get('api',0)
        for item in items():
            release=self.catalog.summary(item['id']) if item['id'] in PACKAGES else {'available':False,'message':'Separate build / guide; not installed by Setup.'}
            device=checked.get(item['id'],{'installed':None,'access':'not-checked'})
            status='guide' if item['distribution']!='signed' else 'unavailable'
            if item['distribution']=='signed' and release['available']:
                status='unsupported' if connected and api<release.get('minSdk',26) else 'available'
                if status!='unsupported' and device.get('installed'):
                    status='update' if device['versionCode']<release.get('versionCode',0) else 'installed'
            result.append({**item,'release':release,'device':device,'status':status})
        return {'schema':2,'connected':connected,'checked':bool(checked),'note':'Optional apps, reviewed one at a time. Home, wallpapers and app order stay saved. Access and connections are separate choices. Android apps cannot run on native LG/Samsung.', 'items':result}
    def refresh(self,value):
        if value!={}:raise SetupError('Unexpected status options.')
        if not self.session.installer.lock.acquire(False):raise SetupError('Another installation is running.')
        d=None
        try:
            connection=dict(self.session.connection or {});d=self._open()
            checked={id:d.status(pkg) for id,pkg in PACKAGES.items()}
            if connection!=self.session.connection:raise SetupError('TV connection changed. Check again.')
            self.checked=checked;self.checked_connection=connection
            return self.catalogue()
        finally:
            if d:d.close()
            self.session.installer.lock.release()
    def settings(self,value):
        if not isinstance(value,dict) or set(value)!={'id'} or value['id'] not in PACKAGES:raise SetupError('Choose a companion.')
        if not self.session.installer.lock.acquire(False):raise SetupError('Another installation is running.')
        d=None
        try:
            d=self._open();d.settings(PACKAGES[value['id']]);return {'message':'App settings opened on TV. Choose access there; Setup did not grant anything.'}
        finally:
            if d:d.close()
            self.session.installer.lock.release()
    def _open(self):
        s=self.session
        if s.mode!='device' or not s.inventory or not s.connection:raise SetupError('Connect to an Android TV first. Native LG/Samsung use separate adapters; preview mode cannot install.')
        c=s.connection;d=self.factory(c['host'],c['port'],s.root,c['paired'])
        try:d.connect();return d
        except Exception:d.close();raise
    def _installed(self,d,item,folder):
        info=d.metadata(item['package'])
        if info is None:return None
        apk=folder/'before.apk';d.pull(info['path'],apk)
        if apk_certificate(apk)!=item['certificate']:raise SetupError('The installed companion has a different signing key. Updating is blocked; nothing will be uninstalled.')
        if info['versionCode']>item['versionCode']:raise SetupError('A newer companion is installed. Downgrading is blocked.')
        return {**info,'sha256':digest(apk)}
    def review(self,value):
        if not isinstance(value,dict) or set(value)!={'id'} or value['id'] not in PACKAGES:raise SetupError('Choose a reviewed companion.')
        item=self.catalog.verify(value['id']);d=None
        if not self.session.installer.lock.acquire(False):raise SetupError('Another installation is running.')
        try:
            d=self._open();api=d.read('api').strip()
            if not api.isdigit() or int(api)<item['minSdk']:raise SetupError('This companion needs a newer Android version.')
            folder=self.session.root/'addon-recovery'/secrets.token_hex(16);folder.mkdir(parents=True,mode=0o700)
            old=self._installed(d,item,folder);home=d.read('home').strip().splitlines()[-1]
            if not re.fullmatch(r'[A-Za-z][\w.]*/[A-Za-z.][\w.$]*',home) or any(x in home.lower() for x in ['factory','resolveractivity','chooseractivity']):raise SetupError('Select a stable Home before reviewing this installation.')
            record={'schema':1,'createdAt':datetime.now(timezone.utc).isoformat(),'device':d.fingerprint(),'originalHome':home,'release':{k:v for k,v in item.items() if k!='path'},'before':old,'state':'reviewed'}
            (folder/'record.json').write_text(json.dumps(record,indent=2)+'\n',encoding='utf-8')
            token=secrets.token_urlsafe(24);self.reviewed={'id':value['id'],'token':token,'folder':folder,'record':record,'created':time.monotonic(),'connection':dict(self.session.connection)}
            return {'id':value['id'],'reviewToken':token,'operation':'Update' if old else 'Install','release':record['release'],'recoveryPath':str(folder),'homeChanges':False,'accessEnabled':False,'dataBackup':False,'note':'The previous APK is saved for updates. Android private app data is not backed up. No downgrade or automatic rollback; keep this recovery record for manual support.'}
        finally:
            if d:d.close()
            self.session.installer.lock.release()
    def install(self,value):
        r=self.reviewed
        if not r or not isinstance(value,dict) or set(value)!={'id','reviewToken'} or value['id']!=r['id'] or value['reviewToken']!=r['token'] or time.monotonic()-r['created']>300 or self.session.connection!=r['connection']:raise SetupError('Review this companion again before installing.')
        if not self.session.installer.lock.acquire(False):raise SetupError('Another installation is running.')
        self.reviewed=None;d=None;record=r['record'];folder=r['folder']
        try:
            item=self.catalog.verify(r['id']);d=self._open()
            if d.fingerprint()!=record['device'] or d.read('home').strip().splitlines()[-1]!=record['originalHome']:raise SetupError('TV or Home changed. Review again; nothing was installed.')
            current=d.metadata(item['package'])
            if (current is None)!=(record['before'] is None):raise SetupError('Installed companion changed. Review again.')
            if current:
                check=folder/'checked.apk';d.pull(current['path'],check)
                if current['versionCode']!=record['before']['versionCode'] or digest(check)!=record['before']['sha256']:raise SetupError('Companion changed after review. Review again.')
            if {k:v for k,v in item.items() if k!='path'}!=record['release']:raise SetupError('Signed companion changed. Review again.')
            record['state']='installing';self._save(folder,record);d.install(item['path'],item['sha256']);d.preserve_home(record['originalHome'])
            current=d.metadata(item['package'])
            if not current or current['versionCode']!=item['versionCode']:raise SetupError('Install result needs review.')
            d.pull(current['path'],folder/'installed.apk')
            if digest(folder/'installed.apk')!=item['sha256']:raise SetupError('Installed APK does not match the reviewed companion.')
            record['state']='verified';self._save(folder,record);self.checked.pop(r['id'],None);return {'installed':True,'id':r['id'],'message':'Installed and verified. Connections, runtime permissions and optional services were not enabled. Open on TV to configure.'}
        except Exception:
            record['state']='needs-attention';self._save(folder,record);raise
        finally:
            if d:d.close()
            self.session.installer.lock.release()
    def launch(self,value):
        if not isinstance(value,dict) or set(value)!={'id'} or value['id'] not in PACKAGES:raise SetupError('Choose a companion.')
        if not self.session.installer.lock.acquire(False):raise SetupError('Another installation is running.')
        d=None
        try:
            item=self.catalog.verify(value['id']);d=self._open();current=d.metadata(item['package'])
            if not current:raise SetupError('Install this reviewed companion first.')
            folder=self.session.root/'addon-checks';folder.mkdir(parents=True,exist_ok=True,mode=0o700);apk=folder/(value['id']+'.apk');d.pull(current['path'],apk)
            if apk_certificate(apk)!=item['certificate'] or digest(apk)!=item['sha256']:raise SetupError('The installed companion differs from this reviewed bundle. Check its source before opening it.')
            d.open_addon(item['package']);return {'opened':True,'message':'Opened on TV. Configure private connection details there. No grants or services were enabled by Setup.'}
        finally:
            if d:d.close()
            self.session.installer.lock.release()
    @staticmethod
    def _save(folder,record):
        temp=folder/'record.tmp';temp.write_text(json.dumps(record,indent=2)+'\n',encoding='utf-8');temp.replace(folder/'record.json')
