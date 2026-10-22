#!/usr/bin/env python3
"""Exercise SBOM integrity boundaries with temporary copies,never original evidence."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('inventory', type=Path)
parser.add_argument('pom_manifest', type=Path)
parser.add_argument('generated', type=Path)
parser.add_argument('schemas', type=Path)
parser.add_argument('--source-sha256', required=True)
parser.add_argument('--output', type=Path, required=True)
args = parser.parse_args()
scripts = Path(__file__).resolve().parent

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def generate(manifest, output):
    return subprocess.run([sys.executable,str(scripts/'generate-runtime-sbom.py'),str(args.inventory),
        str(manifest),str(output),'--source-sha256',args.source_sha256],capture_output=True,text=True)

original = {name: digest(args.generated/name) for name in ['runtime-inputs.cdx.json','runtime-licenses.json']}
with tempfile.TemporaryDirectory(prefix='tvlauncher-sbom-check-') as temporary:
    root = Path(temporary)
    repeated = root/'repeat'
    result = generate(args.pom_manifest,repeated)
    if result.returncode or any(digest(repeated/name)!=value for name,value in original.items()):
        raise SystemExit('SBOM regeneration is not deterministic')
    for case, payload, expected in [
        ('changed-pom',None,'POM evidence size/checksum mismatch'),
        ('dtd-pom',b'<!DOCTYPE project [<!ENTITY x "untrusted">]><project/>','Unsupported POM XML declaration'),
    ]:
        rows = json.loads(args.pom_manifest.read_text())
        target = root/(case+'.pom')
        target.write_bytes(payload if payload is not None else Path(rows[0]['file']).read_bytes()+b'\n')
        rows[0]['file'] = str(target)
        if payload is not None:
            rows[0]['sha256'] = digest(target)
        manifest = root/(case+'.json')
        manifest.write_text(json.dumps(rows))
        rejected = root/(case+'-output')
        result = generate(manifest,rejected)
        if result.returncode==0 or expected not in result.stderr or rejected.exists():
            raise SystemExit('POM integrity rejection failed: '+case)
    bom = json.loads((args.generated/'runtime-inputs.cdx.json').read_text())
    bom['dependencies'][0]['dependsOn'].append('pkg:maven/example/missing@1')
    invalid = root/'invalid.cdx.json'
    invalid.write_text(json.dumps(bom))
    result = subprocess.run([sys.executable,str(scripts/'validate-runtime-sbom.py'),str(invalid),
        str(args.schemas),str(root/'invalid-validation.json')],capture_output=True,text=True)
    if result.returncode==0 or 'Dangling dependency reference' not in result.stderr:
        raise SystemExit('Dangling graph rejection failed')
if any(digest(args.generated/name)!=value for name,value in original.items()):
    raise SystemExit('Original SBOM output changed during verification')
result = {'bomSha256':original['runtime-inputs.cdx.json'],
    'deterministicRegeneration':'pass','tamperedPom':'rejected before output',
    'pomDtdEntityDeclaration':'rejected before output','danglingGraphReference':'rejected',
    'originalOutputsPreserved':True,
    'toolSha256':{name:digest(scripts/name) for name in ['generate-runtime-sbom.py','validate-runtime-sbom.py',
        'fetch-runtime-poms.py','runtime-inventory.init.gradle']},
    'scope':'End-to-end build-tool boundaries,not application security or dependency vulnerability certification.'}
args.output.parent.mkdir(parents=True,exist_ok=True)
args.output.write_text(json.dumps(result,indent=2)+'\n')
print(args.output)
