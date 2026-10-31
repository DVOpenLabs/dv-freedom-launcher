#!/usr/bin/env python3
"""Acknowledged optional-provider operations. No success inferred from an ADB shell exit code."""
import argparse
import re
import secrets
import shlex
import subprocess
import sys
from pathlib import Path

def checked(value, pattern, label):
    if not isinstance(value, str) or '..' in value or not re.fullmatch(pattern, value):
        raise ValueError('Invalid ' + label)
    return value

def command(operation, uri, stage=None, fallback=None, text=None, marker=None):
    checked(uri, r'content://[A-Za-z][A-Za-z0-9_.]*/[A-Za-z0-9_.-]+(?:/[A-Za-z0-9_.-]+)?', 'provider path')
    marker = marker or 'DV_IO_' + secrets.token_hex(16)
    checked(marker, r'[A-Za-z0-9_]{1,80}', 'acknowledgement')
    q = shlex.quote
    if operation == 'delete':
        if stage is not None or fallback is not None or text is not None:
            raise ValueError('Delete does not accept write arguments')
        body = f'o=$(content delete --uri {q(uri)} 2>&1) && [ -z "$o" ]'
    elif operation == 'put':
        checked(stage, r'/data/local/tmp/[A-Za-z0-9_.-]+', 'staging path')
        if fallback is not None:
            checked(fallback, r'/sdcard/Android/data/[A-Za-z0-9_.]+/files/[A-Za-z0-9_./-]+', 'fallback path')
        write = f'o=$(content write --uri {q(uri)} < {q(stage)} 2>&1) && [ -z "$o" ]'
        if fallback is not None:
            # Old Android content tools lack write. Copy is a legacy fallback, not app-readability proof.
            write = f'rm -f {q(fallback)} 2>/dev/null; {{ {write}; }} || {{ mkdir -p {q(fallback.rsplit("/",1)[0])} && cp {q(stage)} {q(fallback)} 2>/dev/null; }}'
        body = '{ ' + write + '; }'
        if text is not None:
            checked(text, r'[A-Za-z0-9_-]{1,80}', 'revision')
            body = f"printf '%s' {q(text)} > {q(stage)} && {body}"
        body += f' && rm -f {q(stage)}'
    else:
        raise ValueError('Unsupported provider operation')
    return f"if {{ {body}; }}; then printf '%s\\n' {q(marker)}; else printf '%s\\n' 'Provider operation failed; staged data retained where available.' >&2; fi", marker

def acknowledged(output, marker):
    # tv.py --batch can echo the command; only a complete returned line counts.
    return marker in output.splitlines()

def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('operation', choices=['put', 'delete']); p.add_argument('--uri', required=True)
    p.add_argument('--stage'); p.add_argument('--fallback'); p.add_argument('--text')
    args = p.parse_args(argv)
    try:
        shell, marker = command(args.operation, args.uri, args.stage, args.fallback, args.text)
        result = subprocess.run([sys.executable, '-I', str(Path(__file__).with_name('tv.py')), shell],
                                capture_output=True, text=True, timeout=120)
        if result.returncode != 0 or not acknowledged(result.stdout, marker):
            print('The TV did not confirm the provider operation. No completion or restart was requested. '
                  'Keep the previous settings and inspect this operation before continuing.', file=sys.stderr)
            return 1
        return 0
    except (ValueError, OSError, subprocess.TimeoutExpired) as failure:
        print('Provider operation was not confirmed (' + type(failure).__name__ + ').', file=sys.stderr)
        return 2

if __name__ == '__main__':
    sys.exit(main())
