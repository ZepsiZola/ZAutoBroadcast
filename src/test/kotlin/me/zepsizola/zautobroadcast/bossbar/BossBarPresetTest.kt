package me.zepsizola.zautobroadcast.bossbar

import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class BossBarPresetTest {
    @Test
    fun `the requested YAML format supports MiniMessage and omitted colour`() {
        val config = YamlConfiguration()
        config.loadFromString("""
            text: "<white>Do <red>/server purge <white>for the Purge Event"
            duration: 10800
            show_on_join: false
            notches: 10
        """.trimIndent())
        val preset = BossBarPreset.load(config)
        assertEquals(MiniMessage.miniMessage().deserialize(config.getString("text")!!), preset.definition.title)
        assertEquals(10800L, preset.definition.durationSeconds)
        assertEquals(BossBar.Color.WHITE, preset.definition.colour)
        assertEquals(BossBar.Overlay.NOTCHED_10, preset.definition.overlay)
        assertFalse(preset.showOnJoin)
    }

    @Test
    fun `invalid durations colours notches and join flags are rejected`() {
        val invalid = listOf(
            "duration" to "0", "duration" to "-2", "duration" to "1.5",
            "duration" to Long.MAX_VALUE.toString(), "colour" to "orange",
            "notches" to "5", "notches" to "10.5", "show_on_join" to "maybe"
        )
        invalid.forEach { (key, value) ->
            val config = YamlConfiguration()
            config.loadFromString("text: Hello\nduration: 600")
            config.set(key, value)
            assertFailsWith<IllegalArgumentException>("$key: $value") { BossBarPreset.load(config) }
        }
    }
}
