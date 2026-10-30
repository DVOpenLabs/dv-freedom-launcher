"""Local publication and PR facts. No AI, token, network, Git writes or automatic review decisions."""
import argparse
import json
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET

PRIVATE_NAMES={'AGENTS.md','CLAUDE.md','GEMINI.md'}
ARTIFACTS={'.apk','.aab','.exe','.msi','.p12','.pfx','.jks','.keystore','.key','.pem','.ipk','.wgt'}
def private_path(path):
    parts=Path(path).parts;name=parts[-1] if parts else ''
    note=Path(path).suffix.lower() in {'.md','.txt','.json','.html'} and re.search(r'(?:^|[-_])(ROADMAP|VALIDATION|AUDIT|REVIEW)(?:[-_.]|$)',name,re.I)
    return bool(set(parts)&{'outputs','work','private','.codex','.agents','.venv','__pycache__','.gradle','build','dist'}) or name in PRIVATE_NAMES or bool(note)
def permission_facts(data):
    if not data:return set(),set()
    root=ET.fromstring(data);ns='{http://schemas.android.com/apk/res/android}'
    permissions={e.get(ns+'name') for e in root if e.tag in {'uses-permission','uses-permission-sdk-23'} and e.get(ns+'name')}
    services=set()
    for e in root.iter('service'):
        binding=e.get(ns+'permission','')
        if binding in {'android.permission.BIND_ACCESSIBILITY_SERVICE','android.permission.BIND_NOTIFICATION_LISTENER_SERVICE'}:services.add((e.get(ns+'name',''),binding))
    return permissions,services
def assess(path,before,after):
    findings=[]
    def add(level,kind,message):findings.append(dict(level=level,kind=kind,path=path,message=message))
    if after is None:return findings
    if private_path(path):add('block','private-path','Private working/generated material does not belong in publication.')
    if Path(path).suffix.lower() in ARTIFACTS:add('block','artifact','Build artifacts, identities and private keys do not belong in source.')
    try:text=after.decode('utf-8')
    except UnicodeDecodeError:
        if Path(path).suffix.lower() in {'.py','.java','.kt','.js','.json','.xml','.md','.gradle','.css','.html','.txt','.yml','.yaml'}:add('block','encoding','Source/document must be valid UTF-8.')
        return findings
    if re.search(r'-----BEGIN (?:RSA |EC |OPENSSH |ENCRYPTED )?PRIVATE KEY-----\s+[A-Za-z0-9+/=\r\n]{60,}-----END ',text):add('block','private-key','Private-key material found; do not publish it.')
    if re.search(r'''(?i)(?:[A-Z]:[\\/]+Users[\\/]+(?!Public[\\/]|Example[\\/]|<)[^\\/\s"']+[\\/]|/(?:Us[e]rs|ho[m]e)/[^/\s]+/)''',text):add('block','personal-path','Absolute home-directory path found. Use an environment variable or portable placeholder.')
    if re.search(r'\bghp_[A-Za-z0-9]{36}\b|\bAIza[A-Za-z0-9_-]{35}\b',text):add('block','token','Possible provider token found. Keep credentials out of source.')
    if path.endswith('AndroidManifest.xml'):
        try:
            old_permissions,old_services=permission_facts(before);permissions,services=permission_facts(after)
            for p in sorted(permissions-old_permissions):add('review','permission',p+' is new. Review purpose, defaults, disclosure and consent before device access.')
            for name,binding in sorted(services-old_services):add('review','special-service',name+' declares '+binding+'. Verify explicit opt-in and least data access.')
            if path.startswith('launcher/app/') and 'android.permission.INTERNET' in permissions:add('block','core-network','The launcher must stay offline; put networking in a separate optional app.')
        except ET.ParseError:add('block','manifest','Manifest XML cannot be parsed.')
    try:old=before.decode('utf-8') if before is not None else ''
    except UnicodeDecodeError:old=''
    for pattern,label in [(r'pm\s+(?:uninstall|disable-user|hide)\b','package-removal'),(r'pm\s+grant\b|enabled_accessibility_services|set-home-activity','device-access'),(r'pull_request_target|permissions:\s*write-all','privileged-workflow')]:
        new_lines=set(line for line in text.splitlines() if re.search(pattern,line))-set(line for line in old.splitlines() if re.search(pattern,line))
        if new_lines:add('review',label,'Changed privileged-operation path; inspect authorization, device binding, recovery and protected inputs.')
    if Path(path).suffix.lower() in {'.png','.jpg','.jpeg','.webp','.gif','.mp4'}:add('review','media','Check source rights and all visible account/location/device details before publication.')
    if path.endswith(('.yml','.yaml')) and '.github/workflows/' in path:add('review','workflow','Workflow changes need separate review/activation; do not run PR code with privileged tokens.')
    return findings

def git(root,*args,missing=False):
    r=subprocess.run(['git','--no-pager',*args],cwd=root,stdout=subprocess.PIPE,stderr=subprocess.PIPE,check=False)
    if r.returncode and not missing:raise RuntimeError('Could not inspect Git state; nothing was changed.')
    return None if r.returncode else r.stdout
def review(root,base='HEAD',working=False):
    # These references are arguments, never shell text. Reject options/path syntax.
    if not re.fullmatch(r'[A-Za-z0-9_./~-]+',base) or base.startswith('-') or ':' in base:raise ValueError('Use a local Git revision.')
    git(root,'rev-parse','--verify',base+'^{commit}')
    args=['diff','--name-only','-z','--no-ext-diff']+([] if working else ['--cached'])+[base,'--']
    paths=git(root,*args).decode('utf-8').split('\0');findings=[]
    for path in filter(None,paths):
        before=git(root,'show',base+':'+path,missing=True)
        if working:
            file=root/path;after=file.read_bytes() if file.is_file() else None
        else:after=git(root,'show',':'+path,missing=True)
        findings.extend(assess(path,before,after))
    return dict(schema=1,mode='working-tree' if working else 'staged',base=base,changedFiles=len(list(filter(None,paths))),findings=findings,blocked=any(x['level']=='block' for x in findings),requiresReview=any(x['level']=='review' for x in findings),note='Deterministic facts only. No upload, Git writes, CI activation, AI calls or merge decisions. Empty findings do not certify safety.')
def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--base',default='HEAD');p.add_argument('--working-tree',action='store_true');p.add_argument('--strict',action='store_true',help='Also fail when human review facts remain.');args=p.parse_args()
    root=Path(__file__).resolve().parents[1];result=review(root,args.base,args.working_tree);print(json.dumps(result,ensure_ascii=True,indent=2));return 1 if result['blocked'] or args.strict and result['requiresReview'] else 0
if __name__=='__main__':raise SystemExit(main())
