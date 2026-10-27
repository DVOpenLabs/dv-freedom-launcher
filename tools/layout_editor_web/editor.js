'use strict';
const $=id=>document.getElementById(id),token=document.querySelector('meta[name="editor-token"]').content;
let state=null,draft=null,review=null,busy=false,position=null;
const clone=value=>JSON.parse(JSON.stringify(value));
const icons={up:'M6 14l6-6 6 6',down:'M6 10l6 6 6-6',hide:'M6 6l12 12M18 6L6 18'};
function message(text,error=false){$('message').textContent=text;$('message').classList.toggle('error',error);}
async function api(path,value){
 const response=await fetch('/api/'+path,{method:value?'POST':'GET',headers:{'X-Editor-Token':token,...(value?{'Content-Type':'application/json'}:{})},...(value?{body:JSON.stringify(value)}:{})});
 const data=await response.json();if(!response.ok)throw new Error(data.error||'Could not complete this action');return data;
}
function request(){return {revision:state.revision,profile:state.profile,layout:clone(draft),position};}
function name(pkg){return draft.labels[pkg]||state.apps.find(app=>app.package===pkg)?.label||pkg;}
function dirty(){return !!state&&!!draft&&(JSON.stringify(draft)!==JSON.stringify(state.layout)||position!==(state.position??null));}
function changed(focusPackage=null){review=null;$('confirm').close();$('changes').replaceChildren();const li=document.createElement('li');li.textContent='Draft updated. Preview changes before applying.';$('changes').append(li);render();if(focusPackage){const row=[...$('favorites').children].find(row=>row.dataset.package===focusPackage);row?.querySelector('input')?.focus();}message('Draft only. Nothing has changed on the TV.');}
function iconButton(kind,label,action,disabled=false){
 const button=document.createElement('button');button.type='button';button.className='icon-button';button.setAttribute('aria-label',label);button.title=label;button.disabled=disabled;
 button.innerHTML='<svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.8"><path d="'+icons[kind]+'"/></svg>';button.onclick=action;return button;
}
function renderPreview(){
 const header=state.header||{},now=new Date(),effective=position||header.effectivePosition||'top';
 $('home-preview').dataset.position=effective;
 $('preview-greeting').textContent=header.available?header.greeting||'':'';
 $('preview-clock').textContent=header.available?header.clock||'':now.toLocaleTimeString([],{hour:'numeric',minute:'2-digit'});
 $('preview-date').textContent=header.available?header.date||'':now.toLocaleDateString([],{weekday:'long',month:'short',day:'numeric'});
 $('preview-weather').textContent=header.weather||'Weather unavailable';
 $('preview-inputs').hidden=header.inputs===false;$('preview-settings').textContent=header.settings||'Settings';$('preview-settings').hidden=header.settings==='';
 $('preview-note').textContent=(header.available?'Header captured from TV at '+new Date(header.capturedAt).toLocaleTimeString()+'. Reload to refresh weather.':'Home is not visible: clock uses PC time; weather is unavailable.')+(header.inputs===false?' Inputs are hidden by this launcher configuration.':'')+' Buttons are illustrations. App artwork and TV sizing may differ.';
 document.querySelectorAll('[data-position]').forEach(button=>{button.setAttribute('aria-pressed',String(button.dataset.position===effective));button.disabled=busy;});
 $('follow-position').disabled=busy||position===null;
 const preview=$('preview-apps');preview.replaceChildren();draft.favorites.forEach((pkg,index)=>{const tile=document.createElement('div');tile.className='preview-tile';const icon=document.createElement('div');icon.className='preview-icon tone-'+index%8;icon.textContent=name(pkg).replace(/[^\p{L}\p{N}]/gu,'').slice(0,3).toLocaleUpperCase();const label=document.createElement('div');label.className='preview-name';label.textContent=name(pkg);tile.append(icon,label);preview.append(tile);});}
function render(){
 $('reload').disabled=busy;$('import').disabled=busy||!state;if(!state)return;$('target').textContent=state.target;$('profile').textContent=state.profile?'Profile: '+state.profile:'Current Home';$('count').textContent=draft.favorites.length+' apps';
 const list=$('favorites');list.replaceChildren();
 draft.favorites.forEach((pkg,index)=>{
  const row=document.createElement('div');row.className='app-row';row.dataset.package=pkg;
  const position=document.createElement('span');position.className='number';position.textContent=String(index+1).padStart(2,'0');row.append(position);
  const info=document.createElement('div');info.className='app-info';const input=document.createElement('input');input.value=name(pkg);input.maxLength=60;input.setAttribute('aria-label','Name for '+(state.apps.find(a=>a.package===pkg)?.label||pkg));
  input.onchange=()=>{const label=input.value.trim();if(!label){input.value=name(pkg);return;}draft.labels[pkg]=label;review=null;renderPreview();$('preview').disabled=busy||!dirty();$('reset').disabled=busy||!dirty();message('Draft only. Nothing has changed on the TV.');};
  const packageName=document.createElement('div');packageName.className='package';packageName.textContent=pkg;packageName.title=pkg;info.append(input,packageName);row.append(info);
  row.append(iconButton('up','Move '+name(pkg)+' earlier',()=>{[draft.favorites[index-1],draft.favorites[index]]=[pkg,draft.favorites[index-1]];changed(pkg);},index===0||busy));
  row.append(iconButton('down','Move '+name(pkg)+' later',()=>{[draft.favorites[index],draft.favorites[index+1]]=[draft.favorites[index+1],pkg];changed(pkg);},index===draft.favorites.length-1||busy));
  row.append(iconButton('hide','Hide '+name(pkg),()=>{draft.favorites.splice(index,1);if(!draft.hidden.includes(pkg))draft.hidden.push(pkg);changed();},draft.favorites.length===1||busy));list.append(row);
 });
 const available=$('available');available.replaceChildren();const query=$('search').value.toLocaleLowerCase();
 const apps=state.apps.filter(app=>!draft.favorites.includes(app.package)&&(app.label+' '+app.package).toLocaleLowerCase().includes(query));
 apps.forEach(app=>{const row=document.createElement('div');row.className='available-row';const label=document.createElement('span');label.textContent=name(app.package);if(draft.hidden.includes(app.package)){const hidden=document.createElement('small');hidden.textContent='Hidden from Home';label.append(hidden);}const add=document.createElement('button');add.textContent='Add';add.setAttribute('aria-label','Add '+app.label+' to favorites');add.disabled=busy||draft.favorites.length>=32;add.onclick=()=>{draft.favorites.push(app.package);draft.hidden=draft.hidden.filter(pkg=>pkg!==app.package);changed();};row.append(label,add);available.append(row);});
 if(!apps.length){const empty=document.createElement('p');empty.className='empty';empty.textContent=query?'No apps match your search.':'All installed apps are already in your favorites.';available.append(empty);}
 renderPreview();
 $('preview').disabled=busy||!dirty();$('reset').disabled=busy||!dirty();$('undo').disabled=busy||!state.canUndo;$('export').disabled=busy;$('reload').disabled=busy;
}
async function action(work){if(busy)return;busy=true;render();try{await work();}catch(error){message(error.message,true);}finally{busy=false;render();}}
async function load(){await action(async()=>{state=await api('state');draft=clone(state.layout);position=state.position??null;review=null;$('changes').replaceChildren();const li=document.createElement('li');li.textContent='Make a change, then preview it here.';$('changes').append(li);message('Current layout loaded. Changes stay in this draft until you apply.');});}
$('reload').onclick=()=>{if(dirty()&&!confirm('Discard this unsaved draft and reload the TV?'))return;load();};
$('reset').onclick=()=>{draft=clone(state.layout);position=state.position??null;changed();};
$('positions').onclick=event=>{const button=event.target.closest('[data-position]');if(!button||busy)return;position=button.dataset.position;changed();};
$('follow-position').onclick=()=>{position=null;changed();};$('search').oninput=render;
$('preview').onclick=()=>action(async()=>{review=await api('preview',request());$('changes').replaceChildren();$('confirm-changes').replaceChildren();review.changes.forEach(text=>{for(const id of ['changes','confirm-changes']){const li=document.createElement('li');li.textContent=text;$(id).append(li);}});if(review.canApply){$('confirm-target').textContent='Target: '+state.target+(state.profile?' · '+state.profile:' · Current Home');$('confirm').showModal();$('cancel').focus();}else message('No changes to apply.');});
$('cancel').onclick=()=>$('confirm').close();
$('apply').onclick=()=>action(async()=>{$('confirm').close();const data=await api('apply',request());state=data.state;draft=clone(state.layout);position=state.position??null;review=null;message('Home layout saved. A backup was created; Undo is available.');$('changes').replaceChildren();});
$('undo').onclick=()=>action(async()=>{if(dirty()&&!confirm('Discard your draft and undo the last TV change?'))return;const data=await api('undo',request());state=data.state;draft=clone(state.layout);position=state.position??null;review=null;message('Previous Home layout restored. Wallpaper and other settings were preserved.');$('changes').replaceChildren();});
$('export').onclick=()=>{const url=URL.createObjectURL(new Blob([JSON.stringify(draft,null,2)],{type:'application/json'}));const a=document.createElement('a');a.href=url;a.download='cleanyourtv-layout.json';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);};
$('import').onclick=()=>{if(dirty()&&!confirm('Replace this unsaved draft with an imported layout?'))return;$('draft-file').click();};
$('draft-file').onchange=()=>action(async()=>{const file=$('draft-file').files[0];if(!file)return;if(file.size>65536)throw new Error('Draft is too large.');const response=await fetch('/api/import',{method:'POST',headers:{'X-Editor-Token':token,'Content-Type':'application/json'},body:await file.text()});const data=await response.json();if(!response.ok)throw new Error(data.error);draft=data.layout;review=null;$('draft-file').value='';message('Draft imported. Preview and review it before applying to this TV.');});
if(document.querySelector('meta[name="has-wallpaper"]').content==='true'){$('wallpaper').src='/wallpaper';$('wallpaper').hidden=false;}
load();
