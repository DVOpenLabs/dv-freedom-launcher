import json
from pathlib import Path
import tempfile
import unittest
from setup_app.core import Session
from setup_app.support import report
from setup_app.extras import catalogue


class SupportTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.session = Session(Path(self.tmp.name))
        from setup_app.release import Catalog
        self.session.installer.catalog = Catalog(Path(self.tmp.name) / 'missing-bundle')

    def test_report_does_not_serialize_private_state_or_errors(self):
        secret = 'PRIVATE-SENTINEL-192.168.50.20-owner-city-path-key'
        self.session.mode = 'device'
        self.session.connection = {'host': secret, 'port': 5555}
        self.session.inventory = dict(api=34, model=secret, apps=[{'package': secret}], raw={'error': secret})
        self.session.backup = {'path': secret}
        self.session.job = {'status': 'error', 'message': secret}
        data = report(self.session)
        self.assertNotIn(secret, json.dumps(data))
        self.assertEqual(data['androidApi'], 34)
        self.assertEqual(set(data), {'schema','kind','createdAt','computerOS','mode','androidApi','release','checks','privacy'})

    def test_preview_never_claims_device_or_release_acceptance(self):
        self.session.preview()
        data = report(self.session)
        self.assertIsNone(data['androidApi'])
        self.assertEqual(data['checks'][1]['status'], 'not-checked')
        self.assertFalse(data['release']['available'])

    def test_extras_have_explicit_scope_and_no_install_payload(self):
        data = catalogue()
        self.assertGreaterEqual(len(data['items']), 6)
        for item in data['items']:
            self.assertEqual(set(item), {'name','category','summary','url','platforms','access'})
            self.assertTrue(item['url'].startswith('https://'))
            self.assertTrue(item['access'])
            self.assertNotIn('lg', item['platforms'])
