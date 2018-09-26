package fr.rhaz.minecraft.sockets

import BukkitEvent
import TriConsumer
import com.google.gson.JsonParser
import fr.rhaz.sockets.*
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.ChatColor.LIGHT_PURPLE
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.event.PostLoginEvent
import net.md_5.bungee.config.Configuration
import net.md_5.bungee.config.ConfigurationProvider
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.player.PlayerJoinEvent
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.logging.Logger
import net.md_5.bungee.api.plugin.Event as BungeeEvent
import net.md_5.bungee.api.plugin.Listener as BungeeListener
import net.md_5.bungee.api.plugin.Plugin as BungeePlugin
import net.md_5.bungee.config.YamlConfiguration as BungeeYaml
import net.md_5.bungee.event.EventHandler as BungeeEventHandler
import org.bukkit.event.EventHandler as BukkitEventHandler
import org.bukkit.event.Listener as BukkitListener
import org.bukkit.plugin.java.JavaPlugin as BukkitPlugin

open class Sockets4Bukkit: BukkitPlugin() {
    override fun onEnable() { server.scheduler.runTask(this){Sockets4MC.bukkit(this)} }
    override fun onDisable() = Sockets4MC.disable()
}

open class Sockets4Bungee: BungeePlugin() {
    override fun onEnable() { proxy.scheduler.schedule(this, {Sockets4MC.bungee(this)}, 8, TimeUnit.SECONDS) }
    override fun onDisable() = Sockets4MC.disable()
}

object Sockets4MC {
    lateinit var plugin: Any
    lateinit var environment: String
    var logger: Logger = Logger.getGlobal()
    var dataFolder = File(".")
    var debug = false

    val sockets = mutableMapOf<String, SocketHandler>()
    fun disable() = sockets.values.forEach {
        when(it){
            is SocketClient -> it.interrupt()
            is SocketServer -> it.close()
        }
        sockets.clear()
    }

    fun bungee(plugin: Sockets4Bungee){

        this.plugin = plugin
        plugin.update(15938, LIGHT_PURPLE)
        environment = "bungee"
        logger = plugin.logger
        dataFolder = plugin.dataFolder

        val file = File(dataFolder, "config.yml")
        plugin.load(file)?.apply {

            debug = getBoolean("debug", false)
            for(key in keys) {
                if(key == "debug") continue

                getSection(key).apply section@{

                    val enabled = getBoolean("enabled", true)
                    if(!enabled) return@section
                    val name = getString("name", "MyProxy")
                    val port = getInt("port", 25598)
                    val password = getString("password", "mypassword")

                    logger.info("Starting $name (#$key)...")
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
                            SocketEvent.Bungee.Server.Enabled().also {
                                it.socket = this
                                it.key = key
                                ProxyServer.getInstance().pluginManager.callEvent(it)
                            }
                        }

                        "client" -> {
                            val host = getString("host", "localhost").lc
                            SocketClient(client, name, host, port, password).apply {
                                config.buffer = getInt("buffer", 100)
                                config.timeout = getLong("timeout", 1000)
                                sockets[key] = this
                                start()
                                logger.info("Successfully started client $name (#$key)")
                                SocketEvent.Bungee.Client.Enabled().also {
                                    it.socket = this
                                    it.key = key
                                    ProxyServer.getInstance().pluginManager.callEvent(it)
                                }
                            }
                        }
                    }
                }
            }
        } ?: logger.warning("Could not load configuration")
    }

    fun bukkit(plugin: Sockets4Bukkit){

        this.plugin = plugin
        plugin.update(15938, LIGHT_PURPLE)
        environment = "bukkit"
        logger = plugin.logger
        dataFolder = plugin.dataFolder

        val file = File(dataFolder, "config.yml")
        plugin.load(file)?.apply {
            debug = getBoolean("debug", false)

            for (key in getKeys(false)) {
                if(key == "debug") continue

                getConfigurationSection(key).apply section@{

                    val enabled = getBoolean("enabled", true)
                    if(!enabled) return@section
                    val name = getString("name", "MyBukkit")
                    val port = getInt("port", 25598)
                    val password = getString("password", "mypassword")

                    logger.info("Starting $name (#$key)...")
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
                            SocketEvent.Bukkit.Server.Enabled().also {
                                it.socket = this
                                it.key = key
                                Bukkit.getPluginManager().callEvent(it)
                            }
                        }

                        "client" -> {
                            val host = getString("host", "localhost").lc
                            SocketClient(client, name, host, port, password).apply {
                                config.buffer = getInt("buffer", 100)
                                config.timeout = getLong("timeout", 1000)
                                sockets[key] = this
                                start()
                                logger.info("Successfully started client $name (#$key)")
                                SocketEvent.Bukkit.Client.Enabled().also {
                                    it.socket = this
                                    it.key = key
                                    Bukkit.getPluginManager().callEvent(it)
                                }
                            }
                        }
                    }
                }
            }
        } ?: logger.warning("Could not load configuration")
    }

    val server = object: SocketApp.Server(){
        override fun log(err: String) = if(debug) logger.info(err) else Unit
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
        override fun log(err: String) = if(debug) logger.info(err) else Unit
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
    enum class Type{ message, connected, disconnected, handshake, enabled }
    val side: Side ; val type: Type

    open class Bukkit(
        override val side: Side, override val type: Type
    ): SocketEvent, BukkitEvent() {

        open class Server(override val type: Type): Bukkit(Side.server, type) {
            open class Enabled: Server(Type.enabled){
                lateinit var socket: SocketServer
                lateinit var key: String
            }
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
            open class Enabled: Client(Type.enabled){
                lateinit var socket: SocketClient
                lateinit var key: String
            }
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

    open class Bungee(
        override val side: Side, override val type: Type
    ): SocketEvent, BungeeEvent(){

        open class Server(override val type: Type): Bungee(Side.server, type){
            open class Enabled: Server(Type.enabled){
                lateinit var socket: SocketServer
                lateinit var key: String
            }
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
            open class Enabled: Client(Type.enabled){
                lateinit var socket: SocketClient
                lateinit var key: String
            }
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

fun onClientEnable(plugin: BungeePlugin, listener: SocketClient.(String) -> Unit): BungeeListener{
    return object:BungeeListener{
        @BungeeEventHandler
        fun onEnable(e: SocketEvent.Bungee.Client.Enabled){
            listener(e.socket, e.key)
        }
    }.also { plugin.proxy.pluginManager.registerListener(plugin, it) }
}

fun onServerEnable(plugin: BungeePlugin, listener: SocketServer.(String) -> Unit): BungeeListener{
    return object:BungeeListener{
        @BungeeEventHandler
        fun onEnable(e: SocketEvent.Bungee.Server.Enabled){
            listener(e.socket, e.key)
        }
    }.also { plugin.proxy.pluginManager.registerListener(plugin, it) }
}

fun onServerEnable(plugin: BukkitPlugin, listener: SocketServer.(String) -> Unit): BukkitListener{
    return object:BukkitListener{
        @BukkitEventHandler
        fun onEnable(e: SocketEvent.Bukkit.Server.Enabled){
            listener(e.socket, e.key)
        }
    }.also { plugin.server.pluginManager.registerEvents(it, plugin) }
}

fun onClientEnable(plugin: BukkitPlugin, listener: SocketClient.(String) -> Unit): BukkitListener{
    return object:BukkitListener{
        @BukkitEventHandler
        fun onEnable(e: SocketEvent.Bukkit.Client.Enabled){
            listener(e.socket, e.key)
        }
    }.also { plugin.server.pluginManager.registerEvents(it, plugin) }
}

fun onClientEnable(plugin: BungeePlugin, key: String, listener: SocketClient.() -> Unit): BungeeListener{
    return object:BungeeListener{
        @BungeeEventHandler
        fun onEnable(e: SocketEvent.Bungee.Client.Enabled){
            if(e.key != key) return
            listener(e.socket)
        }
    }.also { plugin.proxy.pluginManager.registerListener(plugin, it) }
}

fun onServerEnable(plugin: BungeePlugin, key: String, listener: SocketServer.() -> Unit): BungeeListener{
    return object:BungeeListener{
        @BungeeEventHandler
        fun onEnable(e: SocketEvent.Bungee.Server.Enabled){
            if(e.key != key) return
            listener(e.socket)
        }
    }.also { plugin.proxy.pluginManager.registerListener(plugin, it) }
}

fun onServerEnable(plugin: BukkitPlugin, key: String, listener: SocketServer.() -> Unit): BukkitListener{
    return object:BukkitListener{
        @BukkitEventHandler
        fun onEnable(e: SocketEvent.Bukkit.Server.Enabled){
            if(e.key != key) return
            listener(e.socket)
        }
    }.also { plugin.server.pluginManager.registerEvents(it, plugin) }
}

fun onClientEnable(plugin: BukkitPlugin, key: String, listener: SocketClient.() -> Unit): BukkitListener{
    return object:BukkitListener{
        @BukkitEventHandler
        fun onEnable(e: SocketEvent.Bukkit.Client.Enabled){
            if(e.key != key) return
            listener(e.socket)
        }
    }.also { plugin.server.pluginManager.registerEvents(it, plugin) }
}

fun SocketClient.onMessage(plugin: BungeePlugin, listener: SocketClient.(String, JSONMap) -> Unit): BungeeListener{
    val socket = this;
    return object:BungeeListener{
        @BungeeEventHandler
        fun onMessage(e: SocketEvent.Bungee.Client.Message){
            if(e.client != socket) return
            listener(e.channel, e.message)
        }
    }.also { plugin.proxy.pluginManager.registerListener(plugin, it) }
}

fun SocketClient.onMessage(plugin: BungeePlugin, channel: String, listener: SocketClient.(JSONMap) -> Unit): BungeeListener{
    val socket = this;
    return object:BungeeListener{
        @BungeeEventHandler
        fun onMessage(e: SocketEvent.Bungee.Client.Message){
            if(e.client != socket) return
            if(e.channel != channel) return
            listener(e.message)
        }
    }.also { plugin.proxy.pluginManager.registerListener(plugin, it) }
}

fun SocketClient.onHandshake(plugin: BungeePlugin, listener: SocketClient.() -> Unit): BungeeListener{
    val socket = this;
    return object:BungeeListener{
        @BungeeEventHandler
        fun onHandshake(e: SocketEvent.Bungee.Client.Handshake){
            if(e.client != socket) return
            listener()
        }
    }.also { plugin.proxy.pluginManager.registerListener(plugin, it) }
}

fun SocketClient.onConnect(plugin: BungeePlugin, listener: SocketClient.() -> Unit): BungeeListener{
    val socket = this;
    return object:BungeeListener{
        @BungeeEventHandler
        fun onConnect(e: SocketEvent.Bungee.Client.Connected){
            if(e.client != socket) return
            listener()
        }
    }.also { plugin.proxy.pluginManager.registerListener(plugin, it) }
}

fun SocketClient.onDisconnect(plugin: BungeePlugin, listener: SocketClient.() -> Unit): BungeeListener{
    val socket = this;
    return object:BungeeListener{
        @BungeeEventHandler
        fun onDisconnect(e: SocketEvent.Bungee.Client.Disconnected){
            if(e.client != socket) return
            listener()
        }
    }.also { plugin.proxy.pluginManager.registerListener(plugin, it) }
}

fun SocketClient.onMessage(plugin: BukkitPlugin, listener: SocketClient.(String, JSONMap) -> Unit): BukkitListener{
    val socket = this
    return object: BukkitListener{
        @BukkitEventHandler
        fun onMessage(e: SocketEvent.Bukkit.Client.Message){
            if(e.client != socket) return;
            listener(e.channel, e.message)
        }
    }.also { plugin.server.pluginManager.registerEvents(it, plugin) }
}

fun SocketClient.onMessage(plugin: BukkitPlugin, channel: String, listener: SocketClient.(JSONMap) -> Unit){
    val socket = this
    plugin.server.pluginManager.registerEvents(object: BukkitListener{
        @BukkitEventHandler
        fun onMessage(e: SocketEvent.Bukkit.Client.Message){
            if(e.client != socket) return
            if(e.channel != channel) return
            listener(e.message)
        }
    }, plugin)
}

fun SocketClient.onHandshake(plugin: BukkitPlugin, listener: SocketClient.() -> Unit){
    val socket = this
    plugin.server.pluginManager.registerEvents(object: BukkitListener{
        @BukkitEventHandler
        fun onHandshake(e: SocketEvent.Bukkit.Client.Handshake){
            if(e.client != socket) return;
            listener()
        }
    }, plugin)
}

fun SocketClient.onConnect(plugin: BukkitPlugin, listener: SocketClient.() -> Unit): BukkitListener{
    val socket = this
    return object: BukkitListener{
        @BukkitEventHandler
        fun onConnect(e: SocketEvent.Bukkit.Client.Connected){
            if(e.client != socket) return;
            listener()
        }
    }.also { plugin.server.pluginManager.registerEvents(it, plugin) }
}

fun SocketClient.onDisconnect(plugin: BukkitPlugin, listener: SocketClient.() -> Unit){
    val socket = this
    plugin.server.pluginManager.registerEvents(object: BukkitListener{
        @BukkitEventHandler
        fun onDisconnect(e: SocketEvent.Bukkit.Client.Disconnected){
            if(e.client != socket) return;
            listener()
        }
    }, plugin)
}

fun SocketServer.onHandshake(plugin: BukkitPlugin, listener: SocketMessenger.(String) -> Unit): BukkitListener{
    val socket = this
    return object: BukkitListener{
        @BukkitEventHandler
        fun onHandshake(e: SocketEvent.Bukkit.Server.Handshake){
            if(e.messenger.server != socket) return
            listener(e.messenger, e.messenger.target.name)
        }
    }.also { plugin.server.pluginManager.registerEvents(it, plugin) }
}

fun SocketServer.onHandshake(plugin: BukkitPlugin, name: String, listener: SocketMessenger.() -> Unit): BukkitListener{
    val socket = this
    return object: BukkitListener{
        @BukkitEventHandler
        fun onHandshake(e: SocketEvent.Bukkit.Server.Handshake){
            if(e.messenger.server != socket) return
            if(e.messenger.target.name != name) return
            listener(e.messenger)
        }
    }.also { plugin.server.pluginManager.registerEvents(it, plugin) }
}

fun SocketServer.onConnect(plugin: BukkitPlugin, listener: SocketMessenger.() -> Unit): BukkitListener{
    val socket = this
    return object: BukkitListener{
        @BukkitEventHandler
        fun onConnect(e: SocketEvent.Bukkit.Server.Connected){
            if(e.messenger.server != socket) return
            listener(e.messenger)
        }
    }.also { plugin.server.pluginManager.registerEvents(it, plugin) }
}

fun SocketServer.onDisconnect(plugin: BukkitPlugin, listener: SocketMessenger.(String) -> Unit): BukkitListener{
    val socket = this
    return object: BukkitListener{
        @BukkitEventHandler
        fun onDisconnect(e: SocketEvent.Bukkit.Server.Disconnected){
            if(e.messenger.server != socket) return
            listener(e.messenger, e.messenger.target.name)
        }
    }.also { plugin.server.pluginManager.registerEvents(it, plugin) }
}

fun SocketMessenger.onMessage(plugin: BukkitPlugin, listener: SocketMessenger.(String, JSONMap) -> Unit): BukkitListener {
    val mess = this
    return object: BukkitListener{
        @BukkitEventHandler
        fun onMessage(e: SocketEvent.Bukkit.Server.Message){
            if(e.messenger != mess) return
            listener(e.messenger, e.channel, e.message)
        }
    }.also { plugin.server.pluginManager.registerEvents(it, plugin) }
}

fun SocketMessenger.onMessage(plugin: BukkitPlugin, channel: String, listener: SocketMessenger.(JSONMap) -> Unit): BukkitListener {
    val mess = this
    return object: BukkitListener{
        @BukkitEventHandler
        fun onMessage(e: SocketEvent.Bukkit.Server.Message){
            if(e.messenger != mess) return
            if(e.channel != channel) return
            listener(e.messenger, e.message)
        }
    }.also { plugin.server.pluginManager.registerEvents(it, plugin) }
}

fun SocketServer.onHandshake(plugin: BungeePlugin, name: String, listener: SocketMessenger.() -> Unit): BungeeListener {
    val socket = this
    return object: BungeeListener{
        @BungeeEventHandler
        fun onHandshake(e: SocketEvent.Bungee.Server.Handshake){
            if(e.messenger.server != socket) return;
            if(e.messenger.target.name != name) return
            listener(e.messenger)
        }
    }.also { plugin.proxy.pluginManager.registerListener(plugin, it) }
}

fun SocketServer.onHandshake(plugin: BungeePlugin, listener: SocketMessenger.(String) -> Unit): BungeeListener{
    val socket = this
    return object: BungeeListener{
        @BungeeEventHandler
        fun onHandshake(e: SocketEvent.Bungee.Server.Handshake){
            if(e.messenger.server != socket) return;
            listener(e.messenger, e.messenger.target.name)
        }
    }.also { plugin.proxy.pluginManager.registerListener(plugin, it) }
}

fun SocketServer.onConnect(plugin: BungeePlugin, listener: SocketMessenger.() -> Unit): BungeeListener{
    val socket = this
    return object: BungeeListener{
        @BungeeEventHandler
        fun onConnect(e: SocketEvent.Bungee.Server.Connected){
            if(e.messenger.server != socket) return;
            listener(e.messenger)
        }
    }.also { plugin.proxy.pluginManager.registerListener(plugin, it) }
}

fun SocketServer.onDisconnect(plugin: BungeePlugin, listener: SocketMessenger.(String) -> Unit): BungeeListener{
    val socket = this
    return object: BungeeListener {
        @BungeeEventHandler
        fun onDisconnect(e: SocketEvent.Bungee.Server.Disconnected) {
            if (e.messenger.server != socket) return;
            listener(e.messenger, e.messenger.target.name)
        }
    }.also { plugin.proxy.pluginManager.registerListener(plugin, it) }
}

fun SocketMessenger.onMessage(plugin: BungeePlugin, listener: SocketMessenger.(String, JSONMap) -> Unit): BungeeListener{
    val mess = this
    return object: BungeeListener{
        @BungeeEventHandler
        fun onMessage(e: SocketEvent.Bungee.Server.Message){
            if(e.messenger != mess) return
            listener(e.messenger, e.channel, e.message)
        }
    }.also { plugin.proxy.pluginManager.registerListener(plugin, it) }
}

fun SocketMessenger.onMessage(plugin: BungeePlugin, channel: String, listener: SocketMessenger.(JSONMap) -> Unit): BungeeListener{
    val mess = this
    return object: BungeeListener{
        @BungeeEventHandler
        fun onMessage(e: SocketEvent.Bungee.Server.Message){
            if(e.messenger != mess) return
            if(e.channel != channel) return
            listener(e.messenger, e.message)
        }
    }.also { plugin.proxy.pluginManager.registerListener(plugin, it) }
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

fun CommandSender.msg(msg: String) = msg(text(msg))
fun CommandSender.msg(text: TextComponent) = sendMessage(text)
fun text(string: String) = TextComponent(string.replace("&", "§"))

fun spiget(id: Int, callback: (String) -> Unit) = Thread {
    try {
        val base = "https://api.spiget.org/v2/resources/"
        val conn = URL("$base$id/versions?size=100").openConnection()
        val json = InputStreamReader(conn.inputStream).let { JsonParser().parse(it).asJsonArray }
        callback(json.last().asJsonObject["name"].asString)
    } catch (e: IOException){}
}.start()

infix fun String.newerThan(v: String): Boolean = false.also{
    val s1 = split('.');
    val s2 = v.split('.');
    for(i in 0..Math.max(s1.size,s2.size)){
        if(i !in s1.indices) return false;
        if(i !in s2.indices) return true;
        if(s1[i].toInt() > s2[i].toInt()) return true;
        if(s1[i].toInt() < s2[i].toInt()) return false;
    }
}

fun BukkitPlugin.update(id: Int, color: ChatColor) = spiget(id) here@{

    if(!(it newerThan description.version)) return@here;

    val message = text("An update is available for ${description.name}!").apply {
        val url = "https://www.spigotmc.org/resources/$id"
        text += " Download it here: $url"
        this.color = color
        clickEvent = ClickEvent(ClickEvent.Action.OPEN_URL, url)
    }

    server.scheduler.runTaskLater(this, {
        server.consoleSender.spigot().sendMessage(message);
    }, 0)

    server.pluginManager.registerEvents(object: BukkitListener {
        @BukkitEventHandler
        fun onJoin(e: PlayerJoinEvent) {
            if(e.player.hasPermission("rhaz.update"))
                e.player.spigot().sendMessage(message)
        }
    }, this)
}

fun BungeePlugin.update(id: Int, color: ChatColor) = spiget(id) here@{

    if(!(it newerThan description.version)) return@here;

    val message = text("An update is available for ${description.name}!").apply {
        val url = "https://www.spigotmc.org/resources/$id"
        text += " Download it here: $url"
        this.color = color
        clickEvent = ClickEvent(ClickEvent.Action.OPEN_URL, url)
    }

    proxy.scheduler.schedule(this, {
        proxy.console.sendMessage(message);
    }, 0, TimeUnit.SECONDS)

    proxy.pluginManager.registerListener(this, object : BungeeListener {
        @BungeeEventHandler
        fun onJoin(e: PostLoginEvent) {
            if (e.player.hasPermission("rhaz.update"))
                e.player.sendMessage(message)
        }
    })
}

val unit = Unit

fun <T> listener(callable: Consumer<T>): Function1<T, Unit> = { t -> callable.accept(t); Unit }
fun <T,U> listener(callable: BiConsumer<T, U>): Function2<T, U, Unit> = { t, u -> callable.accept(t, u); Unit }
fun <T,U,V> listener(callable: TriConsumer<T, U, V>): Function3<T, U, V, Unit> = { t, u, v -> callable.accept(t, u, v); Unit }

val String.lc get() = toLowerCase()