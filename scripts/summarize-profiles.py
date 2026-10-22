#!/usr/bin/env python3
"""Check merged HRF profiles and export bounded rule-presence evidence, not a speed claim."""
import argparse
import json
from pathlib import Path
from profile_rules import read_profile

parser = argparse.ArgumentParser()
parser.add_argument('baseline', type=Path)
parser.add_argument('startup', type=Path)
parser.add_argument('output', type=Path)
args = parser.parse_args()

baseline, base_rules = read_profile(args.baseline)
startup, start_rules = read_profile(args.startup)
classes = [
    'presentation/LauncherRootKt',
    'feature/home/presentation/screen/HomeScreenKt',
    'feature/home/presentation/component/AppTileKt',
    'feature/settings/presentation/screen/SettingsScreenKt',
    'feature/settings/presentation/component/CommandPanelKt',
    'feature/home/presentation/component/AppContextMenuKt',
    'core/data/repository/DataStoreProfileRepository',
    'core/data/repository/DefaultAppRepository',
]

presence = {}
for name in classes:
    prefix = 'Lfr/deuspheara/oree/' + name + ';'
    matched = [rule for rule in base_rules if rule.startswith(prefix)]
    presence[name] = {'classRule': prefix in base_rules,
                      'methodRules': sum('->' in rule for rule in matched),
                      'present': bool(matched)}
missing_startup = len(set(start_rules) - set(base_rules))
result = {'baseline': baseline, 'startup': startup,
          'startupRulesMissingFromBaseline': missing_startup,
          'requiredClassPresence': presence,
          'scope': 'Merged generated HRF. Presence plus executed journeys supports current-path inclusion, not branch-complete coverage or guaranteed AOT of every method.',
          'limits': 'Rule counts/size do not establish speed. Require install/compilation and same-APK measurements separately.'}
args.output.parent.mkdir(parents=True, exist_ok=True)
args.output.write_text(json.dumps(result, indent=2) + '\n')
if missing_startup or not all(value['present'] for value in presence.values()):
    raise SystemExit('Current-path profile check failed; inspect the exported presence evidence.')
print(args.output)
