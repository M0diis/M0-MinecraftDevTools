# Changelog

Version names follow `j<java>-mc<minecraft>-<version>`. Dates are when the version was set in the project.

## Unreleased

### New tools
- **Chest Finder** (Chest Tracker style): remembers what was in every chest, barrel, shulker box, hopper, furnace, ... you open, per world and dimension (items inside shulker boxes count too). `/finditem <item>` lists where it is with distances and highlights the containers through walls with a pointer to the nearest one; `/finditem gui` (or a keybind) opens a searchable item grid. `/finditem forget`, `/finditem clear`. Broken containers are forgotten automatically.
- **Freelook**: hold Left Alt (or set it to toggle) to look around your player in third person while it keeps facing and walking the same way.
- **Tick Controls**: keys for vanilla `/tick freeze`, `/tick step <n>` and slower tick rates, with a HUD badge while the game is frozen or slowed. Needs permission to run `/tick`.
- **`/glow <type|hostile|passive|items|players|all> [color]`**: client-side glowing outlines for entities; only you see them.
- **Status HUD canvas element**: armor or held-item durability (percent, remaining or both; vertical or horizontal), active effects with timers that blink before they run out, or a durability alert that only shows when gear is low.

### Improvements
- Redstone Timeline: Freeze / Step / "Freeze on change" buttons. "Freeze on change" freezes the game on the next signal change so you can step through it tick by tick.
- `/scanblocks <radius> <block>` outlines every match in the world for 10 minutes (mined blocks disappear), with a line and HUD arrow to the nearest one; `/scanblocks clear`.
- `/math`: variables (`/math x = 5*64`, `ans`, `px py pz`), Minecraft units (`3 stacks + 5`, `2.5 min`, `300 nether`) and conversions (`3000 in stacks`, `1200 ticks in s`, `px in nether`). `/math vars`, `/math clear`.
- Command completion: SNBT keys and values in `/summon` and `/data merge` (picked by entity type or what you're looking at, using 26.x names like `equipment` and `active_effects`), NBT paths in `/data get`, and common values for item components (`/give @s diamond_sword[rarity=`).
- Packet Inspector: packet sizes, in/out bandwidth and packets-per-second graphs, a pinned packet type graph, "Most bytes" list and `/packets stats`. `/packets pin`, `/packets unpin` and `/packets export` are back.
- Module Browser: number settings are sliders; drag, scroll over them (Shift = ×10) or click the value to type one (radius, distance, reach, gamma, zoom FOV, GUI scale, flash alpha).
- Script editor: restyled to match the mod's theme, Ctrl+E recent files (Ctrl+E, Enter flips back to the previous file), `m0` completions show what each helper does, completions use real parameter names.

### Fixes
- JavaScript scripts failed with a host access error on the shared engine. Scripts now run on the GraalJS Polyglot API with one shared engine.
- `m0.print` (used by the docs and snippets) didn't exist; it now writes to the script console.

## 2.0.0-BETA - `j25-mc26.3-2.0.0-BETA` (2026-10-17)

### Minecraft 26.3
- Updated to Minecraft 26.3 and Java 25.
- Macro and script key bindings saved by 1.21.x builds (old GLFW key codes) are converted automatically when loaded, and a `.pre-26.bak` backup of the old file is kept. Saved files now carry a format version.
- New flat dark theme for every screen (buttons, text fields, panels); the Macro Workbench uses sidebar navigation.
- Overlays use the new 26.x render pipeline (rendering is prepared on the game thread and submitted on the render thread).

### Default key changes
Defaults no longer clash with vanilla 26.3 keys or with each other. Your own saved bindings are not changed. If you used the old defaults, rebind in Options → Controls.

| Action                                  | Old | New     | Reason                                                           |
|-----------------------------------------|-----|---------|------------------------------------------------------------------|
| Open Module Browser                     | `O` | `N`     | vanilla Friends List is `O`                                      |
| Toggle Secondary Chat                   | `P` | `Y`     | vanilla Social Interactions is `P`                               |
| Open Entity Radar Screen                | `L` | `R`     | vanilla Advancements is `L`                                      |
| Zoom (hold)                             | `C` | `Z`     | vanilla "save hotbar" activator is `C`                           |
| Open Macro Workbench (Keyboard)         | `U` | unbound | `U` is Freecam; the Keyboard tab is in the Macro Workbench (`M`) |
| Better Command Block UI: area selection | `;` | unbound | `;` is Command History                                           |

- Mod hotkeys are ignored while F3 is held, so vanilla F3+key debug shortcuts (F3+N, F3+B, ...) never also trigger a mod action.

### ClickHUD and modules
- Redesigned ClickHUD:
  - Themed panels with enabled/total counts per category
  - Switches instead of ON/OFF text
  - Settings shown as a label with a colored value; action settings marked with ↵
  - Key hints shown under the panels
- Long lists scroll (with a scrollbar) instead of running off the screen.
- Held arrow keys repeat; PgUp/PgDn/Home/End jump; each category remembers its selected module.
- ClickHUD no longer reacts to arrow keys while a screen (chat, inventory, ...) is open.
- Module Browser:
  - Settings panel on the right that you change with the mouse
  - Toggle switches and favourite stars
  - Opens on the module highlighted in ClickHUD
- Module on/off states and settings are saved between launches and restored when you join a world. Modules that need server opt-in are never switched on automatically.
- Favourites (Right Shift in ClickHUD, ★ in the browser) get their own category.
- Profiles: `/m0profile save|load|list|next`, plus a "next profile" keybind.

### New tools
- **Redstone Timeline**:
  - `/rstimeline start [radius]` records every redstone component around you, tick by tick; `/rstimeline` opens a logic-analyser view
  - Hover for signal and pulse length; click to set a mark that snaps to signal edges and measures the span to the cursor
  - Drag to pan; Copy the recording as CSV
  - The recording area and watched components are outlined in the world
- **Redstone Timeline HUD canvas element** shows the busiest components live (rows, time window, labels and colour are configurable).
- **Range Overlays**: beacon and conduit reach, spawn chunks (`/ranges spawnradius`), random-tick ring, 24/32/128 mob spawn spheres, `/ranges pin`.
- **Debug Views**: collision vs. walk-through shapes, heightmaps, block entities (counts per type, busiest chunk) and fluid levels with flow arrows (`/debugview`).
- **Portal Linker**: remembers nether portals and shows where the one you stand in leads; warns about mismatched return portals (`/portal`).
- **Packet Inspector**: browse sent/received packets with filters, `/packets top`, pin a packet type to graph it, export a log.
- **Ruler**: mark two blocks for size, block count, area and distance.
- **Farm Rate Meter**: `/farmrate start [radius|selection]`, `/farmrate inventory`, items per hour, last 5 minutes, CSV copy.
- **Placement Restriction**: plane, face, column or line placement while holding right-click (`/placement`, keybind).
- **Chat Highlights**: colour words or regex matches in chat, optional ping, `{me}` for your name (`/highlight`).
- **Command history search**: Ctrl+R in chat for fuzzy search of earlier commands.
- **Quick Edit** (`/getdata quick`): edit entity name, health and flags or block state properties; shows the exact commands first.
- **Camera path recording**: `/camera record [seconds]` turns a flight into a camera path.
- **Freecam**: saved camera spots per world (`/freecam save|go|list|clear`), "control player" mode, `/freecam speed`, optional turn-off on damage.
- **`//materials`**: counts every block type in the WorldEdit selection, with stacks and shulkers; `//materials copy` for CSV.
- Chunk Border Overlay can show region file borders and the current `r.X.Z.mca`.
- Light Overlay "Spawn spots" mode (red = any time, yellow = at night).
- Waypoints:
  - Beams and a HUD arrow (`/waypoint track`)
  - Overworld waypoints shown in the nether (÷8) and back
  - TP uses `/tp` when you have permission
  - Saved per server or world (`/waypoint claim` for waypoints from older versions)

### Scripting and automation
- Script editor is now a small IDE:
  - Tabs, find/replace, go to line or symbol, line operations
  - Auto-closing brackets, better highlighting, problem markers
  - Snippets (Ctrl+J), formatter (Ctrl+Shift+F), console tab, status bar
- Scripts run with a 5 s limit on the game thread; `// @async on` runs them in the background (2 min limit). `/runscript stop`, `/runscript running`.
- New `m0` script helper: `message`, `actionbar`, `chat`, `command`, `placeholders`, `macro`, `sleep`, `sync`, `checkCancelled`.
- Macro Workbench Scripts tab: list with preview, run, stop, key binding, rules that use the script, console, "+ New script".
- Automation:
  - Regex matching with capture groups (`{event.group1}`, named groups)
  - `/automation test` explains a rule step by step; `/automation template` adds ready-made rules
  - New SHOW_MESSAGE action
  - Folders, plus export/import through the clipboard (imported rules that run things start switched off)

### Data tools
- Item data editor: Save shows what changed before sending; undo.
- SNBT editor: undo/redo, format/compact, auto-indent, bracket matching, line numbers, error line and column.
- `/getdata` server side: block data is only sent to players with permission level 2 (or the singleplayer owner), only for loaded chunks, and requests are rate limited.

### Fixes
- Redstone Overlay: labels on repeaters, comparators, torches and other components were drawn flat on the block and hidden under its torches. They now float above the block, face the camera and have a dark backing. Repeaters also show when they are locked.
- Structure Bounding Boxes never showed anything (servers don't send structure data). They now work in singleplayer, with structure pieces and name labels and settings for radius, pieces and labels. On servers the module says it isn't available.
- Held Light caused big frame drops: every 2 blocks of movement it marked about 2.5 million block updates for rebuilding. It now rebuilds only the chunk sections the light reaches.


## 1.3.0-BETA - `j21-mc1.21.11-1.3.0-BETA` (2026-06-22)
- Secondary chat:
  - Customisable settings and window management
  - Tabs with per-tab routing
  - Better mouse handling and message selection
- Pick block picks the best hotbar slot.
- WorldEdit module: command registration and configuration options.
- Fix: item pickup notifications for damaged items were split into separate entries.

## 1.2.0-BETA - `j21-mc1.21.11-1.2.0-BETA` (2026-06-14)
- WorldEdit module: command queue, selections, undo/redo, regions with selection shapes, masks and block targets.
- Mob drop tracking (`/mobdrops`) and camera control improvements.
- Removed unused dependencies; fixed a button reference in the Macro Workbench.

## 1.1.0-BETA - `j21-mc1.21.11-1.1.0-BETA` (2026-06-12)
- Automation module: event-based rules, commands and rule management; new events for world leave, player death, weather and level changes.
- JavaScript scripting support with example scripts.
- Mob AI Visualizer and `/mobai` debugging commands.
- `/itemdata` item data management with sync and commands; `/getdata` target view, actions, NBT editing and colour support.
- HUD Tweaks module; vanilla HUD elements on the canvas (move, scale, live previews).
- Server-side opt-in for modules that count as cheats (Xray, Reach, Instant Break, Freecam, Entity Radar).
- Xray: block cache and faster chunk scanning.
- Macro UI components (text fields, toggles, selection lists).

## 1.0.0-BETA - `j21-mc1.21.11-1.0.0-BETA` (2026-06-04)
Version numbers restarted at 1.0.0-BETA.
- Better Command Block UI integrated.
- Mouse Tweaks, Hunger Tweaks and Bridging Tweaks modules.
- All default keys collected in one keybind catalog.
- Script renaming and better suggestions in the script editor.
- Fix: secondary chat settings modal.

## Pre-beta builds (Minecraft 1.21.11)

### 2.2.0 - `j21-mc1.21.11-2.2.0` (2026-06-02)
- Tweaks module (hide particles, offhand, fog, rain, sounds, camera shake and bobbing, permanent sneak/sprint, ...).
- Fast Block Placement and Reach modules.
- HUD canvas: inventory element, layers (z-index), placeholder catalog.

### 2.1.0 - `j21-mc1.21.11-2.1.0` (2026-06-01)
- Held Light module and new HUD layout options.
- Async script execution and script execution modes.
- Entity model rendering on the HUD; script editor improvements.
- HUD rendering moved to Fabric's HUD element registry; ClickGUI renamed to ClickHUD; Freecam fixes.

### 2.0.0 - `j21-mc1.21.11-2.0.0` (2026-05-28)
- HUD canvas element types: icons, bars, values, lists, shapes and state badges.
- Compass arrow placeholders, better item id suggestions.
- Watson / CoreProtect module.
- Custom render layers, camera handling improvements, data sync.

### 1.0.6 - `j21-mc1.21.11-1.0.6` (2026-05-18)
- Updated to Minecraft 1.21.11.
- Macro HUD overlay.

## Minecraft 1.21.4 builds

### 1.0.4 - `j21-mc1.21.4-1.0.4` (2026-01-24)
- Action Runner module with client and server commands.
- Waypoints: management GUI and rendering options.
- Command Block and Redstone Update Order overlays.
- Message history; faster multiplayer server pinger.

### 1.0.3 - `j21-mc1.21.4-1.0.3` (2026-01-05)
- ClickGUI with categories and per-module settings.
- UI Utilities module and other module tweaks.

### 1.0.2 - `j21-mc1.21.4-1.0.2` (2026-01-04)
- NBT get command.
- Secondary chat: options to reset transparency on hover and to disable transparency while chat is open.

### 1.0.1 - `j21-mc1.21.4-1.0.1` (2025-12-31)
- Secondary chat: smaller resize handle; position and size are saved once you finish dragging.
- Config toggles are saved.

### 1.0.0 - `j21-mc1.21.4-1.0.0` (2025-12-30)
- Initial release.
