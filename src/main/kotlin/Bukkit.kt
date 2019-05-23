package hazae41.minecraft.sockets.bukkit

import hazae41.minecraft.kotlin.bukkit.*
import hazae41.minecraft.kotlin.textOf
import hazae41.minecraft.sockets.Sockets
import hazae41.minecraft.sockets.Sockets.onSocketEnable
import hazae41.minecraft.sockets.Sockets.sockets
import hazae41.sockets.*
import io.ktor.http.cio.websocket.send
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND
import java.util.concurrent.TimeUnit

class Plugin : BukkitPlugin() {

    override fun onEnable() {
        update(15938)

        init(Config)

        Config.sockets.forEach { start(it) }

        command("sockets", permission = "sockets.list"){ args ->
            msg("Available sockets:")
            sockets.keys.forEachIndexed { i, key ->
                if(i != 0) msg(", ")
                textOf(key){
                    clickEvent = ClickEvent(RUN_COMMAND, "/socket $key")
                    msg(this)
                }
            }
        }

        command("socket", permission = "sockets.info"){ args ->

            val name = args.getOrNull(0)
            ?: return@command msg("/socket <name> | /socket <name> key")

            val socket = Sockets.sockets[name]
            ?: return@command msg("Unknown socket")

            msg("Available connections for $name:")
            msg(socket.connections.keys.joinToString(", "))
        }

        if(Config.test){
            onSocketEnable {
                onConversation("/test"){
                    send("it works!")
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

    class Socket(config: ConfigSection, name: String): ConfigSection(config, name){
        val port by int("port")

        val ConnectionsConfig = ConfigSection(this, "connections")
        val connections get() = ConnectionsConfig.config.keys.map {
                name -> Connection(ConnectionsConfig, name)
        }

        inner class Connection(config: ConfigSection, name: String): ConfigSection(config, name){
            val host by string("host")
            val port by int("port")
        }
    }
}


fun Plugin.start(config: Config.Socket) {
    val socket = Socket(config.port)
    sockets[config.path] = socket

    config.connections.forEach {
        config ->socket.connectTo(config.path, config.host, config.port)
    }

    schedule(delay = 0, unit = TimeUnit.SECONDS) {
        Sockets.socketsNotifiers.forEach { it(socket, config.path) }
        socket.start(dataFolder)
        info("Started ${config.path} on port ${config.port}")
    }
}
