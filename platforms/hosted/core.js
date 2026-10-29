/* DV Freedom hosted policies. Platform services and secrets live only in the paired companion. */
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory(require('../shared-web/core.js'));else root.FreedomHosted=factory(root.FreedomShared);}(this,function(Shared){
 'use strict';var C=Shared.create({label:'__LABEL__',key:'dv-freedom.__PLATFORM__.v1',extended:true,favoriteLimit:8});
 function check(value,message){if(!value)throw new Error(message);}
 C.plan=function(current,settings,favorites,available){
  C.validate(current);var next=C.copy(current),s=C.settings(settings),seen=Object.create(null);check(Array.isArray(favorites)&&favorites.length<=8,'Choose up to eight favorite apps');
  var known=C.catalog(available.map(function(a){return {id:a.id,label:a.label};})),p=next.profiles.filter(function(x){return x.id===next.active;})[0];
  favorites.forEach(function(id){check(C.id(id)&&!seen[id],'Invalid or repeated favorite');seen[id]=true;var entry=known.filter(function(a){return a.id===id;})[0]||next.apps.filter(function(a){return a.id===id;})[0];check(entry,'App is no longer in the catalog');if(!next.apps.some(function(a){return a.id===id;}))next.apps.push({id:entry.id,label:entry.label});});
  p.settings=s;p.favorites=favorites.slice();C.validate(next);return next;
 };
 C.ink=function(s){C.settings(s);return s.text==='dark'?'dark':s.text==='light'?'light':s.wallpaper==='coast'&&s.position==='top'?'dark':'light';};
 C.profileBackup=function(state){return JSON.stringify({format:'dv-freedom-__PLATFORM__',schema:1,state:C.validate(C.copy(state))},null,2);};
 C.shared=function(){throw new Error('Portable Android/LG imports are not supported in this hosted pilot.');};
 return C;
}));
