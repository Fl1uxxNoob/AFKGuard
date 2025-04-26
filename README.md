# AFKGuard

Advanced AFK detection and management system for Minecraft servers.

![version](https://img.shields.io/badge/version-1.2-blue.svg)
![MC version](https://img.shields.io/badge/MC-1.8.8-brightgreen.svg)

## Overview

AFKGuard is a comprehensive plugin for Minecraft servers that detects, manages, and prevents abuse by AFK (Away From Keyboard) players. The plugin provides customizable detection methods, verification systems, and administrative tools to ensure a fair and active server environment.

## Features

### 🕹️ AFK Detection
- **Multiple Detection Methods**:
  - Simple: Basic detection of player movement
  - Advanced: Sophisticated detection considering minimal movements, camera rotation, and more
- **Customizable Thresholds**:
  - Minimum movement distance
  - Camera rotation sensitivity
  - AFK trigger timeout

### 🔔 Notification System
- Broadcast AFK status changes to all players (optional)
- Customizable messages with color support

### 👮 Verification System
- Random verification prompts for AFK players
- GUI-based verification: click the correct block to verify
- The correct block changes position randomly each time
- Configurable verification timeout with auto-kick option

### ⚙️ Management Features
- Automatic AFK marking after configurable inactivity period
- AFK command for manual AFK toggling
- Option to kick AFK players after extended periods
- Designated AFK area teleportation
- Command blocking for AFK players (with whitelist)

### 📊 Logging & History
- SQLite database for tracking AFK events
- Comprehensive logging of:
  - Auto-AFK status changes
  - AFK kicks
  - AFK teleportations
- View player AFK history with `/afk history` command

### 🔒 Permissions
- `afkguard.bypass`: Bypass AFK detection and verification
- `afkguard.admin`: Access administrative commands

## Commands

- `/afk` - Toggle AFK status manually
- `/afk check [player]` - Check AFK status for specific player or all players
- `/afk reload` - Reload the plugin configuration
- `/afk verify` - Respond to an AFK verification prompt
- `/afk history [player] [limit]` - View AFK history for a player

## Configuration

### config.yml
```yaml
# Main AFK detection settings
settings:
  afk-time: 300          # Time in seconds before marking player AFK
  kick-time: 600         # Time before kicking AFK players (0 to disable)
  use-afk-area: false    # Whether to teleport instead of kick
  detection-method: "ADVANCED"  # SIMPLE or ADVANCED detection
  block-commands: true   # Whether to block commands while AFK
  allowed-commands:      # Commands allowed while AFK
    - "afk"
    - "msg"
  broadcast-afk-messages: true  # Announce AFK status changes

# Verification system settings
verification:
  enabled: true          # Enable verification system
  interval: 450          # Seconds between verifications
  chance: 65             # Probability of verification (0-100)
  timeout: 60            # Seconds to respond before kick
  # UI customization options...

# AFK area coordinates (when use-afk-area is true)
afk-area:
  world: "world"
  x: 0
  y: 100
  z: 0
  yaw: 0
  pitch: 0

# Messages customization
messages:
  prefix: "&7[&bAFK&fGuard&7] "
  # Various messages...
```

### settings.yml
```yaml
# Detailed AFK detection thresholds
detection:
  advanced:
    min-movement-distance: 0.05
    max-small-movement: 2.0
    min-camera-yaw: 5.0
    min-camera-pitch: 5.0
  simple:
    consider-rotation: false
```

## Installation

1. Download the plugin JAR file
2. Place it in your server's `plugins` folder
3. Restart your server
4. Edit the configuration files as needed in `plugins/AFKGuard/`
5. Use `/afk reload` to apply changes

## Database

AFKGuard uses an embedded SQLite database to track all AFK-related events. The database is automatically created and managed by the plugin, with tables for:

- Auto-AFK events
- AFK kicks
- AFK teleports

You can query this data using the `/afk history` command.

## Permissions

- `afkguard.bypass` - Players with this permission will not be marked as AFK automatically and will not be verified
- `afkguard.admin` - Access to admin commands (check, reload, history)

## Support

If you encounter any issues or have suggestions for improvement, please create an issue on the GitHub repository.

## License

AFKGuard is licensed under the **GNU General Public License v3.0** (GPL-3.0).  
You are free to use, modify, and distribute this software under the terms of the license.  
A copy of the license is available in the [LICENSE](./LICENSE) file.
