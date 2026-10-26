# webOS helper recovery patch

This directory is the first reviewed webOS source integration layer. It is **not a complete webOS
launcher, installer or deployable helper package**. Nothing here contacts a TV. Root components are
excluded. Android and webOS remain separate build targets in the same product repository.

Upstream: [Razdva122/clean-your-tv-lg-webos](https://github.com/Razdva122/clean-your-tv-lg-webos), pinned at
`d5804f6a7ce5d4eb35c0093ca19ae011e5a6df7f`. Retain [the upstream licence](LICENSE.upstream)
and the notices extracted with its source. No upstream binaries or artwork are redistributed here.

The patch separates intentional triple-Home recovery (`EXIT_HOME`, exit 22) from input overflow
(`RELEASE`, exit 27). The existing controller already opens LG Home after cleanup for 22. Overflow
continues to release input without launching another Home. Existing enum values are preserved.

## Reproduce locally

First obtain the upstream Git source and ensure the pinned commit is available locally. Then run,
from this repository root, using a **new** output directory:

```sh
python platforms/webos/prepare_helper.py /path/to/upstream /path/to/new-source
node platforms/webos/test-home-recovery.cjs /path/to/new-source
```

The preparation tool reads only pinned blobs, creates an isolated Git directory, checks and applies
the patch. It does not fetch, build, package, install or execute the helper. Existing output is refused.
An unsuccessful preparation leaves its new directory for inspection; it never resets an existing checkout.

Compile the portable filter/startup tests locally with a C11 compiler, from the prepared source:

```sh
cc -std=c11 -Wall -Wextra native/prototype/filter.c native/prototype/test_filter.c -o /tmp/cytv-filter
/tmp/cytv-filter
cc -std=c11 -Wall -Wextra native/prototype/test_startup.c -o /tmp/cytv-startup
/tmp/cytv-startup
```

MSVC equivalents are `cl /std:c11 /W3 filter.c test_filter.c` and `cl /std:c11 /W3 test_startup.c`.
Write generated objects/executables outside the public source tree. The controller test needs a desktop
Node runtime; it runs the real `finish` function with fake OS/filesystem/Luna/cleanup adapters, including
pending-launch and repeated-exit cases. It does not start `native.js` as an application.

## Acceptance still required

The portable tests do not exercise Linux evdev/ioctl, the compiled relay on an LG, Node 0.12, real
UID/GID checks, held physical keys, standby/wake, Developer Mode expiry or the window manager. Review
and build the remaining no-root helper/package and platform launcher source before deployment. Inventory
both LG models/firmware first. Keep the recovery shortcut independent of profiles and themes.
