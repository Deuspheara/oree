#!/usr/bin/env python3
"""Create a local release key once; never print or overwrite credentials."""

import os
from pathlib import Path
import secrets
import shutil
import subprocess
import sys


def main() -> int:
    root = Path(__file__).resolve().parent.parent
    signing_dir = root / ".signing"
    keytool = shutil.which("keytool")
    if not keytool:
        print("keytool is required; install JDK 21 first.", file=sys.stderr)
        return 1
    os.umask(0o077)
    try:
        signing_dir.mkdir(mode=0o700)
    except FileExistsError:
        print(".signing already exists; refusing to replace signing material.", file=sys.stderr)
        return 1

    password = secrets.token_hex(32)
    properties = signing_dir / "release.properties"
    # Persist the password before key generation, so interruptions cannot orphan a key.
    properties.write_text(
        f"storePassword={password}\nkeyAlias=oree\nkeyPassword={password}\n",
        encoding="utf-8",
    )
    env = os.environ.copy()
    env["OREE_SIGNING_PASSWORD"] = password
    command = [
        keytool, "-genkeypair", "-keystore", str(signing_dir / "oree-release.p12"),
        "-storetype", "PKCS12", "-alias", "oree", "-keyalg", "RSA", "-keysize", "3072",
        "-validity", "10000", "-dname", "CN=Oree Release",
        "-storepass:env", "OREE_SIGNING_PASSWORD", "-keypass:env", "OREE_SIGNING_PASSWORD",
    ]
    try:
        subprocess.run(command, env=env, check=True, stdout=subprocess.DEVNULL)
    except (OSError, subprocess.CalledProcessError):
        print("Key generation failed. Inspect .signing before retrying; nothing was overwritten.", file=sys.stderr)
        return 1
    print("Created .signing/oree-release.p12 and .signing/release.properties with owner-only permissions.")
    print("Back up both files securely; losing them prevents signing future updates with this key.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
