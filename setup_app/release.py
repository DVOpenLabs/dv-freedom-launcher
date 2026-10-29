"""Pinned signed offline catalog. APK bytes are authenticated before reaching a device."""
import base64
import hashlib
import json
from pathlib import Path
import re
import struct
from .core import SetupError

MAX_APK=40*1024*1024
def canonical(value):return json.dumps(value,sort_keys=True,separators=(',',':'),ensure_ascii=True).encode()
def digest(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def lp(data,offset=0):
    if offset+4>len(data):raise SetupError('Invalid APK signing data.')
    n=struct.unpack_from('<I',data,offset)[0];end=offset+4+n
    if n>MAX_APK or end>len(data):raise SetupError('Invalid APK signing data.')
    return data[offset+4:end],end
def apk_certificate(path):
    """Extract one v2 signer certificate; this does not replace whole-file signature verification."""
    data=Path(path).read_bytes()
    if len(data)>MAX_APK:raise SetupError('APK is too large.')
    eocd=data.rfind(b'PK\x05\x06',max(0,len(data)-65557))
    if eocd<0 or eocd+22>len(data):raise SetupError('APK ZIP footer is invalid.')
    comment=struct.unpack_from('<H',data,eocd+20)[0]
    if eocd+22+comment!=len(data):raise SetupError('APK ZIP footer is invalid.')
    central=struct.unpack_from('<I',data,eocd+16)[0]
    if central<32 or data[central-16:central]!=b'APK Sig Block 42':raise SetupError('A v2-signed APK is required.')
    size=struct.unpack_from('<Q',data,central-24)[0];start=central-size-8
    if start<0 or size>MAX_APK or struct.unpack_from('<Q',data,start)[0]!=size:raise SetupError('Invalid APK signing block.')
    offset=start+8;certificates=[]
    while offset<central-24:
        n=struct.unpack_from('<Q',data,offset)[0];offset+=8
        if n<4 or offset+n>central-24:raise SetupError('Invalid APK signing block.')
        block_id=struct.unpack_from('<I',data,offset)[0];block=data[offset+4:offset+n];offset+=n
        if block_id==0x7109871a:
            signers,_=lp(block);signer,finish=lp(signers)
            if finish!=len(signers):raise SetupError('Multiple APK signers are not supported.')
            signed,_=lp(signer);_,next_field=lp(signed);certs,_=lp(signed,next_field);cert,_=lp(certs)
            certificates.append(hashlib.sha256(cert).hexdigest())
    if len(certificates)!=1:raise SetupError('Exactly one v2 APK signer is required.')
    return certificates[0]

class Catalog:
    def __init__(self,folder=None,trust=None):
        self.folder=Path(folder) if folder else Path(__file__).with_name('release_bundle')
        self.trust=Path(trust) if trust else Path(__file__).with_name('trust.json')
    def verify(self):
        try:
            from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PublicKey
            raw=(self.folder/'catalog.json').read_bytes()
            if len(raw)>8192:raise ValueError()
            envelope=json.loads(raw);body=envelope['release'];key=json.loads(self.trust.read_text(encoding='utf-8'))
            Ed25519PublicKey.from_public_bytes(base64.b64decode(key['publicKey'],validate=True)).verify(base64.b64decode(envelope['signature'],validate=True),canonical(body))
            if set(body)!={'schema','package','versionCode','versionName','minSdk','file','bytes','sha256','certificate','channel'} or body['schema']!=1:raise ValueError()
            if body['package']!='com.dvopenlabs.freedomlauncher' or body['file']!='launcher.apk' or body['channel']!='private-alpha':raise ValueError()
            if type(body['bytes']) is not int or not 0<body['bytes']<=MAX_APK or type(body['versionCode']) is not int or body['versionCode']<1 or body['minSdk']!=26:raise ValueError()
            if any(not isinstance(body[k],str) or not re.fullmatch('[0-9a-f]{64}',body[k]) for k in ['sha256','certificate']):raise ValueError()
            apk=self.folder/body['file']
            if apk.stat().st_size!=body['bytes'] or digest(apk)!=body['sha256'] or apk_certificate(apk)!=body['certificate']:raise ValueError()
            return {**body,'path':apk}
        except Exception:
            raise SetupError('The offline launcher release is missing or failed verification. Rebuild or obtain a trusted DV Freedom Setup package. Nothing was installed.')
    def summary(self):
        try:
            value=self.verify();value.pop('path');return {'available':True,**value}
        except SetupError as error:return {'available':False,'message':str(error)}
