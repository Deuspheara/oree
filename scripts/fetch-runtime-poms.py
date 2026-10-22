#!/usr/bin/env python3
"""Cache versioned primary POM evidence for resolved runtime modules and license parents."""
import argparse
import concurrent.futures
import hashlib
import json
from pathlib import Path
import re
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET

MAX_POM = 2 * 1024 * 1024
NS = {'m': 'http://maven.apache.org/POM/4.0.0'}
REPOSITORIES = ['https://dl.google.com/dl/android/maven2/', 'https://repo.maven.apache.org/maven2/']

def coordinate(value):
    if not isinstance(value, str) or not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.+\-]*', value):
        raise ValueError('Unsafe Maven coordinate')
    return value

def pom_xml(payload):
    if len(payload) > MAX_POM or b'<!DOCTYPE' in payload.upper() or b'<!ENTITY' in payload.upper():
        raise ValueError('Oversized POM or unsupported XML declaration')
    return ET.fromstring(payload)

def run(inventory_path, cache, offline):
    if inventory_path.stat().st_size > 10 * 1024 * 1024:
        raise ValueError('Oversized inventory')
    inventory = json.loads(inventory_path.read_text())
    if not 0 < len(inventory['components']) <= 10_000:
        raise ValueError('Invalid component count')
    manifest = cache / 'pom-provenance.json'
    old = {}
    if manifest.exists():
        if manifest.stat().st_size > 10 * 1024 * 1024:
            raise ValueError('Oversized POM manifest')
        old = {r['component']: r for r in json.loads(manifest.read_text())}
    target = cache / 'poms'
    target.mkdir(parents=True, exist_ok=True)
    def fetch(c):
        group, name, version = [coordinate(c[k]) for k in ['group', 'name', 'version']]
        ref = f'pkg:maven/{group}/{name}@{version}'
        if ref != c['ref']:
            raise ValueError('Maven identity mismatch')
        path = target / (group + '__' + name + '__' + version + '.pom')
        suffix = f'{group.replace(".", "/")}/{name}/{version}/{name}-{version}.pom'
        repos = REPOSITORIES if group.startswith('androidx.') else list(reversed(REPOSITORIES))
        urls = [repo + suffix for repo in repos]
        prior = old.get(ref)
        if prior and path.exists() and path.stat().st_size <= MAX_POM:
            payload = path.read_bytes()
            if hashlib.sha256(payload).hexdigest() == prior.get('sha256') and prior.get('url') in urls:
                pom_xml(payload)
                return {**prior, 'file': str(path)}
        if offline:
            raise ValueError('No verified cached POM: ' + ref)
        failures = []
        for url in urls:
            try:
                with urllib.request.urlopen(url, timeout=30) as response:
                    final = urllib.parse.urlsplit(response.geturl())
                    if final.scheme != 'https' or final.hostname not in ['dl.google.com', 'repo.maven.apache.org']:
                        raise ValueError('Unexpected repository redirect')
                    payload = response.read(MAX_POM + 1)
                pom_xml(payload)
                temporary = path.with_suffix('.tmp')
                temporary.write_bytes(payload)
                temporary.replace(path)
                return {'component': ref, 'url': url, 'sha256': hashlib.sha256(payload).hexdigest(), 'file': str(path)}
            except Exception as error:
                failures.append(type(error).__name__ + ': ' + str(error))
        raise ValueError('Cannot fetch ' + ref + ': ' + '; '.join(failures))
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        selected = list(pool.map(fetch, inventory['components']))
    records = {r['component']: r for r in selected}
    def license_parent(ref, seen=()):
        if ref in seen or len(seen) > 8:
            raise ValueError('Cyclic/deep POM license inheritance')
        root = pom_xml(Path(records[ref]['file']).read_bytes())
        if root.findall('m:licenses/m:license', NS):
            return
        parent = root.find('m:parent', NS)
        if parent is None:
            raise ValueError('POM has no license or parent: ' + ref)
        group, name, version = [coordinate(parent.findtext('m:' + key, namespaces=NS))
                                for key in ['groupId', 'artifactId', 'version']]
        parent_ref = f'pkg:maven/{group}/{name}@{version}'
        if parent_ref not in records:
            records[parent_ref] = {**fetch({'group': group, 'name': name, 'version': version, 'ref': parent_ref}),
                                   'role': 'license-parent'}
        license_parent(parent_ref, (*seen, ref))
    for item in selected:
        license_parent(item['component'])
    data = [records[ref] for ref in sorted(records)]
    temporary = manifest.with_suffix('.tmp')
    temporary.write_text(json.dumps(data, indent=2) + '\n')
    temporary.replace(manifest)
    print('Verified', len(selected), 'runtime POMs;', len(data) - len(selected), 'additional license parents')

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('inventory', type=Path)
    parser.add_argument('cache', type=Path)
    parser.add_argument('--offline', action='store_true')
    args = parser.parse_args()
    run(args.inventory, args.cache, args.offline)
