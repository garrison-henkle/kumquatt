package tech.ghenkle.kumquatt.callback

import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import tech.ghenkle.kumquatt.data.KumquattToken
import tech.ghenkle.kumquatt.exception.KumquattException

/**
 * Implementation of the [IMqttActionListener] that removes boilerplate code using lambdas.
 */
class KumquattStatusCallback(
    private val onSuccess: ((token: KumquattToken) -> Unit)?,
    private val onError: ((ex: KumquattException) -> Unit)?
) : IMqttActionListener{
    override fun onSuccess(asyncActionToken: IMqttToken) {
        onSuccess?.invoke(KumquattToken(mqttToken = asyncActionToken))
    }

    override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
        onError?.invoke(KumquattException(cause = exception, token = asyncActionToken))
    }
}