"""Build/sign the optional native phone app using the existing external release identity."""
import argparse
import base64
import json
import os
from pathlib import Path
import subprocess
from cryptography.hazmat.primitives import serialization
from .phone_app import PhoneCatalog
from .release import canonical,digest,apk_certificate,verify_resource_paths

ROOT=Path(__file__).resolve().parents[1]
def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--key-dir',required=True,type=Path);args=parser.parse_args();keys=args.key_dir.resolve()
    if keys==ROOT or ROOT in keys.parents:raise SystemExit('Private keys must remain outside source.')
    key=serialization.load_pem_private_key((keys/'catalog.key').read_bytes(),None)
    public=base64.b64encode(key.public_key().public_bytes(serialization.Encoding.Raw,serialization.PublicFormat.Raw)).decode()
    if public!=json.loads((ROOT/'setup_app/trust.json').read_text(encoding='utf-8'))['publicKey']:raise SystemExit('Pinned catalogue identity differs. Do not replace existing keys.')
    env=os.environ.copy();env['DV_PHONE_PASSWORD']=(keys/'password.txt').read_text(encoding='utf-8')
    subprocess.run([str(ROOT/'launcher/gradlew.bat'),'-p',str(ROOT/'mobile'),'--no-daemon',':app:testReleaseUnitTest',':app:lintRelease',':app:assembleRelease'],cwd=ROOT,env=env,check=True)
    folder=ROOT/'mobile/app/build/outputs/apk/release';m=json.loads((folder/'output-metadata.json').read_text(encoding='utf-8'));item=m['elements'][0]
    if m['applicationId']!='com.dvopenlabs.freedom.setup':raise SystemExit('Unexpected phone identity.')
    sdk=Path(env['ANDROID_HOME']);java=Path(env['JAVA_HOME'])/'bin/java.exe';signer=sdk/'build-tools/34.0.0/lib/apksigner.jar';bundle=ROOT/'setup_app/release_bundle';bundle.mkdir(exist_ok=True)
    out=bundle/'phone-setup.apk'
    subprocess.run([str(java),'-jar',str(signer),'sign','--ks',str(keys/'release.p12'),'--ks-type','PKCS12','--ks-key-alias','dv-freedom','--ks-pass','env:DV_PHONE_PASSWORD','--key-pass','env:DV_PHONE_PASSWORD','--v4-signing-enabled','false','--out',str(out),str(folder/item['outputFile'])],env=env,check=True)
    subprocess.run([str(java),'-jar',str(signer),'verify',str(out)],env=env,check=True);verify_resource_paths(out)
    r=dict(schema=1,package=m['applicationId'],versionCode=item['versionCode'],versionName=item['versionName'],minSdk=29,file=out.name,bytes=out.stat().st_size,sha256=digest(out),certificate=apk_certificate(out),channel='private-alpha')
    (bundle/'phone-catalog.json').write_text(json.dumps({'release':r,'signature':base64.b64encode(key.sign(canonical(r))).decode()},indent=2)+'\n',encoding='utf-8');PhoneCatalog().verify();print('Native phone APK signed and verified. Nothing was installed or shared.')
if __name__=='__main__':main()
