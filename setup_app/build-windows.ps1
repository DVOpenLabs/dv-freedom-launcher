# Run inside a developer environment with setup_app/requirements-build.txt installed.
# Produces a private-alpha Windows EXE with a separately signed offline Android APK.
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    if (-not (Test-Path -LiteralPath (Join-Path $projectRoot 'setup_app/release_bundle/catalog.json'))) { throw 'Prepare the signed offline launcher bundle with setup_app.build_release first.' }
    $pythonLicense = & python -c "import sys; from pathlib import Path; print(Path(sys.base_prefix) / 'LICENSE.txt')"
    if (-not (Test-Path -LiteralPath $pythonLicense)) { throw 'Python licence file is missing.' }
    python -m PyInstaller --noconfirm --clean --onefile --windowed --name DV-Freedom-Setup `
      --distpath setup_app/dist --workpath setup_app/build --specpath setup_app/build `
      --paths . --hidden-import platforms.webos.prepare_launcher --hidden-import platforms.tizen.prepare_launcher --hidden-import platforms.hosted.prepare_launcher --hidden-import platforms.roku.prepare_channel --collect-all adb_shell --collect-all rsa --collect-all cryptography --collect-all paho --collect-all qrcode --collect-all colorama `
      --copy-metadata adb-shell --copy-metadata rsa --copy-metadata cryptography `
      --copy-metadata pyasn1 --copy-metadata cffi --copy-metadata pycparser --copy-metadata pyinstaller --copy-metadata altgraph `
      --copy-metadata paho-mqtt --copy-metadata qrcode --copy-metadata colorama `
      --add-data "${projectRoot}/platforms/webos/launcher:platforms/webos/launcher" `
      --add-data "${projectRoot}/platforms/tizen/launcher:platforms/tizen/launcher" `
      --add-data "${projectRoot}/platforms/shared-web:platforms/shared-web" `
      --add-data "${projectRoot}/platforms/hosted/bridge.js:platforms/hosted" `
      --add-data "${projectRoot}/platforms/hosted/core.js:platforms/hosted" `
      --add-data "${projectRoot}/platforms/roku/channel:platforms/roku/channel" `
      --add-data "${projectRoot}/setup_app/web:setup_app/web" `
      --add-data "${projectRoot}/setup_app/trust.json:setup_app" `
      --add-data "${projectRoot}/setup_app/release_bundle/catalog.json:setup_app/release_bundle" `
      --add-data "${projectRoot}/setup_app/release_bundle/launcher.apk:setup_app/release_bundle" `
      --add-data "${projectRoot}/launcher/app/src/main/res/font/outfit.ttf:launcher/app/src/main/res/font" `
      --add-data "${projectRoot}/launcher/FONT-LICENSE-OFL.txt:launcher" `
      --add-data "${projectRoot}/LICENSE:." `
      --add-data "${projectRoot}/launcher/FONT-LICENSE-OFL.txt:licenses" `
      --add-data "${pythonLicense}:licenses/python" `
      --add-data "${projectRoot}/product/licenses/Paho-EDL-1.0.txt:licenses" `
      --add-data "${projectRoot}/product/licenses/Paho-NOTICE.txt:licenses" `
      --add-data "${projectRoot}/product/licenses/python-qrcode-LICENSE.txt:licenses" `
      --add-data "${projectRoot}/product/licenses/colorama-LICENSE.txt:licenses" `
      --add-data "${projectRoot}/LICENSE:licenses" setup_app/main.py
    if ($LASTEXITCODE -ne 0) { throw 'The setup build failed.' }
} finally { Pop-Location }
