package tech.ghenkle.kumquatt.callback

import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import tech.ghenkle.kumquatt.Kumquatt
import tech.ghenkle.kumquatt.data.KumquattToken
import tech.ghenkle.kumquatt.exception.KumquattException

/**
 * Implementation of the [IMqttActionListener] for connection attempts that removes boilerplate code using lambdas.
 */
class KumquattConnectionStatusCallback(
    private val client: Kumquatt,
    private val onSuccess: (Kumquatt.(token: KumquattToken) -> Unit)? = null,
    private val onError: ((ex: KumquattException) -> Unit)? = null
) : IMqttActionListener {
    override fun onSuccess(asyncActionToken: IMqttToken) {
        onSuccess?.invoke(client, KumquattToken(mqttToken = asyncActionToken))
    }

    override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
        onError?.invoke(KumquattException(cause = exception, token = asyncActionToken))
    }
}