"""Fixed signed native-phone artifact. No device operations or arbitrary download paths."""
import base64
import json
from pathlib import Path
import re
from .core import SetupError
from .release import canonical, digest, apk_certificate, MAX_APK

class PhoneCatalog:
    def __init__(self,folder=None,trust=None):
        self.folder=Path(folder) if folder else Path(__file__).with_name('release_bundle')
        self.trust=Path(trust) if trust else Path(__file__).with_name('trust.json')
    def verify(self):
        try:
            from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PublicKey
            raw=(self.folder/'phone-catalog.json').read_bytes()
            if len(raw)>8192:raise ValueError()
            envelope=json.loads(raw)
            if set(envelope)!={'release','signature'}:raise ValueError()
            r=envelope['release'];key=json.loads(self.trust.read_text(encoding='utf-8'))
            Ed25519PublicKey.from_public_bytes(base64.b64decode(key['publicKey'],validate=True)).verify(base64.b64decode(envelope['signature'],validate=True),canonical(r))
            if set(r)!={'schema','package','versionCode','versionName','minSdk','file','bytes','sha256','certificate','channel'}:raise ValueError()
            if r['schema']!=1 or r['package']!='com.dvopenlabs.freedom.setup' or r['file']!='phone-setup.apk' or r['channel']!='private-alpha' or r['minSdk']!=29:raise ValueError()
            if type(r['versionCode']) is not int or r['versionCode']<1 or type(r['bytes']) is not int or not 0<r['bytes']<=MAX_APK or not isinstance(r['versionName'],str) or len(r['versionName'])>80:raise ValueError()
            if any(not isinstance(r[k],str) or not re.fullmatch('[a-f0-9]{64}',r[k]) for k in ('sha256','certificate')):raise ValueError()
            apk=self.folder/r['file']
            if apk.stat().st_size!=r['bytes'] or digest(apk)!=r['sha256'] or apk_certificate(apk)!=r['certificate']:raise ValueError()
            return {**r,'path':apk}
        except Exception:raise SetupError('Native phone Setup is not included or failed signature verification. Use the desktop path; nothing was downloaded or installed.')
    def summary(self):
        try:return {'available':True,**{k:v for k,v in self.verify().items() if k!='path'}}
        except SetupError as error:return {'available':False,'message':str(error)}
