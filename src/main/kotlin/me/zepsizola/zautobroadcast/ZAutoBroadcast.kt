package me.zepsizola.zautobroadcast

import org.bstats.bukkit.Metrics
import com.tcoded.folialib.FoliaLib
import com.tcoded.folialib.wrapper.task.WrappedTask
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

class ZAutoBroadcast : JavaPlugin() {

    private lateinit var autoBroadcasts: ConcurrentHashMap<String, List<Component>>
    lateinit var forcedBroadcasts: ConcurrentHashMap<String, List<Component>>
    private lateinit var weightedKeys: List<String>
    var interval: Long = 300 // Default value (300 seconds is 5 minutes)
    private var papiEnabled = false
    private var broadcastTask: WrappedTask? = null
    val foliaLib = FoliaLib(this)


    override fun onEnable() {
        logger.info("ZAutoBroadcast has begun enabling.")

        // Initialize bStats
        val metrics = Metrics(this, 22351)

        val mainCommand = MainCommand(this)
        getCommand("zab")?.setExecutor(mainCommand)
        getCommand("zab")?.setTabCompleter(mainCommand)
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
        // Check if PlaceholderAPI is available
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            papiEnabled = true
            logger.info("PlaceholderAPI enabled.")
        } else {
            logger.info("PlaceholderAPI not found. PAPI support disabled.")
        }
    }

    private fun startBroadcastTask() {
        broadcastTask = foliaLib.impl.runTimer(Runnable {
            // Randomly select a key from the weighted list of keys
            val key = weightedKeys[ThreadLocalRandom.current().nextInt(weightedKeys.size)]
            // Get the list of messages associated with the key
            val messageList = autoBroadcasts[key]
            // If the list is not null, broadcast each message in the list
            messageList?.forEach { Bukkit.getServer().sendMessage(it) }
        }, 1, interval, TimeUnit.SECONDS) // Runs every [interval] seconds.
    }

    @Synchronized
    fun reloadConfigs(isStartup: Boolean) {
        // Clear the current messages
        if(!isStartup){
            autoBroadcasts.clear()
            forcedBroadcasts.clear()
            broadcastTask?.cancel()
        }

        // Load the broadcasts.yml file
        val broadcastFile = File(dataFolder, "broadcasts.yml")
        if (!broadcastFile.exists()) saveResource("broadcasts.yml", false)

        // Parse the messages from the broadcasts.yml file
        val broadcasts = YamlConfiguration.loadConfiguration(broadcastFile)

        // COMPLICATED PIECE OF CODE
        // Basically takes the broadcast.yml configuration and converts each configured message to be in a Map<String, List<Component>> while also turning the message into a Component.
        autoBroadcasts.putAll(broadcasts.getConfigurationSection("auto-broadcasts")?.getKeys(false)?.associateWith { key -> broadcasts.getStringList("auto-broadcasts.$key.message").map { MiniMessage.miniMessage().deserialize(it) } } ?: emptyMap())

        // Does the same with forced broadcasts.
        forcedBroadcasts.putAll(broadcasts.getConfigurationSection("forced-broadcasts")?.getKeys(false)?.associateWith { key -> broadcasts.getStringList("forced-broadcasts.$key.message").map { MiniMessage.miniMessage().deserialize(it) } } ?: emptyMap())

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

    fun getBroadcastMessage(key: String): List<Component>? {
        return forcedBroadcasts[key]
    }

}