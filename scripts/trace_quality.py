#!/usr/bin/env python3
"""Offline completeness checks for TV navigation traces, never performance acceptance.

Pass a locally installed trace_processor_shell binary (not an auto-download wrapper).
Raw SQL output and trace paths belong in the repository's ignored private evidence.
"""
import argparse
import csv
import hashlib
import io
import json
from pathlib import Path
import re
import subprocess


def assess(stats, tracks):
    """Fail closed on reported loss/setup errors or missing attribution prerequisites."""
    issues = []
    for row in stats:
        value = int(row['value'])
        name = row['name']
        if value and (
            row['severity'] in ('error', 'data_loss')
            or name == 'ftrace_setup_errors'
            or re.search(r'overrun|overwrite|overwritten|dropped|loss', name)
        ):
            issues.append(f"{name}[{row['idx']}]={value} ({row['severity']})")
    for name in ('app_slices', 'app_sched', 'app_actual_frames', 'app_expected_frames'):
        if tracks.get(name, 0) <= 0:
            issues.append(f'Missing required rows: {name}')
    return {
        'attributionPrerequisitesPresent': not issues,
        'issues': issues,
        'tracks': tracks,
        'nonzeroStats': stats,
        'acceptance': 'Not assessed: workload coverage, event/frame association, GPU counters, '
                      'clock alignment and actual display cadence need separate validation.',
    }


def query(processor, trace, sql):
    result = subprocess.run(
        [str(processor), str(trace), '-Q', sql], check=True,
        capture_output=True, text=True, timeout=120,
    )
    return list(csv.DictReader(io.StringIO(result.stdout)))


def sha256(path):
    digest = hashlib.sha256()
    with path.open('rb') as source:
        for block in iter(lambda: source.read(1024 * 1024), b''):
            digest.update(block)
    return digest.hexdigest()


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('trace', type=Path)
    parser.add_argument('--processor', required=True, type=Path)
    parser.add_argument('--output', required=True, type=Path)
    parser.add_argument('--package', default='fr.deuspheara.oree')
    args = parser.parse_args(argv)
    if not re.fullmatch(r'[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z][A-Za-z0-9_]*)+', args.package):
        parser.error('Invalid package identity')
    if args.output.exists():
        parser.error('Output already exists; preserve previous evidence')
    if not args.trace.is_file() or not args.processor.is_file():
        parser.error('Trace and local processor binary must exist')
    # Execute the exact file whose bytes are checked/hashed, even for a relative argument.
    args.processor = args.processor.resolve()
    # Avoid Perfetto's Python/shell launchers, which can download executables implicitly.
    with args.processor.open('rb') as executable:
        if executable.read(2) == b'#!':
            parser.error('Use the actual cached processor binary, not a script wrapper')
    report = {'attributionPrerequisitesPresent': False}
    try:
        stats = query(args.processor, args.trace,
                      'SELECT name, idx, severity, source, value FROM stats WHERE value != 0')
        if not stats or not {'name', 'idx', 'severity', 'value'}.issubset(stats[0]):
            raise ValueError('Missing or incompatible stats result')
        selects = []
        for name, table, joins in (
            ('app_slices', 'slice s', 'JOIN thread_track tt ON s.track_id=tt.id '
             'JOIN thread t USING(utid) JOIN process p USING(upid)'),
            ('app_sched', 'sched', 'JOIN thread t USING(utid) JOIN process p USING(upid)'),
            ('app_actual_frames', 'actual_frame_timeline_slice', 'JOIN process p USING(upid)'),
            ('app_expected_frames', 'expected_frame_timeline_slice', 'JOIN process p USING(upid)'),
        ):
            selects.append(f"SELECT '{name}' AS track, COUNT(*) AS count FROM {table} {joins} "
                           f"WHERE p.name='{args.package}'")
        tracks = {row['track']: int(row['count']) for row in
                  query(args.processor, args.trace, ' UNION ALL '.join(selects))}
        report = assess(stats, tracks)
    except (OSError, subprocess.SubprocessError, ValueError, KeyError) as error:
        report['issues'] = [f'Quality query failed: {error}']
    report.update(traceSha256=sha256(args.trace), processorSha256=sha256(args.processor),
                  package=args.package)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open('x') as output:
        output.write(json.dumps(report, indent=2) + '\n')
    print('Trace prerequisites present' if report['attributionPrerequisitesPresent']
          else 'Incomplete trace; inspect private quality report')
    return 0 if report['attributionPrerequisitesPresent'] else 2


if __name__ == '__main__':
    raise SystemExit(main())
