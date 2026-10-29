"""One setup catalogue; installation, Home replacement and acceptance are separate facts."""
import copy

PLATFORMS = {
    'android': dict(name='Android TV / Google TV', kind='native', route='android',
        badge='Native Android alpha', needs='TV debugging approval', home='System Home chooser or separately enabled helper'),
    'streamer': dict(name='Google TV Streamer', kind='native', route='android',
        badge='Native Android alpha', needs='Box debugging approval', home='Separately enabled Home-only redirect'),
    'lg': dict(name='LG webOS', kind='native', route='native',
        badge='Native LG pilot', needs='Developer Mode and LG tools', home='Manual launch; optional Home-helper work is separate'),
    'tizen': dict(name='Samsung Tizen', kind='native', route='native',
        badge='Native Samsung pilot', needs='Tizen Studio and your Samsung certificate', home='Manual launch; automatic return is not included'),
    'vidaa': dict(name='Hisense VIDAA', kind='hosted', route='companion',
        badge='Experimental hosted Home', needs='Always-on computer, a working private MQTT profile and TV debug installer',
        home='Open the installed web tile; no automatic Home interception'),
    'vizio': dict(name='Vizio SmartCast', kind='hosted', route='companion',
        badge='Experimental hosted Home', needs='Always-on computer, TV pairing and a trusted TV certificate',
        home='Manual Conjure page launch where firmware allows it; no root or Home takeover'),
    'roku': dict(name='Roku / Roku TV', kind='ambient', route='companion',
        badge='Ambient app only', needs='Developer Mode for manual sideload; one development-app slot',
        home='Roku Home stays in control. This is not an app launcher or an ECP remote'),
    'other': dict(name='Other smart TV', kind='guide', route='guide',
        badge='Privacy guidance', needs='Your TV remote', home='Original Home unchanged')
}

def catalogue():
    rows = copy.deepcopy(PLATFORMS)
    summaries={'android':'Apps run directly on the TV','streamer':'Apps run on your external Google box',
        'lg':'LG tools and Developer Mode','tizen':'Samsung SDK and your certificate',
        'vidaa':'Paired profile + always-on computer','vizio':'TV pairing + always-on computer',
        'roku':'Clock and backgrounds; manual sideload','other':'Manual steps using your TV remote'}
    for key, item in rows.items():
        item['summary']=summaries[key]
        item['hardwareAccepted'] = False
        item['status'] = 'Development support; exact device acceptance required'
    return rows
