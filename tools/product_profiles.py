"""Validate portable local profiles and export existing platform keys. Never contacts a TV."""
import argparse
import json
from pathlib import Path
import re


def validate(document):
    def require(condition, message):
        if not condition:
            raise ValueError(message)

    require(isinstance(document, dict), 'Profile document must be an object')
    require(set(document) == {'schemaVersion', 'apps', 'profiles'}, 'Unknown or missing document fields')
    require(type(document['schemaVersion']) is int and document['schemaVersion'] == 1, 'Unsupported schemaVersion')
    apps, profiles = document['apps'], document['profiles']
    require(isinstance(apps, dict) and bool(apps), 'apps must be a non-empty catalog')
    for key, platforms in apps.items():
        require(isinstance(key, str) and re.fullmatch(r'[a-z][a-z0-9_-]{0,63}', key), 'Invalid logical app ID')
        require(isinstance(platforms, dict) and bool(platforms) and set(platforms) <= {'android', 'webos'}, 'Invalid platform mapping')
        for app in platforms.values():
            require(isinstance(app, str) and re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]{0,254}', app), 'Invalid platform app ID')
    require(isinstance(profiles, list) and 1 <= len(profiles) <= 16, 'Expected 1 to 16 profiles')
    ids = set()
    for profile in profiles:
        require(isinstance(profile, dict) and set(profile) == {'id', 'name', 'favorites', 'hiddenApps', 'columns'}, 'Unknown or missing profile fields')
        key = profile['id']
        require(isinstance(key, str) and re.fullmatch(r'[a-z][a-z0-9_-]{0,63}', key), 'Invalid profile ID')
        require(key not in ids, 'Duplicate profile ID')
        ids.add(key)
        require(isinstance(profile['name'], str) and 1 <= len(profile['name'].strip()) <= 64, 'Invalid profile name')
        require(type(profile['columns']) is int and 2 <= profile['columns'] <= 8, 'columns must be an integer from 2 to 8')
        for field in ('favorites', 'hiddenApps'):
            values = profile[field]
            require(isinstance(values, list) and len(values) <= 100, 'Invalid app list')
            require(all(isinstance(v, str) and v in apps for v in values), 'Unknown logical app in profile')
            require(len(set(values)) == len(values), 'Duplicate app in profile')
        require(bool(profile['favorites']), 'A profile needs at least one favorite')
        require(not set(profile['favorites']) & set(profile['hiddenApps']), 'A favorite cannot also be hidden')
    return document


def export_profile(document, profile_id, platform, base=None):
    validate(document)
    if platform not in ('android', 'webos'):
        raise ValueError('Unsupported platform')
    profile = next((p for p in document['profiles'] if p['id'] == profile_id), None)
    if profile is None:
        raise ValueError('Unknown profile')
    if base is not None and not isinstance(base, dict):
        raise ValueError('Base configuration must be an object')
    result = dict(base or {})
    def mapped(field):
        values = []
        for logical in profile[field]:
            app = document['apps'][logical].get(platform)
            if app is None:
                raise ValueError('No ' + platform + ' mapping for ' + logical)
            if app in values:
                raise ValueError('Multiple logical apps map to the same platform app ID')
            values.append(app)
        return values
    favorites, hidden = mapped('favorites'), mapped('hiddenApps')
    if set(favorites) & set(hidden):
        raise ValueError('Mapped favorite is also hidden')
    keys = ('dock_apps', 'hidden_apps') if platform == 'android' else ('dockApps', 'hiddenApps')
    result.update({keys[0]: favorites, keys[1]: hidden, 'columns': profile['columns']})
    return result


def read_json(path):
    return json.loads(Path(path).read_text(encoding='utf-8-sig'))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('document')
    parser.add_argument('--profile', required=True)
    parser.add_argument('--platform', choices=('android', 'webos'), required=True)
    parser.add_argument('--base')
    parser.add_argument('--output', required=True)
    args = parser.parse_args()
    try:
        result = export_profile(read_json(args.document), args.profile, args.platform,
                                read_json(args.base) if args.base else None)
        output = Path(args.output)
        output.parent.mkdir(parents=True, exist_ok=True)
        with output.open('x', encoding='utf-8') as stream:
            json.dump(result, stream, indent=2)
            stream.write('\n')
    except (ValueError, OSError) as error:
        parser.exit(2, str(error) + '\n')


if __name__ == '__main__':
    main()
