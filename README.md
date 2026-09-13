# ZAutoBroadcast

Powerful and customizable AutoBroadcaster/AutoAnnouncer plugin for Minecraft servers. It allows server administrators to set up automatic and forced broadcasts with a variety of features, including support for MiniMessage format, MiniPlaceholders. and PlaceholderAPI placeholders.

## Folia Supported ✔️

## Features

- **Automatic Broadcasts**: Set up automatic broadcasts that are sent to all players on the server at a configurable interval.
- **Forced Broadcasts**: Set up forced broadcasts that can only be run via command.
- **Bossbar Announcements**: Send timed or indefinite MiniMessage bossbars to everyone or a specific player, with reusable presets and automatic join messages. Late joiners see active announcements for their remaining duration.
- **Weighted Broadcasts**: Control how often each broadcast is shown with the "weight" field. Broadcasts with a higher weight will appear more often.
- **[MiniMessage Support](https://docs.advntr.dev/minimessage/format.html)**: The messages use MiniMessage format, allowing for rich text formatting and color codes.
- **PlaceholderAPI Support**: Use any PlaceholderAPI placeholders in your messages. (Requires the [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) plugin)
- **MiniPlaceholders Support**: Use any MiniPlaceholder placeholders in your messages. (Requires the [MiniPlaceholders](https://github.com/MiniPlaceholders/MiniPlaceholders/) plugin)


## Commands

- `/zab reload`: Reloads `config.yml`, `broadcasts.yml`, and `bossbar.yml`.
- `/zab broadcast <key>`: Broadcasts a message with the given key.
- `/zab custom <message>`: Broadcasts a custom message.
- `/zab interval get/set <seconds>`: Gets or sets the broadcast interval.
- `/zab bossbar custom <target> <duration> <colour> <content>`: Announces a custom MiniMessage bossbar.
- `/zab bossbar preset <key>`: Announces a preset from `bossbar.yml` to everyone.
- `/zab bossbar stop <all|id>`: Stops all active bossbars, or those matching a preset key or announcement UUID.
- All commands require the `zautobroadcast.admin` permission.

### Bossbars

```text
/zab bossbar custom all 600 red <white>The <red>Purge Event <white>starts soon!
/zab bossbar custom @a -1 purple <gradient:red:gold>Welcome to the server!</gradient>
/zab bossbar custom PlayerName 60 blue <aqua>Hello, PlayerName!
/zab bossbar preset purge
/zab bossbar stop purge
/zab bossbar stop all
```

- **Target:** `all` or `@a` for everyone, or an online player's exact name. Player targets are tracked by UUID so they can reconnect and see the remaining duration.
- **Duration:** A positive whole number of seconds, or `-1` to show until stopped or the server shuts down. Timed bars count down from full to empty. Indefinite bars stay full.
- **Colour:** `pink`, `blue`, `red`, `green`, `yellow`, `purple`, or `white` (case-insensitive). This colours the bar itself; use MiniMessage to style the text.
- **Content:** MiniMessage text, including spaces. No surrounding quotes are needed in commands.

An announcement's lifetime starts when the command runs, even if nobody is online. For example, a player joining 200 seconds into a 600-second announcement sees the bar for the remaining 400 seconds. Joining does not restart or extend the announcement, and expired bars are not shown to new players. Active bars are kept in memory until they expire, are stopped, or the plugin stops.

#### Stopping bossbars

Custom and preset announcement commands report a UUID you can use with `/zab bossbar stop <id>`. Presets can also be stopped by their configuration key, such as `/zab bossbar stop purge`, which stops all active copies of that preset. `/zab bossbar stop all` stops every active bossbar, including indefinite bars and announcements with nobody currently online. Use tab completion after `/zab bossbar stop ` to find active IDs. A preset named `all` can be stopped individually using its UUID.

Stopped announcements disappear from current viewers and are no longer shown to late joiners or reconnecting targets. New announcements can be started immediately. Presets configured with `show_on_join: true` create fresh automatic bars on future joins; to disable that automatic display, set `show_on_join: false` and run `/zab reload`.

#### `bossbar.yml`

This file is created automatically in `plugins/ZAutoBroadcast/`:

```yaml
bossbars:
  purge:
    text: "<white>Do <red>/server purge <white>for the Purge Event"
    duration: 10800 # Seconds; -1 means until stopped or the server shuts down.
    show_on_join: false
    notches: 10
    colour: white # Optional; defaults to white.
```

`text` and `duration` are required. `notches` supports `0` (continuous), `6`, `10`, `12`, or `20`; it defaults to `0`. Custom bossbars use a continuous bar.

`show_on_join` defaults to `false`. Set it to `true` to automatically show the preset to each joining player for its full duration. If that preset is currently announced server-wide, the player sees the active announcement's remaining time instead of a duplicate automatic bar. Automatic join bars are removed on disconnect and start fresh on the next join.

**Command-triggered presets always include late joiners, even with `show_on_join: false`.** Running the same preset command again replaces its existing bars and restarts its duration. Different presets and custom announcements can appear together.

Use `/zab reload` after editing the file. Updated definitions apply to subsequent commands and automatic join bars; existing announcements keep their original settings and expiry. To remove all preset definitions, use `bossbars: {}`.

## Version Compatibility

- Compatible with [**Folia**](https://github.com/PaperMC/Folia), a powerful library for Minecraft plugins.
- Must be run on Paper or a Paper fork.
- Requires Java 17 or higher.
- Compatible with at least 1.20 and above.

## Dependencies

- [MCKotlin](https://modrinth.com/plugin/mckotlin)

## Installation

1. Download the latest version of ZAutoBroadcast from [Modrinth](https://modrinth.com/plugin/zautobroadcast/versions).
2. Download the appropriate version of MCKotlin from [Modrinth](https://modrinth.com/plugin/mckotlin)
3. Place the downloaded `.jar` files into your server's `plugins/` folder.
4. Restart your server.
5. Configure the plugin to your liking by editing the `config.yml`, `broadcasts.yml`, and `bossbar.yml` files in the `plugins/ZAutoBroadcast/` folder.

## Configuration

Refer to the comments in the `config.yml`, `broadcasts.yml`, and `bossbar.yml` files for information on how to configure the plugin.

## Building

Build with Java 17. With SDKMAN:

```sh
sdk use java 17.0.20-tem
./gradlew clean build --no-daemon
```

Kotlin compilation runs inside Gradle's JVM so it uses the selected JDK instead of reusing a Kotlin compiler daemon started under another Java version. The plugin JAR is written to `build/libs/ZAutoBroadcast-1.5.jar`.

## Support

If you encounter any issues or have any questions, please [open an issue](https://github.com/Zepsi/ZAutoBroadcast/issues) on GitHub.

## License

ZAutoBroadcast is licensed under the [MIT License](https://github.com/Zepsi/ZAutoBroadcast/blob/main/LICENSE).
