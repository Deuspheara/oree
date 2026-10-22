package fr.deuspheara.oree.core.platform.status

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Available outputs are not a claim about the active media route. No device names/IDs retained. */
enum class AudioOutput {
    SPEAKER,
    HDMI,
    HEADPHONES,
    BLUETOOTH,
    USB,
    OTHER,
}

enum class NetworkKind {
    UNKNOWN,
    OFFLINE,
    WIFI,
    ETHERNET,
    VPN,
    OTHER,
}

data class ControlStatus(
    val network: NetworkKind = NetworkKind.UNKNOWN,
    val validated: Boolean = false,
    val audio: Set<AudioOutput> = emptySet(),
)

/** Registered only while collected by the visible, STARTED command panel. No polling or traffic. */
fun controlStatus(context: Context) = callbackFlow {
    val connectivity = context.getSystemService(ConnectivityManager::class.java)
    val audio = context.getSystemService(AudioManager::class.java)
    val handler = Handler(Looper.getMainLooper())
    var current = ControlStatus()
    fun publishNetwork(c: NetworkCapabilities?) {
        current =
            current.copy(
                network =
                    when {
                        c == null -> NetworkKind.OFFLINE

                        c.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkKind.VPN

                        c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkKind.WIFI

                        c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->
                            NetworkKind.ETHERNET

                        else -> NetworkKind.OTHER
                    },
                validated = c?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
            )
        trySend(current)
    }
    fun publishAudio() {
        val outputs = runCatching {
            audio
                ?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                ?.map { device ->
                    when (device.type) {
                        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> AudioOutput.SPEAKER

                        AudioDeviceInfo.TYPE_HDMI,
                        AudioDeviceInfo.TYPE_HDMI_ARC,
                        AudioDeviceInfo.TYPE_HDMI_EARC,
                        -> AudioOutput.HDMI

                        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                        AudioDeviceInfo.TYPE_WIRED_HEADSET,
                        -> AudioOutput.HEADPHONES

                        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                        AudioDeviceInfo.TYPE_BLE_HEADSET,
                        AudioDeviceInfo.TYPE_BLE_SPEAKER,
                        -> AudioOutput.BLUETOOTH

                        AudioDeviceInfo.TYPE_USB_DEVICE,
                        AudioDeviceInfo.TYPE_USB_HEADSET,
                        AudioDeviceInfo.TYPE_USB_ACCESSORY,
                        -> AudioOutput.USB

                        else -> AudioOutput.OTHER
                    }
                }
                ?.toSet()
                .orEmpty()
        }
            .getOrDefault(emptySet())
        current = current.copy(audio = outputs)
        trySend(current)
    }
    val networkCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = publishNetwork(capabilities)

            override fun onLost(network: Network) = publishNetwork(null)
        }
    val audioCallback =
        object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = publishAudio()

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = publishAudio()
        }
    var networkRegistered = false
    var audioRegistered = false
    val start = Runnable {
        publishAudio()
        try {
            publishNetwork(connectivity?.getNetworkCapabilities(connectivity.activeNetwork))
            connectivity?.registerDefaultNetworkCallback(networkCallback, handler)
            networkRegistered = connectivity != null
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: RuntimeException) {
            current = current.copy(network = NetworkKind.UNKNOWN)
            trySend(current)
        }
        try {
            audio?.registerAudioDeviceCallback(audioCallback, handler)
            audioRegistered = audio != null
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: RuntimeException) {}
    }
    handler.post(start)
    awaitClose {
        handler.removeCallbacks(start)
        handler.post {
            if (networkRegistered) {
                runCatching { connectivity?.unregisterNetworkCallback(networkCallback) }
            }
            if (audioRegistered) runCatching { audio?.unregisterAudioDeviceCallback(audioCallback) }
        }
    }
}
    .distinctUntilChanged()
