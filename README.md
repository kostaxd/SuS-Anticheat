# SuS AntiCheat Plugin for Minecraft 1.21.4

A comprehensive, high-performance AntiCheat plugin for Minecraft Java Edition 1.21.4 servers running on Java 21.

## Features

### Detection Modules
- **Movement Detection**: Fly, Speed, NoFall, Jesus/Water Walk
- **Combat Detection**: KillAura, Reach, Auto-clicker, Critical hits
- **Block Detection**: Nuker, FastBreak, X-Ray (statistical)
- **Network Detection**: Packet spam, Timer detection
- **Bot Detection**: Anti-bot measures with behavioral analysis

### Core Features
- Real-time cheat detection with minimal performance impact
- Advanced pattern analysis for movement and combat
- Configurable thresholds to prevent false positives
- Automatic punishments (warn, kick, temporary ban, permanent ban)
- Comprehensive logging and database storage
- Staff alert system with customizable formats
- Performance monitoring and adaptive checking
- Extensible architecture for custom detection modules

### Compatibility
- Supports Spigot, Paper, and popular server forks
- Optimized for Minecraft 1.21.4
- Java 21 compatible
- SQLite and MySQL database support

## Installation

1. Download the latest release JAR file
2. Place it in your server's `plugins` folder
3. Start the server to generate default configuration
4. Configure the plugin in `plugins/SuS/config.yml`
5. Restart the server or use `/susreload`

## Configuration

The plugin comes with extensive configuration options in `config.yml`:

### Detection Settings
Each detection module can be individually configured with:
- Enable/disable toggle
- Maximum violations before punishment
- Violation decay time
- Punishment type
- Sensitivity adjustments

### Punishment System
Supports multiple punishment types:
- **Warn**: Send warning message to player
- **Kick**: Kick player from server
- **TempBan**: Temporary ban with configurable duration
- **Ban**: Permanent ban

### Performance Optimization
- Automatic performance mode when server TPS drops
- Configurable check intervals
- Async database operations
- Player data caching

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/sus help` | `sus.admin` | Show help message |
| `/sus reload` | `sus.admin.reload` | Reload configuration |
| `/sus info <player>` | `sus.admin.info` | View player statistics |
| `/sus stats` | `sus.admin` | Show server statistics |
| `/suskick <player> [reason]` | `sus.admin.kick` | Kick a player |
| `/susban <player> [reason]` | `sus.admin.ban` | Ban a player |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `sus.admin` | Full access to SuS AntiCheat | op |
| `sus.bypass` | Bypass all cheat detection | false |
| `sus.admin.reload` | Reload configuration | op |
| `sus.admin.info` | View player information | op |
| `sus.admin.kick` | Kick players | op |
| `sus.admin.ban` | Ban players | op |

## Database Support

### SQLite (Default)
- Zero configuration required
- Automatic database file creation
- Suitable for small to medium servers

### MySQL
Configure MySQL connection in `config.yml`:
```yaml
database:
  type: "mysql"
  mysql:
    host: "localhost"
    port: 3306
    database: "sus_anticheat"
    username: "sus_user"
    password: "sus_password"
```

## Detection Details

### Movement Checks
- **Fly**: Detects unauthorized flight and hovering
- **Speed**: Monitors horizontal movement speed
- **NoFall**: Detects fall damage bypass
- **Jesus**: Detects walking on water

### Combat Checks
- **KillAura**: Detects automated combat with angle and timing analysis
- **Reach**: Monitors attack distance beyond vanilla limits
- **Auto-clicker**: Analyzes click patterns and CPS
- **Criticals**: Detects fake critical hits

### Block Checks
- **Nuker**: Detects rapid block destruction
- **FastBreak**: Monitors block breaking speed
- **X-Ray**: Statistical analysis of ore-finding patterns

### Network Checks
- **Packet Spam**: Detects excessive packet sending
- **Timer**: Detects client-side game speed modification

## Performance

The plugin is optimized for minimal server impact:
- Async database operations
- Efficient player data caching
- Performance mode for low TPS situations
- Configurable check intervals
- Smart violation decay system

## Building from Source

Requirements:
- Java 21 JDK
- Maven 3.6+

```bash
git clone https://github.com/susanticheat/sus.git
cd sus
mvn clean package
```

The compiled JAR will be in the `target` directory.

## API for Developers

SuS AntiCheat provides an API for developers to create custom detection modules:

```java
public class MyCustomCheck extends Check {
    public MyCustomCheck(SuSAntiCheat plugin) {
        super(plugin, "MyCheck", "custom.mycheck");
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!shouldCheck(event.getPlayer())) return;
        
        // Your detection logic here
        if (suspiciousActivity) {
            flag(event.getPlayer(), "Detected suspicious activity");
        }
    }
}
```

## Support

- Create an issue on GitHub for bug reports
- Join our Discord server for community support
- Check the wiki for detailed configuration guides

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## Changelog

### v1.0.0
- Initial release
- All core detection modules
- Database integration
- Configurable punishment system
- Performance optimization features
- Comprehensive logging system