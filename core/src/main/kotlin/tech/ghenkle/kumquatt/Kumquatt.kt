package tech.ghenkle.kumquatt

import com.beust.klaxon.Klaxon
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import tech.ghenkle.kumquatt.callback.KumquattConnectionStatusCallback
import tech.ghenkle.kumquatt.callback.KumquattStatusCallback
import tech.ghenkle.kumquatt.KumquattScheme.*
import tech.ghenkle.kumquatt.data.KumquattMessage
import tech.ghenkle.kumquatt.data.KumquattToken
import tech.ghenkle.kumquatt.exception.KumquattException
import tech.ghenkle.kumquatt.utils.utf8Bytes

//TODO add more backpressure strategies to match the RxJava implementation
/**
 * The main Kumquatt client class. It implements an asynchronous MQTT client using coroutines.
 *
 * @see withHost
 * @see withUri
 */
@Suppress("unused", "memberVisibilityCanBePrivate")
class Kumquatt private constructor(
    val pahoClient: IMqttAsyncClient,
    val options: MqttConnectOptions
){
    /* Connection Management */

    /**
     * Connect to the broker defined in the client.
     *
     * @param onError Optional handler for failed connections to the broker.
     * @param onSuccess Optional handler for successful connections to the broker. A reference to the client is passed
     * to the handler to allow for concise usage.
     *
     * @return A token that can be used to track the status of the connection operation.
     */
    fun connect(
        onError: ((ex: KumquattException) -> Unit)? = null,
        onSuccess: (Kumquatt.(token: KumquattToken) -> Unit)? = null
    ): KumquattToken = try{
        KumquattToken(pahoClient.connect(options, null,
            KumquattConnectionStatusCallback(this, onSuccess, onError)))
    } catch (ex: Exception){
        throw KumquattException(ex)
    }

    /**
     * Disconnect from the broker defined in the client.
     *
     * @param timeout Sets the timeout used for gracefully closing connections. Defaults to 30 seconds.
     * @param onError Optional handler for failed disconnections from the broker.
     * @param onSuccess Optional handler for successful disconnections from the broker.
     *
     * @see disconnectAndClose
     * @see forceDisconnect
     *
     * @return A token that can be used to track the status of the disconnect operation.
     */
    fun disconnect(
        timeout: Long? = null,
        onError: ((ex: KumquattException) -> Unit)? = null,
        onSuccess: ((token: KumquattToken) -> Unit)? = null
    ): KumquattToken = try{
        KumquattToken(
            timeout?.let{
                pahoClient.disconnect(it, null, KumquattStatusCallback(onSuccess, onError))
            } ?: pahoClient.disconnect(null, KumquattStatusCallback(onSuccess, onError))
        )
    } catch (ex: Exception){
        throw KumquattException(ex)
    }

    /**
     * Forcibly disconnects the client from the broker. Useful for situations in which the [disconnect] does not
     * succeed.
     *
     * @see disconnectAndClose
     * @see disconnect
     */
    fun forceDisconnect(): Unit = try{
        pahoClient.disconnectForcibly()
    } catch (ex: Exception){
        throw KumquattException(ex)
    }

    /**
     * Closes the client. WILL throw an error if all connections are not disconnected prior to the call.
     *
     * Prefer [disconnectAndClose] for a safer way of closing the client.
     * @see disconnectAndClose
     */
    fun close(): Unit = try{
        pahoClient.close()
    } catch (ex: Exception){
        throw KumquattException(ex)
    }

    /**
     * Safely closes the client by disconnecting all connections and closing the server.
     *
     * This method will first attempt a graceful disconnect.  If the graceful disconnect fails, it will then force the
     * disconnect. The client will be closed regardless of which disconnect method finally succeeded.
     *
     * @param timeout Sets the timeout used for gracefully closing connections. Defaults to 30 seconds.
     */
    fun disconnectAndClose(timeout: Long? = null) = try{
        disconnect(
            timeout = timeout,
            onSuccess = {
                close()
            }, onError = {
                forceDisconnect()
                close()
            }
        )
        Unit
    } catch (ex: Exception){
        throw KumquattException(ex)
    }

    /* Publishing */

    /**
     * Publishes a message to a topic.
     *
     * @param message The message to send.
     * @param qos The quality of service level for this message.
     * @param retained Allow the server to retain the message.
     * @param onError Optional error handler for failed publishes.
     * @param onSuccess Optional success handler for successful publishes.
     */
    fun publish(
        message: KumquattMessage,
        qos: KumquattQos = KumquattQos.EXACTLY_ONCE,
        retained: Boolean = false,
        onError: ((ex: KumquattException) -> Unit)? = null,
        onSuccess: ((token: KumquattToken) -> Unit)? = null
    ): IMqttDeliveryToken = pahoClient.publish(message.topic ?: "kumquatt", message.mqttMessage.payload,
        qos.ordinal, retained,
        null, KumquattStatusCallback(onSuccess, onError))

    /**
     * Publishes a message to a topic.
     *
     * @param topic The topic to publish to.
     * @param message The text to be sent. Will be encoded as UTF-8 bytes.
     * @param qos The quality of service level for this message.
     * @param retained Allow the server to retain the message.
     * @param onError Optional error handler for failed publishes.
     * @param onSuccess Optional success handler for successful publishes.
     */
    fun publish(
        topic: String,
        message: String,
        qos: KumquattQos = KumquattQos.EXACTLY_ONCE,
        retained: Boolean = false,
        onError: ((ex: KumquattException) -> Unit)? = null,
        onSuccess: ((token: KumquattToken) -> Unit)? = null
    ): IMqttDeliveryToken = pahoClient.publish(topic, message.utf8Bytes(), qos.ordinal, retained, null,
            KumquattStatusCallback(onSuccess, onError))

    /**
     * Publishes a message to a topic.
     *
     * @param topic The topic to publish to.
     * @param message The object to be sent. It is converted to a json string and then encoded into UTF-8 bytes.
     * @param qos The quality of service level for this message.
     * @param retained Allow the server to retain the message.
     * @param onError Optional error handler for failed publishes.
     * @param onSuccess Optional success handler for successful publishes.
     */
    fun <T> publish(
        topic: String,
        message: T,
        qos: KumquattQos = KumquattQos.EXACTLY_ONCE,
        retained: Boolean = false,
        onError: ((ex: KumquattException) -> Unit)? = null,
        onSuccess: ((token: KumquattToken) -> Unit)? = null
    ): IMqttDeliveryToken = pahoClient.publish(topic, klaxon.toJsonString(message).utf8Bytes(), qos.ordinal, retained,
            null, KumquattStatusCallback(onSuccess, onError))

    /* Subscriptions */

    //no collect

    /**
     * Subscribe to a topic with the specified quality of service level.
     *
     * @param topic The topic to subscribe to.
     * @param qos The quality of service level for the topic.
     * @param onError Optional error handler for failed subscriptions.
     * @param onSuccess Optional success handler for successful subscriptions.
     */
    fun subscribe(
        topic: String,
        onSuccess: ((token: KumquattToken) -> Unit)? = null,
        onError: ((ex: KumquattException) -> Unit)? = null,
        qos: KumquattQos = KumquattQos.EXACTLY_ONCE,
    ) = KumquattSubscription(
        subscribe = { messageCallback, statusCallback ->
            pahoClient.subscribe(topic, qos.ordinal, null, statusCallback, messageCallback)
        }, unsubscribe = {
            pahoClient.unsubscribe(topic)
        }, onError = {
            onError?.invoke(it)
        }, onSuccess = {
            onSuccess?.invoke(it)
        }
    )

    /**
     * Subscribe to topics with a quality of service level for all topics.
     *
     * @param topics The topics to subscribe to.
     * @param qos The quality of service level for the topics.
     * @param onError Optional error handler for failed subscriptions.
     * @param onSuccess Optional success handler for successful subscriptions.
     */
    fun subscribe(
        vararg topics: String,
        onSuccess: ((token: KumquattToken) -> Unit)? = null,
        onError: ((ex: KumquattException) -> Unit)? = null,
        qos: KumquattQos = KumquattQos.EXACTLY_ONCE,
    ) = KumquattSubscription(
        subscribe = { messageCallback, statusCallback ->
            pahoClient.subscribe(topics, IntArray(topics.size){ qos.ordinal }, null, statusCallback,
                Array(topics.size){ messageCallback })
        }, unsubscribe = {
            pahoClient.unsubscribe(topics)
        }, onError = {
            onError?.invoke(it)
        }, onSuccess = {
            onSuccess?.invoke(it)
        }
    )

    /**
     * Subscribe to topics with a quality of service level specified for each individual topic.
     *
     * @param topics The topics to subscribe to.
     * @param qos The quality of service levels for each of the topics.
     * @param onError Optional error handler for failed subscriptions.
     * @param onSuccess Optional success handler for successful subscriptions.
     */
    fun subscribe(
        vararg topics: String,
        onSuccess: ((token: KumquattToken) -> Unit)? = null,
        onError: ((ex: KumquattException) -> Unit)? = null,
        qos: Array<KumquattQos>,
    ) = KumquattSubscription(
        subscribe = { messageCallback, statusCallback ->
            pahoClient.subscribe(topics, qos.map{ it.ordinal }.toTypedArray().toIntArray(), null,
                statusCallback, Array(topics.size){ messageCallback })
        }, unsubscribe = {
            pahoClient.unsubscribe(topics)
        }, onError = {
            onError?.invoke(it)
        }, onSuccess = {
            onSuccess?.invoke(it)
        }
    )

    /**
     * Subscribe to a list of topics with a single quality of service level for all topics.
     *
     * @param topics The list of topics to subscribe to.
     * @param qos The quality of service level for the topics.
     * @param onError Optional error handler for failed subscriptions.
     * @param onSuccess Optional success handler for successful subscriptions.
     */
    fun subscribe(
        topics: List<String>,
        onSuccess: ((token: KumquattToken) -> Unit)? = null,
        onError: ((ex: KumquattException) -> Unit)? = null,
        qos: KumquattQos = KumquattQos.EXACTLY_ONCE,
    ) = KumquattSubscription(
        subscribe = { messageCallback, statusCallback ->
            pahoClient.subscribe(topics.toTypedArray(), IntArray(topics.size){ qos.ordinal }, null,
                statusCallback, Array(topics.size){ messageCallback })
        }, unsubscribe = {
            pahoClient.unsubscribe(topics.toTypedArray())
        }, onError = {
            onError?.invoke(it)
        }, onSuccess = {
            onSuccess?.invoke(it)
        }
    )

    /**
     * Subscribe to a list of topics with a quality of service level specified for each individual topic.
     *
     * @param topics The list of topics to subscribe to.
     * @param qos The  levels for each of the topics.
     * @param onError Optional error handler for failed subscriptions.
     * @param onSuccess Optional success handler for successful subscriptions.
     */
    fun subscribe(
        topics: List<String>,
        onSuccess: ((token: KumquattToken) -> Unit)? = null,
        onError: ((ex: KumquattException) -> Unit)? = null,
        qos: List<KumquattQos>,
    ) = KumquattSubscription(
        subscribe = { messageCallback, statusCallback ->
            pahoClient.subscribe(topics.toTypedArray(), qos.map{ it.ordinal }.toTypedArray().toIntArray(),
                null, statusCallback, Array(topics.size){ messageCallback })
        }, unsubscribe = {
            pahoClient.unsubscribe(topics.toTypedArray())
        }, onError = {
            onError?.invoke(it)
        }, onSuccess = {
            onSuccess?.invoke(it)
        }
    )

    //without unsubscribe

    /**
     * Subscribe to a topic with the specified quality of service level.
     *
     * @param topic The topic to subscribe to.
     * @param qos The quality of service level for the topic.
     * @param onError Optional error handler for failed subscriptions.
     * @param onSuccess Optional success handler for successful subscriptions.
     * @param collect A function to handle a received message. It is provided with a single parameter: 1) the message
     * being received.
     */
    fun subscribe(
        topic: String,
        onSuccess: ((token: KumquattToken) -> Unit)? = null,
        onError: ((ex: KumquattException) -> Unit)? = null,
        qos: KumquattQos = KumquattQos.EXACTLY_ONCE,
        collect: ((message: KumquattMessage) -> Unit)
    ) = KumquattSubscription(
        subscribe = { messageCallback, statusCallback ->
            pahoClient.subscribe(topic, qos.ordinal, null, statusCallback, messageCallback)
        }, unsubscribe = {
            pahoClient.unsubscribe(topic)
        }, onError = {
            onError?.invoke(it)
        }, onSuccess = {
            onSuccess?.invoke(it)
        }
    ).collect(collect)

    /**
     * Subscribe to topics with a quality of service level for all topics.
     *
     * @param topics The topics to subscribe to.
     * @param qos The quality of service level for the topics.
     * @param onError Optional error handler for failed subscriptions.
     * @param onSuccess Optional success handler for successful subscriptions.
     * @param collect A function to handle a received message. It is provided with a single parameter: 1) the message
     * being received.
     */
    fun subscribe(
        vararg topics: String,
        onSuccess: ((token: KumquattToken) -> Unit)? = null,
        onError: ((ex: KumquattException) -> Unit)? = null,
        qos: KumquattQos = KumquattQos.EXACTLY_ONCE,
        collect: (message: KumquattMessage) -> Unit
    ) = KumquattSubscription(
        subscribe = { messageCallback, statusCallback ->
            pahoClient.subscribe(topics, IntArray(topics.size){ qos.ordinal }, null, statusCallback,
                Array(topics.size){ messageCallback })
        }, unsubscribe = {
            pahoClient.unsubscribe(topics)
        }, onError = {
            onError?.invoke(it)
        }, onSuccess = {
            onSuccess?.invoke(it)
        }
    ).collect(collect)

    /**
     * Subscribe to topics with a quality of service level specified for each individual topic.
     *
     * @param topics The topics to subscribe to.
     * @param qos The quality of service levels for each of the topics.
     * @param onError Optional error handler for failed subscriptions.
     * @param onSuccess Optional success handler for successful subscriptions.
     * @param collect A function to handle a received message. It is provided with a single parameter: 1) the message
     * being received.
     */
    fun subscribe(
        vararg topics: String,
        onSuccess: ((token: KumquattToken) -> Unit)? = null,
        onError: ((ex: KumquattException) -> Unit)? = null,
        qos: Array<KumquattQos>,
        collect: (message: KumquattMessage) -> Unit
    ) = KumquattSubscription(
        subscribe = { messageCallback, statusCallback ->
            pahoClient.subscribe(topics, qos.map{ it.ordinal }.toTypedArray().toIntArray(), null,
                statusCallback, Array(topics.size){ messageCallback })
        }, unsubscribe = {
            pahoClient.unsubscribe(topics)
        }, onError = {
            onError?.invoke(it)
        }, onSuccess = {
            onSuccess?.invoke(it)
        }
    ).collect(collect)

    /**
     * Subscribe to a list of topics with a single quality of service level for all topics.
     *
     * @param topics The list of topics to subscribe to.
     * @param qos The quality of service level for the topics.
     * @param onError Optional error handler for failed subscriptions.
     * @param onSuccess Optional success handler for successful subscriptions.
     * @param collect A function to handle a received message. It is provided with a single parameter: 1) the message
     * being received.
     */
    fun subscribe(
        topics: List<String>,
        onSuccess: ((token: KumquattToken) -> Unit)? = null,
        onError: ((ex: KumquattException) -> Unit)? = null,
        qos: KumquattQos = KumquattQos.EXACTLY_ONCE,
        collect: (message: KumquattMessage) -> Unit
    ) = KumquattSubscription(
        subscribe = { messageCallback, statusCallback ->
            pahoClient.subscribe(topics.toTypedArray(), IntArray(topics.size){ qos.ordinal }, null,
                statusCallback, Array(topics.size){ messageCallback })
        }, unsubscribe = {
            pahoClient.unsubscribe(topics.toTypedArray())
        }, onError = {
            onError?.invoke(it)
        }, onSuccess = {
            onSuccess?.invoke(it)
        }
    ).collect(collect)

    /**
     * Subscribe to a list of topics with a quality of service level specified for each individual topic.
     *
     * @param topics The list of topics to subscribe to.
     * @param qos The  levels for each of the topics.
     * @param onError Optional error handler for failed subscriptions.
     * @param onSuccess Optional success handler for successful subscriptions.
     * @param collect A function to handle a received message. It is provided with a single parameter: 1) the message
     * being received.
     */
    fun subscribe(
        topics: List<String>,
        onSuccess: ((token: KumquattToken) -> Unit)? = null,
        onError: ((ex: KumquattException) -> Unit)? = null,
        qos: List<KumquattQos>,
        collect: (message: KumquattMessage) -> Unit
    ) = KumquattSubscription(
        subscribe = { messageCallback, statusCallback ->
            pahoClient.subscribe(topics.toTypedArray(), qos.map{ it.ordinal }.toTypedArray().toIntArray(),
                null, statusCallback, Array(topics.size){ messageCallback })
        }, unsubscribe = {
            pahoClient.unsubscribe(topics.toTypedArray())
        }, onError = {
            onError?.invoke(it)
        }, onSuccess = {
            onSuccess?.invoke(it)
        }
    ).collect(collect)

    //with unsubscribe

    /**
     * Subscribe to a topic with the specified  level.
     *
     * @param topic The topic to subscribe to.
     * @param qos The  level for the topic.
     * @param onError Optional error handler for failed subscriptions.
     * @param onSuccess Optional success handler for successful subscriptions.
     * @param collect A function to handle a received message. It is provided with two parameters: 1) the message being
     * received and 2) a function that can be called to stop the current collection.
     */
    fun subscribe(
        topic: String,
        onSuccess: ((token: KumquattToken) -> Unit)? = null,
        onError: ((ex: KumquattException) -> Unit)? = null,
        qos: KumquattQos = KumquattQos.EXACTLY_ONCE,
        collect: (message: KumquattMessage, unsubscribe: () -> Unit) -> Unit
    ) = KumquattSubscription(
        subscribe = { messageCallback, statusCallback ->
            pahoClient.subscribe(topic, qos.ordinal, null, statusCallback, messageCallback)
        }, unsubscribe = {
            pahoClient.unsubscribe(topic)
        }, onError = {
            onError?.invoke(it)
        }, onSuccess = {
            onSuccess?.invoke(it)
        }
    ).collect(collect)

    /**
     * Subscribe to topics with a  level for all topics.
     *
     * @param topics The topics to subscribe to.
     * @param qos The  level for the topics.
     * @param onError Optional error handler for failed subscriptions.
     * @param onSuccess Optional success handler for successful subscriptions.
     * @param collect A function to handle a received message. It is provided with two parameters: 1) the message being
     * received and 2) a function that can be called to stop the current collection.
     */
    fun subscribe(
        vararg topics: String,
        onSuccess: ((token: KumquattToken) -> Unit)? = null,
        onError: ((ex: KumquattException) -> Unit)? = null,
        qos: KumquattQos = KumquattQos.EXACTLY_ONCE,
        collect: (message: KumquattMessage, unsubscribe: () -> Unit) -> Unit
    ) = KumquattSubscription(
        subscribe = { messageCallback, statusCallback ->
            pahoClient.subscribe(topics, IntArray(topics.size){ qos.ordinal }, null, statusCallback,
                    Array(topics.size){ messageCallback })
        }, unsubscribe = {
            pahoClient.unsubscribe(topics)
        }, onError = {
            onError?.invoke(it)
        }, onSuccess = {
            onSuccess?.invoke(it)
        }
    ).collect(collect)

    /**
     * Subscribe to topics with a  level specified for each individual topic.
     *
     * @param topics The topics to subscribe to.
     * @param qos The  levels for each of the topics.
     * @param onError Optional error handler for failed subscriptions.
     * @param onSuccess Optional success handler for successful subscriptions.
     * @param collect A function to handle a received message. It is provided with two parameters: 1) the message being
     * received and 2) a function that can be called to stop the current collection.
     */
    fun subscribe(
        vararg topics: String,
        onSuccess: ((token: KumquattToken) -> Unit)? = null,
        onError: ((ex: KumquattException) -> Unit)? = null,
        qos: Array<KumquattQos>,
        collect: (message: KumquattMessage, unsubscribe: () -> Unit) -> Unit
    ) = KumquattSubscription(
        subscribe = { messageCallback, statusCallback ->
            pahoClient.subscribe(topics, qos.map{ it.ordinal }.toTypedArray().toIntArray(), null,
                statusCallback, Array(topics.size){ messageCallback })
        }, unsubscribe = {
            pahoClient.unsubscribe(topics)
        }, onError = {
            onError?.invoke(it)
        }, onSuccess = {
            onSuccess?.invoke(it)
        }
    ).collect(collect)

    /**
     * Subscribe to a list of topics with a single  level for all topics.
     *
     * @param topics The list of topics to subscribe to.
     * @param qos The quality of service level for the topics.
     * @param onError Optional error handler for failed subscriptions.
     * @param onSuccess Optional success handler for successful subscriptions.
     * @param collect A function to handle a received message. It is provided with two parameters: 1) the message being
     * received and 2) a function that can be called to stop the current collection.
     */
    fun subscribe(
        topics: List<String>,
        onSuccess: ((token: KumquattToken) -> Unit)? = null,
        onError: ((ex: KumquattException) -> Unit)? = null,
        qos: KumquattQos = KumquattQos.EXACTLY_ONCE,
        collect: (message: KumquattMessage, unsubscribe: () -> Unit) -> Unit
    ) = KumquattSubscription(
        subscribe = { messageCallback, statusCallback ->
            pahoClient.subscribe(topics.toTypedArray(), IntArray(topics.size){ qos.ordinal }, null,
                statusCallback, Array(topics.size){ messageCallback })
        }, unsubscribe = {
            pahoClient.unsubscribe(topics.toTypedArray())
        }, onError = {
            onError?.invoke(it)
        }, onSuccess = {
            onSuccess?.invoke(it)
        }
    ).collect(collect)

    /**
     * Subscribe to a list of topics with a quality of service level specified for each individual topic.
     *
     * @param topics The list of topics to subscribe to.
     * @param qos The quality of service levels for each of the topics.
     * @param onError Optional error handler for failed subscriptions.
     * @param onSuccess Optional success handler for successful subscriptions.
     * @param collect A function to handle a received message. It is provided with two parameters: 1) the message being
     * received and 2) a function that can be called to stop the current collection.
     */
    fun subscribe(
        topics: List<String>,
        onSuccess: ((token: KumquattToken) -> Unit)? = null,
        onError: ((ex: KumquattException) -> Unit)? = null,
        qos: List<KumquattQos>,
        collect: (message: KumquattMessage, unsubscribe: () -> Unit) -> Unit
    ) = KumquattSubscription(
        subscribe = { messageCallback, statusCallback ->
            pahoClient.subscribe(topics.toTypedArray(), qos.map{ it.ordinal }.toTypedArray().toIntArray(),
                null, statusCallback, Array(topics.size){ messageCallback })
        }, unsubscribe = {
            pahoClient.unsubscribe(topics.toTypedArray())
        }, onError = {
            onError?.invoke(it)
        }, onSuccess = {
            onSuccess?.invoke(it)
        }
    ).collect(collect)

    companion object{
        val defaultOptions get() = MqttConnectOptions().apply {
            isAutomaticReconnect = true
            connectionTimeout = 10
        }

        /**
         * Creates a new options object with the specified user.
         *
         * @param user The username to use during login.
         * @param password The password to use during login.
         *
         * @return The default options with the username and password set.
         */
        fun authenticated(user: String, password: String): MqttConnectOptions = defaultOptions.apply{
            userName = user
            this.password = password.toCharArray()
        }

        /**
         * Create a Kumquatt client instance.
         *
         * @param brokerUri The uri of the broker. Must include a scheme.
         * @param options The options for the client.
         * @param clientId The id used by the client for identification by the broker.
         * @param persistence Persistence strategy to use for quality of service 1 and 2 packets.
         */
        fun withUri(
            brokerUri: String,
            options: MqttConnectOptions = defaultOptions,
            clientId: String = MqttAsyncClient.generateClientId(),
            persistence: MqttClientPersistence = MemoryPersistence()
        ): Kumquatt = try{
            Kumquatt(pahoClient = MqttAsyncClient(brokerUri, clientId, persistence), options = options)
        } catch (ex: MqttException){
            throw KumquattException(ex)
        }

        /**
         * Create a Kumquatt client instance.
         *
         * @param host The host to connect to.
         * @param port The port of the broker.
         * @param scheme The scheme for the connection, defaulting to TCP.
         * @param options The options for the client.
         * @param clientId The id used by the client for identification by the broker.
         * @param persistence Persistence strategy to use for quality of service 1 and 2 packets.
         */
        fun withHost(
            host: String,
            port: Int = 1883,
            scheme: KumquattScheme = TCP,
            options: MqttConnectOptions = defaultOptions,
            clientId: String = MqttAsyncClient.generateClientId(),
            persistence: MqttClientPersistence = MemoryPersistence()
        ): Kumquatt = try{
            val uri = "${scheme.string}://$host:$port"
            Kumquatt(pahoClient = MqttAsyncClient(uri, clientId, persistence), options = options)
        } catch (ex: MqttException){
            throw KumquattException(ex)
        }

        private val klaxon = Klaxon()
    }
}