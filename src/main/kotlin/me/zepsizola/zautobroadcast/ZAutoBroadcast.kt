package me.zepsizola.zautobroadcast

import com.tcoded.folialib.FoliaLib
import com.tcoded.folialib.wrapper.task.WrappedTask
import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.text.minimessage.MiniMessage
//import me.zepsizola.zautobroadcast.bstats.bukkit.Metrics
import org.bstats.bukkit.Metrics
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import io.github.miniplaceholders.api.MiniPlaceholders;
import me.zepsizola.zautobroadcast.command.MainCommand
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver.resolver


class ZAutoBroadcast : JavaPlugin() {

    private lateinit var autoBroadcasts: ConcurrentHashMap<String, List<String>>
    private lateinit var forcedBroadcasts: ConcurrentHashMap<String, List<String>>
    private lateinit var weightedKeys: List<String>
    private var interval: Long = 300 // Default value (300 seconds is 5 minutes)
    private var papiEnabled = false
    private var miniPlaceholdersEnabled = false
    private var broadcastTask: WrappedTask? = null

    internal companion object { const val ADMIN_PERMISSION = "zautobroadcast.admin" }

    internal val foliaLib = FoliaLib(this)

    override fun onEnable() {
        logger.info("ZAutoBroadcast has begun enabling.")

        // Initialize bStats
        val metrics = Metrics(this, 22351)

        val mainCommand = MainCommand(this)
        getCommand("zab")?.setExecutor(mainCommand)
        getCommand("zab")?.setTabCompleter(mainCommand)

        setupMiniPlaceholders()
        setupPAPI()

        autoBroadcasts = ConcurrentHashMap()
        forcedBroadcasts = ConcurrentHashMap()
        reloadConfigs(true) // Reloads the broadcasts/interval and also initialises the broadcast schedule with new interval

        logger.info("ZAutoBroadcast has finished enabling.")
    }

    override fun onDisable() {
        logger.info("ZAutoBroadcast disabling...")
        broadcastTask?.cancel() // Cancels broadcasts.
        logger.info("ZAutoBroadcast has finished disabling.")
    }

    private fun setupPAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            papiEnabled = true
            logger.info("PlaceholderAPI enabled.")
        } else {
            logger.info("PlaceholderAPI not found. PAPI support disabled.")
        }
    }
    private fun setupMiniPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("MiniPlaceholders") != null) {
            miniPlaceholdersEnabled = true
            logger.info("MiniPlaceholders enabled.")
        } else {
            logger.info("MiniPlaceholders not found. MiniPlaceholders support disabled.")
        }
    }

    private fun startBroadcastTask() {
        broadcastTask = foliaLib.impl.runTimer(Runnable {
            // Randomly select a key from the weighted list of keys
            val key = weightedKeys[ThreadLocalRandom.current().nextInt(weightedKeys.size)]
            // Get the list of messages associated with the key
            val messageList = autoBroadcasts[key]
            // If the list is not null, broadcast each message in the list
            broadcastMessage(messageList ?: return@Runnable)
        }, 1, interval, TimeUnit.SECONDS) // Runs every [interval] seconds.
    }

    private fun populateBroadcastsMap(map: ConcurrentHashMap<String, List<String>>, section: String, broadcastFile: File) {
        val broadcasts = YamlConfiguration.loadConfiguration(broadcastFile)
        map.putAll(broadcasts.getConfigurationSection(section)?.getKeys(false)?.associateWith { key -> broadcasts.getStringList("$section.$key.message").map { it } } ?: emptyMap())
    }

    internal fun broadcastMessage(messageList: List<String>) {
        if (papiEnabled) {
            this.server.onlinePlayers.forEach { player: Player ->
                messageList.forEach {
                    val messageString = PlaceholderAPI.setPlaceholders(player, it)
                    player.sendMessage(MiniMessage.miniMessage().deserialize(messageString))
                }
            }
        } else if (miniPlaceholdersEnabled) {
            this.server.onlinePlayers.forEach { player: Player ->
                val resolver = resolver(
                    MiniPlaceholders.getGlobalPlaceholders(),
                    MiniPlaceholders.getAudiencePlaceholders(player))
                messageList.forEach {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(it, resolver))
                }
            }
        } else {
            this.server.onlinePlayers.forEach { player: Player ->
                messageList.forEach {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(it))
                }
            }
        }
    }

    @Synchronized
    internal fun reloadConfigs(isStartup: Boolean) {
        // Clear the current messages
        if (!isStartup) {
            autoBroadcasts.clear()
            forcedBroadcasts.clear()
            broadcastTask?.cancel()
        }

        // Load the broadcasts.yml file
        val broadcastFile = File(dataFolder, "broadcasts.yml")
        if (!broadcastFile.exists()) saveResource("broadcasts.yml", false)

        // Parse the messages from the broadcasts.yml file
        val broadcasts = YamlConfiguration.loadConfiguration(broadcastFile)

        populateBroadcastsMap(autoBroadcasts, "auto-broadcasts", broadcastFile)
        populateBroadcastsMap(forcedBroadcasts, "forced-broadcasts", broadcastFile)

        // Create the weighted list of keys
        weightedKeys = mutableListOf<String>().apply {
            broadcasts.getConfigurationSection("auto-broadcasts")?.getKeys(false)?.forEach { key ->
                val weight = broadcasts.getInt("auto-broadcasts.$key.weight", 1)
                repeat(weight) {
                    add(key)
                }
            }
        }

        // Load the config.yml file
        val configFile = File(dataFolder, "config.yml")
        if (!configFile.exists()) saveResource("config.yml", false)
        val config = YamlConfiguration.loadConfiguration(configFile)
        interval = config.getLong("interval", 300) // Convert to ticks
        startBroadcastTask()
    }

    internal fun getInterval(): Long {
        return interval
    }

    internal fun getForcedBroadcasts(): ConcurrentHashMap<String, List<String>> {
        return forcedBroadcasts
    }


}