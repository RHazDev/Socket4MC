import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

// --- Java example of Bukkit plugin ---
// Let's call our server Alice and our client Bob
//
import fr.rhaz.sockets.*; // RHazSockets

import static fr.rhaz.minecraft.sockets.Sockets4MC.getSocket;
import static fr.rhaz.minecraft.sockets.Sockets4MC.onSocketEnable;
import static fr.rhaz.sockets.JsonKt.jsonMap;

public class JavaTest{

    public class BungeeTest extends Plugin{

        @Override public void onEnable() {

            onSocketEnable(/*plugin*/ this, /*id*/ "default", (socket) -> {

                getLogger().info("Socket #default is available");

                // When any connection is ready
                socket.onReady(connection -> {
                    getLogger().info("Connection to "+connection.getTargetName()+" is available");
                    // Send a message over the channel "Test"
                    connection.msg(/*channel*/ "Test", /*data*/ "How are you?");
                });

                // When a message is received over the channel "Test"
                socket.onMessage(/*channel*/ "Test", (connection, msg) -> {
                    if(msg.get(/*key*/ "data").equals("It works fine!"))
                    getLogger().info(connection.getTargetName()+" works fine!");
                });

            });
        }

        public void sendInfo(ProxiedPlayer player) throws Exception {
            String serverName = player.getServer().getInfo().getName();

            MultiSocket socket = getSocket(/*id*/ "default");
            if(socket == null) throw new Exception("Socket #default is not available");

            Connection connection = socket.getConnection(/*target*/ serverName);
            if(connection == null) throw new Exception("Connection to "+serverName+" is not available");

            connection.msg(/*channel*/ "PlayerInfo", jsonMap(
                /*key*/ "uuid", /*value*/ player.getUniqueId(),
                /*key*/ "name", /*value*/ player.getName(),
                /*key*/ "displayname", /*value*/ player.getDisplayName()
            ));
        }
    }

    public class BukkitTest extends JavaPlugin{

        @Override public void onEnable(){
            onSocketEnable(/*plugin*/ this, /*id*/ "default", socket -> {
                getLogger().info("Socket #default is available");

                socket.onReady(connection -> {
                    getLogger().info("Connection to "+connection.getTargetName()+" is available");
                });

                socket.onMessage(/*channel*/ "Test", (connection, msg) -> {
                    if(msg.get(/*key*/ "data").equals("How are you?"))
                        connection.msg(/*channel*/ "Test", /*data*/ "It works fine!");
                });
            });
        }

        public void sendInfo(Player player) throws Exception {
            String proxyName = "MyProxy";

            MultiSocket socket = getSocket(/*id*/ "default");
            if(socket == null) throw new Exception("Socket #default is not available");

            Connection connection = socket.getConnection(/*target*/ proxyName);
            if(connection == null) throw new Exception("Connection to "+proxyName+" is not available");

            connection.msg(/*channel*/ "PlayerInfo", jsonMap(
                /*key*/ "uuid", /*value*/ player.getUniqueId(),
                /*key*/ "name", /*value*/ player.getName(),
                /*key*/ "displayname", /*value*/ player.getDisplayName(),
                /*key*/ "exp", /*value*/ player.getExp()
            ));
        }
    }
}
