const assert=require('node:assert/strict');
const Shared=require('./core.js');
for(const options of [{appearance:true,label:'LG'},{extended:true,label:'Samsung'}]){
 const C=Shared.create(options),state=C.initial();state.apps=[{id:'one.app',label:'One'},{id:'two.app',label:'Two'}];state.profiles[0].favorites=['one.app'];
 const before=JSON.stringify(state);assert.equal(C.visibleApps(state,state.profiles[0]).length,2);assert.equal(JSON.stringify(state),before);
 state.profiles[0].settings.familyHome=true;assert.deepEqual(C.visibleApps(state,state.profiles[0]).map(a=>a.id),['one.app']);assert.equal(state.apps.length,2);
 for(const mediaFit of ['fill','fit','matte'])assert.equal(C.settings({mediaFit}).mediaFit,mediaFit);
 assert.throws(()=>C.settings({familyHome:'true'}));assert.throws(()=>C.settings({mediaFit:'remote-url'}));assert.equal(C.validate(state),state);
 const memory={getItem(){return null;},setItem(){throw new Error('Unexpected write');}};assert.equal(new C.Store(memory).read().profiles[0].settings.familyHome,undefined);
}
console.log('Shared native family-view and local picture-framing policies passed. No TV contacted.');
