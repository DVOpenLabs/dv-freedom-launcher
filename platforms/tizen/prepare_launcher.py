"""Stage a manual Samsung pilot and optional UNSIGNED WGT. Never connects, signs or installs."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import struct
import tempfile
import xml.etree.ElementTree as ET
import zipfile
import zlib

ROOT = Path(__file__).resolve().parents[2]
SOURCE = Path(__file__).with_name('launcher')
FILES = ('index.html', 'style.css', 'core.js', 'app.js', 'media.js', 'config.xml')


def icon():
    def pixel(x, y):
        ink = 20 <= x < 29 and 17 <= y < 64 or 20 <= x < 61 and 17 <= y < 26 or 20 <= x < 52 and 35 <= y < 44
        return bytes((199, 231, 255, 255) if ink else (17, 29, 48, 255))
    raw = b''.join(b'\0' + b''.join(pixel(x, y) for x in range(80)) for y in range(80))
    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)
    return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 80, 80, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')


def media_files(folder):
    if folder is None:
        return [], []
    folder = Path(folder)
    if folder.is_symlink() or not folder.is_dir():
        raise ValueError('Use a real local media directory, not a link.')
    entries, files, total = [], [], 0
    names = {}
    for p in folder.iterdir():
        if p.is_symlink() or not p.is_file() or not re.fullmatch(r'[a-z0-9_-]+\.(jpg|png|mp4)', p.name):
            raise ValueError('Media must be flat, unlinked lowercase JPG/PNG/MP4 files only.')
        limit = 100 * 1024 * 1024 if p.suffix == '.mp4' else 8 * 1024 * 1024
        if not 0 < p.stat().st_size <= limit:
            raise ValueError('A media file is empty or exceeds its size limit.')
        total += p.stat().st_size
        if total > 256 * 1024 * 1024:
            raise ValueError('Media exceeds the 256 MiB pilot limit.')
        names[p.name] = p
    for name, p in sorted(names.items()):
        if p.suffix == '.mp4':
            if p.stem + '.jpg' not in names and p.stem + '.png' not in names:
                raise ValueError('Every MP4 needs a matching JPG or PNG still fallback.')
            continue
        if any(e['still'].split('/')[-1].rsplit('.', 1)[0] == p.stem for e in entries):
            raise ValueError('Use one still per scene, not both JPG and PNG.')
        with p.open('rb') as stream:
            header = stream.read(32)
        if p.suffix == '.png':
            if len(header) < 24 or header[:8] != b'\x89PNG\r\n\x1a\n' or not all(0 < v <= 8192 for v in struct.unpack('>II', header[16:24])):
                raise ValueError('Invalid or oversized PNG still.')
        elif not header.startswith(b'\xff\xd8\xff'):
            raise ValueError('Invalid JPEG still.')
        entry = {'still': 'media/' + name}
        if p.stem + '.mp4' in names:
            video = names[p.stem + '.mp4']
            with video.open('rb') as stream:
                first = stream.read(16)
            if len(first) < 12 or first[4:8] != b'ftyp':
                raise ValueError('Invalid MP4 container. Codec playback still needs device testing.')
            entry['video'] = 'media/' + video.name
        entries.append(entry)
    if not 1 <= len(entries) <= 8:
        raise ValueError('Choose one to eight local wallpaper scenes.')
    return entries, list(names.values())


def validate_manifest(path):
    xml = ET.parse(path).getroot()
    ns = {'w': 'http://www.w3.org/ns/widgets', 't': 'http://tizen.org/ns/widgets'}
    app = xml.find('t:application', ns)
    if app is None or app.get('id') != 'DVFreedom1.Launcher' or app.get('package') != 'DVFreedom1':
        raise ValueError('Unexpected Samsung pilot identity.')
    privileges = {p.get('name') for p in xml.findall('t:privilege', ns)}
    if privileges != {'http://tizen.org/privilege/application.info', 'http://tizen.org/privilege/application.launch'}:
        raise ValueError('Unexpected Samsung pilot privileges.')
    if xml.findall('t:service', ns) or xml.findall('w:access', ns) or xml.findall('t:metadata', ns):
        raise ValueError('No service, network access or startup metadata is allowed in this pilot.')
    setting = xml.find('t:setting', ns)
    if setting is None or setting.get('background-support') != 'disable':
        raise ValueError('Background execution must stay disabled.')


def prepare(output, media=None, archive=None):
    output = Path(output).absolute()
    if output.exists() or output.is_symlink():
        raise ValueError('Choose a new staging directory; existing data is never overwritten.')
    archive = Path(archive).absolute() if archive else None
    if archive and (archive.exists() or archive.is_symlink() or archive.suffix.lower() != '.wgt'):
        raise ValueError('Choose a new .wgt output path.')
    if archive and output in archive.parents:
        raise ValueError('Keep the archive outside the staged app directory.')
    entries, files = media_files(media)
    output.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='.freedom-stage-', dir=output.parent) as temp:
        staged = Path(temp) / 'app'
        staged.mkdir()
        for name in FILES:
            shutil.copyfile(SOURCE / name, staged / name)
        for name, source in [('shared-core.js', ROOT / 'platforms/shared-web/core.js'), ('shared-media.js', ROOT / 'platforms/shared-web/media.js'), ('scene.svg', ROOT / 'setup_app/web/scene.svg')]:
            shutil.copyfile(source, staged / name)
        (staged / 'fonts').mkdir()
        shutil.copyfile(ROOT / 'launcher/app/src/main/res/font/outfit.ttf', staged / 'fonts/outfit.ttf')
        (staged / 'licenses').mkdir()
        shutil.copyfile(ROOT / 'launcher/FONT-LICENSE-OFL.txt', staged / 'licenses/Outfit-OFL.txt')
        shutil.copyfile(ROOT / 'LICENSE', staged / 'licenses/MIT.txt')
        (staged / 'licenses/DVOpenLabs.txt').write_text('DV Freedom Samsung pilot additions: Copyright (c) 2026 DVOpenLabs. MIT terms apply; see MIT.txt. No third-party Samsung launcher code or user media is bundled by default.\n', encoding='utf-8')
        html = (staged / 'index.html').read_text(encoding='utf-8').replace('../../shared-web/core.js', 'shared-core.js').replace('../../shared-web/media.js', 'shared-media.js')
        (staged / 'index.html').write_text(html, encoding='utf-8')
        css = (staged / 'style.css').read_text(encoding='utf-8').replace('../../../launcher/app/src/main/res/font/outfit.ttf', 'fonts/outfit.ttf').replace('../../../setup_app/web/scene.svg', 'scene.svg')
        (staged / 'style.css').write_text(css, encoding='utf-8')
        (staged / 'icon.png').write_bytes(icon())
        if files:
            (staged / 'media').mkdir()
            for p in files:
                shutil.copyfile(p, staged / 'media' / p.name)
        (staged / 'media.js').write_text('window.FREEDOM_MEDIA=' + json.dumps(entries, separators=(',', ':')) + ';\n', encoding='utf-8')
        validate_manifest(staged / 'config.xml')
        # Both paths are resolved beneath the explicitly selected staging parent. Rename only our new tree.
        if staged.resolve().parent.parent != output.parent.resolve() or output.exists():
            raise ValueError('Staging path changed; no output was replaced.')
        os.rename(staged, output)
    result = {'staged': str(output), 'manualLaunchOnly': True, 'signed': False, 'mediaScenes': len(entries), 'sourceBytes': sum(p.stat().st_size for p in output.rglob('*') if p.is_file())}
    if archive:
        archive.parent.mkdir(parents=True, exist_ok=True)
        with archive.open('xb') as target:
            with zipfile.ZipFile(target, 'w', zipfile.ZIP_DEFLATED) as z:
                for p in sorted(output.rglob('*')):
                    if p.is_file():
                        z.write(p, p.relative_to(output).as_posix())
        result.update(archive=str(archive), bytes=archive.stat().st_size, sha256=hashlib.sha256(archive.read_bytes()).hexdigest())
    return result


if __name__ == '__main__':
    cli = argparse.ArgumentParser(description=__doc__)
    cli.add_argument('output', type=Path)
    cli.add_argument('--media', type=Path)
    cli.add_argument('--archive', type=Path)
    args = cli.parse_args()
    try:
        print(json.dumps(prepare(args.output, args.media, args.archive), indent=2))
    except (OSError, ValueError, ET.ParseError) as error:
        cli.exit(1, str(error) + '\n')
