package tech.ghenkle.kumquatt

/**
 * The quality of service level attached to a MQTT message.
 */
enum class KumquattQos {
    /**
     * The message is delivered at most once, or it may not be delivered at all. Its delivery across the network is not
     * acknowledged. The message is not stored. The message could be lost if the client is disconnected, or if the
     * server fails. QoS0 is the fastest mode of transfer. It is sometimes called "fire and forget".
     *
     * Description taken from eclipse paho documentation found
     * <a href="https://www.eclipse.org/paho/files/mqttdoc/MQTTClient/html/qos.html">here</a>
     */
    AT_MOST_ONCE,

    /**
     * The message is always delivered at least once. It might be delivered multiple times if there is a failure before
     * an acknowledgment is received by the sender. The message must be stored locally at the sender, until the sender
     * receives confirmation that the message has been published by the receiver. The message is stored in case the
     * message must be sent again.
     *
     * Description taken from eclipse paho documentation found
     * <a href="https://www.eclipse.org/paho/files/mqttdoc/MQTTClient/html/qos.html">here</a>
     */
    AT_LEAST_ONCE,

    /**
     * The message is always delivered exactly once. The message must be stored locally at the sender, until the sender
     * receives confirmation that the message has been published by the receiver. The message is stored in case the
     * message must be sent again. QoS2 is the safest, but slowest mode of transfer. A more sophisticated handshaking
     * and acknowledgement sequence is used than for QoS1 to ensure no duplication of messages occurs.
     *
     * Description taken from eclipse paho documentation found
     * <a href="https://www.eclipse.org/paho/files/mqttdoc/MQTTClient/html/qos.html">here</a>
     */
    EXACTLY_ONCE;

    companion object{
        private val cachedValues = values()

        /**
         * Returns the [KumquattQos] with the ordinal matching the provided [ordinal] value. If the value is out of
         * bounds, it returns [EXACTLY_ONCE].
         *
         * @param ordinal The ordinal of the [KumquattQos].
         *
         * @return the [KumquattQos] corresponding to the ordinal.
         */
        fun of(ordinal: Int): KumquattQos = if(ordinal in 0..2) cachedValues[ordinal] else EXACTLY_ONCE
    }
}