<h3 align=center>
    <img src="https://i.imgur.com/FwZRaEn.png"/><br>
</h3>
<br>

[![](https://i.imgur.com/3bVmcOF.png)](https://www.spigotmc.org/resources/sockets4mc-no-more-plugin-messaging-channels.15938/)
---

#### Tired of Plugin Messaging Channels?
This API simply allows developers to send any data from a server to another

### Short example

    // Server-side
    mess.write("MyChannel", "What is the answer to life?")

    // Client-side
    client.write("MyChannel", "42")

### Use it

- Example in Kotlin

- Example in Java

### Implement it

- Kotlin DSL: add this to your build.gradle.kts

      repositories {
          maven { url = URI("https://mymavenrepo.com/repo/NIp3fBk55f5oF6VI1Wso/")}
      }

      dependencies {
          compileOnly("fr.rhaz.minecraft:sockets4mc:4.0")
      }

- Gradle: add this to your build.gradle

      repositories {
          maven { url 'https://mymavenrepo.com/repo/NIp3fBk55f5oF6VI1Wso/' }
      }

      dependencies {
          compileOnly 'fr.rhaz.minecraft:sockets4mc:4.0'
      }


- Maven: add this to your pom.xml

      <repositories>
        <repository>
            <id>rhazdev</id>
            <url>https://mymavenrepo.com/repo/NIp3fBk55f5oF6VI1Wso/</url>
        </repository>
      </repositories>

      <dependencies>
        <dependency>
            <groupId>fr.rhaz.minecraft</groupId>
            <artifactId>sockets4mc</artifactId>
            <version>4.0</version>
            <scope>provided</scope>
        </dependency>
      </dependencies>
