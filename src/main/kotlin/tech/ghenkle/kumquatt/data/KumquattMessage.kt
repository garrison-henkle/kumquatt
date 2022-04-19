package tech.ghenkle.kumquatt.data

import com.beust.klaxon.Klaxon
import org.eclipse.paho.client.mqttv3.MqttMessage
import tech.ghenkle.kumquatt.core.KumquattQos
import tech.ghenkle.kumquatt.utils.utf8
import tech.ghenkle.kumquatt.utils.utf8Bytes

/**
 * Wrapper for the [MqttMessage] class with convenience methods for the creation of messages and the accessing of
 * message payloads.
 *
 * @param mqttMessage The [MqttMessage] this [KumquattMessage] wraps.
 * @param topic The topic this message is associated with. This value is not typically associated with [MqttMessage]s,
 * so it may be null if the message was received with Kumquatt.
 */
@Suppress("unused", "memberVisibilityCanBePrivate")
class KumquattMessage(val mqttMessage: MqttMessage, var topic: String? = null){
    /**
     * The MQTT id associated with the message.
     */
    val id = mqttMessage.id

    /**
     * The payload of the message in bytes.
     *
     * @see text
     * @see typedPayload
     */
    val payload: ByteArray = mqttMessage.payload

    /**
     * The payload of the message as a UTF-8 string.
     *
     * @see payload
     * @see typedPayload
     */
    val text: String = mqttMessage.payload.utf8()

    /**
     * The quality of service level of this message.
     */
    val qos = KumquattQos.of(mqttMessage.qos)

    /**
     * Indicates whether the server is or is not retaining this message.
     */
    val retained = mqttMessage.isRetained

    constructor(topic: String, payload: ByteArray)
            : this(topic = topic, payload = payload, qos = KumquattQos.EXACTLY_ONCE, retained = false)
    constructor(topic: String, payload: ByteArray, qos: KumquattQos)
            : this(topic = topic, payload = payload, qos = qos, retained = false)
    constructor(topic: String, payload: ByteArray, qos: KumquattQos, retained: Boolean) : this(
        mqttMessage = MqttMessage(payload).apply{
            setQos(qos.ordinal)
            isRetained = retained
        }, topic = topic
    )

    /**
     * Returns the decoded message payload as a string. The decoding is achieved using the default charset of the
     * system, so it is safer to use [text] to ensure a consistent charset is used.
     */
    override fun toString(): String = mqttMessage.toString()

    /**
     * Returns the message's payload as a typed object.
     *
     * This assumes the payload is a UTF-8 encoded json representation of an object, so the payload will need to be
     * manually parsed if this is not the case.
     *
     * @see payload
     * @see text
     *
     * @return The typed object representation of the message's payload.
     */
    inline fun <reified T> typedPayload(): T? = klaxon.parse(payload.utf8())

    companion object {
        val klaxon = Klaxon()

        /**
         * Creates a message from a byte payload.
         *
         * @param topic The topic the message will be sent on / has been received on.
         * @param payload The message bytes.
         * @param qos The quality of service for this message.
         * @param retained Determines whether the message will be retained by the server.
         */
        fun create(
            topic: String,
            payload: ByteArray,
            qos: KumquattQos = KumquattQos.EXACTLY_ONCE,
            retained: Boolean = false
        ) = KumquattMessage(topic = topic, payload = payload, qos = qos, retained = retained)

        /**
         * Creates a message from a string payload. The string is converted to UTF-8 bytes.
         *
         * @param topic The topic the message will be sent on / has been received on.
         * @param message The string message.
         * @param qos The quality of service for this message.
         * @param retained Determines whether the message will be retained by the server.
         */
        fun create(
            topic: String,
            message: String,
            qos: KumquattQos = KumquattQos.EXACTLY_ONCE,
            retained: Boolean = false
        ) = create(topic = topic, payload = message.utf8Bytes(), qos = qos, retained = retained)

        /**
         * Creates a message from an object payload. The object is converted to a json string then to UTF-8 bytes.
         *
         * @param topic The topic the message will be sent on / has been received on.
         * @param payload The message object.
         * @param qos The quality of service for this message.
         * @param retained Determines whether the message will be retained by the server.
         */
        fun <T> create(
            topic: String,
            payload: T,
            qos: KumquattQos = KumquattQos.EXACTLY_ONCE,
            retained: Boolean = false
        ) = create(topic = topic, payload = klaxon.toJsonString(payload).utf8Bytes(), qos = qos, retained = retained)
    }
}