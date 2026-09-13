package me.zepsizola.zautobroadcast.command

import me.zepsizola.zautobroadcast.ZAutoBroadcast
import me.zepsizola.zautobroadcast.bossbar.BossBarDefinition
import me.zepsizola.zautobroadcast.bossbar.BossBarManager
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Server
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BossBarCommandTest {
    private val plugin = mock(ZAutoBroadcast::class.java)
    private val manager = mock(BossBarManager::class.java)
    private val server = mock(Server::class.java)
    private val sender = mock(CommandSender::class.java)
    private val command = mock(Command::class.java)
    private val messages = mutableListOf<String>()
    private lateinit var main: MainCommand

    @BeforeEach
    fun setup() {
        `when`(plugin.bossBarManager).thenReturn(manager)
        `when`(plugin.server).thenReturn(server)
        `when`(sender.hasPermission(ZAutoBroadcast.ADMIN_PERMISSION)).thenReturn(true)
        doAnswer {
            messages.add(it.getArgument(0))
            null
        }.`when`(sender).sendMessage(anyString())
        main = MainCommand(plugin)
    }

    @Test
    fun `main command routes custom bossbars and preserves MiniMessage with spaces`() {
        execute("bossbar custom all 600 ReD <white>Hello <red>world")
        verify(manager).announce(
            BossBarDefinition(MiniMessage.miniMessage().deserialize("<white>Hello <red>world"), 600, BossBar.Color.RED),
            null
        )
        assertTrue(messages.single().contains("600 seconds"))
    }

    @Test
    fun `all player selector supports indefinite announcements`() {
        execute("bossbar custom @a -1 blue Hello")
        verify(manager).announce(BossBarDefinition(Component.text("Hello"), -1, BossBar.Color.BLUE), null)
        assertTrue(messages.single().contains("until server shutdown"))
    }

    @Test
    fun `player names resolve to UUID targets`() {
        val player = mock(Player::class.java)
        val uuid = UUID.randomUUID()
        `when`(server.getPlayerExact("PlayerName")).thenReturn(player)
        `when`(player.uniqueId).thenReturn(uuid)
        `when`(player.name).thenReturn("PlayerName")
        execute("bossbar custom PlayerName 60 green Hello")
        verify(manager).announce(BossBarDefinition(Component.text("Hello"), 60, BossBar.Color.GREEN), uuid)
    }

    @Test
    fun `bossbar commands require the admin permission`() {
        `when`(sender.hasPermission(ZAutoBroadcast.ADMIN_PERMISSION)).thenReturn(false)
        execute("bossbar custom all 600 white Hello")
        execute("bossbar preset purge")
        verifyNoInteractions(manager)
        assertTrue(messages.all { it.contains("permission") })
        assertTrue(complete("bossbar", "preset", "").isEmpty())
    }

    @Test
    fun `invalid custom arguments return feedback without announcing anything`() {
        listOf(
            "custom", "custom all 600 white", "custom all 0 white Hello",
            "custom all -2 white Hello", "custom all 1.5 white Hello",
            "custom all 9223372036854775807 white Hello", "custom all 600 orange Hello",
            "custom MissingPlayer 600 white Hello", "preset", "preset purge extra"
        ).forEach { arguments ->
            messages.clear()
            execute("bossbar $arguments")
            assertTrue(messages.isNotEmpty(), "Expected feedback for $arguments")
        }
        verifyNoInteractions(manager)
    }

    @Test
    fun `presets route through the main command with feedback for unknown keys`() {
        `when`(manager.announcePreset("purge")).thenReturn(true)
        execute("bossbar preset purge")
        assertTrue(messages.single().contains("announced"))
        messages.clear()
        execute("bossbar preset missing")
        assertTrue(messages.single().contains("Unknown bossbar preset"))
    }

    @Test
    fun `tab completion includes preset keys targets and colours with prefix filtering`() {
        `when`(manager.presetKeys()).thenReturn(listOf("purge", "welcome"))
        `when`(server.onlinePlayers).thenReturn(emptyList())
        assertTrue("bossbar" in complete(""))
        assertEquals(listOf("custom", "preset"), complete("bossbar", ""))
        assertEquals(listOf("purge"), complete("bossbar", "preset", "pu"))
        assertEquals(listOf("all", "@a"), complete("bossbar", "custom", ""))
        assertEquals(listOf("red"), complete("bossbar", "custom", "all", "600", "r"))
        assertTrue(complete("bossbar", "custom", "all", "600", "red", "").isEmpty())
    }

    private fun execute(arguments: String) {
        assertTrue(main.onCommand(sender, command, "zab", arguments.split(" ").toTypedArray()))
    }

    private fun complete(vararg arguments: String): List<String> =
        main.onTabComplete(sender, command, "zab", arrayOf(*arguments)).orEmpty()
}
