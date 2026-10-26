// Execute the real pinned controller finish function with mocked OS/Luna adapters. No TV operations.
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict'),path=require('node:path');
const repo=path.resolve(process.argv[2]||'');
const source=fs.readFileSync(path.join(repo,'native/app/native.js'),'utf8');
const start=source.indexOf('function finish(code,signal)'),end=source.indexOf('function start()',start);
assert(start>=0&&end>start);
const relay=fs.readFileSync(path.join(repo,'native/prototype/relay.c'),'utf8');
assert(relay.includes('if(a==RELEASE||a==EXIT_HOME){reason=filter_exit_code(a);'));
function run(code, pending){
 const launched=[],writes=[],cleanups=[],exits=[];let time=0;
 const context={stopping:false,screenWatch:null,timer:1,log(){},clearInterval(){},ready:true,launching:pending,
  root:'/helper',data:'/state',pidPath:'/state/pid',monotonic:()=>time,
  policy:require(path.join(repo,'native/app/runtime-policy')),
  fs:{writeFileSync:p=>writes.push(p),unlinkSync(){}},process:{exit:n=>exits.push(n)},
  cp:{spawn(file,args){cleanups.push(args);return{on(event,fn){if(event==='exit')fn(0);}};}},
  launch(id,callback){launched.push(id);callback(true);},setTimeout(fn,delay){time+=delay;fn();}};
 vm.runInNewContext(source.slice(start,end)+'\nfinish('+code+',null);finish('+code+',null);',context);
 assert(writes.includes('/state/blocked'));assert.equal(cleanups.length,1);assert.equal(exits.length,1);
 return{launched,exits,time};
}
for(const pending of [false,true]){
 const recovery=run(22,pending);assert.deepEqual(recovery.launched,['com.webos.app.home']);assert.deepEqual(recovery.exits,[0]);
 if(pending)assert(recovery.time>=3200&&recovery.time<4000);
 const overflow=run(27,pending);assert.equal(overflow.launched.length,0);assert.deepEqual(overflow.exits,[27]);
}
console.log('PASS: intentional recovery opens LG Home after cleanup; overflow only releases; pending launch bounded; finish idempotent.');
