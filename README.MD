# M0-Dev Tools
M0-Dev Tools is a client-side Fabric toolbox for Minecraft modders, builders, and technical players. It combines overlays, HUDs, macros, scripting, movement helpers, command tooling, and debugging utilities in one mod.
## Quick links
- Docs/Wiki source: `docs/README.md`
- Architecture: `docs/architecture.md`
- Module catalog: `docs/module-catalog.md`
- Automation deep dive: `docs/automation-engine.md`
- Macro + placeholder deep dive: `docs/macro-workbench-and-placeholders.md`
## Feature overview
### Visual overlays and inspectors
- Light level, redstone power, redstone update/BUD overlays
- Slime chunk, biome border, chunk border, and structure bounding overlays
- Command block overlay
- Block inspector / WAILA-style block text
- NBT info HUD, NBT tooltips, shulker preview tooltips
- Entity radar and health bars
### Building, movement, and utility modules
- Xray, instant break, fast block placement, reach
- Inventory move, mouse tweaks, bridging tweaks
- Fullbright, held light, zoom, freecam
- Waypoints and quick teleport
- Hunger tweaks and dynamic FPS
### Chat and workflow helpers
- Secondary chat
- Command history and message history
- Better command block UI integration
- UI utilities for packet send/delay and screen behavior helpers
### Macros, scripting, and automation
- Macro manager and keybind workflows
- Macro workbench canvas/editor
- Placeholder system and provider extension points
- Script execution: Groovy (`.groovy`), Kotlin (`.kts`), JavaScript (`.js`)
- Rule-based automation engine (`/automation`)
- Action runner command set
### Debug/admin tools
- `/draw` shape/debug rendering tools
- `/getdata` block/entity data inspection
- `/itemdata` item component inspector/editor
- Watson/CoreProtect-style tracking helpers
## Architecture notes
- Entrypoints:
  - `me.m0dii.M0DevTools`
  - `me.m0dii.M0DevToolsClient`
  - `bettercommandblockui.main.BetterCommandBlockUI`
- Mixins:
  - `src/main/resources/m0dev.mixins.json`
  - `src/main/resources/bettercommandblockui.mixins.json`
- Module packages are documented in `docs/module-catalog.md`.
## Installation
1. Download the latest release from GitHub Releases or build from source.
2. Place the mod JAR in your Minecraft `mods` folder.
3. Launch Minecraft with Fabric Loader.
## Build from source
Windows:
```powershell
.\gradlew.bat build
```
Linux/macOS:
```bash
./gradlew build
```
Build outputs are written to `build/libs/`.
## Usage
- Open modules from keybinds / ClickHUD.
- Configure modules in the UI.
- Use command workflows for advanced tools:
  - `/draw`
  - `/getdata`
  - `/itemdata`
  - `/automation`
  - `/runscript`
  - `/waypoint`
  - `/watson`
## Testing
Run unit tests:
```powershell
.\gradlew.bat test
```
Current test coverage includes:
- automation engine (`AutomationEngineTest`, `AutomationEventTest`, `ActionExecutorTest`)
- item component metadata (`ItemComponentMetadataTest`)
- utility parsing/evaluation (`StyledTextParserTest`, `MathExpressionEvaluatorTest`, `NbtEditorUtilsTest`)
## Requirements
- Minecraft `1.21.11`
- Java `21+`
- Fabric Loader
- Fabric API
## Contributing
Contributions, issues, and feature requests are welcome.
When making substantial feature changes, update the corresponding docs page in `docs/` so wiki content stays current.
## Support
When reporting issues, include:
- Minecraft version
- mod version
- relevant logs (`logs/latest.log`)
