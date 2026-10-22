package fr.deuspheara.oree.diagnostics

import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import fr.deuspheara.oree.BuildConfig

/** Opt-in, debug-only event observation. Never consumes input or runs in optimized builds. */
object InputDiagnostics {
    private const val TAG = "LauncherInput"

    private fun enabled() = BuildConfig.DEBUG && Log.isLoggable(TAG, Log.DEBUG)

    fun install(window: android.view.Window) {
        if (!BuildConfig.DEBUG) return
        val original = window.callback
        window.callback =
            object : android.view.Window.Callback by original {
                override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                    observe(event)
                    return original.dispatchKeyEvent(event)
                }

                override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
                    observe(event)
                    return original.dispatchGenericMotionEvent(event)
                }

                override fun dispatchTouchEvent(event: MotionEvent): Boolean {
                    observe(event)
                    return original.dispatchTouchEvent(event)
                }
            }
    }

    fun observe(event: KeyEvent) {
        if (!enabled()) return
        Log.d(
            TAG,
            "key source=${event.source} device=${event.deviceId} action=${event.action} " +
                "code=${event.keyCode} repeat=${event.repeatCount} time=${event.eventTime} down=${event.downTime}",
        )
    }

    fun observe(event: MotionEvent) {
        if (!enabled()) return
        val axes =
            InputDevice.getDevice(event.deviceId)?.motionRanges.orEmpty().joinToString {
                "${MotionEvent.axisToString(it.axis)}=${event.getAxisValue(it.axis)}"
            }
        Log.d(
            TAG,
            "motion source=${event.source} device=${event.deviceId} action=${event.actionMasked} " +
                "x=${event.x} y=${event.y} relativeX=${event.getAxisValue(MotionEvent.AXIS_RELATIVE_X)} " +
                "relativeY=${event.getAxisValue(MotionEvent.AXIS_RELATIVE_Y)} axes=[$axes] time=${event.eventTime}",
        )
    }
}
