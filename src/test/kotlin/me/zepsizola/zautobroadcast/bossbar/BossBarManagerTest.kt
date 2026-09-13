package me.zepsizola.zautobroadcast.bossbar

import com.tcoded.folialib.FoliaLib
import com.tcoded.folialib.enums.EntityTaskResult
import com.tcoded.folialib.impl.ServerImplementation
import com.tcoded.folialib.wrapper.task.WrappedTask
import me.zepsizola.zautobroadcast.ZAutoBroadcast
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import org.bukkit.Server
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.*
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.logging.Logger
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BossBarManagerTest {
    @TempDir
    lateinit var dataFolder: Path

    private val plugin = mock(ZAutoBroadcast::class.java)
    private val server = mock(Server::class.java)
    private val foliaLib = mock(FoliaLib::class.java)
    private val scheduler = mock(ServerImplementation::class.java)
    private val timerTask = mock(WrappedTask::class.java)
    private val online = mutableListOf<Player>()
    private val visible = mutableMapOf<Player, MutableSet<BossBar>>()
    private val queued = ArrayDeque<Consumer<WrappedTask>>()
    private lateinit var timer: Runnable
    private lateinit var manager: BossBarManager
    private var now = 0L
    private var onEntityScheduler = false

    @BeforeEach
    fun setup() {
        `when`(plugin.server).thenReturn(server)
        `when`(plugin.foliaLib).thenReturn(foliaLib)
        `when`(plugin.dataFolder).thenReturn(dataFolder.toFile())
        `when`(plugin.logger).thenReturn(Logger.getAnonymousLogger())
        `when`(foliaLib.impl).thenReturn(scheduler)
        `when`(server.onlinePlayers).thenAnswer { online.toList() }
        doAnswer {
            timer = it.getArgument(0)
            timerTask
        }.`when`(scheduler).runTimer(any(Runnable::class.java), eq(1L), eq(1L), eq(TimeUnit.SECONDS))
        doAnswer {
            queued.add(it.getArgument(1))
            CompletableFuture.completedFuture(EntityTaskResult.SUCCESS)
        }.`when`(scheduler).runAtEntity(any(Entity::class.java), any())
        manager = BossBarManager(plugin) { now }
        manager.start()
    }

    @Test
    fun `late joiners share the original expiry and countdown`() {
        val first = join()
        manager.announce(definition(600), null)
        flush()
        val firstBar = visible.getValue(first).single()
        assertEquals(1f, firstBar.progress())

        advance(200)
        val late = join()
        assertEquals(400f / 600f, visible.getValue(late).single().progress(), 0.0001f)
        assertFalse(firstBar === visible.getValue(late).single(), "Players must not share mutable Adventure bars")

        advance(400)
        assertTrue(visible.getValue(first).isEmpty())
        assertTrue(visible.getValue(late).isEmpty())
        assertTrue(visible.getValue(join()).isEmpty())
    }

    @Test
    fun `announcements survive an empty server but expire without viewers`() {
        manager.announce(definition(600), null)
        advance(599)
        val late = join()
        assertEquals(1f / 600f, visible.getValue(late).single().progress(), 0.0001f)
        quit(late)
        advance(1)
        assertTrue(visible.getValue(join()).isEmpty())
    }

    @Test
    fun `single player targets follow UUID across reconnects`() {
        val target = join()
        val other = join()
        manager.announce(definition(600), target.uniqueId)
        flush()
        assertEquals(1, visible.getValue(target).size)
        assertTrue(visible.getValue(other).isEmpty())
        quit(target)

        advance(200)
        val reconnected = join(target.uniqueId)
        assertEquals(400f / 600f, visible.getValue(reconnected).single().progress(), 0.0001f)
        advance(400)
        assertTrue(visible.getValue(reconnected).isEmpty())
    }

    @Test
    fun `a delayed player callback cannot show an expired announcement`() {
        val player = join()
        manager.announce(definition(1), null)
        now = TimeUnit.SECONDS.toNanos(2)
        // Expiry must be checked on the entity thread even before the next global timer run.
        flush()
        assertTrue(visible.getValue(player).isEmpty())
    }

    @Test
    fun `indefinite bars survive until shutdown and queued work cannot restore them`() {
        val player = join()
        manager.announce(definition(-1), null)
        flush()
        advance(1_000_000)
        assertEquals(1f, visible.getValue(player).single().progress())
        assertEquals(1, visible.getValue(join()).size)
        timer.run()

        // Shutdown, like quit, performs cleanup without submitting new plugin tasks.
        onEntityScheduler = true
        manager.shutdown()
        onEntityScheduler = false
        flush()
        assertTrue(visible.values.all { it.isEmpty() })
        verify(timerTask).cancel()
    }

    @Test
    fun `show on join false still lets late joiners see commanded presets`() {
        loadPreset(showOnJoin = false)
        val first = join()
        assertTrue(visible.getValue(first).isEmpty())
        assertTrue(manager.announcePreset("purge"))
        flush()
        assertEquals(BossBar.Overlay.NOTCHED_10, visible.getValue(first).single().overlay())
        advance(200)
        assertEquals(400f / 600f, visible.getValue(join()).single().progress(), 0.0001f)
        advance(400)
        assertTrue(visible.values.all { it.isEmpty() })
        assertFalse(manager.announcePreset("missing"))
    }

    @Test
    fun `automatic presets get per join lifetimes and commanded versions replace them`() {
        loadPreset(showOnJoin = true)
        val first = join()
        advance(200)
        val second = join()
        assertEquals(400f / 600f, visible.getValue(first).single().progress(), 0.0001f)
        assertEquals(1f, visible.getValue(second).single().progress())

        manager.announcePreset("purge")
        flush()
        assertEquals(1f, visible.getValue(first).single().progress())
        assertEquals(1f, visible.getValue(second).single().progress())
        advance(200)
        assertEquals(400f / 600f, visible.getValue(join()).single().progress(), 0.0001f)
        advance(400)
        assertTrue(visible.values.all { it.isEmpty() })
        assertEquals(1f, visible.getValue(join()).single().progress())
    }

    @Test
    fun `automatic indefinite presets do not accumulate across reconnects`() {
        loadPreset(showOnJoin = true, duration = -1)
        val player = join()
        quit(player)
        val reconnected = join(player.uniqueId)
        assertEquals(1, visible.getValue(reconnected).size)
        assertTrue(visible.getValue(player).isEmpty())
    }

    @Test
    fun `reloading definitions preserves active expiry and reannouncing restarts a preset`() {
        loadPreset(showOnJoin = false)
        val player = join()
        manager.announcePreset("purge")
        flush()
        advance(200)
        loadPreset(showOnJoin = false, duration = 60)
        assertEquals(400f / 600f, visible.getValue(player).single().progress(), 0.0001f)

        manager.announcePreset("purge")
        flush()
        assertEquals(1, visible.getValue(player).size)
        assertEquals(1f, visible.getValue(player).single().progress())
        advance(60)
        assertTrue(visible.getValue(player).isEmpty())
    }

    @Test
    fun `invalid presets are skipped without discarding valid presets`() {
        loadPreset(showOnJoin = false)
        val file = dataFolder.resolve("bossbar.yml")
        file.toFile().appendText("\n  invalid:\n    text: Bad duration\n    duration: 0\n")
        manager.reloadPresets()
        assertEquals(listOf("purge"), manager.presetKeys())
        file.writeText("bossbars: [broken YAML")
        manager.reloadPresets()
        assertEquals(listOf("purge"), manager.presetKeys())
    }

    private fun definition(duration: Long) = BossBarDefinition(Component.text("Announcement"), duration)

    private fun join(uuid: UUID = UUID.randomUUID()): Player {
        val player = mock(Player::class.java)
        `when`(player.uniqueId).thenReturn(uuid)
        `when`(player.isOnline).thenAnswer { player in online }
        visible[player] = linkedSetOf()
        doAnswer {
            assertTrue(onEntityScheduler, "Bossbars must be shown on the player scheduler")
            visible.getValue(player).add(it.getArgument(0))
            null
        }.`when`(player).showBossBar(any(BossBar::class.java))
        doAnswer {
            assertTrue(onEntityScheduler, "Bossbars must be hidden on the player scheduler or during cleanup")
            visible.getValue(player).remove(it.getArgument<BossBar>(0))
            null
        }.`when`(player).hideBossBar(any(BossBar::class.java))
        online.add(player)
        manager.onJoin(PlayerJoinEvent(player, Component.empty()))
        flush()
        return player
    }

    private fun quit(player: Player) {
        onEntityScheduler = true
        manager.onQuit(PlayerQuitEvent(player, Component.empty(), PlayerQuitEvent.QuitReason.DISCONNECTED))
        onEntityScheduler = false
        online.remove(player)
    }

    private fun advance(seconds: Long) {
        now += TimeUnit.SECONDS.toNanos(seconds)
        timer.run()
        flush()
    }

    private fun flush() {
        onEntityScheduler = true
        try {
            while (queued.isNotEmpty()) queued.removeFirst().accept(timerTask)
        } finally {
            onEntityScheduler = false
        }
    }

    private fun loadPreset(showOnJoin: Boolean, duration: Long = 600) {
        dataFolder.resolve("bossbar.yml").writeText("""
            bossbars:
              purge:
                text: "<white>Do <red>/server purge <white>for the Purge Event"
                duration: $duration
                show_on_join: $showOnJoin
                notches: 10
        """.trimIndent())
        manager.reloadPresets()
    }
}
