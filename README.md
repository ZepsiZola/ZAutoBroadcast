# ZAutoBroadcast

ZAutoBroadcast is a powerful and customizable broadcasting plugin for Minecraft servers. It allows server administrators to set up automatic and forced broadcasts with a variety of features, including support for MiniMessage format and PlaceholderAPI placeholders.

## Features

- **Automatic Broadcasts**: Set up automatic broadcasts that are sent to all players on the server at a configurable interval.
- **Forced Broadcasts**: Set up forced broadcasts that can only be run via command.
- **Weighted Broadcasts**: Control how often each broadcast is shown with the "weight" field. Broadcasts with a higher weight will appear more often.
- **MiniMessage Support**: The messages use MiniMessage format, allowing for rich text formatting and color codes.
- **PlaceholderAPI Support**: Use any PlaceholderAPI placeholders in your messages. (Requires the PlaceholderAPI plugin)

## Commands

- `/zab reload`: Reloads the `broadcasts.yml` file.
- `/zab broadcast <key>`: Broadcasts a message with the given key.
- `/zab custom <message>`: Broadcasts a custom message.
- `/zab interval get/set <seconds>`: Gets or sets the broadcast interval.

## Version Compatibility

- Compatible with [**Folia**](https://github.com/PaperMC/Folia), a powerful library for Minecraft plugins.
- Must be run on Paper or a Paper fork.
- Requires Java 17 or higher.
- Compatible with at least 1.20.4 and above.

## Installation

1. Download the latest version of ZAutoBroadcast from the [releases page](https://github.com/Zepsi/ZAutoBroadcast/releases).
2. Place the downloaded `.jar` file into your server's `plugins` folder.
3. Restart your server.
4. Configure the plugin to your liking by editing the `config.yml` and `broadcasts.yml` files in the `plugins/ZAutoBroadcast` folder.

## Configuration

Refer to the comments in the `config.yml` and `broadcasts.yml` files for information on how to configure the plugin.

## Support

If you encounter any issues or have any questions, please [open an issue](https://github.com/Zepsi/ZAutoBroadcast/issues) on GitHub.

## License

ZAutoBroadcast is licensed under the [MIT License](https://github.com/Zepsi/ZAutoBroadcast/blob/main/LICENSE).