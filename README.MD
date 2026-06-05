# M0-Dev Tools

M0-Dev Tools is a client-side Fabric toolbox for Minecraft modders, builders, and technical players. It combines overlays, HUDs, macros, scripting, movement helpers, and debugging utilities into one mod.

## Feature overview

### Visual overlays & inspectors

- Light level overlay
- Redstone power overlay
- Redstone dust update order / BUD overlay
- Slime chunk overlay
- Biome border overlay
- Chunk border overlay
- Structure bounding box overlay
- Command block overlay
- Block inspector / WAILA-style block text
- NBT info HUD
- NBT tooltips
- Shulker preview tooltips
- Entity radar / entity list
- Item pickup notifier
- ClickHUD

### Building, movement & utility tools

- Xray
- Instant break
- Fast block placement
- Reach
- Inventory move
- Mouse Tweaks support
- Bridging tweaks
- Fullbright
- Held light
- Zoom
- Freecam
- Quick teleport
- Waypoints
- Hunger tweaks
- Dynamic FPS

### Chat & workflow helpers

- Secondary chat
- Command history
- Message history
- Longer chat history
- Better command block UI
- UI utilities for send/delay packets, save/restore GUI state, de-sync screens, close without packet, copy GUI title JSON, and use an in-GUI command/message input field

### Macros, scripting & automation

- Macro manager
- Macro workbench canvas
- Macro workbench keyboard layout
- Macro placeholders
- Groovy scripting
- Kotlin scripting
- JavaScript scripting
- In-game script editor
- Action runner for queued client actions such as item use, offhand use, left click, sneak, jump, drop, command execution, offhand swapping, and hotbar selection

### Debugging & admin tools

- `/draw` for lines, boxes, circles, cylinders, spheres, selection outlines, saved shapes, listing/removing/clearing shapes, saving/loading, and the shape UI
- `/getdata` for block and entity NBT / data inspection, including crosshair targets and selectors
- Watson CoreProtect viewer, tracers, vectors, TTL controls, synthetic test data, and teleport helpers
- NBT Editor shortcut / integration

### Tweaks module

- Hide your own effect particles
- Hide the offhand item
- Disable block breaking particles
- Disable entity rendering
- Disable Nether fog
- Disable rain effects
- Disable sounds
- Disable wall-unsprint behavior
- Angel block
- Permanent sneak
- Permanent sprint
- Disable hurt camera
- Disable view bobbing
- Disable render distance fog

---

## Installation

1. Download the latest release from GitHub Releases or build from source.
2. Place the mod JAR into your Minecraft `mods` folder.
3. Launch Minecraft with Fabric Loader.

### Building from source

1. Clone this repository.
2. Run `./gradlew build` on Linux/macOS or `gradlew.bat build` on Windows.
3. The built JAR will be in `build/libs/`.

---

## Usage

- Open the modules screen from the keybinds menu.
- Most modules can be toggled and configured from the ClickGUI.
- Macros and scripting are available from the in-game GUI.
- Groovy (`.groovy`), Kotlin (`.kts`), and JavaScript (`.js`) scripts are supported; the script type is chosen by file extension.
- Common client commands include `/draw`, `/getdata`, `/actionrunner`, `/waypoint`, and `/watson`.

### Example scripts

- Curated examples live under `src/main/resources/example_scripts/`.
- Scripts are grouped by type: `client-effects/`, `inventory/`, `utility/`, and `world/`.
- Every example has Groovy (`.groovy`), Kotlin (`.kts`), and JavaScript (`.js`) counterparts.
- Current bundled examples:
  - `client-effects/particle-ring`
  - `inventory/give-starter-kit`
  - `utility/report-player-status`
  - `world/place-diamond-block-below`

---

## Requirements

- Minecraft 1.21.11
- Java 21+
- Fabric Loader
- Fabric API

---

## Contributing

Contributions, bug reports, and feature requests are welcome. Please open an issue or pull request on GitHub.

---

## Support

- For help, open an issue or join the Discord.
- Please include your Minecraft version, mod version, and logs when reporting a bug.

---
