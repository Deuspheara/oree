#!/usr/bin/env python3
"""Capture the real catalog tour with bounded, local-only Perfetto tracing.
Requires TV_DEVICE and an installed non-debuggable fr.deuspheara.oree.
No HOME, display, accessibility, power-policy or existing-app changes.
"""
import argparse
import os
from pathlib import Path
import re
import subprocess
import time

from trace_quality import main as validate_trace

parser = argparse.ArgumentParser()
parser.add_argument('--label', required=True)
parser.add_argument('--trace-processor', required=True, type=Path,
                    help='Existing local trace_processor_shell binary; no download wrapper')
parser.add_argument('--config', type=Path, default=Path('scripts/trace-config.pbtxt'),
                    help='Explicit trace coverage; CPU/input-only alternative: scripts/trace-config-cpu.pbtxt')
args = parser.parse_args()
config = args.config.read_text()
if not re.fullmatch('[a-z0-9-]{1,40}', args.label):
    parser.error('Label must contain 1–40 lowercase letters, digits or hyphens.')
serial = os.environ.get('TV_DEVICE')
if not serial:
    parser.error('Set TV_DEVICE to the authorized local device serial.')
if not args.trace_processor.is_file() or not os.access(args.trace_processor, os.X_OK):
    parser.error('An executable local trace processor is required before capture.')
with args.trace_processor.open('rb') as processor:
    if processor.read(2) == b'#!':
        parser.error('Pass the cached processor binary, not an auto-download wrapper.')
package = 'fr.deuspheara.oree'
output = Path('.private/perfetto') / args.label
output.mkdir(parents=True, exist_ok=False)
def adb(*cmd, **kwargs):
    return subprocess.check_output(['adb', '-s', serial, *cmd], text=True, **kwargs)
def shell(*cmd, **kwargs):
    return adb('shell', *cmd, **kwargs)

def foreground():
    text = shell('dumpsys', 'activity', 'activities')
    return any(package+'/.MainActivity' in line for line in text.splitlines()
               if 'ResumedActivity' in line or 'topResumedActivity' in line)

# Preserve advertised sources before starting the app. Registration does not establish that
# requested ftrace events work; post-capture quality checks still have to pass.
(output/'data-sources.txt').write_text(shell('perfetto', '--query'))
shell('input','keyevent','KEYCODE_WAKEUP')
shell('am', 'start', '-W', '-n', package+'/.MainActivity')
time.sleep(2)
if not foreground():
    raise SystemExit('Launcher is not foreground; trace aborted.')
(output/'trace-config.pbtxt').write_text(config)
remote = '/data/misc/perfetto-traces/tvl-'+args.label+'.pftrace'
# Keep the adb/perfetto foreground handle alive; a detached process can outlive
# its printed PID, so kill -0 is not sufficient proof that output is complete.
log = (output/'capture.log').open('w')
trace = subprocess.Popen(['adb','-s',serial,'shell','perfetto','-c','-','--txt','-o',remote],
                         stdin=subprocess.PIPE, stdout=log, stderr=log, text=True)
trace.stdin.write(config)
trace.stdin.close()
time.sleep(1)
if trace.poll() is not None:
    raise SystemExit('Trace recorder exited before the tour; inspect capture.log.')
for key in (['22']*4+['20']+['21']*4+['19'])*6:
    shell('input', 'keyevent', key)
    time.sleep(.12)
if not foreground():
    raise SystemExit('Foreground changed during trace; sample invalid.')
try:
    result = trace.wait(timeout=45)
except subprocess.TimeoutExpired:
    raise SystemExit('Trace recorder still live; inspect its handle before restarting.')
finally:
    log.close()
if result:
    raise SystemExit('Perfetto capture failed; inspect capture.log.')
adb('pull',remote,str(output/'tour.pftrace'))
if (output/'tour.pftrace').stat().st_size < 1024:
    raise SystemExit('Trace is empty/truncated; this is not valid performance evidence.')
(output/'gfxinfo.txt').write_text(shell('dumpsys','gfxinfo',package,'framestats'))
(output/'protocol.txt').write_text('Legacy diagnostic: 60 keys; 120ms host pause plus ADB overhead; foreground verified before/after; configuration archived verbatim. Input timestamps/destinations and input-to-presentation are not measured. Quality validation checks prerequisites, not workload coverage or acceptance. Raw trace may contain personal process metadata; do not publish.\n')
print(output)
# Keep incomplete captures for limited diagnosis, but never return success for them.
raise SystemExit(validate_trace([str(output/'tour.pftrace'), '--processor',
                                str(args.trace_processor), '--output', str(output/'quality.json')]))
