package hazae41.minecraft.sockets.bungee

import hazae41.minecraft.kotlin.bungee.*
import hazae41.minecraft.sockets.Sockets
import hazae41.minecraft.sockets.Sockets.onSocketEnable
import hazae41.minecraft.sockets.Sockets.sockets
import hazae41.minecraft.sockets.Sockets.socketsNotifiers
import hazae41.sockets.*
import io.ktor.http.cio.websocket.send
import java.util.concurrent.TimeUnit.SECONDS
import javax.crypto.SecretKey

class Plugin : BungeePlugin(){

    override fun onEnable() {
        update(15938)

        init(Config)

        Config.sockets.forEach { start(it) }

        command("sockets", permission = "sockets.list"){ args ->
            msg("Available sockets:")
            msg(sockets.keys.joinToString(", "))
        }

        command("socket", permission = "sockets.info"){ args ->

            val name = args.getOrNull(0)
            ?: return@command msg("/socket <name> | /socket <name> key")

            val socket = sockets[name]
            ?: return@command msg("Unknown socket")

            msg("Available connections for $name:")
            msg(socket.connections.keys.joinToString(", "))
            msg("Use '/socket $name key' to see the secret key")
        }

        if(Config.test){
            command("test"){ args ->
                val (socketName, connectionName) = args
               
                val socket = sockets[socketName]
                ?: return@command msg("Unknown socket")

                val connection = socket.connections[connectionName]
                ?: return@command msg("Unknown connection")
                            
                connection.conversation("/test"){
                    val (_, decrypt) = aes()
                    println(readMessage().decrypt())
                }
            }
            
            command("hello"){ args ->
                sockets.forEach{ name, socket ->
                    socket.connections.forEach{ _, connection ->
                        connection.conversation("/test/hello"){
                            send("hello from $name")
                        }
                    }
                }
            }

            onSocketEnable { name ->
                onConversation("/test"){
                    val (encrypt) = aes()
                    send("it works!".encrypt())
                }

                onConversation("/test/hello"){
                    println(readMessage())
                    send("hello back from $name")
                }
            }
        }
    }
}

object Config: ConfigFile("config"){
    val test by boolean("test")

    val Sockets = ConfigSection(this, "sockets")
    val sockets get() = Sockets.config.keys.map {
        name -> Socket(Sockets, name)
    }

    class Socket(config: ConfigSection, path: String): ConfigSection(config, path){
        val port by int("port")

        val ConnectionsConfig = ConfigSection(this, "connections")
        val connections get() = ConnectionsConfig.config.keys.map {
            name -> Connection(ConnectionsConfig, name)
        }

        inner class Connection(config: ConfigSection, path: String): ConfigSection(config, path){
            val host by string("host")
            val port by int("port")
        }
    }
}

fun String.aes(): SecretKey {
    if(isBlank()) return AES.generate()
    return AES.toKey(this)
}

fun Plugin.start(config: Config.Socket) {
    val socket = Socket(config.port)
    Sockets.sockets[config.path] = socket

    config.connections.forEach {
        config -> socket.connectTo(config.path, config.host, config.port)
    }

    schedule(delay = 0, unit = SECONDS) {
        socketsNotifiers.forEach { it(socket, config.path) }
        socket.start()
        info("Started ${config.path}")
    }
}
