"""Small shared HRF utilities for local profile evidence/merging."""
import hashlib
from pathlib import Path


def read_profile(path):
    path = Path(path)
    if path.stat().st_size > 16 * 1024 * 1024:
        raise ValueError('Profile exceeds the tooling input limit')
    lines = [line.strip() for line in path.read_text().splitlines() if line.strip() and not line.strip().startswith('#')]
    if not lines:
        raise ValueError('Empty profile')
    rules = {}
    for line in lines:
        key = line.lstrip('HSP')
        flags = line[:len(line) - len(key)]
        if not key.startswith('L') or ';' not in key:
            raise ValueError('Unsupported HRF rule shape')
        rules.setdefault(key, set()).update(flags)
    return {'rawRules': len(lines), 'semanticRules': len(rules),
            'sha256': hashlib.sha256(path.read_bytes()).hexdigest()}, rules


def main_code_sha():
    modules = [Path('app'), *Path('core').glob('*'), *Path('feature').glob('*')]
    files = [p for module in modules for p in (module / 'src/main').rglob('*')
             if p.is_file() and 'generated' not in p.parts]
    files += [module / 'build.gradle.kts' for module in modules if (module / 'build.gradle.kts').is_file()]
    files += [p for p in Path('build-logic/src').rglob('*') if p.is_file()]
    files += [Path(name) for name in ('build.gradle.kts', 'settings.gradle.kts', 'gradle.properties',
              'gradle/libs.versions.toml', 'build-logic/build.gradle.kts', 'build-logic/settings.gradle.kts')]
    digest = hashlib.sha256()
    for path in sorted(files):
        digest.update(str(path).encode() + b'\0' + path.read_bytes() + b'\0')
    return digest.hexdigest()


def render_profile(rules):
    return ''.join(''.join(flag for flag in 'HSP' if flag in rules[key]) + key + '\n' for key in sorted(rules))
