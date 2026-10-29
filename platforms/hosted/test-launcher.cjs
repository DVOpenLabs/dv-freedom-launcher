'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const C=require('./core.js');let count=0;function test(fn){fn();count++;}
const apps=[{id:'app_video',label:'Video'},{id:'app_music',label:'Music'}];
test(()=>{const s=C.initial();const n=C.plan(s,C.settings(s.profiles[0].settings),['app_video'],apps);assert.equal(s.apps.length,0);assert.deepEqual(n.profiles[0].favorites,['app_video']);});
test(()=>assert.throws(()=>C.plan(C.initial(),{},['unknown'],apps)));
test(()=>assert.throws(()=>C.plan(C.initial(),{},['app_video','app_video'],apps)));
test(()=>assert.throws(()=>C.settings({position:'left'})));
test(()=>assert.throws(()=>C.shared('{}')));
test(()=>{for(const position of ['top','middle','bottom'])for(const size of ['small','medium','large'])for(const spacing of ['compact','comfortable','wide']){const s=C.defaults?C.defaults():C.initial().profiles[0].settings;assert.doesNotThrow(()=>C.settings({...s,position,iconSize:size,spacing}));}});
const requests=[],session=new Map(),persistent=new Map(),secret='s'.repeat(43);let removed=false;
class XHR{constructor(){this.headers={};requests.push(this);}open(method,path){this.method=method;this.path=path;}setRequestHeader(k,v){this.headers[k]=v;}send(body){this.body=body;}abort(){this.aborted=true;}complete(value,status=200){this.status=status;this.responseText=JSON.stringify(value);this.onload();}}
const starter={apps,settings:C.initial().profiles[0].settings};
const ctx={window:{FreedomHosted:C,FREEDOM_BOOTSTRAP:starter},location:{hash:'#'+secret,pathname:'/'},history:{replaceState(){removed=true;}},sessionStorage:{getItem:k=>session.get(k)||null,setItem:(k,v)=>session.set(k,v)},XMLHttpRequest:XHR,document:{getElementById:()=>({textContent:''})},Error,JSON};
vm.runInNewContext(fs.readFileSync(__dirname+'/bridge.js','utf8'),ctx);
test(()=>{assert.equal(session.get('freedom-session'),secret);assert.equal(removed,true);assert.equal(persistent.size,0);});
const adapter=new C.Adapter();let observed=[];
test(()=>{adapter.inventory((e,v)=>observed.push(v));assert.equal(requests.at(-1).headers['X-Freedom-Session'],secret);assert.equal(requests.at(-1).path,'/api/catalog');});
const old=requests.at(-1);
test(()=>{adapter.inventory((e,v)=>observed.push(v));old.complete({apps:[]});assert.equal(old.aborted,true);assert.equal(observed.length,0);requests.at(-1).complete({apps,starter});assert.equal(observed.length,1);});
test(()=>{const before=requests.length;adapter.launch('unknown',apps,e=>assert(e));assert.equal(requests.length,before);});
test(()=>{adapter.launch('app_video',apps,(e,v)=>{assert.equal(e,null);assert.equal(v.foregroundVerified,false);});const r=requests.at(-1);assert.equal(r.path,'/api/launch');assert.deepEqual(JSON.parse(r.body),{id:'app_video'});r.complete({requested:true,foregroundVerified:false});});
test(()=>{const store=new C.Store({getItem:k=>persistent.get(k)||null,setItem:(k,v)=>persistent.set(k,v)}),s=store.read();assert.deepEqual(s.profiles[0].favorites,['app_video','app_music']);assert.equal(persistent.size,0);store.save(s,0);assert.equal(persistent.size,1);});
test(()=>{const before=requests.length;adapter.exit();assert.equal(requests.length,before);});
console.log(`${count} hosted policy, draft, callback, session-capability and exact-launch checks passed. No TV contacted.`);
