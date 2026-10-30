import base64
import json
from pathlib import Path
import tempfile
import unittest
import http.client
import re
import threading
from unittest.mock import patch
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
from cryptography.hazmat.primitives import serialization
from setup_app.phone_app import PhoneCatalog
from setup_app.release import canonical, digest
from setup_app.core import SetupError
from setup_app.server import serve
from setup_app.core import Session


class PhoneCatalogTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.key = Ed25519PrivateKey.generate()
        self.trust = self.root / 'trust.json'
        self.trust.write_text(json.dumps({'publicKey': base64.b64encode(self.key.public_key().public_bytes(serialization.Encoding.Raw, serialization.PublicFormat.Raw)).decode()}), encoding='utf-8')
        self.apk = self.root / 'phone-setup.apk'
        self.apk.write_bytes(b'fixed test artifact')
        self.release = dict(schema=1, package='com.dvopenlabs.freedom.setup', versionCode=1,
                            versionName='test', minSdk=29, file=self.apk.name,
                            bytes=self.apk.stat().st_size, sha256=digest(self.apk),
                            certificate='a'*64, channel='private-alpha')
        self.save()
        self.catalog = PhoneCatalog(self.root, self.trust)
        mock = patch('setup_app.phone_app.apk_certificate', return_value='a'*64)
        mock.start()
        self.addCleanup(mock.stop)

    def save(self):
        (self.root / 'phone-catalog.json').write_text(json.dumps({'release': self.release, 'signature': base64.b64encode(self.key.sign(canonical(self.release))).decode()}), encoding='utf-8')

    def test_verified_summary_does_not_expose_file_paths(self):
        self.assertEqual(self.catalog.verify()['path'], self.apk)
        self.assertTrue(self.catalog.summary()['available'])
        self.assertNotIn(str(self.root), json.dumps(self.catalog.summary()))

    def test_tampered_artifact_cannot_be_downloaded(self):
        self.apk.write_bytes(b'changed')
        with self.assertRaises(SetupError):
            self.catalog.verify()

    def test_signed_path_identity_and_platform_substitution_are_refused(self):
        for key, value in [('file', '../other.apk'), ('package', 'other.app'), ('minSdk', 26), ('channel', 'public')]:
            with self.subTest(key=key):
                before = self.release[key]
                self.release[key] = value
                self.save()
                with self.assertRaises(SetupError):
                    self.catalog.verify()
                self.release[key] = before

    def test_metadata_cannot_change_without_signature(self):
        doc = json.loads((self.root / 'phone-catalog.json').read_text(encoding='utf-8'))
        doc['release']['versionCode'] = 99
        (self.root / 'phone-catalog.json').write_text(json.dumps(doc), encoding='utf-8')
        self.assertFalse(self.catalog.summary()['available'])

    def test_missing_bundle_is_a_safe_desktop_fallback(self):
        (self.root / 'phone-catalog.json').unlink()
        result = self.catalog.summary()
        self.assertFalse(result['available'])
        self.assertNotIn(str(self.root), result['message'])


class PhoneDownloadTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.http, _ = serve(Session(self.temp.name))
        self.thread = threading.Thread(target=self.http.serve_forever, daemon=True)
        self.thread.start()
        _, body = self.get('/')
        self.token = re.search(rb'name="setup-token" content="([^"]+)"', body)[1].decode()

    def tearDown(self):
        self.http.shutdown()
        self.http.server_close()
        self.thread.join()
        self.temp.cleanup()

    def get(self, path, token=None):
        client = http.client.HTTPConnection('127.0.0.1', self.http.server_port, timeout=3)
        headers = {'Host': '127.0.0.1:' + str(self.http.server_port)}
        if token is not None:
            headers['X-Setup-Token'] = token
        client.request('GET', path, headers=headers)
        response = client.getresponse()
        result = response.status, response.read()
        client.close()
        return result

    def test_phone_apk_needs_current_loopback_session(self):
        with patch('setup_app.phone_app.PhoneCatalog.verify') as verify:
            self.assertEqual(self.get('/api/phone-app-apk')[0], 403)
            verify.assert_not_called()

    def test_only_verified_fixed_apk_is_served(self):
        artifact = Path(self.temp.name) / 'fixed.apk'
        artifact.write_bytes(b'verified test bytes')
        with patch('setup_app.phone_app.PhoneCatalog.verify', return_value={'path': artifact}):
            self.assertEqual(self.get('/api/phone-app-apk', self.token), (200, b'verified test bytes'))
            self.assertEqual(self.get('/api/phone-app-apk/other.apk', self.token)[0], 404)

    def test_bad_signature_returns_no_artifact(self):
        with patch('setup_app.phone_app.PhoneCatalog.verify', side_effect=SetupError('Verification refused')):
            status, body = self.get('/api/phone-app-apk', self.token)
            self.assertEqual(status, 400)
            self.assertEqual(json.loads(body)['error'], 'Verification refused')
