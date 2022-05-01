package tech.ghenkle.kumquatt

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.cancellable
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttMessage
import tech.ghenkle.kumquatt.callback.KumquattMessageCallback
import tech.ghenkle.kumquatt.callback.KumquattStatusCallback
import tech.ghenkle.kumquatt.data.KumquattMessage
import tech.ghenkle.kumquatt.data.KumquattToken
import tech.ghenkle.kumquatt.exception.KumquattException
import kotlin.coroutines.CoroutineContext

/**
 * Represents a subscription to a topic. It is a cold data stream, so messages are only received if the subscription is
 * actively being listened to via one of the [collect] methods.
 *
 * @param unsubscribe Function to handle unsubscribing from the topic.
 * @param onSuccess Optional handler for successful subscriptions.
 * @param onError Optional handler for failed subscriptions.
 * @param context Optional default context for the [collect]s to run on. Defaults to [Dispatchers.IO]. Note that this
 * can be changed for segments of the [collect] blocks by using [withContext].
 * @param subscribe Function to handle subscribing to the topic.
 */
class KumquattSubscription(
    unsubscribe: () -> IMqttToken,
    private val onSuccess: ((KumquattToken) -> Unit)? = null,
    private val onError: ((KumquattException) -> Unit)? = null,
    context: CoroutineContext = Dispatchers.Default,
    private val subscribe: (KumquattMessageCallback, IMqttActionListener) -> IMqttToken
){
    private val scope = CoroutineScope(context)
    private var messageCallback: KumquattMessageCallback? = null
    private val statusCallback = KumquattStatusCallback(onSuccess, onError)
    private val flow = callbackFlow {
        val messageCallback = object : KumquattMessageCallback {
            override fun close() = this@callbackFlow.cancel()
            override fun messageArrived(topic: String, message: MqttMessage) {
                trySend(KumquattMessage(mqttMessage = message, topic = topic))
                    .onFailure { ex -> ex?.also{ throw KumquattException(it) } }
            }
        }
        registerCallbackAndSubscribe(messageCallback)
        awaitClose {
            unsubscribe()
            closeFlow()
        }
    }.buffer(capacity = Channel.BUFFERED)

    /**
     * Collects from this subscription using the subscription's current [scope].
     *
     * @param handler A function to handle a received message. It is provided with two parameters: 1) the message being
     * received and 2) a function that can be called to stop the current [collect]ion.
     *
     * @return The current [KumquattSubscription] instance.
     */
    fun collect(
        handler: (message: KumquattMessage, stopCollecting: () -> Unit) -> Unit
    ): KumquattSubscription = this.also{
        scope.launch {
            flow.cancellable().collect{
                handler(it) { this.coroutineContext.job.cancel() }
            }
        }
    }

    /**
     * Collects from this subscription using the subscription's current [scope].
     *
     * @param handler A function to handle a received message. It is provided with a single parameter: 1) the message
     * being received.
     *
     * @return The current [KumquattSubscription] instance.
     */
    fun collect(handler: (message: KumquattMessage) -> Unit): KumquattSubscription = this.also{
        scope.launch {
            flow.cancellable().collect{
                handler(it)
            }
        }
    }

    /**
     * Registers a message callback and attempts to start the subscription.
     */
    private fun registerCallbackAndSubscribe(callback: KumquattMessageCallback){
        this.messageCallback = callback
        subscribe(callback, statusCallback)
    }

    /**
     * Closes the current subscription. The flow associated with the subscription is canceled, so collections using
     * [collect] can no longer occur after this call.
     */
    private fun closeFlow(){
        messageCallback?.close()
        messageCallback = null
    }
}