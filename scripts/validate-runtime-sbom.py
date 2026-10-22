#!/usr/bin/env python3
"""Validate a generated SBOM against local official schemas; never fetch references."""
import argparse
import hashlib
import importlib.metadata
import json
from pathlib import Path
from jsonschema import Draft7Validator
from referencing import Registry, Resource
from referencing.jsonschema import DRAFT7

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('bom', type=Path)
parser.add_argument('schema_directory', type=Path)
parser.add_argument('output', type=Path)
args = parser.parse_args()
names = ['bom-1.6.schema.json', 'spdx.schema.json', 'jsf-0.82.schema.json']
registry = Registry()
schemas, hashes = {}, {}
for name in names:
    path = args.schema_directory / name
    data = path.read_bytes()
    if len(data) > 4 * 1024 * 1024:
        raise SystemExit('Oversized schema')
    schema = json.loads(data)
    schemas[name] = schema
    uri = schema.get('$id', 'http://cyclonedx.org/schema/' + name)
    registry = registry.with_resource(uri, Resource.from_contents(schema, default_specification=DRAFT7))
    hashes[name] = hashlib.sha256(data).hexdigest()
schema = schemas[names[0]]
Draft7Validator.check_schema(schema)
data = json.loads(args.bom.read_text())
validator = Draft7Validator(schema, registry=registry, format_checker=Draft7Validator.FORMAT_CHECKER)
errors = sorted(validator.iter_errors(data), key=lambda e: str(e.absolute_path))
if errors:
    for error in errors[:20]:
        print('/'.join(map(str, error.absolute_path)), error.message)
    raise SystemExit('CycloneDX schema validation failed')
refs = {c['bom-ref'] for c in data['components']}
root = data['metadata']['component']['bom-ref']
if len(refs) != len(data['components']) or root in refs:
    raise SystemExit('Duplicate component identities')
all_refs = refs | {root}
edges = data['dependencies']
if {edge['ref'] for edge in edges} != all_refs or len(edges) != len(all_refs):
    raise SystemExit('Incomplete/duplicate graph nodes')
if any(not set(edge['dependsOn']) <= all_refs for edge in edges):
    raise SystemExit('Dangling dependency reference')
result = {'bomSha256': hashlib.sha256(args.bom.read_bytes()).hexdigest(),
    'specification': 'CycloneDX1.6', 'validator': 'jsonschema' + importlib.metadata.version('jsonschema'),
    'schemasSha256': hashes, 'schemaValidation': 'pass',
    'graphReferenceValidation': 'pass', 'componentCount': len(refs), 'graphNodesIncludingRoot': len(all_refs),
    'scope': 'Schema,available format checks and graph integrity only; not vulnerability,license-notice or APK-bytecode verification.'}
args.output.parent.mkdir(parents=True, exist_ok=True)
args.output.write_text(json.dumps(result, indent=2) + '\n')
print(args.output)
