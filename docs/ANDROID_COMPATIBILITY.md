# Android compatibility

| Setting | Value |
|---|---|
| Application ID | `fr.deuspheara.oree` |
| Minimum API | 26 |
| Target API | 36 |
| Compile SDK | 37 |
| Required device feature | Android TV / Leanback |
| Touchscreen | Optional |

Configured SDK levels do not establish a complete device/firmware support matrix. Validate native focus, system IME, permissions, provider availability, external launches, screensaver behavior and rendering on each intended device class, including modest 32-bit hardware. Keep emulator and physical evidence separate.

## Optional platform access

Photo access uses the platform's version-appropriate image permission or system picker. Public TV sources require available public providers and permitted access. An empty result does not prove an absence of viewing history. Denied/unsupported sources must preserve app-only operation and cached display data where appropriate.

PackageManager and Android resolve installed apps, inputs, settings routes and external destinations. Resolution is not proof an OEM viewer will launch successfully. Validate intent schemes/components and handle missing/security failures without changing app-ops or firmware policies.

The screensaver is an ordinary Android DreamService protected by `BIND_DREAM_SERVICE`; Android owns selection and power behavior. No root, privileged APIs, accessibility service, overlay, wake lock or intercepted system HOME/POWER key is part of the product.
