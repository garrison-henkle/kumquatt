package tech.ghenkle.kumquatt

import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence
import tech.ghenkle.kumquatt.core.Kumquatt

fun main() {
    Kumquatt.withHost(host = "test.mosquitto.org", persistence = MqttDefaultFilePersistence()).connect {
        val subscription = subscribe(topic = "kumquatt")
        subscription.collect{ message ->
            println("${message.topic}: ${message.text}")
        }
        subscription.collect{ message, unsubscribe ->
            if(message.text == "unsubscribe")
                unsubscribe()
            else
                println("${message.topic}: ${message.text}")
        }

        subscribe(topic = "kumquatt"){ message ->
            println("${message.topic}: ${message.text}")
        }

        subscribe(topic = "kumquatt"){ message, unsubscribe ->
            if(message.text == "unsubscribe")
                unsubscribe()
            else
                println("${message.topic}: ${message.text}")
        }
    }


}