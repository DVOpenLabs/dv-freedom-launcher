"""Bounded cinema drafts over the existing authorized connection; TV review remains mandatory."""
import json
import re
import shlex
from urllib.parse import urlsplit
from .core import SetupError

KEYS={'schema','logos','glow','mascot','autoOpen','sceneBackground','scene','sceneArt','collectionName','collectionStyle','iconStyle','appLogos','taglines','moods','titles','worlds','worldsEnabled'}
def validate(value):
    if not isinstance(value,dict) or set(value)-KEYS or type(value.get('schema')) is not int or value['schema']!=1:raise SetupError('Unsupported cinema setup.')
    if len(json.dumps(value,ensure_ascii=True))>60000:raise SetupError('Keep this setup below 60 KB.')
    def text(v,n):
        if not isinstance(v,str) or not v.strip() or len(v)>n or any(ord(c)<32 or 127<=ord(c)<=159 for c in v):raise SetupError('Check your cinema text fields.')
    def package(v):return isinstance(v,str) and bool(re.fullmatch(r'[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z][A-Za-z0-9_]*)+',v))
    def asset(v):
        if not isinstance(v,str) or v and not re.fullmatch('[a-f0-9]{64}',v):raise SetupError('Artwork must reference a local imported file.')
    for key in ('logos','glow','mascot','autoOpen','sceneBackground','worldsEnabled'):
        if key in value and type(value[key]) is not bool:raise SetupError('Use on/off cinema choices.')
    if value.get('scene','off') not in ('off','aurora','fireflies','mist') or value.get('collectionStyle','posters') not in ('posters','books','characters'):raise SetupError('Choose an available scene and collection style.')
    asset(value.get('sceneArt',''));text(value.get('collectionName','My cinema'),48)
    if value.get('iconStyle','original') not in ('original','gold','silver','ember'):raise SetupError('Choose an available icon treatment.')
    moods=value.get('moods',[])
    if not isinstance(moods,list) or len(moods)>12 or any(not isinstance(m,str) for m in moods):raise SetupError('Use up to 12 mood names.')
    for m in moods:text(m,32)
    if len({m.lower() for m in moods})!=len(moods) or any(m.lower()=='anything' for m in moods):raise SetupError('Give moods unique names. Anything is automatic.')
    for key in ('appLogos','taglines'):
        items=value.get(key,{})
        if not isinstance(items,dict) or len(items)>64:raise SetupError('Use up to 64 app customizations.')
        for pkg,v in items.items():
            if not package(pkg):raise SetupError('Choose an installed app package.')
            asset(v) if key=='appLogos' else text(v,100)
    worlds=value.get('worlds',[]);world_ids=set();wallpapers=set()
    if not isinstance(worlds,list) or len(worlds)>8:raise SetupError('Use up to eight cinematic worlds.')
    for world in worlds:
        if not isinstance(world,dict) or set(world)-{'id','wallpaper','libraryArt','galleryArt','idleArt','hopArt','covers'}:raise SetupError('Invalid world fields.')
        ident=world.get('id');wallpaper=world.get('wallpaper')
        if not isinstance(ident,str) or ident not in ('hogwarts','hobbit','rivendell','ayodhya','ironman','avengers','infinity','doomsday') or ident in world_ids:raise SetupError('Choose a unique cinematic world.')
        if not isinstance(wallpaper,str) or not re.fullmatch(r'p-[a-f0-9]{8}-(?:[a-f0-9]{4}-){3}[a-f0-9]{12}',wallpaper) or wallpaper in wallpapers:raise SetupError('Pair each world with a different imported wallpaper.')
        world_ids.add(ident);wallpapers.add(wallpaper)
        for role in ('libraryArt','galleryArt','idleArt','hopArt'):asset(world.get(role,''))
        covers=world.get('covers',[])
        if not isinstance(covers,list) or len(covers)>8:raise SetupError('Use up to eight local book covers.')
        for cover in covers:asset(cover)
    titles=value.get('titles',[]);seen=set()
    if not isinstance(titles,list) or len(titles)>48:raise SetupError('Use up to 48 titles.')
    for t in titles:
        if not isinstance(t,dict) or set(t)-{'id','name','package','url','poster','character','backdrop','accent','moods'}:raise SetupError('Invalid title fields.')
        ident=t.get('id','')
        if not isinstance(ident,str) or not re.fullmatch('[a-z0-9][a-z0-9-]{0,47}',ident) or ident in seen:raise SetupError('Titles need unique identifiers.')
        seen.add(ident);text(t.get('name'),80)
        if not package(t.get('package')):raise SetupError('Choose an app for each title.')
        link=t.get('url','')
        if not isinstance(link,str) or len(link)>2048:raise SetupError('Use a shorter HTTPS title link.')
        if link:
            try:
                parsed=urlsplit(link)
                if parsed.scheme!='https' or not parsed.hostname or parsed.username or parsed.password or parsed.port not in (None,443):raise ValueError()
            except ValueError:raise SetupError('Use an HTTPS title link without credentials.')
        for role in ('poster','character','backdrop'):asset(t.get(role,''))
        if not isinstance(t.get('accent','#75CFFF'),str) or not re.fullmatch('#[a-fA-F0-9]{6}',t.get('accent','#75CFFF')):raise SetupError('Use a #RRGGBB accent.')
        chosen=t.get('moods',[])
        if not isinstance(chosen,list) or any(not isinstance(m,str) for m in chosen) or len(chosen)>12 or len(set(chosen))!=len(chosen) or not set(chosen)<=set(moods):raise SetupError('Choose existing moods for each title.')
    return value

def call(session,method,value):
    if session.mode!='device' or session.connection is None:raise SetupError('Connect an Android TV first. The offline creator still works without a TV.')
    if method not in ('cinema_read','cinema_stage'):raise SetupError('Unsupported cinema operation.')
    if method=='cinema_read':
        if value!={}:raise SetupError('Unexpected read options.')
    else:
        if not isinstance(value,dict) or set(value)!={'schema','package','profile','revision','draft'} or type(value['schema']) is not int or value['schema']!=1:raise SetupError('Read this TV before sending a draft.')
        validate(value['draft'])
        for key in ('profile','revision'):
            if not isinstance(value[key],str) or not (0 if key=='profile' else 1)<=len(value[key])<=200 or any(ord(c)<32 for c in value[key]):raise SetupError('Read this TV again.')
    # A unique compatible product installation is required; this never migrates or updates its identity.
    device=session.connection
    installed=set(re.findall(r'^package:([\w.]+)\s*$',device.read('packages'),re.M))
    targets=installed & {'com.dvopenlabs.freedomlauncher','com.dvopenlabs.cytv.usabilitytest'}
    if len(targets)!=1:raise SetupError('Use the native Cinema editor: this TV has no unique compatible DV Freedom installation.')
    package=next(iter(targets))
    if method=='cinema_stage' and value.get('package')!=package:raise SetupError('The selected launcher changed. Read this TV again.')
    payload={k:v for k,v in value.items() if k!='package'}
    command='content call --uri content://'+package+'.theme --method '+method
    if method=='cinema_stage':command+=' --arg '+shlex.quote(json.dumps(payload,ensure_ascii=True,separators=(',',':')))
    device=session.connection
    if device.paired:raw=device._adb(['-s',f'{device.host}:{device.port}','shell',command])
    else:raw=device.device.shell(command,transport_timeout_s=8,read_timeout_s=20)
    marker=raw.find('json=')
    if marker<0:raise SetupError('Update the compatible standalone DV Freedom launcher and unlock editing, then read the TV again.')
    try:result=json.JSONDecoder().raw_decode(raw[marker+5:])[0]
    except (TypeError,ValueError):raise SetupError('The TV returned an invalid cinema response.')
    if result.get('schema')!=1:raise SetupError('Unsupported cinema interface.')
    return result
