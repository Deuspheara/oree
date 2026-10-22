#!/usr/bin/env python3
"""Export avatar masters, or verify the checked-in files with --check."""

import argparse
import hashlib
import json
from pathlib import Path
import shutil
import struct
import subprocess

ROOT = Path(__file__).resolve().parent.parent
MASTERS = ROOT / "assets/avatars/masters"
RUNTIME = ROOT / "core/designsystem/src/main/res/drawable-nodpi"
MANIFEST = MASTERS.parent / "manifest.json"


def validate_masters():
    sources = [MASTERS / f"avatar_{number:02}.png" for number in range(1, 9)]
    for source in sources:
        if not source.is_file():
            raise SystemExit(f"Missing master: {source}")
        header = source.read_bytes()[:24]
        if (
            len(header) != 24
            or header[:8] != b"\x89PNG\r\n\x1a\n"
            or struct.unpack(">II", header[16:24]) != (1024, 1024)
        ):
            raise SystemExit(f"Master must be a 1024 x 1024 PNG: {source}")
    return sources


def records(sources):
    result = []
    for source in sources:
        target = RUNTIME / f"{source.stem}.webp"
        if not target.is_file():
            raise SystemExit(f"Missing runtime image: {target}")
        result.append({
            "id": source.stem,
            "master": source.relative_to(ROOT).as_posix(),
            "masterSha256": hashlib.sha256(source.read_bytes()).hexdigest(),
            "runtime": target.relative_to(ROOT).as_posix(),
            "runtimeBytes": target.stat().st_size,
            "runtimeSha256": hashlib.sha256(target.read_bytes()).hexdigest(),
        })
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Verify hashes and paths without exporting")
    args = parser.parse_args()
    sources = validate_masters()
    if not args.check:
        encoder = shutil.which("cwebp")
        if encoder is None:
            raise SystemExit("Install libwebp's cwebp to export avatars, or use --check")
        RUNTIME.mkdir(parents=True, exist_ok=True)
        for source in sources:
            subprocess.run([
                encoder, "-quiet", "-q", "90", "-m", "6", "-resize", "256", "256",
                str(source), "-o", str(RUNTIME / f"{source.stem}.webp"),
            ], check=True)
    current = records(sources)
    if args.check:
        expected = json.loads(MANIFEST.read_text(encoding="utf-8"))
        if current != expected:
            raise SystemExit("Avatar manifest does not match the checked-in masters/runtime files")
        print(f"Verified {len(current)} avatar masters and runtime images")
    else:
        MANIFEST.write_text(json.dumps(current, indent=2) + "\n", encoding="utf-8")
        print(f"Exported {len(current)} avatars; runtime total {sum(item['runtimeBytes'] for item in current)} bytes")


if __name__ == "__main__":
    main()
