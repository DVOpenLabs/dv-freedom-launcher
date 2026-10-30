"""Reviewed offline installation, durable recovery points and native timed previews."""
import copy
from datetime import datetime,timezone
import hashlib
import json
import os
import re
from pathlib import Path
import secrets
import threading
from .core import SetupError,validate_plan
from .release import Catalog,apk_certificate,digest,canonical
from .transport import InstallerDevice,PACKAGE

class Installer:
    def __init__(self,session,catalog=None,factory=InstallerDevice):
        self.session=session;self.catalog=catalog or Catalog();self.factory=factory
        self.points=session.root/'recovery';self.reviewed=None;self.active=None;self.cancelled=threading.Event()
        self.progress={'status':'idle','steps':[]};self.lock=threading.Lock()
    def state(self):
        return {'release':self.catalog.summary(),'transaction':{**self.progress,'steps':list(self.progress.get('steps',[]))},
          'recoveryPoints':[{'id':p.stem,'createdAt':v.get('createdAt'),'state':v.get('state'),'version':v.get('release',{}).get('versionName')} for p,v in self._records()]}
    def _records(self):
        result=[]
        if self.points.is_dir():
            for p in sorted(self.points.glob('*.json'),key=lambda p:p.stat().st_mtime,reverse=True)[:30]:
                try:
                    if p.stat().st_size<=1024*1024:
                        value=json.loads(p.read_text(encoding='utf-8'))
                        if isinstance(value,dict) and value.get('id')==p.stem and isinstance(value.get('release'),dict) and all(isinstance(value.get(k),str) for k in ['createdAt','state','device']):result.append((p,value))
                except (OSError,ValueError):continue
        return result
    def _save(self,value):
        self.points.mkdir(parents=True,exist_ok=True,mode=0o700);p=self.points/(value['id']+'.json');tmp=p.with_suffix('.tmp')
        fd=os.open(tmp,os.O_WRONLY|os.O_CREAT|os.O_TRUNC,0o600)
        with os.fdopen(fd,'w',encoding='utf-8') as out:json.dump(value,out,indent=2);out.flush();os.fsync(out.fileno())
        os.replace(tmp,p)
    def _load(self,id):
        if not isinstance(id,str) or len(id)!=32 or any(c not in '0123456789abcdef' for c in id):raise SetupError('Invalid recovery point.')
        try:
            file=self.points/(id+'.json')
            if file.stat().st_size>1024*1024:raise ValueError()
            value=json.loads(file.read_text(encoding='utf-8'))
            if not isinstance(value,dict) or value.get('id')!=id or not {'device','release','choices','state','originalHome','backup'}<=set(value):raise ValueError()
            return value
        except (OSError,ValueError):raise SetupError('This recovery point is unavailable.')
    def _open(self):
        target=getattr(self.session,'connection',None)
        if self.session.mode!='device' or not self.session.inventory or not target:raise SetupError('Connect to the TV first. Preview mode cannot install.')
        d=self.factory(target['host'],target['port'],self.session.root,target['paired']);d.connect();return d
    def _native(self,d):
        value=d.provider('status')
        if value.get('api')!=1 or value.get('package')!=PACKAGE:raise SetupError('The launcher setup interface is incompatible. No settings were applied.')
        return value
    def _installed(self,d,release,point=None):
        info=d.metadata()
        if info is None:return None
        self.points.mkdir(parents=True,exist_ok=True,mode=0o700);path=self.points/('checked-'+secrets.token_hex(8)+'.apk')
        try:
            d.pull(info['path'],path)
            if apk_certificate(path)!=release['certificate']:raise SetupError('An app with this identity has a different signing key. Updating it is blocked; nothing will be uninstalled.')
            if info['versionCode']>release['versionCode']:raise SetupError('A newer launcher is installed. Downgrading is blocked.')
            value={**info,'sha256':digest(path)}
            if point is not None:
                old=self.points/(point+'-before.apk');os.replace(path,old);value['backupApk']=old.name
            return value
        finally:
            if path.exists():path.unlink()
    def review(self,choices):
        if not self.lock.acquire(blocking=False):raise SetupError('Wait for the current installer operation.')
        d=None
        try:
            release=self.catalog.verify();plan=validate_plan(choices,self.session.inventory['apps'] if self.session.inventory else [])
            d=self._open();fingerprint=d.fingerprint();native=None;id=secrets.token_hex(16)
            installed=self._installed(d,release,id)
            if installed:native=self._native(d)
            lines=d.read('home').strip().splitlines();home=lines[-1] if lines else ''
            if not re.fullmatch(r'[A-Za-z][\w.]*/[A-Za-z.][\w.$]*',home) or any(x in home.lower() for x in ['factory','hkcfac']):raise SetupError('The original Home could not be safely recorded. Installation is blocked.')
            if 'ResolverActivity' in home or 'ChooserActivity' in home:raise SetupError('Select your current original Home as the default in TV settings first, then prepare the recovery point again. Nothing was installed.')
            point={'id':id,'createdAt':datetime.now(timezone.utc).isoformat(),'state':'reviewed','device':fingerprint,'originalHome':home,
              'release':{k:v for k,v in release.items() if k!='path'},'installed':installed,'backup':native['backup'] if native else None,
              'beforeProfile':native['profile'] if native else None,'beforeRevision':native['revision'] if native else None,'choices':plan}
            self._save(point);token=secrets.token_urlsafe(24);self.reviewed={'id':id,'token':token,'hash':hashlib.sha256(canonical(plan)).hexdigest()}
            return {'id':id,'reviewToken':token,'operation':'Update launcher' if installed else 'Install launcher',
              'version':release['versionName'],'backupPath':str(self.points/(id+'.json')),
              'backupKind':'Launcher settings, app order and media references' if native else 'Original Home and device binding (new app)',
              'mediaIncluded':False,'defaultHomeChanges':False,'signedApk':True,'newProfile':True}
        finally:
            if d:d.close()
            self.lock.release()
    def start(self,value):
        if not isinstance(value,dict) or set(value)!={'id','reviewToken'} or not self.reviewed or value!= {'id':self.reviewed['id'],'reviewToken':self.reviewed['token']}:raise SetupError('Review these exact choices again before installation.')
        point=self._load(value['id']);self.reviewed=None
        return self._job(point,self._install)
    def _job(self,point,operation):
        if not self.lock.acquire(blocking=False):raise SetupError('Another installer operation is running.')
        self.active=point['id'];self.cancelled.clear();self.progress={'id':point['id'],'status':'running','steps':[]}
        def run():
            try:operation(point)
            except Exception as error:
                point['state']='needs-attention';point['lastError']=str(error) if isinstance(error,SetupError) else 'Connection interrupted. Reconnect and check this recovery point before continuing.';self._save(point)
                self.progress.update(status='needs-attention',message=point['lastError'])
            finally:self.lock.release()
        threading.Thread(target=run,daemon=True).start();return {'started':True,'id':point['id']}
    def _step(self,point,label,state):
        point['state']=state;self._save(point);self.progress['steps'].append(label);self.progress['message']=label
    def _check_cancel(self,point):
        if self.cancelled.is_set():
            point['state']='paused';self._save(point);self.progress.update(status='paused',message='Stopped at a safe boundary. Original default Home is unchanged. Reconnect and check the recovery point to continue.');return True
        return False
    def _bound(self,d,point):
        if d.fingerprint()!=point['device']:raise SetupError('This is not the TV recorded in the recovery point. No changes were made.')
    def _install(self,point):
        d=None
        try:
            release=self.catalog.verify()
            if release['sha256']!=point['release']['sha256']:raise SetupError('The offline release changed. Review again.')
            d=self._open();self._bound(d,point);current=self._installed(d,release)
            if current!=({k:v for k,v in point['installed'].items() if k!='backupApk'} if point['installed'] else None):raise SetupError('The installed launcher changed since review. Back up and review again.')
            if current:
                native=self._native(d)
                if native['revision']!=point['beforeRevision'] or native['profile']!=point['beforeProfile']:raise SetupError('Home changed since review. Review the new setup before updating.')
            home=d.read('home').strip().splitlines()
            if not home or home[-1]!=point['originalHome']:raise SetupError('The default Home changed since review. Review again before installation.')
            self._step(point,'Verified release, TV identity and recovery point','verified')
            if self._check_cancel(point):return
            if not current or current['sha256']!=release['sha256']:
                self._step(point,'Installing the signed launcher; preserving your recorded Home','installing')
                d.install(release['path'],release['sha256'])
            self._step(point,'Checking the installed app','installed')
            current=self._installed(d,release)
            if not current or current['sha256']!=release['sha256']:raise SetupError('The installed APK did not match the verified release. Use the recovery point; default Home was not changed.')
            d.preserve_home(point['originalHome'])
            d.launch();native=self._native(d)
            if point['backup'] is None:point['backup']=native['backup'];point['beforeProfile']=native['profile'];point['beforeRevision']=native['revision'];self._save(point)
            if self._check_cancel(point):return
            self._preview(d,point,native)
        finally:
            if d:d.close()
    def _preview(self,d,point,native):
        self._step(point,'Opening the timed preview on your TV','previewing')
        value=d.provider('preview',{'revision':native['revision'],'profile':native['profile'],'requestId':point['id'],'choices':point['choices']})
        point['previewId']=value['pending']['id'] if value.get('pending') else None;self._save(point)
        self.progress.update(status='awaiting-tv',message='Look at your TV. Keep this Home, or choose Revert. It reverts automatically after 45 seconds.')
    def check(self,value):
        if not isinstance(value,dict) or set(value)!={'id'}:raise SetupError('Choose a recovery point.')
        point=self._load(value['id'])
        if not self.lock.acquire(blocking=False):raise SetupError('Wait for the current operation.')
        d=None
        try:
            d=self._open();self._bound(d,point);release=self.catalog.verify()
            if release['sha256']!=point['release']['sha256']:raise SetupError('This recovery point uses another release. Keep its backup and review the current release separately.')
            installed=self._installed(d,release)
            if not installed or installed['sha256']!=release['sha256']:
                return {'id':point['id'],'state':'not-installed','canContinue':False,'message':'The verified release is not installed. Review a new installation attempt; no automatic retry was made.'}
            native=self._native(d);pending=native.get('pending');result=native.get('result','')
            if point['backup'] is None and not pending:
                point['backup']=native['backup'];point['beforeProfile']=native['profile'];point['beforeRevision']=native['revision']
            if pending and pending.get('requestId')==point['id']:
                point['previewId']=pending['id'];point['state']='awaiting-tv'
            elif isinstance(result,str) and result:
                try:
                    receipt=json.loads(result)
                    if receipt.get('requestId')==point['id'] and point['state']!='restored':point['state']=receipt['outcome'];point['previewId']=receipt['id']
                except ValueError:pass
            if point['state'] in ['installing','installed','paused','needs-attention','verified','previewing']:
                point['state']='ready-to-preview'
            self._save(point);self.active=point['id'];self.progress={'id':point['id'],'status':point['state'],'steps':[],
              'message':'Home kept on the TV. Choose its default Home only if you want to.' if point['state']=='kept' else 'Previous launcher profile restored.' if point['state']=='reverted' else 'Look at the TV to Keep or Revert.' if point['state']=='awaiting-tv' else 'Release verified. Continue the TV preview when ready.'}
            return {'id':point['id'],'state':point['state'],'revision':native['revision'],'canContinue':point['state']=='ready-to-preview','canRestore':bool(point['backup'])}
        finally:
            if d:d.close()
            self.lock.release()
    def continue_preview(self,value):
        checked=self.check(value)
        if not checked['canContinue']:raise SetupError('This recovery point is not ready for another preview.')
        point=self._load(value['id'])
        def operation(p):
            d=self._open()
            try:self._bound(d,p);d.preserve_home(p['originalHome']);self._preview(d,p,self._native(d))
            finally:d.close()
        return self._job(point,operation)
    def action(self,kind,value):
        if not isinstance(value,dict) or set(value)!={'id','revision'}:raise SetupError('Review the current TV state first.')
        point=self._load(value['id'])
        if not self.lock.acquire(blocking=False):raise SetupError('Wait for the current operation.')
        d=None
        try:
            d=self._open();self._bound(d,point);release=self.catalog.verify();self._installed(d,release);native=self._native(d)
            if native['revision']!=value['revision']:raise SetupError('Home changed. Check the recovery point and review again.')
            if kind=='restore':
                if not point['backup']:raise SetupError('No launcher restore point is available.')
                if native.get('pending'):d.provider('revert',{'id':native['pending']['id']});native=self._native(d)
                d.provider('restore',{'revision':native['revision'],'backup':point['backup']});d.launch();point['state']='restored';self._save(point)
            elif kind in ['home','accessibility']:
                if point['state']!='kept':raise SetupError('Keep the TV preview before choosing the default Home.')
                d.provider(kind,{'revision':native['revision']})
            elif kind=='previous':d.open_previous(point['originalHome'])
            elif kind=='revert':
                if not native.get('pending') or native['pending'].get('requestId')!=point['id']:raise SetupError('This preview is no longer pending.')
                d.provider('revert',{'id':native['pending']['id']});d.launch();point['state']='reverted';self._save(point)
            else:raise SetupError('Unsupported recovery action.')
            return {'done':True,'state':point['state']}
        finally:
            if d:d.close()
            self.lock.release()
