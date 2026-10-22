#!/usr/bin/env python3
"""Build a deterministic CycloneDX1.6 runtime-input SBOM from resolved Gradle evidence.
No resolution,downloads or execution of dependency code. POM declarations are not a notice audit.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET

MAX_JSON_BYTES = 10 * 1024 * 1024
MAX_POM_BYTES = 2 * 1024 * 1024
NS = {'m': 'http://maven.apache.org/POM/4.0.0'}
LICENSE_IDS = {
    'The Apache Software License, Version 2.0': 'Apache-2.0',
    'The Apache License, Version 2.0': 'Apache-2.0',
    'Apache-2.0': 'Apache-2.0',
    'BSD-3-Clause': 'BSD-3-Clause',
}

def read_json(path):
    if path.stat().st_size > MAX_JSON_BYTES:
        raise ValueError('Oversized JSON evidence')
    return json.loads(path.read_text())

def coordinate(value):
    if not isinstance(value, str) or not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.+\-]*', value):
        raise ValueError('Unsafe Maven coordinate')
    return value

def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def prop(name, value):
    return {'name': 'tvlauncher:' + name, 'value': str(value)}

def generate(inventory_path, pom_manifest_path, output, source_hash):
    inventory = read_json(inventory_path)
    if inventory['schemaVersion'] != 2 or not inventory['dependencyEdgesExcludeConstraints']:
        raise ValueError('Require current inventory with dependency constraints excluded from edges')
    if not re.fullmatch('[0-9a-f]{64}', source_hash):
        raise ValueError('Invalid app source hash')
    components = inventory['components']
    if not 0 < len(components) <= 10_000:
        raise ValueError('Invalid component count')
    refs = {c['ref'] for c in components}
    if len(refs) != len(components):
        raise ValueError('Duplicate component identities')
    for c in components:
        expected = 'pkg:maven/{}/{}@{}'.format(*(coordinate(c[k]) for k in ['group', 'name', 'version']))
        if c['ref'] != expected or not set(c['dependencies']) <= refs:
            raise ValueError('Invalid component reference or unknown dependency')
    if not set(inventory['rootDependencies']) <= refs:
        raise ValueError('Unknown root dependency')
    artifacts = {}
    for a in inventory['artifacts']:
        if a['component'] not in refs or not re.fullmatch('[0-9a-f]{64}', a['sha256']):
            raise ValueError('Invalid artifact evidence')
        if a['component'] in artifacts:
            raise ValueError('Multiple artifacts per module need explicit variant modeling')
        if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.+\-]*', a['filename']) or a['bytes'] <= 0:
            raise ValueError('Invalid artifact identity')
        artifacts[a['component']] = a
    rows = read_json(pom_manifest_path)
    poms = {r['component']: r for r in rows}
    if len(rows) != len(poms):
        raise ValueError('Duplicate POM provenance')
    parsed = {}
    for ref, item in poms.items():
        path = Path(item['file'])
        if path.stat().st_size > MAX_POM_BYTES or sha256(path) != item['sha256']:
            raise ValueError('POM evidence size/checksum mismatch')
        if not item['url'].startswith(('https://dl.google.com/dl/android/maven2/',
                                       'https://repo.maven.apache.org/maven2/')):
            raise ValueError('Unexpected POM repository')
        payload = path.read_bytes()
        if b'<!DOCTYPE' in payload.upper() or b'<!ENTITY' in payload.upper():
            raise ValueError('Unsupported POM XML declaration')
        parsed[ref] = ET.fromstring(payload)
    def declarations(ref, seen=()):
        if ref in seen or len(seen) > 8 or ref not in parsed:
            raise ValueError('Unresolved/cyclic license inheritance: ' + ref)
        root = parsed[ref]
        licenses = root.findall('m:licenses/m:license', NS)
        if licenses:
            result = []
            for license_node in licenses:
                name = license_node.findtext('m:name', default='', namespaces=NS)
                if name not in LICENSE_IDS:
                    raise ValueError('Unmapped declared license: ' + name)
                result.append({'id': LICENSE_IDS[name], 'name': name,
                               'url': license_node.findtext('m:url', default='', namespaces=NS)})
            return result, ref
        parent = root.find('m:parent', NS)
        if parent is None:
            raise ValueError('No declared license: ' + ref)
        parts = [coordinate(parent.findtext('m:' + key, namespaces=NS))
                 for key in ['groupId', 'artifactId', 'version']]
        return declarations('pkg:maven/{}/{}@{}'.format(*parts), (*seen, ref))
    bom_components, evidence = [], []
    for c in sorted(components, key=lambda c: c['ref']):
        licenses, license_ref = declarations(c['ref'])
        artifact = artifacts.get(c['ref'])
        source = poms[c['ref']]
        license_source = poms[license_ref]
        entry = {'type': 'library', 'bom-ref': c['ref'], 'group': c['group'], 'name': c['name'],
                 'version': c['version'], 'purl': c['ref'],
                 'licenses': [{'license': {'id': l['id'], 'acknowledgement': 'declared', 'url': l['url']}}
                              for l in licenses],
                 'properties': [prop('inputKind', 'artifact' if artifact else 'metadata-only')],
                 'externalReferences': [{'type': 'build-meta', 'url': source['url'],
                     'hashes': [{'alg': 'SHA-256', 'content': source['sha256']}]}]}
        if artifact:
            entry['hashes'] = [{'alg': 'SHA-256', 'content': artifact['sha256']}]
            entry['properties'] += [prop('artifactFilename', artifact['filename']), prop('artifactBytes', artifact['bytes'])]
        if license_ref != c['ref']:
            entry['properties'].append(prop('licenseInheritedFrom', license_ref))
            entry['externalReferences'].append({'type': 'build-meta', 'url': license_source['url'],
                'hashes': [{'alg': 'SHA-256', 'content': license_source['sha256']}]})
        bom_components.append(entry)
        evidence.append({'component': c['ref'], 'licenses': licenses,
            'sourcePom': {k: source[k] for k in ['url', 'sha256']},
            'licensePom': {k: license_source[k] for k in ['url', 'sha256']},
            'inherited': license_ref != c['ref'], 'artifact': artifact})
    app = inventory['application']
    app_ref = 'tvlauncher:runtime-inputs:' + app['version']
    bom = {'$schema': 'http://cyclonedx.org/schema/bom-1.6.schema.json', 'bomFormat': 'CycloneDX',
        'specVersion': '1.6', 'version': 1,
        'metadata': {'component': {'type': 'application', 'bom-ref': app_ref, 'name': app['id'],
            'version': app['version'], 'licenses': [{'license': {'id': 'Apache-2.0', 'acknowledgement': 'declared'}}]},
            'properties': [prop('configuration', inventory['configuration']), prop('mainCodeSha256', source_hash),
                prop('inventorySha256', sha256(inventory_path)),
                prop('scope', 'Resolved runtime inputs before R8; metadata-only modules are explicit. Build/test tools and embedded notice audit are separate release gates.')]},
        'components': bom_components,
        'dependencies': [{'ref': app_ref, 'dependsOn': sorted(inventory['rootDependencies'])}] +
            [{'ref': c['ref'], 'dependsOn': sorted(c['dependencies'])} for c in sorted(components, key=lambda c: c['ref'])]}
    report = {'mainCodeSha256': source_hash, 'configuration': inventory['configuration'],
        'componentCount': len(components), 'uniqueArtifactCount': len(artifacts),
        'rawArtifactSelections': inventory['rawArtifactSelections'], 'metadataOnlyCount': len(components) - len(artifacts),
        'scope': 'Declared POM licenses,including bounded parent inheritance. Not a bundled-notice,source-header,build/test-tool or security audit.',
        'components': evidence}
    output.mkdir(parents=True, exist_ok=True)
    for name, data in [('runtime-inputs.cdx.json', bom), ('runtime-licenses.json', report)]:
        path = output / name
        tmp = path.with_suffix('.tmp')
        tmp.write_text(json.dumps(data, indent=2) + '\n')
        tmp.replace(path)
    print('Generated', len(components), 'components;', len(artifacts), 'unique artifacts')

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('inventory', type=Path)
    parser.add_argument('pom_manifest', type=Path)
    parser.add_argument('output', type=Path)
    parser.add_argument('--source-sha256', required=True)
    args = parser.parse_args()
    generate(args.inventory, args.pom_manifest, args.output, args.source_sha256)
