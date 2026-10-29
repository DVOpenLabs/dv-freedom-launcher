import http.client
import json
from pathlib import Path
import tempfile
import threading
import time
import unittest
from setup_app.core import Session,SetupError,address,validate_plan,DEMO_APPS,COMMANDS,inspect
from setup_app.server import serve

class FakeDevice:
    calls=[]
    def __init__(self,*args):self.closed=False
    def connect(self):self.calls.append('connect')
    def read(self,key):
        self.calls.append(key)
        return {'model':'Example TV','android':'14','api':'34','space':'Available 1000000',
          'home':'example.stock/.Home','apps':'example.video/.Main\nexample.music/.Main',
          'packages':'package:example.video\npackage:example.music','disabled':''}[key]
    def close(self):self.closed=True

class CoreTest(unittest.TestCase):
    def setUp(self):self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup);self.s=Session(self.tmp.name,FakeDevice)
    def draft(self):return dict(style='clean',position='bottom',size='medium',spacing='comfortable',wallpaper='coast',apps=['example.video'])
    def test_restricts_target_to_literal_home_network(self):
        for host in ['127.0.0.1','169.254.1.1','8.8.8.8','localhost','host;whoami','192.168.1.50:5555','::1','224.0.0.1']:
            with self.subTest(host=host),self.assertRaises(SetupError):address(host,5555)
        self.assertEqual(address('192.168.1.50',5555),('192.168.1.50',5555))
        for port in [True,'5555',0,-1,65536]:
            with self.assertRaises(SetupError):address('192.168.1.50',port)
    def test_preview_record_is_not_a_real_backup(self):
        self.s.preview();result=self.s.snapshot();self.assertTrue(result['backup']['preview']);self.assertEqual(list(Path(self.tmp.name).iterdir()),[])
    def test_plan_is_validated_against_inventory_and_never_applied(self):
        self.s.preview();draft=self.draft();plan=self.s.plan(draft);self.assertFalse(plan['applied']);draft['apps'].clear();self.assertEqual(plan['choices']['apps'],['example.video'])
        for change in [{'apps':['not.installed']},{'apps':[]},{'apps':['example.video']*2},{'size':'enormous'},{'command':'reboot'}]:
            bad={**self.draft(),**change}
            with self.assertRaises(SetupError):self.s.plan(bad)
    def test_plan_is_not_possible_without_connection_or_preview(self):
        with self.assertRaises(SetupError):self.s.plan(self.draft())
    def test_readonly_probe_closes_transport_and_records_limits(self):
        FakeDevice.calls=[];self.s.connect({'host':'192.168.1.50','port':5555})
        for _ in range(100):
            if self.s.job['status']!='running':break
            time.sleep(.01)
        self.assertEqual(self.s.job['status'],'done');self.assertEqual(set(FakeDevice.calls),{'connect',*COMMANDS})
        self.assertNotIn('raw',self.s.public()['device']);self.assertEqual(len(self.s.public()['device']['apps']),2)
        result=self.s.snapshot();record=json.loads(Path(result['backup']['path']).read_text());self.assertEqual(record['kind'],'device-record');self.assertIn('not contain app private data',record['limits'])
    def test_pair_code_and_extra_fields_rejected_before_contact(self):
        for value in [{'host':'192.168.1.50','port':5555,'command':'id'}, {'host':'192.168.1.50','port':5555,'pairPort':37001,'code':'123;id'}]:
            with self.assertRaises(SetupError):self.s.connect(value)
        self.assertEqual(self.s.job['status'],'idle')
    def test_old_android_is_blocked(self):
        d=FakeDevice();original=d.read;d.read=lambda key:'25' if key=='api' else original(key)
        with self.assertRaises(SetupError):inspect(d)
    def test_device_checks_never_include_mutations(self):
        forbidden=['install','reboot','disable','set-home','settings put','input keyevent','grant','force-stop','uninstall']
        for command in COMMANDS.values():
            self.assertFalse(any(word in command for word in forbidden),command)
        self.assertEqual(set(COMMANDS),{'model','android','api','space','home','apps','packages','disabled'})
    def test_connection_error_never_leaks_raw_exception(self):
        class Broken(FakeDevice):
            def connect(self):raise RuntimeError('secret-debug-details')
        self.s.factory=Broken;self.s.connect({'host':'192.168.1.50','port':5555})
        for _ in range(100):
            if self.s.job['status']!='running':break
            time.sleep(.01)
        self.assertEqual(self.s.job['status'],'error');self.assertNotIn('secret',json.dumps(self.s.public()))

class ServerTest(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory();self.session=Session(self.tmp.name,FakeDevice);self.http,_=serve(self.session)
        self.thread=threading.Thread(target=self.http.serve_forever,daemon=True);self.thread.start();self.origin=f'http://127.0.0.1:{self.http.server_port}'
        status,data,headers=self.request('GET','/');self.assertEqual(status,200)
        import re
        self.token=re.search(rb'name="setup-token" content="([^"]+)"',data)[1].decode()
    def tearDown(self):self.http.shutdown();self.http.server_close();self.thread.join();self.tmp.cleanup()
    def request(self,method,path,body=None,headers=None):
        c=http.client.HTTPConnection('127.0.0.1',self.http.server_port,timeout=3)
        base={'Host':self.origin.removeprefix('http://')};base.update(headers or {})
        c.request(method,path,body,base);r=c.getresponse();data=r.read();h=dict(r.getheaders());c.close();return r.status,data,h
    def post(self,path,value,headers=None):return self.request('POST',path,json.dumps(value),{'Origin':self.origin,'Content-Type':'application/json','X-Setup-Token':self.token,**(headers or {})})
    def test_host_origin_and_token_boundaries(self):
        self.assertEqual(self.post('/api/preview',{}, {'Origin':'https://external.invalid'})[0],403)
        self.assertEqual(self.post('/api/preview',{}, {'X-Setup-Token':'wrong'})[0],403)
        self.assertEqual(self.request('GET','/',headers={'Host':'external.invalid'})[0],403)
        self.assertEqual(self.request('GET','/api/state')[0],403)
    def test_no_unreviewed_install_or_arbitrary_shell_endpoint(self):
        self.assertEqual(self.post('/api/install',{})[0],400)
        for path in ['/api/shell','/api/reboot','/api/home']:
            self.assertEqual(self.post(path,{})[0],404)
    def test_no_open_file_route_and_strict_assets(self):
        for path in ['/../LICENSE','/files?path=secret','/app.js?path=secret']:
            self.assertEqual(self.request('GET',path)[0],404)
        for path in ['/app.js','/app.css','/scene.svg','/outfit.ttf']:
            status,data,headers=self.request('GET',path);self.assertEqual(status,200);self.assertTrue(data)
            self.assertIn("frame-ancestors 'none'",headers['Content-Security-Policy'])
    def test_preview_and_plan_roundtrip(self):
        self.assertEqual(self.post('/api/preview',{})[0],200)
        draft=dict(style='clean',position='top',size='small',spacing='wide',wallpaper='dusk',apps=['example.music'])
        status,data,_=self.post('/api/plan',draft);self.assertEqual(status,200);self.assertFalse(json.loads(data)['applied'])
        self.assertEqual(json.loads(data)['choices'],draft)
    def test_native_routes_keep_session_boundaries_and_do_not_offer_shell_or_unreviewed_install(self):
        self.assertEqual(self.request('GET','/native.js')[0],200)
        self.assertEqual(self.post('/api/native-info',{})[0],200)
        self.assertEqual(self.post('/api/native-info',{}, {'X-Setup-Token':'wrong'})[0],403)
        self.assertEqual(self.post('/api/native-info',{'command':'reboot'})[0],400)
        self.assertEqual(self.post('/api/native-install',{})[0],400)
        self.assertEqual(self.post('/api/native-shell',{'command':'anything'})[0],404)
        self.session.native.lock.acquire()
        try:self.assertEqual(self.post('/api/quit',{})[0],400)
        finally:self.session.native.lock.release()

    def test_bounded_and_duplicate_json(self):
        headers={'Origin':self.origin,'Content-Type':'application/json','X-Setup-Token':self.token}
        self.assertEqual(self.request('POST','/api/preview','{"a":1,"a":2}',headers)[0],400)
        self.assertEqual(self.request('POST','/api/preview','x'*8193,headers)[0],413)
    def test_all_platform_catalogue_and_authenticated_ambient_package_download(self):
        self.assertEqual(self.request('GET','/platforms.js')[0],200)
        self.assertEqual(self.request('GET','/companion.js')[0],200)
        self.assertEqual(self.post('/api/companion-info',{})[0],200)
        self.assertEqual(self.post('/api/companion-roku',dict(theme='ocean',clock='24h',confirmed=False))[0],400)
        status,raw,_=self.post('/api/companion-roku',dict(theme='ocean',clock='24h',confirmed=True));self.assertEqual(status,200)
        package=json.loads(raw);self.assertFalse(package['installed'])
        self.assertEqual(self.request('GET',package['download'])[0],403)
        status,body,headers=self.request('GET',package['download'],headers={'X-Setup-Token':self.token});self.assertEqual(status,200);self.assertTrue(body.startswith(b'PK'));self.assertEqual(headers['Content-Type'],'application/zip')
        self.assertIsNone(self.session.inventory);self.assertIsNone(self.session.companion.bridge)
    def test_companion_routes_reject_unreviewed_actions_and_generic_commands(self):
        for route in ('start','open-vizio','capture','pair'):
            self.assertEqual(self.post('/api/companion-'+route,{})[0],400)
        self.assertEqual(self.post('/api/companion-info',{}, {'Origin':'https://external.invalid'})[0],403)
        self.assertEqual(self.post('/api/companion-shell',{'command':'anything'})[0],400)
    def test_active_hosted_session_blocks_quit_or_another_tv_operation(self):
        from types import SimpleNamespace
        self.session.companion.server=SimpleNamespace(close=lambda:None)
        try:
            self.assertEqual(self.post('/api/quit',{})[0],400)
            self.assertEqual(self.post('/api/preview',{})[0],400)
            self.assertEqual(self.post('/api/native-connect',{})[0],400)
            self.assertEqual(self.post('/api/companion-stop',{})[0],200)
            self.assertEqual(self.post('/api/preview',{})[0],200)
        finally:self.session.companion.server=None
    def test_useful_errors_survive_http(self):
        status,data,_=self.post('/api/connect',{'host':'not-an-address','port':5555});self.assertEqual(status,400);self.assertIn('local IPv4',json.loads(data)['error'])

if __name__=='__main__':unittest.main()
