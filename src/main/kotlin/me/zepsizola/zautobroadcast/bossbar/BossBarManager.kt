package me.zepsizola.zautobroadcast.bossbar

import com.tcoded.folialib.wrapper.task.WrappedTask
import me.zepsizola.zautobroadcast.ZAutoBroadcast
import net.kyori.adventure.bossbar.BossBar
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

internal class BossBarManager(
    private val plugin: ZAutoBroadcast,
    private val nanoTime: () -> Long = System::nanoTime
) : Listener {
    // Commands, joins, and player scheduler callbacks can run on different Folia regions.
    private val lock = Any()
    private var presets: Map<String, BossBarPreset> = emptyMap()
    private val activeBars = linkedMapOf<UUID, ActiveBossBar>()
    private val viewers = mutableMapOf<UUID, Viewer>()
    private var updateTask: WrappedTask? = null
    private var stopped = false

    fun start() = synchronized(lock) {
        if (stopped || updateTask != null) return@synchronized
        updateTask = plugin.foliaLib.impl.runTimer(Runnable { tick() }, 1, 1, TimeUnit.SECONDS)
    }

    fun reloadPresets() {
        val file = File(plugin.dataFolder, "bossbar.yml")
        if (!file.exists()) plugin.saveResource("bossbar.yml", false)
        val config = YamlConfiguration()
        try {
            config.load(file)
        } catch (exception: IOException) {
            plugin.logger.warning("Could not read bossbar.yml: ${exception.message}")
            return
        } catch (exception: InvalidConfigurationException) {
            plugin.logger.warning("Could not parse bossbar.yml: ${exception.message}")
            return
        }

        val section = config.getConfigurationSection("bossbars")
        if (section == null) {
            plugin.logger.warning("bossbar.yml must contain a 'bossbars' section. Use 'bossbars: {}' for no presets.")
            return
        }

        val loaded = linkedMapOf<String, BossBarPreset>()
        section.getKeys(false).forEach { key ->
            try {
                val preset = section.getConfigurationSection(key)
                require(preset != null) { "expected a configuration section" }
                loaded[key] = BossBarPreset.load(preset)
            } catch (exception: IllegalArgumentException) {
                plugin.logger.warning("Skipping bossbar preset '$key': ${exception.message}")
            }
        }
        synchronized(lock) {
            // Active announcements retain their original settings and expiry across reloads.
            presets = loaded
        }
    }

    fun presetKeys(): List<String> = synchronized(lock) { presets.keys.toList() }

    fun activeIds(): List<String> = synchronized(lock) {
        val now = nanoTime()
        activeBars.values.filterNot { it.isExpired(now) }.map {
            // "all" is reserved by the stop command; that preset can still use its UUID.
            it.presetKey?.takeUnless { key -> key.equals("all", ignoreCase = true) } ?: it.id.toString()
        }.distinct()
    }

    fun announce(definition: BossBarDefinition, target: UUID?): UUID? {
        val id = synchronized(lock) {
            if (stopped) return null
            addBar(definition, target)
        }
        refreshOnlinePlayers()
        return id
    }

    fun announcePreset(key: String): UUID? {
        val id = synchronized(lock) {
            if (stopped) return null
            val preset = presets[key] ?: return null
            // Re-announcing a preset restarts it, replacing any automatic join copies too.
            activeBars.values.removeIf { it.presetKey == key }
            addBar(preset.definition, null, key)
        }
        refreshOnlinePlayers()
        return id
    }

    fun stopAll(): Int = stopMatching { true }

    fun stop(id: String): Int = stopMatching {
        it.presetKey == id || it.id.toString().equals(id, ignoreCase = true)
    }

    private fun stopMatching(matches: (ActiveBossBar) -> Boolean): Int {
        val count = synchronized(lock) {
            if (stopped) return 0
            val now = nanoTime()
            val matching = activeBars.values.filter(matches)
            matching.forEach { activeBars.remove(it.id) }
            matching.count { !it.isExpired(now) }
        }
        // Entity callbacks read the registry again, so queued shows cannot revive stopped bars.
        refreshOnlinePlayers()
        return count
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        synchronized(lock) {
            if (stopped) return
            val now = nanoTime()
            activeBars.values.removeIf { it.automatic && it.target == player.uniqueId }
            presets.forEach { (key, preset) ->
                val announced = activeBars.values.any {
                    it.presetKey == key && it.target == null && !it.isExpired(now)
                }
                if (preset.showOnJoin && !announced) {
                    addBar(preset.definition, player.uniqueId, key, automatic = true)
                }
            }
        }
        refreshPlayer(player)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        synchronized(lock) {
            val player = event.player
            viewers.remove(player.uniqueId)?.bars?.values?.forEach(player::hideBossBar)
            // Command targets are UUIDs, so reconnecting restores their remaining duration.
            activeBars.values.removeIf { it.automatic && it.target == player.uniqueId }
        }
    }

    fun shutdown() = synchronized(lock) {
        stopped = true
        updateTask?.cancel()
        updateTask = null
        activeBars.clear()
        // Disable callbacks cannot submit new tasks to the plugin's schedulers.
        viewers.values.forEach { viewer -> viewer.bars.values.forEach(viewer.player::hideBossBar) }
        viewers.clear()
    }

    // Called with lock held. An announcement exists independently of its online viewers.
    private fun addBar(
        definition: BossBarDefinition,
        target: UUID?,
        presetKey: String? = null,
        automatic: Boolean = false
    ): UUID {
        val id = UUID.randomUUID()
        activeBars[id] = ActiveBossBar(id, definition, target, nanoTime(), presetKey, automatic)
        return id
    }

    private fun tick() {
        synchronized(lock) {
            if (stopped) return
            val now = nanoTime()
            activeBars.values.removeIf { it.isExpired(now) }
            if (activeBars.isEmpty() && viewers.isEmpty()) return
        }
        refreshOnlinePlayers()
    }

    private fun refreshOnlinePlayers() {
        plugin.server.onlinePlayers.forEach(::refreshPlayer)
    }

    private fun refreshPlayer(player: Player) = synchronized(lock) {
        if (stopped) return@synchronized
        plugin.foliaLib.impl.runAtEntity(player) {
            synchronized(lock) {
                if (!stopped && player.isOnline) updatePlayer(player)
            }
        }
    }

    // Only called on this player's entity scheduler, or their owning Folia region.
    private fun updatePlayer(player: Player) {
        val now = nanoTime()
        val desired = activeBars.values.filter {
            (it.target == null || it.target == player.uniqueId) && !it.isExpired(now)
        }
        val viewer = viewers.getOrPut(player.uniqueId) { Viewer(player) }
        val desiredIds = desired.mapTo(hashSetOf()) { it.id }
        val iterator = viewer.bars.iterator()
        while (iterator.hasNext()) {
            val (id, bar) = iterator.next()
            if (id !in desiredIds) {
                player.hideBossBar(bar)
                iterator.remove()
            }
        }
        desired.forEach { active ->
            val existing = viewer.bars[active.id]
            if (existing != null) {
                existing.progress(active.progress(now))
            } else {
                val definition = active.definition
                // Each player owns a separate Adventure bar; no cross-region mutations.
                val bar = BossBar.bossBar(definition.title, active.progress(now), definition.colour, definition.overlay)
                viewer.bars[active.id] = bar
                player.showBossBar(bar)
            }
        }
        if (viewer.bars.isEmpty()) viewers.remove(player.uniqueId)
    }

    private class Viewer(val player: Player, val bars: MutableMap<UUID, BossBar> = mutableMapOf())

    private class ActiveBossBar(
        val id: UUID,
        val definition: BossBarDefinition,
        val target: UUID?,
        private val startedAt: Long,
        val presetKey: String?,
        val automatic: Boolean
    ) {
        private val durationNanos = TimeUnit.SECONDS.toNanos(definition.durationSeconds)

        fun isExpired(now: Long): Boolean = definition.durationSeconds != -1L && now - startedAt >= durationNanos

        fun progress(now: Long): Float = if (definition.durationSeconds == -1L) {
            1f
        } else {
            (1.0 - (now - startedAt).toDouble() / durationNanos).coerceIn(0.0, 1.0).toFloat()
        }
    }
}
