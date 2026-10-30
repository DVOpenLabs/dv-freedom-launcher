"""Build curated optional companions and authenticate an offline catalogue using the existing key."""
import argparse
import base64
import json
import os
from pathlib import Path
import re
import subprocess
from cryptography.hazmat.primitives import serialization
from .addons import PACKAGES,AddonCatalog
from .release import canonical,digest,apk_certificate,verify_resource_paths

ROOT=Path(__file__).resolve().parents[1]
def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--key-dir',type=Path,required=True);args=parser.parse_args()
    keys=args.key_dir.resolve()
    if ROOT==keys or ROOT in keys.parents:raise SystemExit('Private keys must remain outside source.')
    key=serialization.load_pem_private_key((keys/'catalog.key').read_bytes(),None)
    public=base64.b64encode(key.public_key().public_bytes(serialization.Encoding.Raw,serialization.PublicFormat.Raw)).decode()
    if public!=json.loads((ROOT/'setup_app/trust.json').read_text(encoding='utf-8'))['publicKey']:raise SystemExit('Pinned catalog key mismatch.')
    env=os.environ.copy();env['DV_ADDON_PASSWORD']=(keys/'password.txt').read_text(encoding='utf-8')
    sdk=Path(env['ANDROID_HOME']);java=Path(env['JAVA_HOME'])/'bin/java.exe';signer=sdk/'build-tools/34.0.0/lib/apksigner.jar';aapt=sdk/'build-tools/34.0.0/aapt2.exe'
    bundle=ROOT/'setup_app/release_bundle/addons';bundle.mkdir(parents=True,exist_ok=True);items=[]
    for id,pkg in PACKAGES.items():
        module={'companions':'companions','phone-remote':'remote'}.get(id,'app');project=ROOT/'launcher' if id in {'companions','phone-remote'} else ROOT/'addons'/id
        gradle=ROOT/'launcher/gradlew.bat'
        subprocess.run([str(gradle),'-p',str(project),'--no-daemon',':'+module+':testReleaseUnitTest',':'+module+':lintRelease',':'+module+':assembleRelease'],cwd=ROOT,env=env,check=True)
        folder=project/module/'build/outputs/apk/release';metadata=json.loads((folder/'output-metadata.json').read_text(encoding='utf-8'));element=metadata['elements'][0];apk=folder/element['outputFile'];out=bundle/(id+'.apk')
        if metadata['applicationId']!=pkg:raise SystemExit('Unexpected companion identity.')
        subprocess.run([str(java),'-jar',str(signer),'sign','--ks',str(keys/'release.p12'),'--ks-type','PKCS12','--ks-key-alias','dv-freedom','--ks-pass','env:DV_ADDON_PASSWORD','--key-pass','env:DV_ADDON_PASSWORD','--v4-signing-enabled','false','--out',str(out),str(apk)],env=env,check=True)
        out.with_suffix('.apk.idsig').unlink(missing_ok=True)
        subprocess.run([str(java),'-jar',str(signer),'verify',str(out)],env=env,check=True);verify_resource_paths(out)
        manifest=subprocess.check_output([str(aapt),'dump','badging',str(out)],text=True,encoding='utf-8')
        permissions=re.findall(r"uses-permission(?:-sdk-\d+)?: name='([^']+)'",manifest);minimum=re.search(r"sdkVersion:'(\d+)'",manifest)
        items.append(dict(id=id,package=pkg,file=out.name,versionCode=element['versionCode'],versionName=element['versionName'],minSdk=int(minimum[1]),bytes=out.stat().st_size,sha256=digest(out),certificate=apk_certificate(out),permissions=sorted(set(permissions))))
    body={'schema':1,'items':items};(bundle/'catalog.json').write_text(json.dumps({'addons':body,'signature':base64.b64encode(key.sign(canonical(body))).decode()},indent=2)+'\n',encoding='utf-8')
    for id in PACKAGES:AddonCatalog().verify(id)
    print('Six optional companions built, signed and verified. No device grants or network connections were enabled.')
if __name__=='__main__':main()
