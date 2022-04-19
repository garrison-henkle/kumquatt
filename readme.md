# Kumquatt

Kumquatt is a Kotlin MQTT client library that wraps the Eclipse Paho Java client. Kumquatt takes advantage of Kotlin's
coroutines and concise syntax to greatly simplify Paho's usage. Take the following subscription operation in Paho:

```
val client = MqttAsyncClient("tcp://test.mosquitto.org", MqttAsyncClient.generateClientId(), MemoryPersistence())
val messageListener = IMqttMessageListener { topic, message ->
   println("$topic: ${message.payload.toString(StandardCharsets.UTF_8)}")
}
val statusListener = object : IMqttActionListener{
   val QOS_EXACTLY_ONCE = 2
   override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {}
   override fun onSuccess(asyncActionToken: IMqttToken) {
      client.subscribe("kumquatt", QOS_EXACTLY_ONCE, messageListener)
   }
}
client.connect(null, statusListener)
```

Compare this to the exact same operation in Kumquatt:

```
Kumquatt.withHost(host = "test.mosquitto.org").connect {
   subscribe(topic = "kumquatt", qos = KumquattQos.EXACTLY_ONCE){ message ->
      println("${message.topic}: ${message.text}")
   }
}
```

Kumquatt replaces Paho's callback-heavy syntax with a concise DSL. It also improves upon subscribing by exposing a
cold data stream for receiving messages.

# Usage

## Creating a client

Kumquatt exposes two functions that can be used to connect to a broker: `withHost` and `withUri`. `withUri` takes a URI
in the format `scheme://host` or `scheme://host:port`. Currently, the Paho client only supports two schemes: TCP
and SSL. `withHost` is essentially a wrapper around `withUri`, so the uri can be built using the `host`, `port`, and
`scheme` parameters. As a result, the following are identical:

```
Kumquatt.withHost(host = "test.mosquitto.org", port = 1883)
Kumquatt.withUri(brokerUri = "tcp://test.mosquitto.org:1883")
```

Note that `withHost` defaults to TCP by default.

Both functions take an `MqttConnectOptions` that can be used to set additional settings, such as usernames, passwords,
clean sessions, and automatic reconnecting. The `defaultOptions` variable in the companion of `Kumquatt` contains a set
of default settings. These include the Paho defaults in addition to automatic reconnecting and a 10s connection timeout.

A persistence strategy can also be provided. This defaults to `MemoryPersistence`, but can alternatively be set to
`MqttDefaultFilePersistence`. The persistence strategy is used for storing messages that need to be acknowledged, so
using the file variant will allow for messages to be persisted through a possible client shutdown.

## Connections

### Connect

Connecting a client to the broker is done with the `connect` method:

```
client.connect()
```

`onError` and `onSuccess` lambdas can be used to handle failed and successful connections. The `onSuccess` lambda is
especially useful because it provides the client as a receiver, enabling code using the client to only execute if the
connection is successful:

```
client.connect{
   subscribe(...){
      ...
   }
   publish(...)
}
```

### Disconnect

The client can be disconnected from the server gracefully (with quiesce time) or forcefully:

```
client.disconnect() //defaults to a 30s timeout
client.disconnect(timeout = 5_000L)
client.forceDisconnect()
```

### Close

The Paho client only allows for the client to be closed if all connections have been closed. For that reason, it is
recommended that the client be closed using the `disconnectAndClose` method:

```
client.disconnectAndClose()
```

This method will try a graceful disconnect (`disconnect`) and fallback to a forceful disconnect (`forceDisconnect`) in
case of failure. It will close the client regardless of which disconnect method succeeded.

If the connections are already closed, then the following can be used:

```
client.close()
```

## Publishing

String or byte array messages can be directly passed to the `publish` method. Strings are converted to UTF-8 encoded
bytes:

```
client.publish(topic = "kumquatt", message = "Hello World!")
```

Alternatively, a `KumquattMessage` can be created with the various `create` methods and passed directly to `publish`:

``` 
val message = KumquattMessage.create(topic = "kumquatt", message = "Hello World!")
client.publish(message = message)
```

The final overload of `publish` allows any object to be passed as a message. Objects will be converted to json strings
and encoded as UTF-8 byte arrays:

```
data class HelloWorld(val message: String = "Hello World!")
client.publish(topic = "kumquatt", message = HelloWorld())
```

All the publishing methods can be provided with a few additional parameters, such as the quality of service level
(`qos`), whether the message should be retained on the server (`retained`), and `onError` / `onSuccess` handler lambdas.

## Subscribing

A majority of the improvements over Paho are found in subscription methods. Rather than just return Paho's token,
Kumquatt subscriptions return a `KumquattSubscription`. `KumquattSubscription`s are cold data streams that can be used
to access any incoming messages associated with a subscription. These are creating by using one of the several
`subscribe` methods. All the methods take one or more topics and one or more quality of service levels. The different
combinations are:

| Topics (topic or topics) | Quality of Service (qos) |
|--------------------------|--------------------------|
| String                   | KumquattQos              |
| vararg String            | KumquattQos              |
| vararg String            | Array&lt;KumquattQos>    |
| List&lt;String>          | KumquattQos              |
| List&lt;String>          | List&lt;KumquattQos>     |

For `subscribe` methods with multiple topics, either the same quality of service can be used for all topics
(`KumquattQos` variant), or an individual quality of service can be set for each topic (using the List or Array variants).

For handling successful or failed connections, the `onSuccess` and `onError` parameters can be used to pass handler
lambdas.

An optional `collect` parameter can also be used as a shortcut to access one of the two `KumquattSubscription.collect`
methods that will be discussed in the next section.

A few simple examples:

```
subscribe(topic = "kumquatt"){ message ->
    println("${message.topic}: ${message.text}")
}

subscribe(
    topic = "kumquatt",
    qos = EXACTLY_ONCE,
    onSuccess = { println("successfully subscribed") },
    onError = { error("failed to subscribe") }
){ message, unsubscribe ->
    if(message.text == "end")
        unsubscribe()
    else
        println("${message.topic}: ${message.text}")
}

subscribe("kumquatt", "mqtt", "paho", qos = arrayOf(AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE)){ message ->
    println("${message.topic}: ${message.text}")
}

subscribe(topics = listOf("kumquatt", "mqtt", "paho"), qos = listOf(AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE)){ message ->
    println("${message.topic}: ${message.text}")
}
```

### KumquattSubscription

The `KumquattSubscription` is a cold data stream for receiving messages on a topic. Since it is cold, it must be
observed using one of the two `collect` methods for messages to be received. `collect` takes a handler lambda that is
passed a `KumquattMessage` and, optionally, an `unsubscribe` lambda that can be used to stop the observing the stream.

To use either of the variants, the parameters of the lambdas must be *explicitly* named to avoid overload resolution
ambiguity. This was an intentional design decision, as it makes the code far more readable. By convention, these
parameters should be named `message` and `unsubscribe`. This is just a convention, however, so you may choose to name
them something else.

Examples of each variant being used:

```
val subscription = subscribe(topic = "kumquatt")

//message only
subscription.collect{ message ->
    println("${message.topic}: ${message.text}")
}

//message and unsubscribe
subscription.collect{ message, unsubscribe ->
    if(message.text == "unsubscribe")
        unsubscribe()
    else
        println("${message.topic}: ${message.text}")
}

//shortcut with only message
subscribe(topic = "kumquatt"){ message ->
    println("${message.topic}: ${message.text}")
}

//shortcut with message and unsubscribe
subscribe(topic = "kumquatt"){ message, unsubscribe ->
    if(message.text == "unsubscribe")
        unsubscribe()
    else
        println("${message.topic}: ${message.text}")
}
```

### KumquattMessage

All `collect` methods return messages as `KumquattMessage`s. `KumquattMessage` is a wrapper class around
Paho's `MqttMessage` class. It is essentially identical, but contains a few upgrades:
- The `topic` variable can be used to access the topic associated with the message, if any exists.
- The `qos` variable returns an enumeration (`KumquattQos`) instead of an integer.
- Several shortcuts are included for handling payloads:
  - `payload` - the raw byte array payload
  - `text` - the UTF-8 string representation of the payload
  - `typedPayload()` - function to convert the payload into an object. This is achieved by converting the payload bytes
    into a UTF-8 encoded json string and parsing the string as the passed type parameter.

# Miscellaneous Definitions

Confused by any of the definitions above? Here's a quick run down of some important MQTT concepts (taken from the
Paho documentation):

### Clean Sessions

Whether a session is clean determines if the client and server should remember state across restarts and reconnects.

 - If set to false, both the client and server will maintain state across restarts of the client, the server, and the
   connection. As state is maintained:
   - Message delivery will be reliable meeting the specified QOS even if the client, server, or connection are restarted.
          The server will treat a subscription as durable.
 - If set to true, the client and server will not maintain state across restarts of the client, the server, or the
   connection. This means:
   - Message delivery to the specified QOS cannot be maintained if the client, server, or connection are restarted
          The server will treat a subscription as non-durable 

### Quality of Service (QOS)

The MQTT protocol provides three qualities of service for delivering messages between clients and servers.

 - Level 0: "At most once"
   - The message is delivered at most once, or it may not be delivered at all. Its delivery across the network is not
     acknowledged. The message is not stored. The message could be lost if the client is disconnected, or if the server
     fails. Level 0 is the fastest mode of transfer. It is sometimes called "fire and forget". The MQTT protocol does
     not require servers to forward publications at level 0 to a client. If the client is disconnected at the time the
     server receives the publication, the publication might be discarded, depending on the server implementation.

 - Level 1: "At least once"
   - The message is always delivered at least once. It might be delivered multiple times if there is a failure before
     an acknowledgment is received by the sender. The message must be stored locally at the sender, until the sender
     receives confirmation that the message has been published by the receiver. The message is stored in case the
     message must be sent again.

 - Level 2: "Exactly once"
   - The message is always delivered exactly once. The message must be stored locally at the sender, until the sender
     receives confirmation that the message has been published by the receiver. The message is stored in case the
     message must be sent again. Level 2 is the safest but slowest mode of transfer. A more sophisticated handshaking
     and acknowledgement sequence is used than for level 1 to ensure no duplication of messages occurs.

# FAQ

1. Why is it called Kumquatt?

     - It's a Kotlin library, so obviously the first thing to do is slap a K on the front. K + MQTT = KMQTT. Try
       sounding that out.


2. Are all of Paho's features implemented in Kumquatt?

    - No, there are several features of Paho that are not implemented. For example, Paho's synchronous client is not
      currently implemented. These may or may not be added at a later date, but will likely remain unimplemented.

# License

```
Copyright 2022 Garrison Henkle

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```