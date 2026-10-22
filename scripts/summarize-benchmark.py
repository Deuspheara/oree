#!/usr/bin/env python3
"""Export aggregate AndroidX metrics without private build fingerprints or trace paths."""
import argparse
import json
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument('input', type=Path)
parser.add_argument('output', type=Path)
args = parser.parse_args()
data = json.loads(args.input.read_text())
context = data['context']
summary = {
    'device': {
        'model': context['build']['model'],
        'sdk': context['build']['version']['sdk'],
        'cpuCoreCount': context['cpuCoreCount'],
        'cpuLockedReported': context['cpuLocked'],
    },
    'benchmarks': [],
}
for benchmark in data['benchmarks']:
    result = {'name': benchmark['name'], 'params': benchmark.get('params'),
              'metrics': benchmark.get('metrics'), 'sampled': {}}
    for name, metric in benchmark.get('sampledMetrics', {}).items():
        result['sampled'][name] = {key: value for key, value in metric.items() if key != 'runs'}
        if name == 'frameOverrunMs':
            runs = metric['runs']
            if any(not run for run in runs):
                raise SystemExit('Empty frame sample is not valid performance evidence.')
            result['positiveOverrunPerIteration'] = [
                {'frames': len(run), 'positiveOverrun': sum(value > 0 for value in run),
                 'positiveOverrunPercent': 100 * sum(value > 0 for value in run) / len(run)}
                for run in runs
            ]
    summary['benchmarks'].append(result)
args.output.parent.mkdir(parents=True, exist_ok=True)
args.output.write_text(json.dumps(summary, indent=2) + '\n')
print(args.output)
