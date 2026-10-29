# Run inside a developer environment with setup_app/requirements-build.txt installed.
# Produces a development preview executable; no signing or TV install is implied.
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    $pythonLicense = & python -c "import sys; from pathlib import Path; print(Path(sys.base_prefix) / 'LICENSE.txt')"
    if (-not (Test-Path -LiteralPath $pythonLicense)) { throw 'Python licence file is missing.' }
    python -m PyInstaller --noconfirm --clean --onefile --windowed --name DV-Freedom-Setup `
      --distpath setup_app/dist --workpath setup_app/build --specpath setup_app/build `
      --paths . --collect-all adb_shell --collect-all rsa --collect-all cryptography `
      --copy-metadata adb-shell --copy-metadata rsa --copy-metadata cryptography `
      --copy-metadata pyasn1 --copy-metadata cffi --copy-metadata pycparser --copy-metadata pyinstaller --copy-metadata altgraph `
      --add-data "${projectRoot}/setup_app/web:setup_app/web" `
      --add-data "${projectRoot}/launcher/app/src/main/res/font/outfit.ttf:launcher/app/src/main/res/font" `
      --add-data "${projectRoot}/launcher/FONT-LICENSE-OFL.txt:licenses" `
      --add-data "${pythonLicense}:licenses/python" `
      --add-data "${projectRoot}/LICENSE:licenses" setup_app/main.py
    if ($LASTEXITCODE -ne 0) { throw 'The setup build failed.' }
} finally { Pop-Location }
