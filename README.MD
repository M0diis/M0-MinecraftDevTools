# M0-Dev Tools
M0-Dev Tools is a client-side Fabric toolbox for Minecraft modders, builders, and technical players. It combines overlays, HUDs, macros, scripting, movement helpers, command tooling, and debugging utilities in one mod.

## Feature overview
### Visual overlays and inspectors
- Light level, redstone power, redstone update/BUD overlays
- Slime chunk, biome border, chunk border, and structure bounding overlays (structure boxes need singleplayer: servers
  don't send structure data)
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
- Secondary chat with addon/replacement render modes, draggable multi-window custom chat, tabs, per-tab regex routing, unread indicators, timestamps, and compact repeated messages
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
### Modules, favourites and profiles
- Module on/off states and settings are remembered between launches (restored when you join a world). Modules that
  need server opt-in (reach, xray, instant break, freecam, entity radar) are never switched on automatically.
- ClickHUD (`` ` ``): arrows to move (hold to repeat), PgUp/PgDn/Home/End to jump, Right for settings, Enter to toggle.
  Long lists scroll.
- Press `N` to open the module browser: search all modules, toggle them, and click settings with the mouse. Opened
  from ClickHUD it starts on the highlighted module. Click the star to favourite. In ClickHUD, Right Shift stars the
  selected module and favourites get their own category.
- `/m0profile save <name>`, `/m0profile load <name>`, `/m0profile next` (also a keybind), `/m0profile list`.
- Default keys avoid vanilla 26.3 keys; the full list is in `description.md` and key changes are in `changelog.md`.
  Mod hotkeys are ignored while F3 is held.
### New tools
- Ruler: bind "Ruler: mark point" (or `/ruler mark`) and mark two blocks to see size, block count, area and distance.
- Portal Linker (Overlays): remembers portals you visit and shows where the one you stand at leads, and warns when the
  way back ends at a different portal. `/portal` prints it, `/portal forget` clears this world.
- Packet Inspector: `/packets` (or its keybind) to browse sent/received packets with filters; `/packets top` for counts.
  `/packets pin <name>` (or the Pin button) graphs one packet type per second; `/packets export` (or Export) saves
  the log to `config/m0-dev-tools/packet-logs/` as a tab-separated file.
- Redstone Timeline (Utilities): `/rstimeline start [radius]` records every redstone component around you tick by tick;
  `/rstimeline` opens a logic-analyser view (hover for signal and pulse length, click to set a mark and measure
  delays in game and redstone ticks; the mark snaps to signal edges). Drag to pan, "Copy" puts the recording on the
  clipboard as CSV, and the recorded area is outlined in the world while recording. Add a "Redstone Timeline" element
  on the HUD canvas to watch the busiest tracks live. It reads what the client sees, so 0-tick pulses don't show.
- `//materials` counts every block type in the WorldEdit selection (with stacks and shulkers), `//materials copy`
  copies it as CSV.
- Chat Highlights (Chat): `/highlight add <color> <word>` or `/highlight addregex <color> <regex>` colours matching
  text, `/highlight sound <n>` adds a ping. `{me}` stands for your name (on by default). `/highlight list`, `remove`, `test`.
- Command history search: in chat press Ctrl+R and type to search earlier commands (fuzzy), Ctrl+R again for older
  matches, Ctrl+Shift+R for newer.
- Item data editor: Save first shows what changed, press Confirm to send it. Undo button / Ctrl+Z undoes edits.
- Range Overlays (Overlays): beacon and conduit reach, spawn chunks (`/ranges spawnradius <n>` to match the
  server), the 128-block random-tick ring and the 24/32/128 mob spawn/despawn spheres. `/ranges pin` keeps the rings
  at your AFK spot. The Chunk Border Overlay can also show region file borders and which `r.X.Z.mca` you're in.
- Debug Views (Overlays, next to Mob AI Visualizer): collision shapes (red) vs. walk-through outlines (yellow),
  heightmaps (green = world surface, red = motion blocking), block entities with counts per type and the busiest
  chunk, and fluid levels with flow arrows. `/debugview <collision|heightmaps|blockentities|fluids> [on|off]`.
- Quick Edit: `/getdata quick` (or "Quick Edit" in the data browser) edits an entity's name, health and flags
  (glowing, no gravity, silent, invulnerable, no AI, persistent, name visible) or a block's state properties and
  container name. It shows the exact `/data` and `/setblock` commands before sending them.
- SNBT editor: Ctrl+Z/Ctrl+Y undo/redo, Format and Compact buttons (Ctrl+Shift+F), auto-indent, closing brackets and
  quotes, bracket matching, line numbers, the error's line and column (Ctrl+E jumps there), Ctrl+arrows by word.
  Saving from `/getdata` first lists what changed.
- Placement Restriction (Utilities): while you hold right-click, blocks only go on the `plane`, `face`, `column`
  or `line` set by the first block. `/placement <mode|off>` or the "Cycle placement restriction" key.
- Farm Rate Meter (Utilities): `/farmrate start [radius|selection]` counts items that appear in a zone,
  `/farmrate inventory` counts what you pick up. Shows totals, items per hour and the last 5 minutes;
  `/farmrate copy` gives CSV. Works on any server (`/mobdrops` is the exact server-side tracker).
- Freecam: `/freecam save [1-9]`, `/freecam go <1-9>`, `/freecam list` keep camera spots per world (also keys
  "save camera spot" / "go to next saved spot"); "Freecam: control player" moves your player while the camera
  stays; `/freecam speed <x>`; freecam turns off when you take damage (setting).
- `/camera record [seconds]` … `/camera record stop` turns a flight into a camera path.
- Light Overlay has a "Spawn spots" mode (red = mobs can spawn any time, yellow = at night); the Redstone Overlay
  also shows repeater delay, comparator output and powered components.
- Waypoints: beams, `/waypoint track <id|nearest|off>` for a HUD arrow, overworld waypoints shown in the nether
  (÷8) and back, and TP uses the server's `/tp` when you have permission.
### Scripting
- Scripts in `config/m0-dev-tools/scripts` run with `/runscript <file>`, a keybind, a HUD button or an automation rule.
- Scripts run on the game thread with a 5 s limit. Put `// @async on` as the first line to run a script in the
  background instead (2 min limit, one at a time). `/runscript stop` stops running scripts, `/runscript running` lists them.
- Every script gets an `m0` helper: `m0.message(text)`, `m0.actionbar(text)`, `m0.chat(text)`, `m0.command(cmd)`,
  `m0.placeholders("{player.x}")`, `m0.macro(id)`, `m0.sleep(ms)` (async only), `m0.sync { ... }` / `m0.call { ... }`
  (run on the game thread from an async script) and `m0.checkCancelled()` for long loops.
- The script editor has a Console tab (`m0.print(...)`, results, errors), Ctrl+Shift+R runs the selection, and
  autocomplete knows `m0` and `event`.
- Ctrl+J opens the snippet list (nearby entities, block scan, repeat with delay, ...); Ctrl+Shift+F re-indents the
  script (Ctrl+Z undoes it).
- The macro workbench (M) has a Scripts tab: every script with a coloured preview, Run (F5), Stop all, a key
  binding, which automation rules use it, and the script console. Double-click or Enter opens it in the editor,
  which returns to the workbench when closed. "+ New script" creates a Kotlin, Groovy or JS starter file.
- The script editor works like a small IDE:
  - Files open in tabs (● = unsaved); click × or middle-click to close, Ctrl+W, Ctrl+Tab to switch. Open files and
    unsaved edits stay open until you quit the game. Ctrl+N new, Ctrl+S save, Ctrl+Shift+S save all.
  - Ctrl+F find, Ctrl+H replace (Alt+C match case, Alt+W whole word), Ctrl+G go to line, Ctrl+O go to a
    function/class/value.
  - Ctrl+/ comment, Ctrl+D duplicate, Alt+Up/Down move lines, Ctrl+Shift+K delete line, Ctrl+Left/Right by word,
    brackets and quotes close themselves (and wrap a selection), Enter between brackets opens an indented block.
  - Highlighting understands block comments, multi-line strings, annotations, function calls and the built-in
    variables; red line numbers mark problems (hover for the message); a status bar shows line, column and what's
    running. F5 runs, Shift+F5 stops all scripts, the console has a Clear button.
- Groovy and JavaScript scripts can be stopped mid-loop. Kotlin scripts cannot be interrupted from outside; call
  `m0.checkCancelled()` or `m0.sleep()` inside long Kotlin loops.
### Automation regex
- `REGEX` filters and conditions match anywhere in the text (anchor with `^...$` for a full match) and respect
  "ignore case". Capture groups are available to actions as `{event.group0}` (whole match), `{event.group1}`, ...
  and `{event.group.<name>}` for named groups, e.g. `/tpaccept {event.group1}`.
- `/automation test <rule> [input]` explains step by step whether a rule would fire and what it would run, without
  running it. `/automation template` lists ready-made rules (mention ping, low health, death coordinates, low food,
  teleport-request accept). The new "SHOW_MESSAGE" action shows text only to you, optionally with a ping.
### Automation folders and sharing
- `/automation folder <rule> <folder>` (`-` to remove) groups rules; `/automation folder-on|folder-off <folder>`
  switches a whole folder. `/automation list` shows folders and how often each rule fired this session.
- `/automation export [all|<rule>|folder:<name>]` copies rules to the clipboard as JSON, `/automation import` adds
  them from the clipboard. Imported rules that run commands, scripts, macros or toggle modules are added switched
  off so you can check them first.
### Waypoints
- Waypoints are saved per server (by address) or per singleplayer world. Waypoints from older versions show as
  `[any world]` until you run `/waypoint claim` in the world they belong to.
## Testing
Run unit tests:
```powershell
.\gradlew.bat test
```
Current test coverage includes:
- automation engine (`AutomationEngineTest`, `AutomationEventTest`, `ActionExecutorTest`, `AutomationRegexTest`)
- script runner (`ScriptRunnerTest`)
- item component metadata (`ItemComponentMetadataTest`)
- utility parsing/evaluation (`StyledTextParserTest`, `MathExpressionEvaluatorTest`, `NbtEditorUtilsTest`)
## Requirements
- Minecraft `26.3`
- Java `25+`
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
