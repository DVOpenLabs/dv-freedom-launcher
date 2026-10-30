'use strict';
(() => {
  const make=(tag,text,className)=>{const element=document.createElement(tag);if(text)element.textContent=text;if(className)element.className=className;return element;};
  const dialog=make('dialog',null,'phone-dialog'),heading=make('h2','Set up with your phone.'),close=make('button','Close','quiet');
  dialog.setAttribute('aria-labelledby','phone-title');heading.id='phone-title';
  const top=make('div',null,'dialog-top');top.append(heading,close);dialog.append(top);
  const content=make('div');dialog.append(content);document.body.append(dialog);close.onclick=()=>dialog.close();
  const button=make('button','Use your phone','help-link');document.querySelector('.sidebar-bottom').append(button);
  const banner=make('button','Phone sharing is on \u00b7 Manage','phone-sharing-banner');banner.hidden=true;document.querySelector('.workspace').prepend(banner);
  let timer=null,snapshot=null,qrUrl=null,signature='';
  const token=()=>document.querySelector('meta[name="setup-token"]').content;
  async function call(action,value){const response=await fetch('/api/phone-'+action,{method:value===undefined?'GET':'POST',headers:{'X-Setup-Token':token(),...(value===undefined?{}:{'Content-Type':'application/json'})},body:value===undefined?undefined:JSON.stringify(value)});const result=await response.json();if(!response.ok)throw new Error(result.error||'Phone setup is unavailable. Try again.');return result;}
  function error(message){let box=content.querySelector('[role=alert]');if(!box){box=make('p',null,'error');box.setAttribute('role','alert');content.prepend(box);}box.textContent=message;box.tabIndex=-1;box.focus();}
  async function refresh(force=false){try{snapshot=await call('info');banner.hidden=!snapshot.active;const next=JSON.stringify({...snapshot,seconds:0});if(dialog.open&&(force||next!==signature)){signature=next;draw();}if((snapshot.active||snapshot.transfer?.status==='running')&&!timer)timer=setInterval(()=>refresh(),2000);if(!snapshot.active&&snapshot.transfer?.status!=='running'&&timer){clearInterval(timer);timer=null;}}catch(err){if(dialog.open)error(err.message);}}
  async function open(){if(!dialog.open)dialog.showModal();await refresh(true);}
  button.onclick=banner.onclick=open;const mobile=make('button','Use phone','quiet phone-mobile');mobile.onclick=open;document.querySelector('.topbar').append(mobile);
  function action(host,label,handler,kind='secondary'){const node=make('button',label,kind);node.onclick=async()=>{node.disabled=true;try{await handler();}catch(err){error(err.message);}finally{node.disabled=false;}};host.append(node);return node;}
  function draw(){
    const focused=document.activeElement?.dataset.phoneAction;content.replaceChildren();
    if(qrUrl){URL.revokeObjectURL(qrUrl);qrUrl=null;}
    const native=make('section',null,'essential-row');native.append(make('h3','Android phone app · no computer during TV setup'),make('p','For Android 10+ phones and TVs with pairing-code Wireless debugging. Legacy ADB-only TVs and native LG/Samsung use desktop Setup. The phone APK contains its verified TV installer; Home and permissions stay separate choices.'));
    action(native,'Check native phone app',async()=>{const info=await call('app-info');native.replaceChildren(make('h3','Native Android phone Setup'),make('p',info.available?info.versionName+' · '+Math.ceil(info.bytes/1048576)+' MB · signed APK':info.message));if(info.available){action(native,'Download signed phone APK',async()=>{const r=await fetch('/api/phone-app-apk',{headers:{'X-Setup-Token':token()}});if(!r.ok)throw new Error('Phone APK verification failed. Nothing downloaded.');const blob=await r.blob(),url=URL.createObjectURL(blob),link=make('a');link.href=url;link.download='dv-freedom-phone-setup.apk';link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);});native.append(make('p','Transfer this APK to your Android phone, review its source and normal Android installation prompt, then follow the app. Setup never installs it automatically.'));}});content.append(native,make('h3','Or use a phone with this computer'));
    if(snapshot.active){
      content.append(make('p',snapshot.paired?'Phone paired. New drafts and files will appear below.':'Scan the QR code or open this address on your phone, then enter the code.'));
      const url=make('p',snapshot.url,'phone-address');content.append(url);
      if(!snapshot.paired){const code=make('p',snapshot.pin,'phone-pin');code.setAttribute('aria-label','Pairing code '+snapshot.pin.split('').join(' '));content.append(code);const qr=make('img');qr.alt='QR code for the phone setup address';qr.width=180;qr.height=180;content.append(qr);fetch('/api/phone-qr',{headers:{'X-Setup-Token':token()}}).then(response=>{if(!response.ok)throw new Error();return response.blob();}).then(blob=>{if(!qr.isConnected)return;qrUrl=URL.createObjectURL(blob);qr.src=qrUrl;}).catch(()=>{qr.remove();});}
      content.append(make('p','This session expires within 15 minutes. Closing this window keeps sharing on; use Stop sharing to disconnect immediately.','note'));
      action(content,'Stop sharing',async()=>{await call('stop',{});await refresh(true);}).dataset.phoneAction='stop';
    }else{
      content.append(make('p','Keep this computer open while your phone helps with setup. The phone can send a draft and media; installation and TV changes require a separate review.'));
      if(!state.info){action(content,'Try with an example TV',async()=>{const response=await api('preview',{});state.mode='preview';state.info=response.device;state.choices.apps=response.device.apps.slice(0,6).map(a=>a.package);invalidatePlan();render();await refresh(true);});}
      else{
        const label=make('label','Computer’s home-network address'),select=make('select');select.setAttribute('aria-label','Computer network address');
        for(const address of snapshot.addresses){const option=make('option',address);option.value=address;select.append(option);}label.append(select);content.append(label);
        const apps=make('label',null,'phone-consent'),share=make('input');share.type='checkbox';apps.append(share,document.createTextNode('Share the TV’s visible app names and IDs with this paired phone. Optional; off keeps app order on the computer.'));content.append(apps);
        const consent=make('label',null,'phone-consent'),check=make('input');check.type='checkbox';consent.append(check,document.createTextNode('Start temporary HTTP sharing on this trusted home network. Anyone on the network could reach the pairing page; avoid public/shared networks and port forwarding.'));content.append(consent);
        const start=action(content,'Start phone pairing',async()=>{if(!check.checked)throw new Error('Review and accept temporary sharing first.');await call('start',{address:select.value,shareApps:share.checked,choices:state.choices,consent:true});await refresh(true);});start.disabled=!snapshot.addresses.length;check.onchange=()=>{start.disabled=!check.checked||!snapshot.addresses.length;};start.disabled=true;
        if(!snapshot.addresses.length)content.append(make('p','No home-network interface was found. Connect this computer to your home network and reopen this view.','note'));
      }
    }
    if(snapshot.pending){const draft=make('section',null,'essential-row');draft.append(make('h3','A Home draft arrived.'),make('p','Review the choices on this computer. Receiving a draft has not changed the TV.'));action(draft,'Review received draft',async()=>{const response=await call('claim',{id:snapshot.pending.id});state.choices=response.choices;invalidatePlan();state.step=3;state.reached=Math.max(3,state.reached);render();dialog.close();await refresh();document.querySelector('#main').focus();}).dataset.phoneAction='claim';content.append(draft);}
    const files=make('section',null,'essential-row');files.append(make('h3','Private media inbox'),make('p','Files stay on this computer after sharing stops. TV import adds a library item only after review.'));
    if(!snapshot.files.length)files.append(make('p','No files received yet. Pair a phone and choose a picture or short MP4.','note'));
    for(const item of snapshot.files){const row=make('div',null,'phone-file');row.append(make('strong',item.name),make('small',`${item.kind.toUpperCase()} \u00b7 ${(item.bytes/1048576).toFixed(1)} MB`));const review=action(row,'Review on TV',async()=>{if(snapshot.active)throw new Error('Stop phone sharing before opening TV review.');await call('transfer',{id:item.id,consent:true});await refresh(true);});review.disabled=snapshot.active||snapshot.transfer?.status==='running'||state.mode!=='device';action(row,'Download file',async()=>{const response=await fetch('/api/phone-file/'+item.id,{headers:{'X-Setup-Token':token()}});if(!response.ok)throw new Error('The file is unavailable.');const url=URL.createObjectURL(await response.blob()),link=make('a');link.href=url;link.download='dv-wallpaper-'+item.id+'.'+item.kind;link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);},'quiet');action(row,'Remove from inbox',async()=>{if(!confirm('Remove this received file from this computer? It will not change a wallpaper already imported on a TV.'))return;await call('remove',{id:item.id});await refresh(true);},'quiet');files.append(row);}
    if(snapshot.transfer?.status!=='idle'){const progress=make('p',snapshot.transfer.message||'Media transfer in progress.');progress.setAttribute('role','status');files.append(progress);}content.append(files);
    if(focused)content.querySelector(`[data-phone-action="${focused}"]`)?.focus();
  }
  refresh();
})();
