"""Reviewed, fresh-install-only webOS/Tizen setup. No shell endpoint or Home takeover."""
from __future__ import annotations
import copy
import hashlib
import importlib
import io
import tarfile
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import subprocess
import threading
import time
import xml.etree.ElementTree as ET
import zipfile
from .core import SetupError, address

ROOT=Path(__file__).resolve().parents[1]
LG_ID='com.dvopenlabs.freedom'
TIZEN_ID='DVFreedom1.Launcher'
LG_TOOLS=('ares-setup-device','ares-novacom','ares-device','ares-install','ares-package','ares-launch')

def fields(value,keys):
    if not isinstance(value,dict) or set(value)!=set(keys):raise SetupError('Check the fields for this setup step.')

def text(value,label,maximum=80):
    if not isinstance(value,str) or not value.strip() or len(value)>maximum or re.search(r'[\x00-\x1f\x7f]',value):raise SetupError('Enter '+label+'.')
    return value.strip()

def digest(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()

def atomic(path,value):
    path.parent.mkdir(parents=True,exist_ok=True,mode=0o700)
    part=path.with_suffix('.tmp');part.write_text(json.dumps(value,ensure_ascii=False,indent=2),encoding='utf-8');part.chmod(0o600);os.replace(part,path)

def json_output(raw,kind):
    # CLI may prefix its JSON with a target/status line; never accept a partial/truncated array.
    start=raw.find('[' if kind=='list' else '{')
    try:
        result,end=json.JSONDecoder().raw_decode(raw[start:])
        if start<0 or raw[start+end:].strip() or (kind=='list' and not isinstance(result,list)):raise ValueError()
        return result
    except (ValueError,TypeError):raise SetupError('The vendor tool returned an unfamiliar response. Stop here and keep the private setup record for support.')

class Runner:
    def __call__(self,argv,*,cwd=None,env=None,stdin=None,timeout=45):
        argv=[str(a) for a in argv]
        if os.name=='nt' and Path(argv[0]).suffix.lower() in ('.bat','.cmd'):
            # Windows batch entry points need cmd; reject expansion/metacharacters before quoting every arg.
            if any(re.search(r'["&|<>^%!\r\n]',a) for a in argv):raise SetupError('The SDK or setup path contains characters unsupported by the Windows vendor launcher. Use a simple local folder.')
            argv='"'+str(Path(os.environ.get('SystemRoot','C:/Windows'))/'System32/cmd.exe')+'" /d /s /c "'+' '.join('"'+a+'"' for a in argv)+'"'
        try:
            r=subprocess.run(argv,input=stdin,capture_output=True,text=True,encoding='utf-8',errors='replace',timeout=timeout,cwd=cwd,env=env,shell=False,creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
        except subprocess.TimeoutExpired:raise SetupError('The vendor tool timed out. Check the TV and use Check installation before trying again; installation may have completed.')
        except OSError:raise SetupError('The vendor tool could not start. Recheck its installation folder and try again.')
        if len(r.stdout)+len(r.stderr)>1024*1024:raise SetupError('The vendor response was too large. Stop and recheck the selected tools.')
        if r.returncode:raise SetupError('The vendor tool declined this step. Check Developer Mode, the current pairing code or Samsung certificate and Permit to install. Nothing is uninstalled or reset; check installation before retrying.')
        return r.stdout

class NativeInstaller:
    def __init__(self,root,runner=None):
        self.root=Path(root)/'native';self.runner=runner or Runner();self.lock=threading.Lock();self.cancelled=threading.Event();self.config={};self.connection=None;self.reviewed=None
        self.job={'status':'idle','message':''};self.operation=None;self.config_error=False
        try:
            path=self.root/'tools.json'
            if path.exists():
                if path.stat().st_size>4096:raise ValueError()
                value=json.loads(path.read_text(encoding='utf-8'))
                if not isinstance(value,dict) or set(value)-{'node','lgRoot','samsungRoot'} or any(not isinstance(v,str) for v in value.values()):raise ValueError()
                self.config=value
        except (OSError,ValueError):self.config_error=True
    def paths(self):
        node=self.config.get('node') or shutil.which('node')
        lg=self.config.get('lgRoot') or str(self.root/'lg-tools/node_modules/@webos-tools/cli')
        sdk=self.config.get('samsungRoot') or os.environ.get('TIZEN_SDK') or str(Path.home()/'tizen-studio')
        return {'node':node or '', 'lgRoot':lg,'samsungRoot':sdk}
    def tools(self,platform):
        p=self.paths()
        if self.config_error:raise SetupError('Saved tool settings need attention and have been kept unchanged. Select your official tool paths again to continue.')
        if platform=='lg':
            base=Path(p['lgRoot']);node=Path(p['node'])
            try:package=json.loads((base/'package.json').read_text(encoding='utf-8'))
            except (OSError,ValueError):raise SetupError('LG tools are missing. Choose Prepare LG tools, or select an existing official @webos-tools/cli folder.')
            if not node.is_file() or package.get('name')!='@webos-tools/cli' or package.get('version')!='3.2.6' or not all((base/'bin'/f'{n}.js').is_file() for n in LG_TOOLS):raise SetupError('Use Node.js and the tested official LG CLI 3.2.6. No tool is downloaded automatically.')
            return {n:[str(node),str(base/'bin'/f'{n}.js')] for n in LG_TOOLS}
        if platform!='tizen':raise SetupError('Choose LG webOS or Samsung Tizen.')
        base=Path(p['samsungRoot']);sdb=base/'tools'/('sdb.exe' if os.name=='nt' else 'sdb');cli=base/'tools/ide/bin'/('tizen.bat' if os.name=='nt' else 'tizen')
        if not sdb.is_file() or not cli.is_file():raise SetupError('Install Samsung Tizen Studio with Web CLI, TV Extension and Samsung Certificate Extension, then select its folder.')
        return {'sdb':[str(sdb)],'tizen':[str(cli)]}
    def tool_stamp(self,platform):
        return {p:digest(p) for args in self.tools(platform).values() for p in args}
    def info(self):
        result={}
        for platform in ('lg','tizen'):
            try:self.tools(platform);result[platform]={'ready':True}
            except SetupError as e:result[platform]={'ready':False,'message':str(e)}
        return {'tools':result,'paths':self.paths()}
    def public(self):
        records=[]
        if self.root.exists():
            for path in sorted(self.root.glob('records/*.json'),reverse=True)[:12]:
                try:
                    item=json.loads(path.read_text(encoding='utf-8'));records.append({k:item[k] for k in ('id','platform','model','firmware','status','created')})
                except (OSError,ValueError,KeyError,TypeError):pass
        return {'job':copy.deepcopy(self.job),'device':copy.deepcopy(self.connection and {k:self.connection[k] for k in ('platform','host','model','firmware','reported','catalog')}),'review':copy.deepcopy(self.reviewed),'operation':copy.deepcopy(self.operation),'records':records}
    def configure(self,value):
        fields(value,('node','lgRoot','samsungRoot'))
        for key,val in value.items():
            if not isinstance(val,str) or len(val)>500 or re.search(r'[\x00-\x1f"&|<>^%!]',val) or val and (not Path(val).is_absolute() or val.startswith(('\\\\','//'))):raise SetupError('Choose a local absolute tool path. Network shares and command text are not accepted.')
        self.config=value.copy();self.config_error=False;atomic(self.root/'tools.json',self.config);self.connection=None;self.reviewed=None;return self.info()
    def work(self,message,fn):
        if not self.lock.acquire(blocking=False):raise SetupError('Wait for the current native setup step.')
        self.cancelled.clear();self.job={'status':'running','message':message}
        def run():
            try:fn();self.job={'status':'done','message':'Step complete. Review the result below.'}
            except SetupError as e:self.job={'status':'error','message':str(e)}
            except Exception:self.job={'status':'error','message':'This step did not finish. Keep the local record and use Check installation before retrying.'}
            finally:self.lock.release()
        threading.Thread(target=run,daemon=True).start();return {'started':True}
    def prepare_tools(self,value):
        fields(value,('consent',))
        if value['consent'] is not True:raise SetupError('Confirm the official LG tool download first.')
        node=self.paths()['node'];npm=Path(node).parent/'node_modules/npm/bin/npm-cli.js'
        if not node or not Path(node).is_file() or not npm.is_file():raise SetupError('Install the Node.js LTS Windows installer from nodejs.org, reopen Setup, then try Prepare LG tools again.')
        def action():
            folder=self.root/'lg-tools';folder.mkdir(parents=True,exist_ok=True,mode=0o700)
            self.runner([node,str(npm),'install','--prefix',str(folder),'--registry=https://registry.npmjs.org','--ignore-scripts','--no-audit','--no-fund','@webos-tools/cli@3.2.6'],timeout=240)
            self.config['node']=node;self.config['lgRoot']=str(folder/'node_modules/@webos-tools/cli');self.tools('lg');atomic(self.root/'tools.json',self.config)
        return self.work('Preparing official LG tools on this computer. No TV is contacted.',action)
    def env(self,platform):
        if platform!='lg':return None
        home=self.root/'lg-home';home.mkdir(parents=True,exist_ok=True,mode=0o700)
        # Child environment only: isolate LG registrations/keys from the person's other CLI targets.
        env=os.environ.copy();env.update({'HOME':str(home),'USERPROFILE':str(home),'APPDATA':str(home),'NO_UPDATE_NOTIFIER':'1'});return env
    def call(self,conn,name,args=(),**kw):return self.runner([*self.tools(conn['platform'])[name],*args],env=self.env(conn['platform']),**kw)
    def inventory(self,c):
        if c['platform']=='lg':
            raw=self.call(c,'ares-device',['--system-info','--device',c['target']]);rows=json_output(self.call(c,'ares-install',['--device',c['target'],'--listfull']),'list')
            if any(not isinstance(r,dict) or not isinstance(r.get('id'),str) for r in rows):raise SetupError('LG returned an unfamiliar app list. Installation is blocked.')
            ids=sorted(r['id'] for r in rows);catalog=[{'id':r['id'],'label':re.sub(r'[\x00-\x1f\x7f-\x9f]','',str(r.get('title') or r['id'])).strip()[:64] or r['id']} for r in rows if re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]{0,254}',r['id']) and not r.get('visible') is False and r['id']!=LG_ID][:100];identity='\n'.join(line.strip() for line in raw.splitlines() if re.match(r'\s*(modelName|sdkVersion|firmwareVersion|boardType|otaId)\s*:',line))
            if not re.search(r'modelName\s*:',identity) or not re.search(r'firmwareVersion\s*:',identity):raise SetupError('LG model/firmware information is missing. Installation is blocked.')
        else:
            devices=self.call(c,'sdb',['devices']);matches=[x.split() for x in devices.splitlines() if x.startswith(c['target']+'\t') or x.startswith(c['target']+' ')]
            if len(matches)!=1 or len(matches[0])<2 or matches[0][1]!='device':raise SetupError('This exact Samsung TV is not authorized. Check Developer Mode, its allowed computer address and the TV prompt.')
            identity=self.call(c,'sdb',['-s',c['target'],'capability']).strip()
            version=re.search(r'platform_version\s*[:=]\s*(\d+(?:\.\d+)?)',identity)
            if not version or float(version[1])<6:raise SetupError('This pilot requires reported Tizen 6.0 or newer. Missing/older platform information blocks installation.')
            raw=self.call(c,'sdb',['-s',c['target'],'shell','pkgcmd -l'])
            ids=re.findall(r'pkgid\[([A-Za-z0-9_.-]+)\]',raw)
            if not ids and not re.search(r'total\s*\[?0\]?|no packages',raw,re.I):raise SetupError('Samsung did not expose a recognizable installed-package list. This firmware needs a separate compatibility check; no install was attempted.')
        if not identity or len(identity)>65536:raise SetupError('The TV identity response is invalid. Reconnect before proceeding.')
        return {'identity':hashlib.sha256(identity.encode()).hexdigest(),'apps':ids,'reported':identity[:1200] if c['platform']=='lg' else '\n'.join(x for x in identity.splitlines() if re.match(r'(platform_version|model_name|device_name)\s*[:=]',x))[:1200],'catalog':catalog if c['platform']=='lg' else []}
    def connect(self,value):
        fields(value,('platform','host','model','firmware','code','prepared','pairConsent'))
        platform=value['platform'];port=9922 if platform=='lg' else 26101
        if platform not in ('lg','tizen'):raise SetupError('Choose the TV platform.')
        host,_=address(value['host'],port);model=text(value['model'],'the full model code');firmware=text(value['firmware'],'the installed webOS/Tizen and firmware version')
        if value['prepared'] is not True or value['pairConsent'] is not True:raise SetupError('Complete the TV steps and approve saving a local developer connection first.')
        code=value['code']
        if platform=='tizen' and code!='':raise SetupError('Samsung does not use the LG pairing field.')
        if platform=='lg' and (not isinstance(code,str) or not re.fullmatch(r'[A-Za-z0-9]{6}',code)):raise SetupError('Enter the six-character, case-sensitive passphrase currently shown in LG Developer Mode.')
        self.tools(platform);self.connection=None;self.reviewed=None
        c={'platform':platform,'host':host,'model':model,'firmware':firmware,'target':'dvfreedom_'+secrets.token_hex(5) if platform=='lg' else host+':26101'}
        def action():
            if platform=='lg':
                self.call(c,'ares-setup-device',['--add',c['target'],'--info',json.dumps({'host':host,'port':'9922','username':'prisoner','description':'DV Freedom guided test'})])
                self.call(c,'ares-novacom',['--device',c['target'],'--getkey'],stdin=code+'\n')
            else:self.call(c,'sdb',['connect',c['target']])
            c.update(self.inventory(c));c['tools']=self.tool_stamp(platform);self.connection=c
        return self.work('Checking this TV. LG pairing stores its developer key locally; no app is installed.',action)
    def prepare(self,value):
        if not isinstance(value,dict) or set(value)-{'profile','apps'} or 'profile' not in value:raise SetupError('Check the package choices.')
        if not self.connection:raise SetupError('Connect and check the TV first.')
        c=copy.deepcopy(self.connection);profile=value['profile'];selected=value.get('apps',[])
        if not isinstance(selected,list) or len(selected)>8 or any(not isinstance(x,str) for x in selected) or len(set(selected))!=len(selected) or any(x not in {a['id'] for a in c['catalog']} for x in selected):raise SetupError('Choose up to eight apps reported by this LG TV.')
        if c['platform']!='lg' and selected:raise SetupError('Samsung discovers apps inside its own launcher.')
        starter=[next(a for a in c['catalog'] if a['id']==key) for key in selected]
        if c['platform']=='tizen' and (not isinstance(profile,str) or not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_. -]{0,63}',profile)):raise SetupError('Enter the existing Samsung certificate profile name shown in Certificate Manager.')
        self.reviewed=None
        def action():
            fresh=self.inventory(c);app=LG_ID if c['platform']=='lg' else 'DVFreedom1'
            if app in fresh['apps']:raise SetupError('DV Freedom is already installed. This first-install pilot will not overwrite it without a native profile backup/update flow. Open the existing app instead.')
            if fresh['identity']!=c['identity'] or fresh['apps']!=c['apps']:raise SetupError('The TV changed since connection. Reconnect and review it again.')
            ident=secrets.token_hex(12);folder=self.root/'builds'/ident;folder.mkdir(parents=True,mode=0o700)
            if c['platform']=='lg':
                module=importlib.import_module('platforms.webos.prepare_launcher');module.prepare(folder/'app')
                if starter:(folder/'app/bootstrap.js').write_text('window.FREEDOM_BOOTSTRAP=null;window.FREEDOM_SETUP='+json.dumps({'schema':1,'apps':starter},ensure_ascii=True,separators=(',',':'))+';\n',encoding='utf-8')
                self.call(c,'ares-package',[str(folder/'app'),'--no-minify','-o',str(folder)],cwd=folder,timeout=120);packages=list(folder.glob('*.ipk'))
            else:
                module=importlib.import_module('platforms.tizen.prepare_launcher');module.prepare(folder/'app');self.call(c,'tizen',['package','-t','wgt','-s',profile,'--',str(folder/'app')],cwd=folder,timeout=120);packages=list(folder.rglob('*.wgt'))
            if len(packages)!=1 or not 0<packages[0].stat().st_size<4*1024*1024:raise SetupError('The vendor packager did not produce one expected pilot package.')
            package=packages[0];self.validate_package(c['platform'],package)
            record={'id':ident,'platform':c['platform'],'model':c['model'],'firmware':c['firmware'],'created':time.time(),'status':'review','device':c,'package':str(package),'sha256':digest(package),'bytes':package.stat().st_size,'profile':profile if c['platform']=='tizen' else '', 'steps':[],'starter':starter}
            atomic(self.root/'records'/f'{ident}.json',record)
            self.operation=self.safe_record(record);self.reviewed={**self.safe_record(record),'reviewToken':secrets.token_urlsafe(32),'expires':time.time()+900};self.review_record=record
        return self.work('Building the local app package. Samsung uses only your selected existing certificate profile.',action)
    def validate_package(self,platform,path):
        if platform=='lg':
            raw=Path(path).read_bytes();offset=8;members={}
            if not raw.startswith(b'!<arch>\n'):raise SetupError('LG did not produce an IPK archive.')
            try:
                while offset<len(raw):
                    h=raw[offset:offset+60];size=int(h[48:58]);name=h[:16].decode().strip().rstrip('/')
                    if len(h)!=60 or h[58:]!=b'`\n' or size<0 or offset+60+size>len(raw) or name in members:raise ValueError()
                    members[name]=raw[offset+60:offset+60+size];offset+=60+size+(size%2)
                if set(members)!={'debian-binary','control.tar.gz','data.tar.gz'} or members['debian-binary']!=b'2.0\n':raise ValueError()
                with tarfile.open(fileobj=io.BytesIO(members['control.tar.gz']),mode='r:gz') as control:
                    entries=list(control)
                    if len(entries)!=1 or entries[0].name.removeprefix('./')!='control' or not entries[0].isfile() or entries[0].size>16384:raise ValueError()
                    meta=control.extractfile(entries[0]).read().decode('utf-8')
                    if not re.search(r'^Package: '+re.escape(LG_ID)+r'\s*$',meta,re.M) or not re.search(r'^Version: 0\.1\.0\s*$',meta,re.M):raise ValueError()
                found=False;total=0
                allowed={'app.js','appinfo.json','bootstrap.js','core.js','icon.png','index.html','media.js','shared-core.js','style.css'}
                with tarfile.open(fileobj=io.BytesIO(members['data.tar.gz']),mode='r:gz') as tar:
                    for index,item in enumerate(tar):
                        total+=item.size;name=item.name.removeprefix('./')
                        if index>80 or total>4*1024*1024 or '..' in name.split('/') or not (item.isdir() or item.isfile()):raise ValueError()
                        if item.isdir():
                            if name not in {'usr','usr/palm','usr/palm/applications','usr/palm/packages',f'usr/palm/applications/{LG_ID}',f'usr/palm/packages/{LG_ID}'}:raise ValueError()
                            continue
                        prefix=f'usr/palm/applications/{LG_ID}/'
                        if name==f'usr/palm/packages/{LG_ID}/packageinfo.json':
                            info=json.load(tar.extractfile(item))
                            if info.get('id')!=LG_ID or info.get('app')!=LG_ID:raise ValueError()
                        elif name.startswith(prefix) and name[len(prefix):] in allowed:
                            if name.endswith('/appinfo.json'):
                                app=json.load(tar.extractfile(item));found=True
                                if app.get('id')!=LG_ID or app.get('version')!='0.1.0' or app.get('requiredACG')!=['application.launcher','application.query']:raise ValueError()
                        else:raise ValueError()
                if not found:raise ValueError()
            except (ValueError,TypeError,KeyError,UnicodeError,tarfile.TarError):raise SetupError('The LG package contains an unexpected app, service or payload. Installation is blocked.')
            return
        with zipfile.ZipFile(path) as z:
            if len(z.namelist())>100 or sum(i.file_size for i in z.infolist())>4*1024*1024:raise SetupError('Samsung package exceeds the source-pilot limit.')
            xml=z.read('config.xml');root=ET.fromstring(xml);ns={'t':'http://tizen.org/ns/widgets'};app=root.find('t:application',ns)
            if root.get('version')!='0.1.0' or app is None or app.get('id')!=TIZEN_ID or app.get('package')!='DVFreedom1' or not {'author-signature.xml','signature1.xml'}<=set(z.namelist()):raise SetupError('Samsung package identity/signatures are missing. Create a valid Samsung certificate for this TV.')
            privileges={v.get('name') for v in root.findall('t:privilege',ns)};setting=root.find('t:setting',ns)
            if privileges!={'http://tizen.org/privilege/application.info','http://tizen.org/privilege/application.launch'} or setting is None or setting.get('background-support')!='disable' or root.findall('t:service',ns) or root.findall('t:metadata',ns) or root.findall('{http://www.w3.org/ns/widgets}access'):raise SetupError('Samsung package privileges or background behavior changed. Installation is blocked.')
            # Signatures are generated by Samsung's selected SDK; the TV is the final certificate verifier.
    def safe_record(self,r):return {**{k:copy.deepcopy(r[k]) for k in ('id','platform','model','firmware','created','status','sha256','bytes','profile','steps')},'starter':copy.deepcopy(r.get('starter',[]))}
    def read_record(self,ident):
        if not isinstance(ident,str) or not re.fullmatch('[0-9a-f]{24}',ident):raise SetupError('Choose a saved native installation record.')
        try:
            path=self.root/'records'/f'{ident}.json'
            if path.is_symlink() or path.stat().st_size>65536:raise ValueError()
            r=json.loads(path.read_text(encoding='utf-8'))
        except (ValueError,OSError):raise SetupError('The saved record is unavailable. Nothing was retried.')
        if not isinstance(r,dict) or not {'id','platform','model','firmware','created','status','sha256','bytes','profile','steps','device','package'}<=set(r) or r.get('id')!=ident or r.get('platform') not in ('lg','tizen') or not isinstance(r.get('device'),dict):raise SetupError('The saved record is invalid. Nothing was retried.')
        c=r['device']
        if c.get('platform')!=r['platform'] or not isinstance(c.get('tools'),dict) or not isinstance(r.get('package'),str) or not re.fullmatch('[0-9a-f]{64}',str(r.get('sha256',''))):raise SetupError('The saved record is invalid. Nothing was retried.')
        host,_=address(c.get('host'),9922 if r['platform']=='lg' else 26101)
        expected=bool(re.fullmatch('dvfreedom_[0-9a-f]{10}',str(c.get('target','')))) if r['platform']=='lg' else c.get('target')==host+':26101'
        if not expected:raise SetupError('The saved TV target is invalid. Nothing was retried.')
        p=Path(r['package']).resolve();base=(self.root/'builds'/ident).resolve()
        if not p.is_relative_to(base) or not p.is_file() or p.stat().st_size>4*1024*1024 or digest(p)!=r['sha256']:raise SetupError('The reviewed package changed or is missing. Reconnect and prepare a new package.')
        return r
    def install(self,value):
        fields(value,('id','reviewToken','confirmed'))
        v=self.reviewed
        if value['confirmed'] is not True or not v or value['id']!=v['id'] or not secrets.compare_digest(str(value['reviewToken']),v['reviewToken']) or time.time()>v['expires']:raise SetupError('Review the current TV/package and explicitly confirm this installation.')
        r=self.read_record(value['id']);self.reviewed=None
        def action():
            try:
                c=r['device'];fresh=self.inventory(c);app=LG_ID if c['platform']=='lg' else 'DVFreedom1'
                if self.tool_stamp(c['platform'])!=c['tools'] or fresh['identity']!=c['identity'] or fresh['apps']!=c['apps'] or app in fresh['apps']:raise SetupError('TV, installed apps or tools changed. Reconnect and review before installing.')
                if self.cancelled.is_set():raise SetupError('Stopped before installation. No install was sent.')
                r['status']='installing';self.save_record(r)
                if c['platform']=='lg':out=self.call(c,'ares-install',['--device',c['target'],r['package']],timeout=180)
                else:out=self.call(c,'tizen',['install','-s',c['target'],'--name',Path(r['package']).name,'--',str(Path(r['package']).parent)],timeout=180)
                # Persist uncertainty before checking, including success exit with an unrecognized body.
                r['status']='needs-check';self.save_record(r)
                current=self.inventory(c)
                if app not in current['apps']:raise SetupError('The SDK returned, but the app is not in the TV package list yet. Use Check installation; setup will not repeat the install.')
                r['status']='installed';r['steps']=['Exact TV and package rechecked','Installation verified in TV package list'];self.save_record(r)
            except Exception:
                if r['status']=='installing':r['status']='needs-check';self.save_record(r)
                raise
        return self.work('Installing the reviewed app. Keep the TV/computer on; this package commit cannot be interrupted.',action)
    def save_record(self,r):atomic(self.root/'records'/f"{r['id']}.json",r);self.operation=self.safe_record(r)
    def check(self,value,launch=False):
        fields(value,('id',));r=self.read_record(value['id']);self.reviewed=None
        def action():
            c=r['device'];fresh=self.inventory(c)
            if fresh['identity']!=c['identity'] or self.tool_stamp(c['platform'])!=c['tools']:raise SetupError('This is a different TV or tool set. Reconnect before proceeding.')
            app=LG_ID if c['platform']=='lg' else 'DVFreedom1'
            if app not in fresh['apps']:r['status']='not-found';self.save_record(r);raise SetupError('The app is not listed on this TV. No install was repeated. Reconnect to prepare a new reviewed first installation.')
            r['status']='installed';self.save_record(r)
            if launch:
                if c['platform']=='lg':self.call(c,'ares-launch',['--device',c['target'],LG_ID])
                else:self.call(c,'tizen',['run','-s',c['target'],'-p',TIZEN_ID])
                r['status']='launch-requested';r['steps']=['Installation verified in TV package list','Launch requested; physical remote/playback checks remain'];self.save_record(r)
        return self.work('Checking the installed app'+(' and requesting launch.' if launch else '. No install is repeated.'),action)
