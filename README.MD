# M0-Dev Tools Fabric Mod

A powerful client-side mod for Minecraft that enhances your gameplay experience with advanced tools, overlays, scripting, and automation. Designed for modders, power users, and anyone who wants more control and information in-game.

---

## Features

- [NBT Editor](https://modrinth.com/mod/nbt-editor)
- NBT Tooltips
- Inventory Move
- Block State Cycler
- Entity Radar / List
- Redstone Level overlay
- Chunk Border overlay
- Light Level overlay
- Slime Chunk overlay
- Redstone Dust Update Order overlay
- Command Block command overlay
- Secondary Chat Box
- Waila-like NBT/Block State Viewer
- Macros
- Command History Menu
- Message History Menu
- Longer Chat Box History
- In-Game Groovy/Kotlin Scripting
- Instant block break
- Fullbright
- Freecam
- Waypoints
- Zoom
- Faster Server Pinger

---

## Installation

1. Download the latest release from the [GitHub Releases](https://github.com/M0diis/M0-MinecraftDevTools) page or build from source (see below).
2. Place the mod JAR into your Minecraft `mods` folder.
3. Launch Minecraft with Fabric Loader (see Requirements).

### Building from Source

1. Clone this repository.
2. Run `./gradlew build` (Linux/macOS) or `gradlew.bat build` (Windows).
3. The built JAR will be in `build/libs/`.

---

## Usage

- Most features are accessible via keybinds or the in-game GUI.
- Macros and scripting can be managed from the mod's GUI or via commands.
- Scripting supports both Groovy (`.groovy`) and Kotlin (`.kts`) scripts. The script type is chosen by file extension.

### Example Scripts

#### Groovy (give yourself a diamond sword):
```groovy
import net.minecraft.item.Items
import net.minecraft.item.ItemStack

player.giveItemStack(new ItemStack(Items.DIAMOND_SWORD))

"Gave you a diamond sword!"
```

#### Kotlin (spawn particles around the player):
```kotlin
import net.minecraft.particle.ParticleTypes
for (i in 0 until 20) {
    val angle = Math.toRadians(i * (360.0 / 20))
    val x = player.x + Math.cos(angle) * 1.5
    val y = player.y + 1.0
    val z = player.z + Math.sin(angle) * 1.5
    world.addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0.0, 0.1, 0.0)
}
"Spawned 20 happy villager particles!"
```

---

## Requirements
- Minecraft 1.21.4
- Java 21+

---

## Contributing

Contributions, bug reports, and feature requests are welcome! Please open an issue or pull request on GitHub.

---

## Support
- For help, open an issue or join our Discord.
- Please include your Minecraft version, mod version, and logs if reporting a bug.

---
