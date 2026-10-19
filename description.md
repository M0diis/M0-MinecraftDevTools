# M0-Dev Tools

M0-Dev Tools is a client-side Fabric toolbox for Minecraft modders, builders, and technical players. It bundles overlays, HUDs, movement helpers, macros, scripting, automation, and debugging tools into one mod.

**Version 2.1.0-BETA for Minecraft 26.3 (Java 25, Fabric Loader, Fabric API).**

**Some modules (X-Ray, Freecam, Reach, Instant Break, Entity Radar) only work when the server also has the mod installed and opts in.**

## Disclaimer

This started as a personal project and I decided to release it publicly. It may contain bugs and unfinished features, and some features may not work with certain servers or other mods. Use at your own risk.

Things can change with any update, and I may not be able to provide support for every issue.

## What's new in 2.1.0-BETA

- **New tools**: Chest Finder (`/finditem`, with a contents preview in its GUI), Freelook, Tick Controls, `/glow`, and a Status HUD element (durability, effects, durability alert).
- **Improvements**: Redstone Timeline freeze/step, `/scanblocks`, `/math` variables and units, SNBT/NBT/component command completion, Packet Inspector bandwidth graphs and `/packets stats`, slider settings in the Module Browser, restyled script editor with recent files and better completions.
- **Safer**: slow chat highlight rules are skipped instead of freezing the game, and config files are written atomically.
- **Fixes**: JavaScript scripts on the shared engine, `m0.print`, typing in the SNBT editor on 26.3.

See version changelog for the full list.

## What's new in 2.0.0-BETA

- **Minecraft 26.3.** Macros and script key bindings saved by 1.21.x versions are converted automatically (a backup is kept).
- **New flat dark theme** across every screen, with sidebar navigation in the Macro Workbench.
- **Redesigned ClickHUD**: scrolling lists, switches, coloured setting values, hold-to-repeat keys, and a mouse-driven **Module Browser** with a settings panel.
- **Module states, favourites and profiles** are saved between launches.
- **New tools**: Redstone Timeline, Range Overlays, Debug Views, Portal Linker, Packet Inspector, Ruler, Farm Rate Meter, Placement Restriction, Chat Highlights, Quick Edit, camera path recording, `//materials` and more.
- **Script editor upgraded to a small IDE**, plus async scripts, a script console and a Scripts tab in the Macro Workbench.
- **Fixes**: Redstone Overlay labels on repeaters and comparators, Structure Bounding Boxes, Held Light lag, Freecam "Show Player Model".
- **Default keys** no longer clash with vanilla 26.3. See [Default keybindings](#default-keybindings).

See version changelog for the full list.

## Default keybindings

All keys can be changed in Options → Controls → Key Binds (category "M0-Dev Tools"). Mod hotkeys are ignored while F3 is held, so vanilla F3 debug shortcuts never trigger them.

| Action                         | Default key             |
|--------------------------------|-------------------------|
| Open ClickHUD                  | `` ` `` (grave / tilde) |
| Open Module Browser            | `N`                     |
| Open Macro Manager / Workbench | `M`                     |
| Open Macro Workbench (Canvas)  | `K`                     |
| Open Script Editor             | `,` (comma)             |
| Open Command History           | `;` (semicolon)         |
| Open Message History           | `'` (apostrophe)        |
| Toggle Secondary Chat          | `Y`                     |
| Open Entity Radar Screen       | `R`                     |
| Toggle Freecam                 | `U`                     |
| Zoom (hold)                    | `Z`                     |
| Freelook (hold)                | Left Alt                |
| Toggle Fullbright              | `J`                     |
| Toggle Xray                    | `]`                     |
| Open Xray Menu                 | `[`                     |
| Toggle Block Inspector         | `F10`                   |
| Toggle Watson CoreProtect      | `.` (period)            |

Unbound by default (bind them if you use them): Redstone / Light / Slime Chunk / Biome Border / Command Block overlay toggles, Redstone BUD order, waypoint add/list/GUI/render toggle, quick teleport up/down/forward/left/right, Freecam control player / save spot / next spot, Ruler mark point, Packet Inspector, Tick freeze / step / slower, Chest finder search, cycle placement restriction, next module profile, Macro Workbench (Keyboard tab), and Better Command Block UI area selection.

## Feature overview

### Module menus, favourites and profiles

- **ClickHUD** (`` ` ``) - keyboard menu drawn over the game, so you can keep moving while it is open
  - Categories, modules and settings in three columns, with on/total counts per category
  - Arrows to move (hold to repeat), PgUp/PgDn/Home/End to jump, Right for settings, Enter to toggle or apply
  - Long lists scroll; each category remembers where you were
  - Right Shift stars a module; favourites get their own category
- **Module Browser** (`N`) - search every module by name, id or category, toggle with a click, and change settings with the mouse (number settings are sliders you can drag, scroll or type into). Opened from ClickHUD it starts on the highlighted module
- **Saved state** - module on/off states and settings are remembered between launches and restored when you join a world. Modules that need server opt-in are never switched on automatically
- **Profiles** - `/m0profile save|load|list <name>`, `/m0profile next` (also a keybind)

### Visual overlays & inspectors

- **Light level overlay** - light levels on blocks, with a "Spawn spots" mode (red = mobs can spawn any time, yellow = only at night)
- **Redstone power overlay** - wire power, repeater delay (and lock), comparator output and mode, torch and powered states. Component labels float above the block and face you, so they stay readable
- **Redstone update order / BUD overlay** - shows the order of redstone dust updates
- **Redstone Timeline** - logic-analyser for your contraptions (see [Redstone Timeline](#redstone-timeline))
- **Slime chunk overlay** - highlights slime chunks (singleplayer, needs the seed)
- **Biome border overlay** - biome boundaries
- **Chunk border overlay** - chunk borders, plus region file borders and which `r.X.Z.mca` you're in
- **Structure bounding boxes** - outlines villages, monuments, mineshafts, strongholds and more, with their pieces and name labels (singleplayer only: servers don't send structure data)
- **Range overlays** - beacon and conduit reach, spawn chunks (`/ranges spawnradius <n>`), the 128-block random-tick ring and the 24/32/128 mob spawn/despawn spheres. `/ranges pin` keeps the rings at your AFK spot
- **Debug views** - collision shapes vs. walk-through outlines, heightmaps, block entities with counts per type and the busiest chunk, fluid levels with flow arrows (`/debugview <collision|heightmaps|blockentities|fluids> [on|off]`)
- **Portal linker** - remembers nether portals you use, shows where the portal you stand in leads, and warns when the way back ends at a different portal (`/portal`, `/portal forget`)
- **Command block overlay** - command block contents in the world
- **Block inspector / WAILA-style block text** - details of the block you look at
- **NBT info HUD**, **NBT tooltips**, **shulker preview tooltips**
- **Entity radar / entity list** and **entity health bars**
- **Item pickup notifier**
- **Mob drops tracker** (`/mobdrops`) - kills, items per minute, unique items
- **Mob AI visualizer** (`/mobai`) - active goals, targets and paths

### Building, movement & utility tools

- **Xray** - selected blocks through walls (server opt-in)
- **Instant break**, **fast block placement**, **reach** (server opt-in where needed)
- **Placement restriction** - while holding right-click, blocks only go on the `plane`, `face`, `column` or `line` set by the first block (`/placement <mode|off>` or a keybind)
- **Ruler** - mark two blocks to see size, block count, area and distance (`/ruler mark` or a keybind)
- **Farm rate meter** - `/farmrate start [radius|selection]` counts items appearing in a zone, `/farmrate inventory` counts what you pick up; totals, items per hour, last 5 minutes, `/farmrate copy` for CSV
- **Inventory move**, **Mouse Tweaks**-style controls, **bridging tweaks** (snapping, torch placement, slab assist, replaceable targets)
- **Fullbright**, **held light** (dynamic light from held torches, lanterns, ...), **zoom**
- **Freelook** - hold Left Alt to orbit the camera around your player while it keeps looking and walking the same way (hold or toggle)
- **Chest finder** - remembers what's in every container you open (per world and dimension, shulker box contents included). `/finditem <item>` tells you where it is and highlights the containers through walls with an arrow to the nearest; `/finditem gui` or a keybind opens a searchable grid of everything you've stored
- **Tick controls** - keys for vanilla `/tick freeze`, `/tick step` and slower tick rates, with a HUD badge while frozen (needs `/tick` permission)
- **Freecam** - move the camera away from your body
  - Your player stays visible ("Show Player Model")
  - "Control player" moves your player while the camera stays put
  - Saved camera spots per world (`/freecam save [1-9]`, `/freecam go <1-9>`, `/freecam list`) and `/freecam speed <x>`
  - Optionally turns off when you take damage
- **Camera paths** - `/camera` keyframes and playback; `/camera record [seconds]` turns a flight into a path
- **Waypoints** - beams, `/waypoint track <id|nearest|off>` HUD arrow, overworld waypoints shown in the nether (÷8) and back, saved per server or world
- **Quick teleport** - keybinds to hop up/down/forward/sideways; `/top`, `/up`, `/asc`, `/desc`, `/thru`, `/jumpto`
- **Hunger tweaks**, **HUD tweaks**, **Dynamic FPS** (lower frame rate when the window isn't focused)
- **WorldEdit helpers** - client selection commands (`//pos1`, `//set`, `//replace`, `//stack`, ...), and `//materials` counts every block type in the selection (with stacks and shulkers; `//materials copy` for CSV)

### Chat & workflow helpers

- **Secondary chat** - addon or replacement render modes, draggable multi-window chat, tabs with per-tab regex routing, unread indicators, timestamps, compact repeated messages, message selection and copying
- **Chat highlights** - `/highlight add <color> <word>` or `/highlight addregex <color> <regex>` colours matching text, `/highlight sound <n>` adds a ping; `{me}` matches your name
- **Command history** - browse and rerun earlier commands; in chat press **Ctrl+R** to fuzzy-search them (Ctrl+R again for older, Ctrl+Shift+R for newer)
- **Message history** and longer chat history
- **[Better Command Block UI](https://github.com/Tectato/BetterCommandBlockUI)** - improved command block editor
- **UI utilities** - send/delay packets, save/restore GUI state, de-sync screens, close windows without packets, copy GUI title JSON, in-GUI command input
- **Packet inspector** - `/packets` browses sent and received packets with filters, sizes and in/out bandwidth graphs; `/packets top` for counts, `/packets stats` for bandwidth, `/packets pin <name>` graphs one type per second, `/packets export` saves a log

### Redstone Timeline

- `/rstimeline start [radius]` (or "Record here") records every redstone component around you, tick by tick
- `/rstimeline` opens a logic-analyser view
  - One row per component, game ticks left to right
  - Hover for signal strength and pulse length
  - Click to set a mark that snaps to signal edges; the span to the cursor is measured in game and redstone ticks
  - Drag or arrows to pan, Ctrl+scroll or +/- to zoom
- **Copy** puts the whole recording on the clipboard as CSV
- The recorded area and the watched components are outlined in the world while recording
- **Freeze / Step / Freeze on change** buttons drive vanilla `/tick`: arm "Freeze on change" and the game freezes on the next signal change, then step through it tick by tick
- Add a **Redstone Timeline** element to the HUD canvas to watch the busiest components live while you play
- It reads what the client sees, so 0-tick pulses don't show

### Macros, HUD canvas, scripting & automation

- **Macro manager / workbench** (`M`) - macros with key bindings, a keyboard layout view, a Scripts tab and automation rules
- **HUD canvas** (`K`) - build your own HUD from buttons, text, macro lists, icons, bars, values, lists, inventory views, shapes, state badges, the redstone timeline and status widgets (armor/held durability, effect timers, low-durability alert), plus vanilla HUD elements you can move and scale. Supports layers, anchors, presets and live previews
- **Placeholders** - dynamic values (`{player.x}`, compass arrows, target info, ...) with provider extension points
- **Scripts** in Groovy (`.groovy`), Kotlin (`.kts`) and JavaScript (`.js`)
  - Run with `/runscript <file>`, a keybind, a HUD button or an automation rule
  - Scripts run on the game thread with a 5 s limit; `// @async on` runs one in the background (2 min limit). `/runscript stop` and `/runscript running`
  - Every script gets an `m0` helper: `print`, `message`, `actionbar`, `chat`, `command`, `placeholders`, `macro`, `sleep`, `sync`, `checkCancelled`
- **Script editor** (`,`) - a small IDE
  - Tabs, find/replace, go to line or symbol, comment, duplicate and move lines
  - Auto-closing brackets, syntax highlighting with problem markers
  - Snippets (Ctrl+J), formatting (Ctrl+Shift+F), a console tab, F5 run / Shift+F5 stop
  - Ctrl+E recent files; completions for `m0` and game objects with parameter names
- **Automation engine** (`/automation`)
  - Event triggers (chat, death, health, weather, level, world join/leave, ...), conditions, and actions (commands, scripts, macros, module toggles, show-message)
  - Regex with capture groups (`{event.group1}`)
  - `/automation test` explains whether a rule would fire; `/automation template` lists ready-made rules
  - Folders, and export/import via the clipboard
- **Action runner** (`/actionrunner`) - queued use/attack/sneak/jump/drop/swap/slot actions and commands with delays

### Debugging & admin tools

- **`/draw`** - lines, boxes, circles, cylinders, spheres and selection outlines, saved and managed in a UI
- **`/getdata`** - block and entity NBT inspector and editor with a data browser. **Quick Edit** (`/getdata quick`) changes an entity's name, health and flags or a block's state properties, and shows the exact `/data` / `/setblock` commands first. The SNBT editor has undo/redo, formatting, bracket matching, line numbers and error locations
- **`/itemdata`** - item component inspector and editor with metadata, type hints and templates; Save shows what changed before sending, with undo
- **Watson / CoreProtect tracker** (`/watson`) - Watson-style visualisation of CoreProtect lookups with tracers, vectors, TTL, search and teleport helpers
- **Block scanner** (`/scanblocks <radius> [block]`) - counts blocks; with a block it outlines every match and points to the nearest
- **Math** (`/math`) - expressions, variables (`x = 5*64`, `ans`, `px py pz`) and Minecraft units: `/math 3000 in stacks`, `/math 1200 ticks in s`, `/math px in nether`
- **`/glow <type|hostile|passive|items|players|all> [color]`** - client-side glowing outlines on entities
- **Command completion** for SNBT in `/summon` and `/data` (keys and values), NBT paths, and item component values
- Placeholder tester (`/parseplaceholder`)

### Tweaks module

- **Particles**: hide own effect particles, disable block-breaking particles
- **Rendering**: hide offhand item, hide entities
- **Environment**: disable nether fog, rain effects and render-distance fog
- **Audio**: disable sounds
- **Movement**: disable wall-unsprint, angel block placement, permanent sneak and sprint
- **Camera**: disable hurt shake and view bobbing

## Scripting examples

Open the script editor with `,` (default).

#### Groovy: give yourself a starter kit (singleplayer)
```groovy
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

player.addItem(new ItemStack(Items.DIAMOND_SWORD))
player.addItem(new ItemStack(Items.BREAD, 16))

"Gave you a starter kit!"
```

#### Kotlin: spawn particles around the player
```kotlin
import net.minecraft.core.particles.ParticleTypes
import kotlin.math.cos
import kotlin.math.sin

for (i in 0 until 24) {
    val angle = Math.toRadians(i * (360.0 / 24))
    val x = player.getX() + cos(angle) * 1.5
    val y = player.getY() + 1.0
    val z = player.getZ() + sin(angle) * 1.5
    world.addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0.0, 0.03, 0.0)
}

"Spawned"
```

#### JavaScript: report your position
```javascript
m0.message("You are at " + m0.placeholders("{player.x} {player.y} {player.z}"));
"Done";
```

More examples (Groovy, Kotlin and JavaScript versions of each) are in the mod jar under `example_scripts/`. Put your own scripts in `config/m0-dev-tools/scripts`.
