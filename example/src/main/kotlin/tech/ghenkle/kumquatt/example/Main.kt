package tech.ghenkle.kumquatt.example

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence
import tech.ghenkle.kumquatt.Kumquatt

//note: if you try to run this and someone else is running it at the same time, it will not result in the right response
fun main() {
    val scope = CoroutineScope(Dispatchers.Default)
    val topic = "kumquatt"
    val message1 = "This was sent before anyone was listening, but they'll still get it because it was retained"
    val message2 = "This is a normal message"

    Kumquatt.withHost(host = "test.mosquitto.org", persistence = MqttDefaultFilePersistence()).connect {
        publish(
            topic = topic,
            message = message1,
            retained = true
        )
        println("Sent: $message1")

        scope.launch {
            delay(2_000)

            subscribe(topic){ message ->
                println("${message.topic}: ${message.text}")
            }
            println("Started listening")
        }

        scope.launch {
            delay(4_000)

            publish(
                topic = topic,
                message = message2,
            )
            println("Sent: $message2")
        }

        scope.launch {
            delay(8_000)

            println("Disconnecting. If you don't disconnect, the lock files Paho creates will not be cleaned up.")
            disconnectAndClose()

        }
    }
}