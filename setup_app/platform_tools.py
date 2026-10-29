"""Optional Google pairing tools: explicit licence acceptance, bounded vendor-only download."""
import hashlib
import io
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import threading
import urllib.request
import urllib.parse
import xml.etree.ElementTree as ET
import zipfile
from .core import SetupError

REPOSITORY='https://dl.google.com/android/repository/repository2-3.xml'
FILES={'platform-tools/adb.exe','platform-tools/AdbWinApi.dll','platform-tools/AdbWinUsbApi.dll','platform-tools/NOTICE.txt','platform-tools/source.properties'}
class VendorRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self,req,fp,code,msg,headers,newurl):
        parsed=urllib.parse.urlsplit(newurl)
        if parsed.scheme!='https' or parsed.hostname!='dl.google.com':raise SetupError('The Google tools download redirected outside its trusted vendor.')
        return super().redirect_request(req,fp,code,msg,headers,newurl)
def fetch(url,maximum):
    parsed=urllib.parse.urlsplit(url)
    if parsed.scheme!='https' or parsed.hostname!='dl.google.com' or parsed.username:raise SetupError('Unsupported tools download source.')
    with urllib.request.build_opener(VendorRedirect()).open(url,timeout=30) as response:
        data=response.read(maximum+1)
    if len(data)>maximum:raise SetupError('The tools download exceeded its safe size limit.')
    return data
def metadata(raw):
    if len(raw)>32*1024*1024 or b'<!DOCTYPE' in raw.upper() or b'<!ENTITY' in raw.upper():raise SetupError('Invalid vendor metadata.')
    try:
        root=ET.fromstring(raw);package=next(p for p in root if p.tag.endswith('remotePackage') and p.get('path')=='platform-tools' and (p.find('channelRef') is None or p.find('channelRef').get('ref')=='channel-0'))
        archive=next(a for a in package.findall('./archives/archive') if a.findtext('host-os')=='windows');complete=archive.find('complete')
        file=complete.findtext('url');size=int(complete.findtext('size'));checksum=complete.find('checksum');sha=checksum.text.strip().lower()
        if not re.fullmatch(r'platform-tools_r[0-9.]+-win\.zip',file) or not 0<size<80*1024*1024 or checksum.get('type')!='sha1' or not re.fullmatch('[0-9a-f]{40}',sha):raise ValueError()
        license_id=package.find('uses-license').get('ref');license=next(x.text for x in root if x.tag.endswith('license') and x.get('id')==license_id)
        if not license or len(license)>100000:raise ValueError()
        version='.'.join(package.findtext('revision/'+key,'0') for key in ['major','minor','micro'])
        return {'file':file,'bytes':size,'sha1':sha,'version':version,'license':license,'licenseHash':hashlib.sha256(license.encode()).hexdigest()}
    except (ET.ParseError,ValueError,TypeError,AttributeError,StopIteration):raise SetupError('Could not verify Google’s current Platform Tools metadata.')
def find_adb(root):
    root=Path(root)
    try:
        record=json.loads((root/'platform-tools.json').read_text(encoding='utf-8'));folder=record['folder']
        if not isinstance(folder,str) or not isinstance(record['files'],dict) or not 3<=len(record['files'])<=10:return None
        if re.fullmatch('[0-9a-f]{64}',folder):
            directory=root/'platform-tools'/folder
            if all(re.fullmatch('[A-Za-z0-9_.-]+',name) and hashlib.sha256((directory/name).read_bytes()).hexdigest()==sha for name,sha in record['files'].items()) and {'adb.exe','AdbWinApi.dll','AdbWinUsbApi.dll'}<=set(record['files']):return str(directory/'adb.exe')
    except (OSError,ValueError,KeyError,TypeError,AttributeError):pass
    candidates=[shutil.which('adb')]
    for key in ['ANDROID_HOME','ANDROID_SDK_ROOT']:
        if os.environ.get(key):candidates.append(str(Path(os.environ[key])/'platform-tools/adb.exe'))
    if os.name=='nt':candidates.append(str(Path(os.environ.get('LOCALAPPDATA',Path.home()))/'Android/Sdk/platform-tools/adb.exe'))
    return next((p for p in candidates if p and Path(p).is_file()),None)
class PairingTools:
    def __init__(self,root,downloader=fetch):self.root=Path(root);self.fetch=downloader;self.pending=None;self.lock=threading.Lock();self.progress={'status':'idle'}
    def state(self):return {'available':bool(find_adb(self.root)),'job':dict(self.progress)}
    def info(self):
        value=metadata(self.fetch(REPOSITORY,32*1024*1024));value['token']=secrets.token_urlsafe(24);self.pending=value
        return {'token':value['token'],'version':value['version'],'bytes':value['bytes'],'license':value['license'],'vendor':'Google','url':'https://developer.android.com/studio/terms'}
    def install(self,value):
        if not isinstance(value,dict) or set(value)!={'token','acceptLicense'} or value['acceptLicense'] is not True or not self.pending or value['token']!=self.pending['token']:raise SetupError('Read and accept Google’s SDK licence before setting up pairing support.')
        if not self.lock.acquire(blocking=False):raise SetupError('Pairing support is already being prepared.')
        selected=dict(self.pending);self.pending=None;self.progress={'status':'running','message':'Downloading Google’s pairing tools…'}
        def run():
            staging=None
            try:
                data=self.fetch('https://dl.google.com/android/repository/'+selected['file'],80*1024*1024)
                if len(data)!=selected['bytes'] or hashlib.sha1(data).hexdigest()!=selected['sha1']:raise SetupError('The download did not match Google’s published checksum. Pairing support was not changed.')
                parent=self.root/'platform-tools';parent.mkdir(parents=True,exist_ok=True,mode=0o700);staging=parent/('pending-'+secrets.token_hex(8));staging.mkdir(mode=0o700);hashes={};total=0
                with zipfile.ZipFile(io.BytesIO(data)) as archive:
                    seen=set()
                    for info in archive.infolist():
                        if info.filename not in FILES:continue
                        if info.filename in seen or (info.external_attr>>16)&0o170000==0o120000 or not 0<info.file_size<=60*1024*1024:raise SetupError('Unsafe or repeated entry in the tools archive.')
                        seen.add(info.filename);total+=info.file_size
                        if total>100*1024*1024:raise SetupError('The extracted pairing tools are too large.')
                        content=archive.read(info);name=Path(info.filename).name
                        with (staging/name).open('xb') as out:out.write(content)
                        hashes[name]=hashlib.sha256(content).hexdigest()
                if not {'adb.exe','AdbWinApi.dll','AdbWinUsbApi.dll','NOTICE.txt'}<=set(hashes):raise SetupError('The vendor archive is missing required files or notices.')
                folder=hashlib.sha256(data).hexdigest();destination=parent/folder
                if destination.exists():
                    if not all((destination/n).is_file() and hashlib.sha256((destination/n).read_bytes()).hexdigest()==h for n,h in hashes.items()):raise SetupError('An existing tools folder failed verification. Existing files were not overwritten.')
                else:os.replace(staging,destination);staging=None
                record={'folder':folder,'files':hashes,'version':selected['version'],'licenseHash':selected['licenseHash'],'licenseAccepted':True,'source':'dl.google.com'};tmp=self.root/'platform-tools.json.tmp';tmp.write_text(json.dumps(record,indent=2),encoding='utf-8');os.replace(tmp,self.root/'platform-tools.json');self.progress={'status':'done','message':'Pairing support is ready. Enter the current code and ports from the TV.'}
            except Exception as error:self.progress={'status':'error','message':str(error) if isinstance(error,SetupError) else 'Pairing support could not be downloaded. Check your internet connection and try again.'}
            finally:
                if staging:
                    # Only our fresh random staging folder, under the app-owned tools directory.
                    for p in staging.iterdir():p.unlink()
                    staging.rmdir()
                self.lock.release()
        threading.Thread(target=run,daemon=True).start();return {'started':True}
