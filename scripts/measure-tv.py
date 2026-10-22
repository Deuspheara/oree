#!/usr/bin/env python3
"""Explicit-package, non-destructive M01 sample. Requires TV_DEVICE, optimized APK installed.
No HOME/permission/resolution changes. Raw output is private; sanitize before publication.
"""
import argparse, json, os, pathlib, re, subprocess, time
parser = argparse.ArgumentParser()
parser.add_argument("--label")
parser.add_argument("--package", default="fr.deuspheara.oree", help="Explicit launcher APK identity for matched baseline comparisons")
parser.add_argument("--settle-seconds", type=int, default=0)
args = parser.parse_args()
if args.label is not None and not re.fullmatch("[a-z0-9-]{1,40}", args.label): parser.error("Invalid local evidence label")
if not 0 <= args.settle_seconds <= 300: parser.error("Settling interval must be0–300 seconds")
serial = os.environ.get('TV_DEVICE')
if not serial: raise SystemExit('Set TV_DEVICE to your local authorized ADB serial.')
out = pathlib.Path('.private/measurements')
if args.label is not None: out = out / args.label
out.mkdir(parents=True, exist_ok=args.label is None)
package = args.package
if not re.fullmatch(r'[A-Za-z][A-Za-z0-9_]*(?:[.][A-Za-z][A-Za-z0-9_]*)+', package):
    parser.error('Invalid package identity')
def adb(*args): return subprocess.check_output(['adb', '-s', serial, *args], text=True)
def shell(*args): return adb('shell', *args)
def capture(name, *args):
    value = shell(*args); (out/name).write_text(value); return value
shell('input','keyevent','KEYCODE_WAKEUP')
capture('device.txt', 'getprop', 'ro.build.version.sdk')
capture('display.txt', 'wm', 'size')
capture('package.txt', 'dumpsys', 'package', package)
# force-stop only this test package for a process-cold sample; no app data deletion.
shell('am', 'force-stop', package)
capture('cold.txt', 'am', 'start', '-W', '-n', package+'/.MainActivity')
time.sleep(4)
capture('memory-before.txt', 'dumpsys', 'meminfo', package)
capture('gfx-reset.txt', 'dumpsys', 'gfxinfo', package, 'reset')
keys = ['KEYCODE_DPAD_RIGHT']*4 + ['KEYCODE_DPAD_DOWN'] + ['KEYCODE_DPAD_LEFT']*4 + ['KEYCODE_DPAD_UP']
start = time.monotonic()
for key in keys*6:
    shell('input', 'keyevent', key)
    time.sleep(0.12)
capture('frames.txt', 'dumpsys', 'gfxinfo', package, 'framestats')
tour_elapsed = time.monotonic()-start
capture('memory-after-tour.txt', 'dumpsys', 'meminfo', package)
# Optional explicit settling phase is separate from the measured idle interval.
# Default0 preserves the historical immediate-after-tour protocol.
if args.settle_seconds: time.sleep(args.settle_seconds)
capture('memory-after-settle.txt', 'dumpsys', 'meminfo', package)
capture('idle-frames-before.txt', 'dumpsys', 'gfxinfo', package, 'framestats')
pid = shell('pidof', package).strip()
def foreground():
    text = shell('dumpsys', 'activity', 'activities')
    return any(package+'/.MainActivity' in line for line in text.splitlines()
               if 'ResumedActivity' in line or 'topResumedActivity' in line)
def ticks():
    # utime/stime: split after final ')' so process name spaces do not shift fields.
    raw = shell('cat', f'/proc/{pid}/stat').rsplit(')',1)[1].split()
    return int(raw[11]) + int(raw[12])
try:
    clock_hz = int(shell('getconf', 'CLK_TCK').strip())
    before_power = capture('idle-power-before.txt', 'dumpsys', 'power')
    before_foreground = foreground()
    before = ticks(); idle_start = time.monotonic()
    time.sleep(60)
    after = ticks(); elapsed = time.monotonic()-idle_start
    after_power = capture('idle-power-after.txt', 'dumpsys', 'power')
    after_foreground = foreground()
    (out/'idle-cpu.json').write_text(json.dumps({'intervalSeconds':elapsed,'processTicksDelta':after-before,'clockTicksPerSecond':clock_hz,'singleCorePercent':100*(after-before)/clock_hz/elapsed,'foregroundAtBothEndpoints':before_foreground and after_foreground,'settleSeconds':args.settle_seconds,'awakeAtBothEndpoints':all('mWakefulness=Awake' in v for v in [before_power,after_power])}, indent=2))
except subprocess.CalledProcessError as error:
    (out/'idle-cpu-error.txt').write_text(str(error))
capture('idle-frames-after.txt', 'dumpsys', 'gfxinfo', package, 'framestats')
capture('memory-after-idle.txt', 'dumpsys', 'meminfo', package)
(out/'protocol.json').write_text(json.dumps({'variant':'benchmarkRelease','keys':60,'keyDelaySeconds':0.12,'tourWallSeconds':tour_elapsed,'artworkMaxPixels':[480,270],'imageMemoryCacheMiB':12,'network':'not required','coldDefinition':'process-cold via force-stop, disk/system caches retained','physicalRemoteLatencyMeasured':False,'settleSeconds':args.settle_seconds,'idleSecondsRequested':60,'idleLimit':'endpoint awake/foreground checks do not prove uninterrupted visibility; dumpsys adds small diagnostic CPU overhead'}, indent=2))
print('Private evidence:', out)
