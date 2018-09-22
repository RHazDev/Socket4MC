package fr.rhaz.minecraft

import fr.rhaz.sockets.*
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.config.Configuration
import net.md_5.bungee.config.ConfigurationProvider
import net.md_5.bungee.config.YamlConfiguration as BungeeYaml
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.HandlerList
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.logging.Logger
import net.md_5.bungee.api.plugin.Event as BungeeEvent
import net.md_5.bungee.api.plugin.Plugin as BungeePlugin
import org.bukkit.event.Event as BukkitEvent
import org.bukkit.plugin.java.JavaPlugin as BukkitPlugin
import org.spongepowered.api.plugin.Plugin as SpongePlugin

open class Sockets4Bukkit: BukkitPlugin() {
    val sockets = Sockets4MC()
    override fun onEnable() = sockets.bukkit(this)
    override fun onDisable() = sockets.disable()
}

open class Sockets4Bungee: BungeePlugin() {
    val sockets = Sockets4MC()
    override fun onEnable() = sockets.bungee(this)
    override fun onDisable() = sockets.disable()
}

class Sockets4MC(){
    lateinit var plugin: Any
    lateinit var environment: String
    var logger: Logger = Logger.getGlobal()
    var dataFolder = File(".")
    var debug = false

    val sockets = mutableMapOf<String, Any>()
    fun disable() = sockets.values.forEach {
        when(it){
            is SocketClient -> it.interrupt()
            is SocketServer -> it.close()
        }
        sockets.clear()
    }

    fun bungee(plugin: Sockets4Bungee){
        this.plugin = plugin
        environment = "bungee"
        logger = plugin.logger
        dataFolder = plugin.dataFolder
        val file = File(dataFolder, "config.yml")
        plugin.load(file)?.apply {
            debug = getBoolean("debug", false)
            for(key in keys) {
                if(key == "debug") continue
                getSection(key).apply {
                    val name = getString("name", "MyProxy")
                    val port = getInt("port", 25598)
                    val password = getString("password", "mypassword")
                    logger.info("Starting $name ($key)...")
                    when(getString("type", "server").lc){
                        "server" -> SocketServer(server, name, port, password).apply {
                            config.buffer = getInt("buffer", 100)
                            config.timeout = getLong("timeout", 1000)
                            config.security = when(getString("security", "aes")){
                                "none" -> 0; "aes" -> 1; "rsa" -> 2
                                else -> logger.info(
                                    "Unknown security for $name." +
                                    "Accepted: none, aes, rsa"
                                ).let{1}
                            }
                            sockets[key] = this
                            start()
                            logger.info("Successfully started server $name (#$key)")
                        }
                        "client" -> {
                            val host = getString("host", "localhost").lc
                            SocketClient(client, name, host, port, password).apply {
                                config.buffer = getInt("buffer", 100)
                                config.timeout = getLong("timeout", 1000)
                                sockets[key] = this
                                start()
                                logger.info("Successfully started client $name (#$key)")
                            }
                        }
                    }
                }
            }
            logger.info("Successfully enabled Sockets4MC")
        } ?: logger.warning("Could not load configuration")
    }

    fun bukkit(plugin: Sockets4Bukkit){
        this.plugin = plugin
        environment = "bukkit"
        logger = plugin.logger
        dataFolder = plugin.dataFolder
        val file = File(dataFolder, "config.yml")
        plugin.load(file)?.apply {
            debug = getBoolean("debug", false)
            for (key in getKeys(false)) {
                if(key == "debug") continue
                getConfigurationSection(key).apply {
                    val name = getString("name", "MyBukkit")
                    val port = getInt("port", 25598)
                    val password = getString("password", "mypassword")
                    logger.info("Starting $name ($key)...")
                    when(getString("type", "server").lc){
                        "server" -> SocketServer(server, name, port, password).apply {
                            config.buffer = getInt("buffer", 100)
                            config.timeout = getLong("timeout", 1000)
                            config.security = when(getString("security", "aes")){
                                "none" -> 0; "aes" -> 1; "rsa" -> 2
                                else -> logger.info(
                                    "Unknown security for $name." +
                                    "Accepted: none, aes, rsa"
                                ).let{1}
                            }
                            sockets[key] = this
                            start()
                            logger.info("Successfully started server $name (#$key)")
                        }
                        "client" -> {
                            val host = getString("host", "localhost").lc
                            SocketClient(client, name, host, port, password).apply {
                                config.buffer = getInt("buffer", 100)
                                config.timeout = getLong("timeout", 1000)
                                sockets[key] = this
                                start()
                                logger.info("Successfully started client $name (#$key)")
                            }
                        }
                    }
                }
            }
            logger.info("Successfully enabled Sockets4MC")
        } ?: logger.warning("Could not load configuration")
    }

    val server = object: SocketApp.Server(){
        override fun log(err: String) = logger.info(err)
        override fun onMessage(mess: SocketMessenger, map: JSONMap) {
            when(environment.lc) {
                "bukkit" -> SocketEvent.Bukkit.Server.Message().apply {
                    messenger = mess
                    message = map
                    channel = map.getExtra<String>("channel") ?: "unknown"
                    Bukkit.getPluginManager().callEvent(this)
                }
                "bungee" -> SocketEvent.Bungee.Server.Message().apply {
                    messenger = mess
                    message = map
                    channel = map.getExtra<String>("channel") ?: "unknown"
                    ProxyServer.getInstance().pluginManager.callEvent(this)
                }
            }
        }

        override fun onConnect(mess: SocketMessenger){
            when(environment.lc) {
                "bukkit" -> SocketEvent.Bukkit.Server.Connected().apply {
                    messenger = mess
                    Bukkit.getPluginManager().callEvent(this)
                }
                "bungee" -> SocketEvent.Bungee.Server.Connected().apply {
                    messenger = mess
                    ProxyServer.getInstance().pluginManager.callEvent(this)
                }
            }
        }

        override fun onHandshake(mess: SocketMessenger, name: String){
            when(environment.lc) {
                "bukkit" -> SocketEvent.Bukkit.Server.Handshake().apply {
                    messenger = mess
                    Bukkit.getPluginManager().callEvent(this)
                }
                "bungee" -> SocketEvent.Bungee.Server.Handshake().apply {
                    messenger = mess
                    ProxyServer.getInstance().pluginManager.callEvent(this)
                }
            }
        }

        override fun onDisconnect(mess: SocketMessenger){
            when(environment.lc) {
                "bukkit" -> SocketEvent.Bukkit.Server.Disconnected().apply {
                    messenger = mess
                    Bukkit.getPluginManager().callEvent(this)
                }
                "bungee" -> SocketEvent.Bungee.Server.Disconnected().apply {
                    messenger = mess
                    ProxyServer.getInstance().pluginManager.callEvent(this)
                }
            }
        }
    }

    val client = object: SocketApp.Client(){
        override fun log(err: String) = logger.info(err)
        override fun onMessage(client: SocketClient, map: JSONMap) {
            when(environment.lc) {
                "bukkit" -> SocketEvent.Bukkit.Client.Message().apply {
                    this.client = client
                    message = map
                    channel = map.getExtra<String>("channel") ?: "unknown"
                    Bukkit.getPluginManager().callEvent(this)
                }
                "bungee" -> SocketEvent.Bungee.Client.Message().apply {
                    this.client = client
                    message = map
                    channel = map.getExtra<String>("channel") ?: "unknown"
                    ProxyServer.getInstance().pluginManager.callEvent(this)
                }
            }
        }

        override fun onConnect(client: SocketClient){
            when(environment.lc) {
                "bukkit" -> SocketEvent.Bukkit.Client.Connected().apply {
                    this.client = client
                    Bukkit.getPluginManager().callEvent(this)
                }
                "bungee" -> SocketEvent.Bungee.Client.Connected().apply {
                    this.client = client
                    ProxyServer.getInstance().pluginManager.callEvent(this)
                }
            }
        }

        override fun onHandshake(client: SocketClient){
            when(environment.lc) {
                "bukkit" -> SocketEvent.Bukkit.Client.Handshake().apply {
                    this.client = client
                    Bukkit.getPluginManager().callEvent(this)
                }
                "bungee" -> SocketEvent.Bungee.Client.Handshake().apply {
                    this.client = client
                    ProxyServer.getInstance().pluginManager.callEvent(this)
                }
            }
        }

        override fun onDisconnect(client: SocketClient){
            when(environment.lc) {
                "bukkit" -> SocketEvent.Bukkit.Client.Disconnected().apply {
                    this.client = client
                    Bukkit.getPluginManager().callEvent(this)
                }
                "bungee" -> SocketEvent.Bungee.Client.Disconnected().apply {
                    this.client = client
                    ProxyServer.getInstance().pluginManager.callEvent(this)
                }
            }
        }
    }

}

interface SocketEvent {
    enum class Side{ server, client }
    enum class Type{ message, connected, disconnected, handshake }
    val side: Side ; val type: Type

    open class Bukkit(
        override val side: Side, override val type: Type
    ): SocketEvent, BukkitEvent() {

        open class Server(override val type: Type): Bukkit(Side.server, type) {
            open class Message: Server(Type.message){
                lateinit var messenger: SocketMessenger
                lateinit var message: JSONMap
                lateinit var channel: String
            }
            open class Connected: Server(Type.connected){
                lateinit var messenger: SocketMessenger
            }
            open class Disconnected: Server(Type.disconnected){
                lateinit var messenger: SocketMessenger
            }
            open class Handshake: Server(Type.handshake){
                lateinit var messenger: SocketMessenger
            }
        }

        open class Client(override val type: Type): Bukkit(Side.client, type){
            open class Message: Client(Type.message){
                lateinit var client: SocketClient
                lateinit var message: JSONMap
                lateinit var channel: String
            }
            open class Connected: Client(Type.connected){
                lateinit var client: SocketClient
            }
            open class Disconnected: Client(Type.disconnected){
                lateinit var client: SocketClient
            }
            open class Handshake: Client(Type.handshake){
                lateinit var client: SocketClient
            }
        }

        val _handlers = HandlerList()
        override fun getHandlers() = _handlers
    }

    open class Bungee(
        override val side: Side, override val type: Type
    ): SocketEvent, BungeeEvent(){

        open class Server(override val type: Type): Bungee(Side.server, type){
            open class Message: Server(Type.message){
                lateinit var messenger: SocketMessenger
                lateinit var message: JSONMap
                lateinit var channel: String
            }
            open class Connected: Server(Type.connected){
                lateinit var messenger: SocketMessenger
            }
            open class Disconnected: Server(Type.disconnected){
                lateinit var messenger: SocketMessenger
            }
            open class Handshake: Server(Type.handshake){
                lateinit var messenger: SocketMessenger
            }
        }

        open class Client(override val type: Type): Bungee(Side.client, type){
            open class Message: Client(Type.message){
                lateinit var client: SocketClient
                lateinit var message: JSONMap
                lateinit var channel: String
            }
            open class Connected: Client(Type.connected){
                lateinit var client: SocketClient
            }
            open class Disconnected: Client(Type.disconnected){
                lateinit var client: SocketClient
            }
            open class Handshake: Client(Type.handshake){
                lateinit var client: SocketClient
            }
        }
    }
}

val BungeePlugin.provider get() = ConfigurationProvider.getProvider(BungeeYaml::class.java)
fun BungeePlugin.load(file: File) = try {
    if (!dataFolder.exists()) dataFolder.mkdir()
    val res = file.nameWithoutExtension+"/bungee.yml"
    if (!file.exists()) Files.copy(getResourceAsStream(res), file.toPath())
    provider.load(file)
} catch (e: IOException){ e.printStackTrace(); null }
fun BungeePlugin.save(config: Configuration, file: File) = provider.save(config, file)

fun BukkitPlugin.load(file: File): YamlConfiguration? {
    if (!file.parentFile.exists()) file.parentFile.mkdir()
    val res = file.nameWithoutExtension+"/bukkit.yml"
    if (!file.exists()) Files.copy(getResource(res), file.toPath())
    return YamlConfiguration.loadConfiguration(file) ?: null;
}

val String.lc get() = toLowerCase()