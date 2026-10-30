"""Run from source: python -m setup_app.main. Packaged version needs no Python installation."""
import argparse
import sys
import webbrowser
from setup_app.server import serve,WEB,FONT

def self_check():
    """Validate frozen assets and TCP signing libraries without any network/device access."""
    from tempfile import TemporaryDirectory
    from pathlib import Path
    from adb_shell.adb_device import AdbDeviceTcp
    from adb_shell.auth.keygen import keygen
    from adb_shell.auth.sign_pythonrsa import PythonRSASigner
    assert FONT.is_file()
    assert all((WEB/name).is_file() for name in ['index.html','app.css','app.js','install.js','native.js','companion.js','essentials.js','scene.svg','phone-desktop.js','phone.html','phone.css','phone.js'])
    from platforms.webos.prepare_launcher import SOURCE as lg_source
    from platforms.tizen.prepare_launcher import SOURCE as samsung_source
    assert (lg_source/'appinfo.json').is_file() and (samsung_source/'config.xml').is_file()
    from platforms.webos.prepare_launcher import prepare as stage_lg
    from platforms.tizen.prepare_launcher import prepare as stage_samsung
    with TemporaryDirectory() as folder:
        stage_lg(Path(folder)/'lg');stage_samsung(Path(folder)/'samsung')
        assert (Path(folder)/'lg/shared-core.js').is_file()
        assert (Path(folder)/'samsung/fonts/outfit.ttf').is_file()
        from platforms.hosted.prepare_launcher import prepare as stage_hosted
        from platforms.roku.prepare_channel import prepare as stage_roku
        stage_hosted(Path(folder)/'vidaa','vidaa');stage_hosted(Path(folder)/'vizio','vizio')
        assert stage_roku(Path(folder)/'roku.zip')['installed'] is False
        import paho.mqtt.client
    import qrcode
    from qrcode.image.svg import SvgPathImage
    assert b'<svg' in qrcode.make('http://127.0.0.1:1234',image_factory=SvgPathImage).to_string()
    from setup_app.release import Catalog
    Catalog().verify()
    from setup_app.addons import AddonCatalog,PACKAGES
    for id in PACKAGES:AddonCatalog().verify(id)
    with TemporaryDirectory() as folder:
        key=Path(folder)/'test-key';keygen(str(key))
        signer=PythonRSASigner(key.with_suffix('.pub').read_text(),key.read_text())
        assert signer.GetPublicKey()

def main():
    parser=argparse.ArgumentParser(description='DV Freedom Setup: preview your Home or run read-only TV checks.')
    parser.add_argument('--port',type=int,default=0);parser.add_argument('--no-browser',action='store_true')
    parser.add_argument('--self-check',action='store_true')
    args=parser.parse_args()
    if args.self_check:self_check();return
    http,session=serve(port=args.port);url=f'http://127.0.0.1:{http.server_port}/'
    if sys.stdout:print(url,flush=True)
    if not args.no_browser:webbrowser.open(url)
    try:http.serve_forever()
    except KeyboardInterrupt:pass
    finally:
        http.server_close()
        session.companion.disconnect()
        session.phone.stop()

if __name__=='__main__':main()
