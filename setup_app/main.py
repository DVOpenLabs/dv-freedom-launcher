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
    assert all((WEB/name).is_file() for name in ['index.html','app.css','app.js','install.js','scene.svg'])
    from setup_app.release import Catalog
    Catalog().verify()
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
    http,_=serve(port=args.port);url=f'http://127.0.0.1:{http.server_port}/'
    if sys.stdout:print(url,flush=True)
    if not args.no_browser:webbrowser.open(url)
    try:http.serve_forever()
    except KeyboardInterrupt:pass
    finally:http.server_close()

if __name__=='__main__':main()
