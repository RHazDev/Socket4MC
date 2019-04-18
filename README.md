# mc-multisockets

**WebSockets multiplexing for Minecraft (Spigot & BungeeCord)**

[**Download latest stable version**](https://jitpack.io/com/github/hazae41/mc-multisockets/master-SNAPSHOT/mc-multisockets-master-SNAPSHOT-bundle.jar)

[**All versions**](https://github.com/hazae41/mc-multisockets/releases)

[**Go to JitPack**](https://jitpack.io/#hazae41/mc-multisockets/master-SNAPSHOT)

By default, all routes are public and unencrypted

But you can use AES to encrypt them and a password verification to restrict them

**You must define routes in onSocketEnable{}**

#### Configuration

```yaml
sockets:
  "mysocket":
    port: 25590
    connections:
      "factions":
        host: localhost
        port: 25591
      "hub":
        host: localhost
        port: 25592
```

#### Usage

```kotlin
// When any socket is started
onSocketEnable { name ->
    println("Enabled socket $name on port $port")
    
    // You must define all routes here

    // Create route to /MyPlugin/test
    onConversation("/MyPlugin/test"){
        // Send unencrypted message
        send("it works!")
        // Wait until a message is received
        println(readMessage())
        // Do it as many times as you want
        send("it still works!")
        println(readMessage())
    }
    
    // Start a conversation to /MyPlugin/hello
    conversation("/MyPlugin/hello") {
        // Use AES encryption
        val (encrypt, decrypt) = aes()
        // Send encrypted message
        send("hello world!".encrypt())
        // Wait until a message is received and decrypt it
        println(readMessage().decrypt())
    }
    
    // Short function equivalent to conversation { readMessage() }
    request("/Factions/factions"){
        // Just print the first message received and close the conversation
        result -> println(result)
    }
}
```