package tech.ghenkle.kumquatt.exception

import org.eclipse.paho.client.mqttv3.IMqttToken

/**
 * Exception class for all exceptions in Kumquatt. Wraps the exception and optionally an MQTT [token].
 */
@Suppress("unused", "memberVisibilityCanBePrivate")
class KumquattException(
    override val message: String,
    override val cause: Throwable,
    val token: IMqttToken? = null
) : RuntimeException(){
    constructor(cause: Throwable) : this(cause = cause, token = null)
    constructor(token: IMqttToken) : this(cause = token.exception, token = token)
    constructor(cause: Throwable, token: IMqttToken?) : this(message = cause.message ?: "", cause = cause, token = token)
}