import copy
import io
import json
import os
from pathlib import Path
import tarfile
import tempfile
import time
import unittest
import zipfile
from setup_app.native import NativeInstaller,LG_TOOLS,LG_ID,TIZEN_ID,json_output,Runner
from setup_app.core import SetupError


def ipk(app=LG_ID,extra=None):
    data=io.BytesIO()
    with tarfile.open(fileobj=data,mode='w:gz') as tar:
        values={f'usr/palm/applications/{app}/appinfo.json':json.dumps({'id':app,'version':'0.1.0','requiredACG':['application.launcher','application.query']}),f'usr/palm/packages/{app}/packageinfo.json':json.dumps({'id':app,'app':app})}
        if extra:values.update(extra)
        for name,value in values.items():
            raw=value.encode();entry=tarfile.TarInfo(name);entry.size=len(raw);tar.addfile(entry,io.BytesIO(raw))
    control=io.BytesIO()
    with tarfile.open(fileobj=control,mode='w:gz') as tar:
        raw=f'Package: {app}\nVersion: 0.1.0\n'.encode();entry=tarfile.TarInfo('control');entry.size=len(raw);tar.addfile(entry,io.BytesIO(raw))
    out=b'!<arch>\n'
    for name,raw in [('debian-binary',b'2.0\n'),('control.tar.gz',control.getvalue()),('data.tar.gz',data.getvalue())]:
        header=f'{name+"/":<16}{0:<12}{0:<6}{0:<6}{100644:<8}{len(raw):<10}`\n'.encode();assert len(header)==60
        out+=header+raw+(b'\n' if len(raw)%2 else b'')
    return out

class Vendor:
    def __init__(self):self.calls=[];self.apps=[];self.changed=False;self.uncertain=False;self.error=None;self.target=None
    def __call__(self,args,**kw):
        args=list(map(str,args));self.calls.append((args,kw));name=Path(args[1] if len(args)>1 and args[1].endswith('.js') else args[0]).stem
        if self.error==name:raise SetupError('Simulated vendor refusal')
        if name=='ares-setup-device':self.target=args[args.index('--add')+1];return 'Success'
        if name=='ares-novacom':return 'Success'
        if name=='ares-device':return 'modelName : WEBOS6.0\nsdkVersion : 6.1.0\nfirmwareVersion : '+('02' if self.changed else '01')+'\n'
        if name=='ares-install':
            if '--listfull' in args:return 'target\n'+json.dumps([{'id':i} for i in self.apps])
            self.apps.append(LG_ID)
            if self.uncertain:raise SetupError('Simulated lost install reply')
            return 'Success'
        if name=='ares-package':Path(args[args.index('-o')+1],'pilot.ipk').write_bytes(ipk());return 'Success'
        if name=='ares-launch':return 'Launched'
        if name=='sdb':
            if args[1]=='connect':self.target=args[2];return 'connected'
            if args[1]=='devices':return 'List of devices attached\n'+self.target+'\tdevice\tTEST\n'
            if args[-1]=='capability':return 'platform_version: 6.5\nmodel_name: TEST\n'
            if args[-1]=='pkgcmd -l':return '\n'.join('pkg_type[wgt] pkgid['+a+']' for a in self.apps) if self.apps else 'total[0]'
        if name=='tizen':
            if 'package' in args:
                folder=Path(args[-1]);
                with zipfile.ZipFile(folder/'pilot.wgt','w') as z:
                    z.write(folder/'config.xml','config.xml');z.writestr('author-signature.xml','fixture');z.writestr('signature1.xml','fixture')
                return 'Success'
            if 'install' in args:self.apps.append('DVFreedom1');return 'Success'
            if 'run' in args:return 'Launched'
        raise AssertionError(args)

class NativeTest(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup);self.root=Path(self.tmp.name);self.vendor=Vendor();self.n=NativeInstaller(self.root,self.vendor)
        node=self.root/'node.exe';node.write_bytes(b'not executable fixture');lg=self.root/'lg';(lg/'bin').mkdir(parents=True)
        (lg/'package.json').write_text(json.dumps({'name':'@webos-tools/cli','version':'3.2.6'}))
        for name in LG_TOOLS:(lg/'bin'/f'{name}.js').write_text('fixture')
        sdk=self.root/'sdk';(sdk/'tools/ide/bin').mkdir(parents=True);(sdk/'tools'/('sdb.exe' if os.name=='nt' else 'sdb')).write_text('fixture');(sdk/'tools/ide/bin'/('tizen.bat' if os.name=='nt' else 'tizen')).write_text('fixture')
        self.config={'node':str(node),'lgRoot':str(lg),'samsungRoot':str(sdk)};self.n.configure(self.config)
    def wait(self):
        for _ in range(200):
            if not self.n.lock.locked():return
            time.sleep(.005)
        self.fail('Worker did not finish')
    def done(self):self.wait();self.assertEqual(self.n.job['status'],'done',self.n.job)
    def connect(self,platform='lg'):
        self.n.connect({'platform':platform,'host':'192.168.50.20','model':'TEST MODEL','firmware':'test 01','code':'Ab12Cd' if platform=='lg' else '','prepared':True,'pairConsent':True});self.done()
    def review(self,platform='lg'):
        self.connect(platform);self.n.prepare({'profile':'Owner Certificate' if platform=='tizen' else ''});self.done();return copy.deepcopy(self.n.reviewed)
    def install(self,r):return self.n.install({'id':r['id'],'reviewToken':r['reviewToken'],'confirmed':True})
    def test_lg_pairing_is_isolated_and_code_not_in_argv_records_or_public_state(self):
        self.connect();calls=self.vendor.calls;key=next(x for x in calls if 'ares-novacom.js' in ' '.join(x[0]));self.assertEqual(key[1]['stdin'],'Ab12Cd\n');self.assertNotIn('Ab12Cd',str(key[0]));self.assertTrue(key[1]['env']['APPDATA'].startswith(str(self.n.root)));self.assertNotIn('Ab12Cd',json.dumps(self.n.public()));self.assertFalse((self.n.root/'records').exists())
    def test_corrupt_tool_settings_are_kept_and_block_operations(self):
        path=self.n.root/'tools.json';path.write_text('[]')
        restarted=NativeInstaller(self.root,self.vendor)
        self.assertFalse(restarted.info()['tools']['lg']['ready']);self.assertEqual(path.read_text(),'[]')

    def test_tools_and_connection_require_explicit_choices(self):
        bad={'platform':'lg','host':'8.8.8.8','model':'Test','firmware':'01','code':'Ab12Cd','prepared':True,'pairConsent':True}
        with self.assertRaises(SetupError):self.n.connect(bad)
        bad['host']='192.168.50.20';bad['pairConsent']=False
        with self.assertRaises(SetupError):self.n.connect(bad)
        with self.assertRaises(SetupError):self.n.prepare_tools({'consent':False})
        with self.assertRaises(SetupError):self.n.configure({**self.config,'node':'C:/evil & command.exe'})
        self.assertFalse(self.vendor.calls)
    def test_lg_package_review_install_then_explicit_launch(self):
        r=self.review();self.assertEqual(self.vendor.apps,[]);self.install(r);self.done();self.assertEqual(self.n.operation['status'],'installed');self.assertFalse(any('ares-launch.js' in ' '.join(a) for a,_ in self.vendor.calls));self.n.check({'id':r['id']},launch=True);self.done();self.assertEqual(self.n.operation['status'],'launch-requested')
    def test_lg_starter_apps_use_only_this_checked_catalog_and_keep_order(self):
        self.vendor.apps=['example.video','example.music'];self.connect()
        with self.assertRaises(SetupError):self.n.prepare({'profile':'','apps':['not.reported']})
        self.n.prepare({'profile':'','apps':['example.music','example.video']});self.done()
        self.assertEqual([a['id'] for a in self.n.reviewed['starter']],['example.music','example.video'])
        staged=Path(self.n.review_record['package']).parent/'app/bootstrap.js'
        self.assertIn('FREEDOM_SETUP',staged.read_text());self.assertLess(staged.read_text().index('example.music'),staged.read_text().index('example.video'))
        self.assertEqual(self.vendor.apps,['example.video','example.music'])

    def test_samsung_signing_is_named_profile_and_exact_target(self):
        r=self.review('tizen');self.assertEqual(r['profile'],'Owner Certificate');self.install(r);self.done();self.n.check({'id':r['id']},launch=True);self.done();commands=[a for a,_ in self.vendor.calls];self.assertTrue(any('install' in a and '192.168.50.20:26101' in a for a in commands));self.assertTrue(any('run' in a and TIZEN_ID in a for a in commands));self.assertFalse(any('uninstall' in a or 'root' in a or 'reboot' in a for a in commands))
    def test_unreviewed_wrong_token_expired_and_replayed_install_fail(self):
        with self.assertRaises(SetupError):self.n.install({'id':'0'*24,'reviewToken':'bad','confirmed':True})
        r=self.review()
        with self.assertRaises(SetupError):self.n.install({'id':r['id'],'reviewToken':'bad','confirmed':True})
        with self.assertRaises(SetupError):self.n.install({'id':r['id'],'reviewToken':r['reviewToken'],'confirmed':False})
        self.n.reviewed['expires']=0
        with self.assertRaises(SetupError):self.install(r)
        self.n.reviewed['expires']=time.time()+10;self.install(r);self.done()
        with self.assertRaises(SetupError):self.install(r)
    def test_changed_package_is_never_installed(self):
        r=self.review();Path(self.n.review_record['package']).write_bytes(b'tampered')
        with self.assertRaises(SetupError):self.install(r)
        self.assertEqual(self.vendor.apps,[])
    def test_changed_tv_or_apps_or_tools_block_install(self):
        for change in ('tv','apps','tools'):
            with self.subTest(change=change):
                self.vendor.changed=False;self.vendor.apps=[];r=self.review()
                if change=='tv':self.vendor.changed=True
                if change=='apps':self.vendor.apps=['other.app']
                if change=='tools':Path(self.config['node']).write_text('changed')
                self.install(r);self.wait();self.assertEqual(self.n.job['status'],'error');self.assertNotIn(LG_ID,self.vendor.apps)
    def test_existing_app_blocks_package_preparation(self):
        self.vendor.apps=[LG_ID];self.connect();self.n.prepare({'profile':''});self.wait();self.assertEqual(self.n.job['status'],'error');self.assertIsNone(self.n.reviewed);self.assertFalse(any('ares-package.js' in ' '.join(a) for a,_ in self.vendor.calls))
    def test_lost_install_reply_is_durable_and_check_does_not_reinstall(self):
        r=self.review();self.vendor.uncertain=True;self.install(r);self.wait();self.assertEqual(self.n.job['status'],'error');self.assertEqual(self.n.operation['status'],'needs-check');count=len([1 for a,_ in self.vendor.calls if 'ares-install.js' in ' '.join(a) and '--listfull' not in a]);restarted=NativeInstaller(self.root,self.vendor);self.assertEqual(restarted.public()['records'][0]['status'],'needs-check');restarted.check({'id':r['id']});self.n=restarted;self.done();self.assertEqual(self.n.operation['status'],'installed');self.assertEqual(count,len([1 for a,_ in self.vendor.calls if 'ares-install.js' in ' '.join(a) and '--listfull' not in a]))
    def test_no_silent_reset_or_path_escape_in_saved_record(self):
        r=self.review();path=self.n.root/'records'/f"{r['id']}.json";data=json.loads(path.read_text());data['device']['target']='--remove';path.write_text(json.dumps(data))
        with self.assertRaises(SetupError):self.n.check({'id':r['id']})
        with self.assertRaises(SetupError):self.n.check({'id':'../anything'})
        self.assertEqual(json.loads(path.read_text())['device']['target'],'--remove')
    def test_privileged_wrong_app_or_service_payload_is_rejected(self):
        for raw in (ipk('wrong.app'),ipk(extra={'usr/palm/services/evil/service.js':'evil'})):
            path=self.root/'bad.ipk';path.write_bytes(raw)
            with self.assertRaises(SetupError):self.n.validate_package('lg',path)
        wgt=self.root/'unsigned.wgt'
        with zipfile.ZipFile(wgt,'w') as z:z.writestr('config.xml',f'<widget xmlns:t="http://tizen.org/ns/widgets"><t:application id="{TIZEN_ID}" package="DVFreedom1"/></widget>')
        with self.assertRaises(SetupError):self.n.validate_package('tizen',wgt)
    def test_bounded_cli_response_parser_and_no_unknown_target_fallback(self):
        self.assertEqual(json_output('target\n[]','list'),[])
        for value in ('noise','[]\ntrailing','[{}','{}'):
            with self.assertRaises(SetupError):json_output(value,'list')
    @unittest.skipUnless(os.name=='nt','Windows vendor batch entry-point test')
    def test_windows_vendor_launcher_preserves_spaces_and_rejects_expansion(self):
        folder=self.root/'sdk tools';folder.mkdir();script=folder/'tizen.cmd';script.write_text('@echo off\necho MARKER[%~1][%~2]\n',encoding='ascii')
        out=Runner()([str(script),'Owner Profile','C:/path with spaces/app'])
        self.assertIn('MARKER[Owner Profile][C:/path with spaces/app]',out)
        for value in ('x&whoami','%PATH%','x|anything','x!anything','x\"anything'):
            with self.assertRaises(SetupError):Runner()([str(script),value])

    def test_vendor_failure_does_not_persist_pairing_secret(self):
        self.vendor.error='ares-novacom';self.n.connect({'platform':'lg','host':'192.168.50.20','model':'TEST','firmware':'01','code':'Ab12Cd','prepared':True,'pairConsent':True});self.wait();self.assertEqual(self.n.job['status'],'error');self.assertIsNone(self.n.connection);self.assertNotIn('Ab12Cd',json.dumps(self.n.public()))

if __name__=='__main__':unittest.main()
