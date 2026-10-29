/* Paired companion transport. No token in query strings, logs, persistent storage or launch payloads. */
(function(){'use strict';
 var C=window.FreedomHosted,secret=location.hash.slice(1),saved='';
 try{saved=sessionStorage.getItem('freedom-session');}catch(e){}
 if(/^[A-Za-z0-9_-]{43}$/.test(secret)){try{sessionStorage.setItem('freedom-session',secret);}catch(e){}history.replaceState(null,'',location.pathname);}else secret=saved||'';
 function request(path,data,done){var x=new XMLHttpRequest(),finished=false;x.open(data?'POST':'GET','/api/'+path,true);x.timeout=8000;x.setRequestHeader('X-Freedom-Session',secret);if(data)x.setRequestHeader('Content-Type','application/json');function finish(error,value){if(finished)return;finished=true;done(error,value);}x.onload=function(){try{var r=JSON.parse(x.responseText);if(x.status!==200)throw new Error(r.error||'The companion declined this request.');finish(null,r);}catch(e){finish(e);}};x.onerror=x.ontimeout=function(){finish(new Error('Companion unavailable. Keep the paired computer on, or return to the TV’s original Home.'));};x.send(data?JSON.stringify(data):null);return x;}
 function Adapter(){this.pending=null;this.serial=0;}
 Adapter.prototype.cancel=function(){this.serial++;if(this.pending)this.pending.abort();this.pending=null;};
 Adapter.prototype.inventory=function(done){this.cancel();var self=this,generation=this.serial;this.pending=request('catalog',null,function(e,v){if(generation!==self.serial)return;if(!e&&v.starter){window.FREEDOM_STARTER=v.starter;}done(e,v);});};
 Adapter.prototype.launch=function(id,reviewed,done){if(!reviewed.some(function(a){return a.id===id;})){done(new Error('Refresh the app catalog before launching.'));return;}this.cancel();var self=this,generation=this.serial;this.pending=request('launch',{id:id},function(e,v){if(generation===self.serial)done(e,v);});};
 Adapter.prototype.exit=function(){this.cancel();document.getElementById('notice').textContent='Press Home on the TV remote to return to its original Home.';};
 C.Adapter=Adapter;
 // Read the server-reviewed starter row into memory only for a new local Home. Settings Apply saves it.
 var read=C.Store.prototype.read;
 C.Store.prototype.read=function(){var value=read.call(this),raw=this.storage.getItem(this.key);if(raw===null&&window.FREEDOM_STARTER){var starter=window.FREEDOM_STARTER;value.apps=C.catalog(starter.apps);value.profiles[0].favorites=starter.apps.slice(0,8).map(function(a){return a.id;});value.profiles[0].settings=C.settings(starter.settings);C.validate(value);}return value;};
 // Bootstrap before app.js runs, without synchronous XHR or silently falling back to fake apps.
 window.FREEDOM_STARTER=window.FREEDOM_BOOTSTRAP;
}());
