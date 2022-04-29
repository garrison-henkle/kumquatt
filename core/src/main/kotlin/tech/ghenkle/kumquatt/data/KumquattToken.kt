package tech.ghenkle.kumquatt.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import tech.ghenkle.kumquatt.exception.KumquattException

/**
 * Wrapper for the [IMqttToken] class with convenience fields/methods for accessing data.
 */
@Suppress("unused", "memberVisibilityCanBePrivate")
class KumquattToken(val mqttToken: IMqttToken){
    /**
     * The id of the client.
     */
    val clientId: String get() = mqttToken.client!!.clientId

    /**
     * Indicates whether the action associated with this token is complete.
     */
    val isComplete get() = mqttToken.isComplete

    /**
     * Returns a list of topics for the action associated with this token or an empty array if none exist.
     */
    val topics get() = mqttToken.topics?.filterNotNull()?.toTypedArray() ?: emptyArray()

    /**
     * The id of the message
     */
    val messageId get() = mqttToken.messageId

    /**
     * The quality of services granted to the actions associated with this token.
     */
    val grantedQos get() = mqttToken.grantedQos ?: intArrayOf()

    /**
     * Indicators if a session is present.
     */
    val sessionPresent get() = mqttToken.sessionPresent

    /**
     * The user context associated with this action.
     */
    val userContext: Any? get() = mqttToken.userContext

    /**
     * The [Dispatchers.IO] scope for the [waitForCompletion] methods.
     */
    private val scope by lazy{ CoroutineScope(Dispatchers.IO) }

    /**
     * Suspends the coroutine until the action associated with the token is completed. There is no limit on
     * suspension time.
     */
    suspend fun waitForCompletion() = withContext(scope.coroutineContext) { mqttToken.waitForCompletion() }

    /**
     * Suspends the coroutine until the action associated with the token is completed.
     *
     * @param timeout The time to wait for completion.
     */
    suspend fun waitForCompletion(timeout: Long) =
        withContext(scope.coroutineContext) { mqttToken.waitForCompletion(timeout) }

    /**
     * Returns the message associated with this token or null if the token is not a message.
     */
    fun getMessage(): KumquattMessage? = (mqttToken as? IMqttDeliveryToken)?.message?.let{ KumquattMessage(mqttMessage = it) }

    /**
     * Returns the exception associated with this token or null if one does not exist.
     */
    fun getException(): KumquattException? = mqttToken.exception?.let{ KumquattException(it) }
}