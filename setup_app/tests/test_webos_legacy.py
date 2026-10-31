"""Regression for package resources rejected by webOS 1.x's opaque file origins."""
from html.parser import HTMLParser
import json
from pathlib import Path
import tempfile
import unittest

from platforms.webos.prepare_launcher import prepare, finalize, script_json


class Document(HTMLParser):
    def __init__(self, text):
        super().__init__()
        self.scripts=[];self.policy='';self.styles=[];self.active=None
        self.feed(text)
    def handle_starttag(self, tag, attrs):
        attrs=dict(attrs)
        if tag=='meta' and attrs.get('http-equiv')=='Content-Security-Policy':self.policy=attrs['content']
        if tag=='script':self.scripts.append({'src':attrs.get('src'),'code':''});self.active='script'
        if tag=='style':self.styles.append('');self.active='style'
    def handle_data(self, text):
        if self.active=='script':self.scripts[-1]['code']+=text
        if self.active=='style':self.styles[-1]+=text
    def handle_endtag(self, tag):
        if tag==self.active:self.active=None


class LegacyPackageTest(unittest.TestCase):
    def test_legacy_has_no_external_code_or_resource_permissions(self):
        with tempfile.TemporaryDirectory() as tmp:
            folder=prepare(Path(tmp)/'app',sdk_version='1.4.0')
            document=Document((folder/'index.html').read_text(encoding='utf-8'))
            self.assertEqual(len(document.scripts),5)
            self.assertTrue(all(not s['src'] and s['code'].strip() for s in document.scripts))
            self.assertEqual(len(document.styles),1)
            self.assertIn('button:focus',document.styles[0])
            self.assertIn("script-src 'unsafe-inline'",document.policy)
            for blocked in ('default-src','connect-src','media-src','img-src','object-src','frame-src'):
                self.assertIn(blocked+" 'none'",document.policy)
            self.assertNotIn('unsafe-eval',document.policy)
            self.assertNotIn('file:',document.policy)
            self.assertEqual(json.loads((folder/'appinfo.json').read_text())['requiredACG'],['application.launcher'])

    def test_starter_labels_cannot_create_scripts_and_finalize_is_repeatable(self):
        with tempfile.TemporaryDirectory() as tmp:
            folder=prepare(Path(tmp)/'app',sdk_version='1.4.0')
            label='</script><script>alert(1)</script><!--\u2028 & TV'
            seed={'schema':1,'apps':[{'id':'verified.player','label':label}]}
            with (folder/'bootstrap.js').open('a',encoding='utf-8') as stream:
                stream.write('window.FREEDOM_SETUP='+script_json(seed)+';\n')
            finalize(folder,'1.4.0')
            first=(folder/'index.html').read_text(encoding='utf-8')
            finalize(folder,'1.4.0')
            self.assertEqual(first,(folder/'index.html').read_text(encoding='utf-8'))
            document=Document(first)
            self.assertEqual(len(document.scripts),5)
            bootstrap=document.scripts[1]['code']
            self.assertIn('FREEDOM_PLATFORM',bootstrap)
            payload=bootstrap.split('window.FREEDOM_SETUP=',1)[1].strip().removesuffix(';')
            self.assertEqual(json.loads(payload),seed)

    def test_modern_keeps_external_self_policy(self):
        with tempfile.TemporaryDirectory() as tmp:
            folder=prepare(Path(tmp)/'app',sdk_version='4.0.0')
            document=Document((folder/'index.html').read_text(encoding='utf-8'))
            self.assertTrue(all(s['src'] for s in document.scripts))
            self.assertIn("script-src 'self'",document.policy)
            self.assertNotIn('unsafe-inline',document.policy)

    def test_unexpected_script_terminator_is_refused(self):
        with tempfile.TemporaryDirectory() as tmp:
            folder=prepare(Path(tmp)/'app',sdk_version='1.4.0')
            (folder/'bootstrap.js').write_text('window.X="</script>";',encoding='utf-8')
            with self.assertRaises(ValueError):finalize(folder,'1.4.0')

    def test_reviewed_hdmi_targets_survive_legacy_embedding(self):
        with tempfile.TemporaryDirectory() as tmp:
            folder=prepare(Path(tmp)/'app',sdk_version='1.4.0',inputs=[{'id':'com.webos.app.hdmi2','label':'HDMI 2'}])
            document=Document((folder/'index.html').read_text(encoding='utf-8'))
            self.assertIn('window.FREEDOM_INPUTS=',document.scripts[1]['code'])
            self.assertIn('com.webos.app.hdmi2',document.scripts[1]['code'])
            with self.assertRaises(ValueError):prepare(Path(tmp)/'bad',inputs=[{'id':'arbitrary.app','label':'Wrong'}])
