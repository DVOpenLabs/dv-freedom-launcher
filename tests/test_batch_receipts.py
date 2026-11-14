"""Malformed batches send nothing; remote failures stop before later restore commands."""
import base64
import contextlib
import importlib.util
import io
from pathlib import Path
import re
import sys
import unittest
from unittest import mock
from test_tv_target import load_tv

ROOT=Path(__file__).resolve().parents[1]

class BatchReceiptsTest(unittest.TestCase):
    def setUp(self):self.tv=load_tv({'TV_HOST':'192.0.2.1'})
    def test_invalid_encoding_and_nul_fail_before_connect(self):
        for raw in (b'echo first\n\xff',b'echo first\necho\x00second'):
            with self.subTest(raw=raw),mock.patch.object(sys,'argv',['tv.py','--batch']),mock.patch.object(sys,'stdin',io.TextIOWrapper(io.BytesIO(raw))),mock.patch.object(self.tv,'connect') as connect,contextlib.redirect_stderr(io.StringIO()):
                self.assertEqual(2,self.tv.main());connect.assert_not_called()
    def test_bom_comments_crlf_and_bound(self):
        self.assertEqual(['echo one','echo two'],self.tv.batch_commands(b'\xef\xbb\xbf# note\r\necho one\r\necho two\r\n'))
        with self.assertRaises(ValueError):self.tv.batch_commands(b'x'*(self.tv.MAX_BATCH_BYTES+1))
    def test_failed_remote_status_stops_and_preserves_output(self):
        calls=[]
        class Device:
            def shell(s,command,**kw):
                calls.append(command);marker=re.search(r'DVF_RC_[a-f0-9]+=',''.join(command)).group()
                return 'answer\n'+marker+('0' if len(calls)==1 else '7')+'\n'
        with contextlib.redirect_stdout(io.StringIO()) as out,contextlib.redirect_stderr(io.StringIO()) as err:
            self.assertEqual(1,self.tv.run_batch(Device(),['echo one','exit 7','echo never']))
        self.assertEqual(2,len(calls));self.assertIn('answer',out.getvalue());self.assertIn('remote exit 7',err.getvalue());self.assertNotIn('DVF_RC_',out.getvalue())
    def test_missing_receipt_and_disconnect_never_continue(self):
        for failure in (False,True):
            dev=mock.Mock();dev.shell.side_effect=OSError('lost') if failure else lambda *a,**k:'partial output'
            with contextlib.redirect_stdout(io.StringIO()),contextlib.redirect_stderr(io.StringIO()):self.assertEqual(1,self.tv.run_batch(dev,['first','never']))
            self.assertEqual(1,dev.shell.call_count)
    def test_chunk_command_includes_path_and_protocol_overhead(self):
        spec=importlib.util.spec_from_file_location('bounded_upload',ROOT/'tools/b64push.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
        for payload in (4096,65536):
            for remote in ('/data/local/tmp/file.apk','/data/local/tmp/'+('a'*500)):
                n=m.chunk_size(payload,remote);cmd="echo '"+base64.b64encode(b'x'*n).decode()+"' >> "+m.q(remote+'.b64.part')+' && echo ok999999999999'
                self.assertEqual(0,n%3);self.assertLessEqual(len(cmd.encode()),min(payload-8,m.SHELL_COMMAND_LIMIT))
        with self.assertRaises(ValueError):m.chunk_size(4096,'/'+('a'*5000))
    def test_restore_readback_uses_exact_package_home_and_setting_states(self):
        spec=importlib.util.spec_from_file_location('checked_restore',ROOT/'tools/recovery.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
        plan=['pm enable com.example.keep','cmd package set-home-activity --user 0 com.example.home/.Main','settings put global window_animation_scale 0.5','settings delete global animator_duration_scale']
        with mock.patch.object(m,'restore_commands',return_value=plan):checks=m.restore_checks('unused','com.example.home','unused')
        self.assertEqual(4,len(checks));self.assertIn("grep -Fx 'package:com.example.keep'",checks[0]);self.assertIn("_dvf_home='com.example.home/.Main'",checks[1]);self.assertIn("= '0.5'",checks[2]);self.assertIn("= 'null'",checks[3])

if __name__=='__main__':unittest.main()
