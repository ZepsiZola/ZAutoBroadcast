package me.zepsizola.zautobroadcast.bossbar

import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.configuration.ConfigurationSection
import java.util.Locale

internal data class BossBarDefinition(
    val title: Component,
    val durationSeconds: Long,
    val colour: BossBar.Color = BossBar.Color.WHITE,
    val overlay: BossBar.Overlay = BossBar.Overlay.PROGRESS
) {
    init {
        require(isValidDuration(durationSeconds)) {
            "duration must be -1 or a whole number of seconds between 1 and $MAX_DURATION_SECONDS"
        }
    }

    companion object {
        // Keep elapsed-time comparisons safe when using the monotonic nanosecond clock.
        const val MAX_DURATION_SECONDS = Long.MAX_VALUE / 1_000_000_000L
        val colours = BossBar.Color.entries.map { it.name.lowercase(Locale.ROOT) }

        fun isValidDuration(seconds: Long): Boolean = seconds == -1L || seconds in 1..MAX_DURATION_SECONDS

        fun parseColour(value: String): BossBar.Color? = BossBar.Color.NAMES.value(value.lowercase(Locale.ROOT))

        fun parseOverlay(notches: Int): BossBar.Overlay? = when (notches) {
            0 -> BossBar.Overlay.PROGRESS
            6 -> BossBar.Overlay.NOTCHED_6
            10 -> BossBar.Overlay.NOTCHED_10
            12 -> BossBar.Overlay.NOTCHED_12
            20 -> BossBar.Overlay.NOTCHED_20
            else -> null
        }
    }
}

internal data class BossBarPreset(val definition: BossBarDefinition, val showOnJoin: Boolean) {
    companion object {
        fun load(section: ConfigurationSection): BossBarPreset {
            val text = section.getString("text")
            require(!text.isNullOrBlank()) { "text must contain a MiniMessage message" }

            val duration = section.get("duration")?.toString()?.toLongOrNull()
            require(duration != null) { "duration must be a whole number of seconds (or -1)" }

            val colour = BossBarDefinition.parseColour(section.getString("colour", "white")!!)
            require(colour != null) { "colour must be one of: ${BossBarDefinition.colours.joinToString(", ")}" }

            val notches = section.get("notches", 0).toString().toIntOrNull()
            val overlay = notches?.let(BossBarDefinition::parseOverlay)
            require(overlay != null) { "notches must be one of: 0, 6, 10, 12, 20" }

            require(!section.contains("show_on_join") || section.isBoolean("show_on_join")) {
                "show_on_join must be true or false"
            }

            return BossBarPreset(
                BossBarDefinition(MiniMessage.miniMessage().deserialize(text), duration, colour, overlay),
                section.getBoolean("show_on_join", false)
            )
        }
    }
}
