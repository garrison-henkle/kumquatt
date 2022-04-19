package tech.ghenkle.kumquatt.callback

import org.eclipse.paho.client.mqttv3.IMqttMessageListener

/**
 * Interface for MQTT message callbacks.
 *
 * This differs from the paho [IMqttMessageListener] in that the listener can be [close]d. This allows for the
 * [kotlinx.coroutines.flow.callbackFlow] that bridges the gap between paho and coroutines/flows to be closed gracefully.
 */
interface KumquattMessageCallback : IMqttMessageListener {
    fun close()
}