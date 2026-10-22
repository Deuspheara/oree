#!/usr/bin/env python3
"""Merge only verified converged captures from an unchanged target APK/code tree.
Manifest is local evidence: journey,profile,log,report,targetApkSha256 for each capture.
Reports/logs remain private. The exported summary contains hashes/counts only.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET
from profile_rules import main_code_sha, read_profile, render_profile

parser = argparse.ArgumentParser()
parser.add_argument('manifest', type=Path)
parser.add_argument('--destination', type=Path, default=Path('app/src/main/generated/baselineProfiles'))
parser.add_argument('--summary', type=Path, required=True)
args = parser.parse_args()
data = json.loads(args.manifest.read_text())
if main_code_sha() != data['mainCodeSha256']:
    raise SystemExit('App code changed since collection; do not mix captures.')
if hashlib.sha256(Path(data['targetApk']).read_bytes()).hexdigest() != data['targetApkSha256']:
    raise SystemExit('Collector target APK checksum differs from capture manifest.')
expected = {'startup', 'catalog', 'secondaryScreens'}
if {entry['journey'] for entry in data['captures']} != expected or len(data['captures']) != 3:
    raise SystemExit('Exactly three distinct current journeys are required.')
merged, startup, evidence = {}, {}, []
for entry in data['captures']:
    if entry['targetApkSha256'] != data['targetApkSha256']:
        raise SystemExit('Captures came from different target APKs.')
    report = ET.parse(entry['report']).getroot()
    cases = [case for case in report.iter('testcase')
             if case.get('name') == entry['journey'] and case.get('classname', '').endswith('.LauncherProfileGenerator')]
    if len(cases) != 1 or any(cases[0].find(kind) is not None for kind in ['failure', 'error', 'skipped']):
        raise SystemExit('Capture has no unique successful JUnit case.')
    last_iteration, converged = None, False
    with Path(entry['log']).open(errors='replace') as log:
        for line in log:
            match = re.search(r'(?:Unstable profiles during iteration|Profiles stable in iteration) (\d+)', line)
            if match:
                last_iteration = int(match.group(1))
            if 'Baseline profile for fr.deuspheara.oree is stable.' in line:
                converged = True
    if not converged or last_iteration is None:
        raise SystemExit('Capture did not report convergence; refusing a partial profile.')
    if not Path(entry['profile']).name.startswith('LauncherProfileGenerator_' + entry['journey'] + '-'):
        raise SystemExit('Profile filename does not identify its declared journey.')
    meta, rules = read_profile(entry['profile'])
    if meta['sha256'] != entry['profileSha256']:
        raise SystemExit('Captured profile checksum differs from manifest.')
    for key, flags in rules.items():
        merged.setdefault(key, set()).update(flags)
        if entry['journey'] == 'startup':
            startup.setdefault(key, set()).update(flags)
    evidence.append({'journey': entry['journey'], 'convergedAtIteration': last_iteration, **meta})
args.destination.mkdir(parents=True, exist_ok=True)
for name, rules in [('baseline-prof.txt', merged), ('startup-prof.txt', startup)]:
    path = args.destination / name
    temporary = path.with_suffix('.tmp')
    temporary.write_text(render_profile(rules))
    temporary.replace(path)
args.summary.parent.mkdir(parents=True, exist_ok=True)
args.summary.write_text(json.dumps({'mainCodeSha256': data['mainCodeSha256'],
    'collectorTargetApkSha256': data['targetApkSha256'], 'captures': evidence,
    'merge': 'Semantic rule union with H/S/P flags preserved; Startup Profile uses startup capture only.',
    'limits': 'Per-journey JUnit/convergence evidence; earlier source-scoped failures remain separate historical evidence. Not a timing or branch-complete coverage claim.'}, indent=2) + '\n')
print(args.summary)
