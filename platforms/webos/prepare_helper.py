"""Extract a pinned source slice and apply our recovery fix. Never installs or contacts a TV."""
import argparse
import json
from pathlib import Path
import subprocess

PIN = "d5804f6a7ce5d4eb35c0093ca19ae011e5a6df7f"
FILES = (
    "native/prototype/filter.h", "native/prototype/filter.c", "native/prototype/relay.c",
    "native/prototype/test_filter.c", "native/prototype/startup.h", "native/prototype/test_startup.c",
    "native/app/lock.h", "native/app/native.js", "native/app/runtime-policy.js",
    "LICENSE", "THIRD_PARTY_NOTICES.md",
)

def prepare(upstream, output):
    upstream, output = Path(upstream).resolve(), Path(output).resolve()
    if output.exists():
        raise ValueError("Output must be a new directory; existing files are never overwritten")
    # Read every pinned blob before creating output. No fetch, floating ref, build or helper execution.
    blobs = {name: subprocess.check_output(["git", "-C", str(upstream), "show", f"{PIN}:{name}"])
             for name in FILES}
    output.mkdir(parents=True)
    for name, contents in blobs.items():
        target = output / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(contents)
    # Isolate git apply from any parent checkout. Failure leaves this new source folder for inspection.
    subprocess.run(["git", "init", "--quiet", str(output)], check=True)
    patch = Path(__file__).with_name("patches") / "0001-home-recovery-exit.patch"
    subprocess.run(["git", "-C", str(output), "apply", "--check", str(patch)], check=True)
    subprocess.run(["git", "-C", str(output), "apply", str(patch)], check=True)
    (output / "product-source.json").write_text(json.dumps({
        "repository": "https://github.com/Razdva122/clean-your-tv-lg-webos", "commit": PIN,
        "patch": patch.name, "scope": "reviewed recovery source slice; not a deployable helper package",
    }, indent=2) + "\n", encoding="utf-8")
    return output

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("upstream", help="Local upstream Git checkout containing the pinned commit")
    parser.add_argument("output", help="New local directory for patched source; never a TV path")
    args = parser.parse_args()
    print(prepare(args.upstream, args.output))
