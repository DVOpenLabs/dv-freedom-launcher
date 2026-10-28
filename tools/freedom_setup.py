#!/usr/bin/env python3
"""Read a local app inventory or stage a guided/AI setup draft for on-TV review. Never applies it."""
import argparse,json,os,shlex,subprocess,sys
from pathlib import Path
import layout_editor as editor

SETTINGS={'experience','position','columns','text','textSize','names','strongFocus','idleSeconds'}
PROMPT='Ask how I want DV Freedom Launcher to look and which installed apps I use. Return only schema-1 setup JSON: name, settings, optional favorites. Settings allow experience (legacy/glass/clean/cinema), position (top/middle/bottom), columns (2–8), text (auto/light/dark), textSize (85/100/110/120), names (theme/all/selected/hidden), strongFocus (boolean), idleSeconds (0/30/60/120). Use exact app package IDs from an inventory I explicitly share; omit favorites otherwise. No commands, permissions, network settings, credentials, URLs or paid calls. The draft will be reviewed on the TV before applying.'
def validate(draft,inventory=None):
 if not isinstance(draft,dict) or set(draft)-{'schema','name','settings','favorites'} or type(draft.get('schema')) is not int or draft['schema']!=1:raise ValueError('Unsupported setup draft')
 name=draft.get('name','My Home')
 if not isinstance(name,str) or not name.strip() or len(name.encode('utf-16-le'))//2>40 or any(ord(c)<32 or 127<=ord(c)<160 for c in name):raise ValueError('Invalid profile name')
 values=draft.get('settings')
 if not isinstance(values,dict) or set(values)-SETTINGS:raise ValueError('Unsupported setup setting')
 enums={'experience':{'legacy','glass','clean','cinema'},'position':{'top','middle','bottom'},'text':{'auto','light','dark'},'textSize':{85,100,110,120},'names':{'theme','all','selected','hidden'},'idleSeconds':{0,30,60,120}}
 for key,value in values.items():
  if key in enums and (type(value) is not (int if key in {'textSize','idleSeconds'} else str) or value not in enums[key]):raise ValueError('Invalid '+key)
  if key=='columns' and (type(value) is not int or not 2<=value<=8):raise ValueError('Invalid columns')
  if key=='strongFocus' and type(value) is not bool:raise ValueError('Invalid focus option')
 if 'favorites' in draft:
  apps=draft['favorites']
  if not isinstance(apps,list) or not 1<=len(apps)<=32 or any(not isinstance(p,str) or not editor.PACKAGE.fullmatch(p) or len(p)>200 for p in apps) or len(set(apps))!=len(apps):raise ValueError('Invalid favorite apps')
  if inventory is not None and not set(apps)<={a['package'] for a in inventory['apps']}:raise ValueError('A favorite app is not in the current visible inventory')
 if not values and 'favorites' not in draft:raise ValueError('No setup changes')
 if len(editor.encode(draft).encode())>16384:raise ValueError('Setup draft too large')
 return draft
def call(host,package,method,arg=None,repo=editor.ROOT):
 if method not in {'inventory','stage'}:raise ValueError('Unsupported setup operation')
 bridge=editor.NativeBridge(host,package,repo);command='content call --uri '+shlex.quote('content://'+package+'.theme')+' --method product_setup_'+method
 if arg is not None:command+=' --arg '+shlex.quote(editor.encode(arg))
 env=os.environ.copy();env['TV_HOST']=host;env['PYTHONIOENCODING']='utf-8';candidate=Path(repo)/'tools/venv'/('Scripts/python.exe' if os.name=='nt' else 'bin/python');python=str(candidate) if candidate.is_file() else sys.executable
 result=subprocess.run([python,str(Path(repo)/'tools/tv.py'),command],capture_output=True,text=True,encoding='utf-8',errors='replace',env=env,timeout=35,creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0));index=result.stdout.find('json=')
 if result.returncode or index<0:raise ValueError('Could not stage/read setup. Keep this fork’s launcher visible, check authorized debugging, and read a fresh inventory.')
 return json.JSONDecoder(object_pairs_hook=editor.unique_object).raw_decode(result.stdout[index+5:])[0]
def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--prompt',action='store_true');p.add_argument('--tv');p.add_argument('--package',default='com.gathin.tvhome');p.add_argument('--inventory-out',type=Path);p.add_argument('--validate',type=Path);p.add_argument('--stage',type=Path);args=p.parse_args()
 if args.prompt:print(PROMPT);return
 if args.validate:validate(editor.loads(args.validate.read_text(encoding='utf-8-sig')));print('Valid setup draft; nothing was applied.');return
 if not args.tv:p.error('--tv is required for inventory or staging')
 inventory=call(args.tv,args.package,'inventory')
 if args.inventory_out:
  with args.inventory_out.open('x',encoding='utf-8') as out:json.dump(inventory,out,indent=2,ensure_ascii=False)
  print('Local inventory saved. Share it only with an assistant you choose.');return
 if args.stage:
  draft=validate(editor.loads(args.stage.read_text(encoding='utf-8-sig')),inventory);reply=call(args.tv,args.package,'stage',{'schema':1,'profile':inventory['profile'],'revision':inventory['revision'],'draft':draft});assert reply.get('reviewRequired') and reply.get('staged');print('Draft staged for on-TV review. It has NOT been applied. New-profile mode is the default.');return
 p.error('Choose --inventory-out or --stage')
if __name__=='__main__':
 try:main()
 except (ValueError,OSError,subprocess.SubprocessError) as error:print(str(error),file=sys.stderr);raise SystemExit(1)
