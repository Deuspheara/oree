# Safe device testing and recovery

Keep the current launcher installed and leave default HOME, accessibility, display and standby settings unchanged. Do not clear configured app data. Before testing, verify ADB connectivity and record the existing HOME component privately:

```sh
adb -s "$TV_DEVICE" shell cmd package resolve-activity --brief \
  -a android.intent.action.MAIN -c android.intent.category.HOME
```

Install and start Oree explicitly:

```sh
adb -s "$TV_DEVICE" install -r app/build/outputs/apk/debug/app-debug.apk
adb -s "$TV_DEVICE" shell am start -n fr.deuspheara.oree/.MainActivity
```

If it fails, press the system HOME key to return to the existing default launcher, or open Android settings:

```sh
adb -s "$TV_DEVICE" shell am start -a android.settings.SETTINGS
```

Keep a verified explicit start command for the existing launcher in private notes. Re-resolve components after updates instead of copying another device's package names. No other launcher needs to be disabled or uninstalled.

Back up configured data before instrumentation. Use replacement installs and direct tests under [Testing](TEST_PLAN.md). Do not collect profiles on configured user data: compilation reset may reinstall the target. Any later HOME migration needs separate authorization and a tested recovery path.
