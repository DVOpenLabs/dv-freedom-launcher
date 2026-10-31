"""Exact installer device operations, separated from the read-only onboarding surface."""
import hashlib
import json
import re
import shlex
import time
from .core import Device,SetupError
from .release import MAX_APK
from .home import require_stable_home

PACKAGE='com.dvopenlabs.freedomlauncher'
METHODS={'status','backup','preview','revert','restore','home','accessibility'}
class InstallerDevice(Device):
    def preserve_home(self,component):
        """Restore only a cleared chooser to the exact recorded Home, never override a new choice."""
        require_stable_home(component)
        current=self.read('home').strip().splitlines()
        if current and current[-1]==component:return
        if not current or current[-1]!='android/com.android.internal.app.ResolverActivity':
            raise SetupError('The default Home changed during installation. Check your TV Home settings before continuing; the new choice was not overwritten.')
        candidates=self._shell('cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME').splitlines()
        if component not in [line.strip() for line in candidates]:raise SetupError('The recorded Home is no longer available. Select an available Home in TV settings before continuing.')
        self._shell('cmd package set-home-activity --user 0 '+shlex.quote(component))
        selected=self.read('home').strip().splitlines()
        if not selected or selected[-1]!=component:raise SetupError('Android cleared its previous Home selection. Restore that choice in TV Home settings, then check this recovery point again.')
    def media_call(self,method,value):
        if method not in {'prepare','status','review'}:raise SetupError('Unsupported media action.')
        raw=json.dumps(value,ensure_ascii=True,separators=(',',':')) if isinstance(value,dict) else value
        if not isinstance(raw,str) or len(raw)>8192:raise SetupError('Invalid media request.')
        command='content call --uri content://'+PACKAGE+'.inbox --method '+method+' --arg '+shlex.quote(raw)
        output=self._shell(command);marker=output.find('json=')
        if marker<0:raise SetupError('The TV could not prepare media review. Update the compatible DV Freedom build, keep the current profile unchanged, and finish or discard any file already waiting on the TV.')
        try:reply=json.JSONDecoder().raw_decode(output[marker+5:])[0]
        except (ValueError,TypeError):raise SetupError('The TV returned an invalid media response.')
        if reply.get('schema')!=1:raise SetupError('Unsupported media interface.')
        return reply
    def send_media(self,file,item,native):
        prepared=self.media_call('prepare',dict(schema=1,profile=native['profile'],revision=native['revision'],kind=item['kind'],bytes=item['bytes'],sha256=item['sha256'],name=item['name']))
        id=prepared.get('id')
        if not isinstance(id,str) or not re.fullmatch('[0-9a-f]{32}',id):raise SetupError('The TV did not prepare a safe upload slot.')
        remote='/data/local/tmp/dv-freedom-media-'+id+'.bin'
        try:
            if self.paired:self._adb(['-s',f'{self.host}:{self.port}','push',str(file),remote])
            else:self.device.push(str(file),remote,transport_timeout_s=10,read_timeout_s=90)
            if self._shell('sha256sum '+remote).split()[0]!=item['sha256']:raise SetupError('Media transfer checksum failed. Nothing was imported.')
            self._shell('cat '+remote+' | content write --uri content://'+PACKAGE+'.inbox/upload/'+id,90)
            for _ in range(30):
                status=self.media_call('status',id)
                if status.get('state')=='ready':
                    self.media_call('review',id);return id
                if status.get('state')=='error':break
                time.sleep(.1)
            raise SetupError('The TV could not verify the received media. Nothing was imported or selected.')
        finally:
            try:self._shell('rm -f '+remote)
            except Exception:pass
    def _shell(self,command,timeout=25):
        if self.paired:return self._adb(['-s',f'{self.host}:{self.port}','shell',command])
        return self.device.shell(command,transport_timeout_s=10,read_timeout_s=timeout)
    def fingerprint(self):
        value=self._shell('settings get secure android_id').strip()
        if not re.fullmatch('[0-9a-fA-F]{8,64}',value) or set(value)=={'0'} or value.lower()=='9774d56d682e549c':raise SetupError('The TV did not provide a stable local device identifier. Installation is blocked to avoid targeting the wrong device.')
        return hashlib.sha256(('dv-freedom-device:'+value).encode()).hexdigest()
    def metadata(self,package=PACKAGE):
        if package!=PACKAGE:raise SetupError('Unsupported installer package.')
        value=self._shell('pm path '+PACKAGE).strip()
        if not value:return None
        lines=value.splitlines()
        if len(lines)!=1 or not lines[0].startswith('package:'):raise SetupError('Split or unusual launcher installations require manual review.')
        path=lines[0][8:]
        if not re.fullmatch(r'/data/app/[A-Za-z0-9_./+=~-]+/base\.apk',path) or '..' in path:raise SetupError('The installed APK path could not be verified.')
        info=self._shell('dumpsys package '+PACKAGE);match=re.search(r'\bversionCode=(\d+)',info)
        if not match:raise SetupError('Could not read the installed launcher version.')
        return {'path':path,'versionCode':int(match[1])}
    def pull(self,remote,path):
        if not re.fullmatch(r'/data/app/[A-Za-z0-9_./+=~-]+/base\.apk',remote) or '..' in remote:raise SetupError('Unsupported backup path.')
        size=self._shell('stat -c %s '+shlex.quote(remote)).strip()
        if not size.isdigit() or not 0<int(size)<=MAX_APK:raise SetupError('The installed APK is too large or unavailable.')
        if self.paired:self._adb(['-s',f'{self.host}:{self.port}','pull',remote,str(path)])
        else:self.device.pull(remote,str(path),transport_timeout_s=10,read_timeout_s=30)
    def install(self,apk,expected_hash):
        remote='/data/local/tmp/dv-freedom-'+expected_hash[:16]+'.apk'
        if self.paired:self._adb(['-s',f'{self.host}:{self.port}','push',str(apk),remote])
        else:self.device.push(str(apk),remote,transport_timeout_s=10,read_timeout_s=45)
        try:
            actual=self._shell('sha256sum '+remote).split()[0]
            if actual!=expected_hash:raise SetupError('The APK transfer failed its checksum. Nothing was installed.')
            result=self._shell('pm install -r '+remote,90).strip()
            if result!='Success':raise SetupError('Android refused the installation. The previous Home remains available. Check free space and device installation policy; this app will not bypass protection.')
        finally:
            try:self._shell('rm -f '+remote)
            except Exception:pass
    def provider(self,method,value=None):
        if method not in METHODS:raise SetupError('Unsupported launcher operation.')
        command='content call --uri content://'+PACKAGE+'.theme --method product_installer_'+method
        if value is not None:
            raw=json.dumps(value,ensure_ascii=True,separators=(',',':'))
            if len(raw.encode())>60000:raise SetupError('Launcher backup or setup exceeds the safe transfer limit.')
            command+=' --arg '+shlex.quote(raw)
        output=self._shell(command);marker=output.find('json=')
        if marker<0:
            for text in ['Unlock launcher editing on the TV first','Home changed. Review restore again.','Home changed. Back up and review again.','Maximum 16 profiles. Remove an unused profile first.','Backup belongs to another device, app or format',"The backup's theme files are unavailable",'Keep or revert the preview before choosing Home','Revert the pending preview before restoring']:
                if text in output:raise SetupError(text)
            raise SetupError('The launcher could not complete this action. Unlock editing on the TV and keep the current Home visible. Older builds need a reviewed migration before updating.')
        try:result=json.JSONDecoder().raw_decode(output[marker+5:])[0]
        except (ValueError,TypeError):raise SetupError('The launcher returned an invalid setup response.')
        if result.get('schema')!=1:raise SetupError('Unsupported launcher setup interface.')
        return result
    def launch(self):self._shell('am start -n '+PACKAGE+'/com.gathin.tvhome.MainActivity')
    def open_previous(self,component):
        selected=self._shell('settings get secure enabled_accessibility_services').strip().split(':')
        if PACKAGE+'/com.gathin.tvhome.StockHomeRedirectService' in selected:raise SetupError('Turn off DV Freedom’s Home-only redirect in TV Accessibility settings before opening the original Home. Restoring a profile does not revoke that separate permission.')
        if not isinstance(component,str) or not re.fullmatch(r'[A-Za-z][\w.]*/[A-Za-z.][\w.$]*',component) or any(x in component.lower() for x in ['factory','hkcfac']):raise SetupError('The recorded original Home needs manual selection from TV settings.')
        self._shell('am start -a android.intent.action.MAIN -c android.intent.category.HOME -n '+shlex.quote(component))
