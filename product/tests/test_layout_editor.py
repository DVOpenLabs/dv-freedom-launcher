import copy
import http.client
import importlib.util
import json
import shlex
from pathlib import Path
import tempfile
import threading
import unittest
from unittest.mock import Mock

ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('layout_editor',ROOT/'tools/layout_editor.py')
editor=importlib.util.module_from_spec(spec);spec.loader.exec_module(editor)

class EditorTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup)
        self.bridge=editor.DemoBridge();self.session=editor.Editor(self.bridge,'Test TV',Path(self.temp.name))
    def request(self):
        state=self.session.state();return {'revision':state['revision'],'profile':state['profile'],'layout':copy.deepcopy(state['layout'])}
    def edit(self):
        request=self.request();request['layout']['favorites'].reverse();return request
    def test_preview_has_no_writes_and_apply_requires_exact_review(self):
        before=self.session.state();request=self.edit();self.session.preview(request)
        self.assertEqual(before,self.session.state());self.assertEqual([],list(Path(self.temp.name).iterdir()))
        request['layout']['labels'][request['layout']['favorites'][0]]='Changed after review'
        with self.assertRaisesRegex(ValueError,'Preview'):self.session.apply(request)
    def test_apply_backup_then_undo_roundtrip(self):
        before=self.session.state()['layout'];request=self.edit();self.session.preview(request)
        result=self.session.apply(request);self.assertTrue(Path(result['backup']).is_file());self.assertTrue(result['state']['canUndo'])
        saved=json.loads(Path(result['backup']).read_text(encoding='utf-8'));self.assertEqual(before,saved['snapshot']['layout'])
        result=self.session.undo(self.request());self.assertEqual(before,result['state']['layout']);self.assertFalse(result['state']['canUndo'])
    def test_stale_draft_does_not_write_or_create_backup(self):
        request=self.edit();self.session.preview(request);self.bridge.counter+=1;self.bridge._revision()
        with self.assertRaisesRegex(ValueError,'changed'):self.session.apply(request)
        self.assertEqual([],list(Path(self.temp.name).iterdir()))
    def test_backup_failure_prevents_tv_write(self):
        request=self.edit();self.session.preview(request);before=self.bridge.call('snapshot')
        self.session._backup=Mock(side_effect=OSError('disk full'))
        with self.assertRaises(OSError):self.session.apply(request)
        self.assertEqual(before,self.bridge.call('snapshot'))
    def test_rejects_unsupported_keys_and_conflicting_visibility(self):
        draft=self.request()['layout'];draft['wallpaper']='other.jpg'
        with self.assertRaises(ValueError):editor.validate_layout(draft)
        del draft['wallpaper'];draft['hidden']=[draft['favorites'][0]]
        with self.assertRaises(ValueError):editor.validate_layout(draft)
    def test_rejects_duplicate_json_and_shell_package(self):
        with self.assertRaises(ValueError):editor.loads('{"schema":1,"schema":2}')
        draft=self.request()['layout'];draft['favorites']=['a.b; reboot']
        with self.assertRaises(ValueError):editor.validate_layout(draft)
    def test_native_transport_is_a_single_quoted_content_call(self):
        state=self.bridge.call('snapshot');runner=Mock(return_value=type('Result',(),{'returncode':0,'stdout':'Result: Bundle[{json='+json.dumps(state)+'}]','stderr':''})())
        bridge=editor.NativeBridge('192.168.1.50','org.example.launcher',ROOT,runner)
        request=self.request();request['layout']['labels'][request['layout']['favorites'][0]]="Kid's app; $(reboot)"
        self.assertEqual(state,bridge.call('apply',request))
        args,kwargs=runner.call_args;self.assertIsInstance(args[0],list);self.assertNotIn('shell',kwargs)
        self.assertEqual('192.168.1.50',kwargs['env']['TV_HOST']);self.assertIn('--method product_layout_apply --arg ',args[0][-1])
        self.assertEqual(request,json.loads(shlex.split(args[0][-1])[-1]))
        with self.assertRaises(ValueError):bridge.call('reboot')
    def test_native_target_is_not_a_shell_expression(self):
        with self.assertRaises(ValueError):editor.NativeBridge('tv;reboot','org.example.launcher')

class HttpTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup)
        self.server,self.token=editor.server(editor.Editor(editor.DemoBridge(),'Demo',self.temp.name))
        self.thread=threading.Thread(target=self.server.serve_forever,daemon=True);self.thread.start()
        self.addCleanup(self.server.server_close);self.addCleanup(self.server.shutdown)
        self.port=self.server.server_port
    def call(self,path='/api/state',token=True,method='GET',body=None,extra=None):
        connection=http.client.HTTPConnection('127.0.0.1',self.port,timeout=3)
        headers={'Content-Type':'application/json'}
        if token:headers['X-Editor-Token']=self.token
        if extra:headers.update(extra)
        connection.request(method,path,body=body,headers=headers);response=connection.getresponse();data=response.read();status=response.status;h=dict(response.getheaders());connection.close();return status,data,h
    def test_requires_token_and_correct_host_and_origin(self):
        self.assertEqual(403,self.call(token=False)[0])
        self.assertEqual(403,self.call(extra={'Host':'evil.example'})[0])
        self.assertEqual(403,self.call(extra={'Origin':'http://evil.example'})[0])
        self.assertEqual(200,self.call()[0])
    def test_static_page_has_csp_and_no_open_path_access(self):
        status,data,headers=self.call('/');self.assertEqual(200,status)
        self.assertIn(self.token.encode(),data);self.assertIn("frame-ancestors 'none'",headers['Content-Security-Policy'])
        self.assertEqual(404,self.call('/../../AGENTS.md')[0])
    def test_import_validates_without_mutating_demo(self):
        state=json.loads(self.call()[1]);draft=state['layout'];draft['favorites'].reverse()
        status,data,_=self.call('/api/import',method='POST',body=json.dumps(draft));self.assertEqual(200,status)
        self.assertEqual(draft,json.loads(data)['layout']);self.assertNotEqual(draft,json.loads(self.call()[1])['layout'])
        self.assertEqual(400,self.call('/api/import',method='POST',body='{"schema":1,"schema":2}')[0])
    def test_mutation_rejects_oversized_and_unreviewed_payloads(self):
        state=json.loads(self.call()[1]);request={'revision':state['revision'],'profile':state['profile'],'layout':state['layout']}
        self.assertEqual(400,self.call('/api/apply',method='POST',body=json.dumps(request))[0])
        self.assertEqual(413,self.call('/api/preview',method='POST',body='x'*65537)[0])

if __name__=='__main__':unittest.main()
