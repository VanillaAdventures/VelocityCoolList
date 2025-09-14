### [Hangar page](https://hangar.papermc.io/atikiNBTW/VelocityCoolList)
### [Modrinth page](https://modrinth.com/plugin/velocitycoollist)

VelocityCoolList is a simple and easy-to-use plugin for Minecraft Velocity servers that allows you to create a whitelist based on nicknames.

## LimboAPI Integration
Starting from version 2.1.0, VelocityCoolList includes integration with LimboAPI to ensure whitelist checks happen before LimboAuth teleportation events. This prevents players from being teleported to limbo before whitelist verification.

### Configuration
The integration can be controlled via the `limbo_integration` setting in `config.yml`:
```yaml
# LimboAPI integration - проверка вайтлиста с высоким приоритетом
# для предотвращения телепортации в LimboAuth до проверки вайтлиста
limbo_integration: true
```

When enabled, the plugin will:
- Register high-priority event handlers for `LoginEvent` with priority 100000
- Check whitelist status before any LimboAPI events can process
- Prevent unauthorized players from being teleported to limbo servers

This ensures that whitelist verification always happens first, regardless of the order in which LimboAPI and LimboAuth plugins are loaded.

## Commands and permissions
The main command is ```/vclist```, it shows you information about VelocityCoolList, below are its arguments:
| Argument| Description                               | Permission    |
|---------|-------------------------------------------|---------------|
| enable  | Enables whitelist.                        | vclist.admin  |
| disable | Disables whitelist.                       | vclist.admin  |
| add     | Add player to the whitelist.              | vclist.manage |
| remove  | Remove player from the whitelist.         | vclist.manage |
| list    | Gives you a list of whitelisted players.  | vclist.manage |
| reload  | Reload plugin.                            | vclist.admin  |
| clear   | Clears the whitelist.                     | vclist.manage |
| status  | Get the status of the plugin.             | vclist.admin  |

Aliases: ```/vcl```, ```/velocitycoollist```

## Colors and formatting
This plugin supports MiniMessage modern formatting, which allows you to make gradients and many other things.
Here is what this formatting supports, its documentation is in config and [here](https://docs.advntr.dev/minimessage/format.html#standard-tags)!

![picture1](https://docs.advntr.dev/_images/rainbow_1.png) ![picture2](https://docs.advntr.dev/_images/newline_1.png) ![picture3](https://docs.advntr.dev/_images/insertion_1.png)
