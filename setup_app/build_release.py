"""Developer-only signed offline bundle. Private keys never enter source or the setup executable."""
import argparse
import base64
from datetime import datetime,timedelta,timezone
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import subprocess
from cryptography import x509
from cryptography.hazmat.primitives import hashes,serialization
from cryptography.hazmat.primitives.asymmetric import rsa,ed25519
from cryptography.hazmat.primitives.serialization import pkcs12
from cryptography.x509.oid import NameOID
from setup_app.release import canonical,digest,apk_certificate,verify_resource_paths

ROOT=Path(__file__).resolve().parents[1]
def write_private(path,value):
    fd=os.open(path,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
    with os.fdopen(fd,'wb') as out:out.write(value)
def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--key-dir',type=Path,required=True);p.add_argument('--init-keys',action='store_true');a=p.parse_args()
    folder=a.key_dir.resolve();folder.mkdir(parents=True,exist_ok=True,mode=0o700)
    if ROOT==folder or ROOT in folder.parents:raise SystemExit('Keep signing keys in an ignored private directory outside public source.')
    if os.name=='nt':
        who=subprocess.check_output(['whoami'],text=True).strip();subprocess.run(['icacls',str(folder),'/inheritance:r','/grant:r',who+':(OI)(CI)F'],check=True,stdout=subprocess.DEVNULL)
    if not (folder/'release.p12').exists():
        if not a.init_keys:raise SystemExit('Initial key creation requires --init-keys. Existing keys must be preserved for updates.')
        if any(folder.iterdir()):raise SystemExit('Private key directory must be empty for initialization.')
        password=secrets.token_urlsafe(40);key=rsa.generate_private_key(public_exponent=65537,key_size=3072);name=x509.Name([x509.NameAttribute(NameOID.COMMON_NAME,'DV Freedom Launcher')]);now=datetime.now(timezone.utc)
        cert=x509.CertificateBuilder().subject_name(name).issuer_name(name).public_key(key.public_key()).serial_number(x509.random_serial_number()).not_valid_before(now-timedelta(days=1)).not_valid_after(now+timedelta(days=365*30)).sign(key,hashes.SHA256())
        write_private(folder/'release.p12',pkcs12.serialize_key_and_certificates(b'dv-freedom',key,cert,None,serialization.BestAvailableEncryption(password.encode())))
        write_private(folder/'password.txt',password.encode());catalog=ed25519.Ed25519PrivateKey.generate();write_private(folder/'catalog.key',catalog.private_bytes(serialization.Encoding.PEM,serialization.PrivateFormat.PKCS8,serialization.NoEncryption()))
    catalog=serialization.load_pem_private_key((folder/'catalog.key').read_bytes(),None);public=base64.b64encode(catalog.public_key().public_bytes(serialization.Encoding.Raw,serialization.PublicFormat.Raw)).decode()
    trust=ROOT/'setup_app/trust.json'
    if trust.exists() and json.loads(trust.read_text(encoding='utf-8'))['publicKey']!=public:raise SystemExit('Catalog key differs from the pinned trust anchor. Key rotation needs a reviewed migration.')
    if not trust.exists():trust.write_text(json.dumps({'schema':1,'algorithm':'Ed25519','publicKey':public},indent=2)+'\n',encoding='utf-8')
    env=os.environ.copy();env['DVFREEDOM_KEYSTORE']=str(folder/'release.p12');env['DVFREEDOM_KEY_PASSWORD']=(folder/'password.txt').read_text(encoding='utf-8')
    gradle=ROOT/'launcher'/('gradlew.bat' if os.name=='nt' else 'gradlew');subprocess.run([str(gradle),'--no-daemon','-PcommunityFeatures=true','-PfreedomRelease=true','testReleaseUnitTest','lintRelease','assembleRelease'],cwd=ROOT/'launcher',env=env,check=True)
    apk=ROOT/'launcher/app/build/outputs/apk/release/app-release.apk';verify_resource_paths(apk);sdk=Path(env['ANDROID_HOME']);java=Path(env['JAVA_HOME'])/'bin'/('java.exe' if os.name=='nt' else 'java')
    checked=subprocess.check_output([str(java),'-jar',str(sdk/'build-tools/34.0.0/lib/apksigner.jar'),'verify','--print-certs',str(apk)],text=True)
    certificate=re.search(r'Signer #1 certificate SHA-256 digest: ([0-9a-f]+)',checked)[1]
    if certificate!=apk_certificate(apk):raise SystemExit('Signer extraction did not match Android SDK verification.')
    metadata=json.loads((apk.parent/'output-metadata.json').read_text(encoding='utf-8'));element=metadata['elements'][0]
    if metadata['applicationId']!='com.dvopenlabs.freedomlauncher':raise SystemExit('Unexpected release identity.')
    bundle=ROOT/'setup_app/release_bundle';bundle.mkdir(exist_ok=True);shutil.copyfile(apk,bundle/'launcher.apk')
    body={'schema':1,'package':metadata['applicationId'],'versionCode':element['versionCode'],'versionName':element['versionName'],'minSdk':26,'file':'launcher.apk','bytes':apk.stat().st_size,'sha256':digest(apk),'certificate':certificate,'channel':'private-alpha'}
    (bundle/'catalog.json').write_text(json.dumps({'release':body,'signature':base64.b64encode(catalog.sign(canonical(body))).decode()},indent=2)+'\n',encoding='utf-8');print('Signed offline release prepared; private keys remain outside the bundle.')
if __name__=='__main__':main()
