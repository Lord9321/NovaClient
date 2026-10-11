# Nova Client (test client for your own server)

Fabric, Minecraft 1.21.11. Open the GUI with **Right Shift**.

## Build
1. https://fabricmc.net/develop -> 1.21.11, copy exact versions into `gradle.properties`.
2. JDK 21, `gradle build` (needs Gradle 9.x; or `./gradlew build` if you have the wrapper). After building, check the jar is not empty: `unzip -l build/libs/nova-client-2.7.4.jar` must list `fabric.mod.json` and `nova/*.class`. Jar: `build/libs/nova-client-2.7.4.jar` (not `-sources`). Put it in `mods/` with Fabric API.

## GUI
Icon tabs on the left, module cards in two columns. Left click = toggle, right click = fold out settings,
middle click = bind key (Esc cancels, Delete clears). Search top right. Config: `config/nova.json`.
Click sounds / accent colour: Client tab (GUI, Theme).

## Schematic builder (World > Schematic)
Put `.litematic` files in `.minecraft/schematics` or `config/nova/schematics`. Enable the module, pick the file,
"Load selected file", stand where you want it and press "Origin: my position" (or look at a block + "Origin: block I look at").
It turns smoothly to each face (Aim speed / Max turn), only clicks when the crosshair is on that face, stays in reach.
"Need N item" shows what is missing. In survival it opens chests/barrels within reach and takes the missing items; creative fills from nothing.
"Load into Litematica" (default on) hands the loaded file + origin + rotation to Litematica as a placement, so Litematica renders it (needs the Litematica mod; it follows origin/rotation/offset changes and is removed on Unload).
"Built-in preview" is the old outline renderer, off by default because it is slow; only use it without Litematica.
"Auto walk" walks (W + jump, smooth turning) to the nearest spot where the next block can be placed, then places as usual. "Show path" draws the route.

## Changes in 2.7.4
- **Startup loading screen.** Shown once when the title screen first appears: fades in / out in the menu colours, lists what was loaded (modules, config, custom sounds, schematics, Litematica) with a progress bar. Any key or click skips it. Client > GUI: "Startup splash" (on / off), "Splash length (s)", "Preview splash".

## Changes in 2.7.3
- **Fixed the endless chest <-> build walking loop.** A chest trip is only started when a block that still has to be built needs an item you do not carry (or you have no filler at all). Before, "nothing in reach is placeable" was enough, so when the remaining blocks were just unreachable it walked to the chest, came back, found nothing placeable and went to the chest again.
- Restock loop breaker: after two chest trips in a row with no block built in between, restocking pauses for 60 s and says so in chat / the status line.

## Changes in 2.7.2
- **Builder no longer gets stuck on blocks it cannot place or reach.**
  - Blocks the server keeps refusing (clicked 6 times in a row but never staying) are parked instead of being clicked forever.
  - The "can't place" counter is global now; before it restarted whenever the builder switched to another block, so two failing blocks could take turns forever.
  - Parking gets longer every time a block fails (10 s, 20 s, 40 s ... up to ~5 min). Auto unstick and restocking only forget short parks; blocks that failed 3+ times stay parked.
  - New progress watchdog: if no block was finished for 2x "Unstick after (s)" (restock trips excluded) the current target is skipped. Walking back and forth no longer counts as progress.
  - The status line shows "N blocks skipped (cannot reach / place), retrying later".
- **GUI sounds** (Client > GUI): "Sound" list with 23 built-in sounds (note block instruments, bells, buttons, ...), "Play sound" to preview.
  Your own sounds: put `.wav` / `.aiff` / `.au` files in `config/nova/sounds`, press "Refresh sound list" (or just reopen the menu) and pick `File: yourname.wav`.
  "Custom file volume (%)" and "Custom file: pitch follows action" apply to your files. `.mp3` / `.ogg` are not supported, convert them to `.wav`.

## Changes in 2.7.1
- **Fixed the restock crash** (`NullPointerException ... Map.get` in handleContainer): after taking a stack out of a chest the game empties the slot on the client, so the item was read as air. It is now read before the click.
- Builder errors in chat now include file, line and method (`BuildModules.java:1529 in handleContainer`).
- "Load into Litematica" tries every `createFromFile` variant of the installed Litematica, so it works with newer versions.

## Changes in 2.7.0
- **Build fix**: the workflow now arranges the files itself. Loose `.java` files in the repo root are moved into `src/main/java/<package>` (by their `package` line) and `fabric.mod.json` into `src/main/resources`, so uploading everything flat no longer fails with "NovaClient.java is missing". A project that was extracted into a sub-folder is picked up too. `build.gradle`, `settings.gradle` and `gradle.properties` still have to be in the repo root.
- **Customisable GUI** (Client tab, all changes are live):
  - *Theme*: accent colour (presets, Rainbow, or Custom RGB) and window colour (Nova, Midnight, Amoled, Slate, Forest, Wine, or Custom RGB). Every shade in the menu is derived from the window colour.
  - *Layout*: window width/height, columns (Auto/1/2/3), corner radius, glow strength, window opacity, gradient + header line on/off.
  - *Background*: your own picture. Drop `.png` / `.jpg` / `.gif` / `.bmp` files into `config/nova/images`, press "Refresh image list" (or just reopen the menu), pick it under "Image". Options: draw on the window or the whole screen, Cover/Stretch, image opacity, dim, pixel size (smaller = sharper, a bit heavier), card opacity.
  - New default look: gradient window with accent tint, header line, rounder corners, soft glow.
- **Schematic builder**
  - Double slabs: a double slab is now built as two slabs (second half clicked onto the first). Before, one half counted as done. Item counts ("Need N ...") count two slabs for it.
  - Blocks that depend on the clicked face are placed against the right face: wall torches, ladders, wall signs/banners/heads, tripwire hooks, coral fans (facing = clicked side), standing torches (top face), levers/buttons/grindstones (floor / wall / ceiling).
  - More block types keep their orientation: beds, repeaters, comparators, ender chests, lecterns, stonecutters, looms, carved pumpkins / jack o'lanterns, anvils.

## Changes in 2.6.4
- **Restock bug fixed (builder stopped after taking items from a chest).** After a restock the chest was flagged "emptied" even though it still had items, and that flag only cleared after ~30 s of idle ticks, so with one chest the builder sat there doing nothing. Now a chest is only skipped when it had nothing you need (and only for 60 s, then it is checked again). Both halves of a double chest count as one chest.
- **Marked restock chests** (World > Schematic): "Mark / unmark chest I look at", "Mark chests near me" (everything within 6 blocks, handy for a wall of double chests), "Clear marked chests". Marked chests are outlined in green ("Show marked chests"). "Restock from": *Marked, else any* (default, same as before when nothing is marked), *Marked only*, *Any nearby*. Chest range still applies to marked chests. Marks are cleared when you change world/dimension.
- "Stacks per item" (default 9): how many stacks of one item it takes per chest visit, so one block type does not fill the whole inventory. It also stops when the inventory is full instead of looping.
- **Build mode**: *Continuous* (as before) or *Layer by layer* (finishes the lowest layer completely, restocking when needed, before it goes up). The HUD shows "Layer N / M".
- **Temporary filler / no more getting stuck**:
  - "Restock filler too": a stack of the filler block is taken from the chests as well (before, filler only worked if you happened to carry some).
  - "Remove filler as I build": temporary blocks are broken again as soon as nothing of the schematic is waiting near them (not only when everything is done).
  - "Auto unstick" / "Unstick after (s)": if nothing was placed, taken or walked for that long, parked blocks and skipped chests are forgotten and it retries; in layer mode it skips a layer it cannot finish and comes back to it later.
- Blocks that have no item (portal, fire, ...) count as done so they can no longer hold up a layer.

## Changes in 2.6.3
- Restock chests are found from far away: "Chest range" (World > Schematic, default 64 blocks, up to 160) scans every loaded chunk in range for chests and barrels (trapped and ender chests are skipped), the list is cached so it costs nothing per tick. Before it only looked about 16 blocks around you.
- Chest trips no longer get stuck: the route search used to be boxed in around you and the schematic, so a far chest was either not found or there was no route back. Chest trips now search a wide area with a bigger node budget, and the route back to the schematic uses one box around the schematic and where you stand.
- After restocking, blocks that were parked while there was no material are tried again straight away (previously they could sit parked for ~10 s, which looked like the builder had switched itself off).
- An error inside the builder no longer switches the module off: it prints "Schematic error (kept running): ..." once in chat and in the progress HUD, closes any open container, and carries on. If you see that message, send it to me.

## Changes in 2.6.2
- Wrong blocks are detected and fixed. A block now only counts as built when it is the right block AND has the right facing (stairs, doors, gates, chests, furnaces, glazed terracotta), axis (logs, pillars), and top/bottom half (slabs, stairs, trapdoors). Before, any block of the same type counted as done, so wrongly rotated or flipped blocks were left alone, and a wrong block in a spot blocked it forever.
- "Fix wrong blocks" (World > Schematic, on): wrong blocks that the builder placed itself (including filler that ended up on a schematic spot) are broken (best tool is picked in survival) and placed again. If one comes out wrong 3 times it is accepted as it is, so it can never loop forever. Walks to wrong blocks that are out of reach; they are outlined in red.
- "Replace other wrong blocks" (off): also break wrong blocks the builder did NOT place (terrain, other builds) inside the schematic. Only on when you want the schematic to replace whatever is there; it never breaks unbreakable blocks or blocks whose replacement item it cannot get.
- Progress HUD shows how many wrong blocks there are.

## Changes in 2.6.1
- GUI glow reworked to match the orange look: soft wide ambient glow around the window (smooth quadratic falloff, no more hard rings), brighter accent rim, enabled cards get a soft glow + accent border + top sheen, toggles, tabs and slider fills glow wider. Default accent is a warmer orange.

## Changes in 2.6.0
- Filler blocks (World > Schematic > "Filler blocks", on by default; pick the block with "Filler block"):
  - The route search can now bridge: where there is nothing to stand on it plans a filler block (flat bridge, or a two-block stair step up). Walking crouches and puts the block down before stepping onto it.
  - If the next pending block has nothing to click against (floating part, schematic starting in the air), the builder walks to the ground under it and builds a filler column up to it, then places the real block on top.
  - Filler blocks are never put on a cell that belongs to the schematic.
  - "Remove filler when done" breaks all filler blocks again once the whole schematic is built.
- The builder tries many targets before it says "No route".

## Changes in 2.5.1
- Route search now prefers the part that is already built: standing on built blocks is cheap, stepping off them is expensive, so the builder stays on the built part whenever a route on it exists.
- Route search tries the 24 nearest placeable blocks at once instead of one block at a time (a single unreachable block no longer stops the build), is limited to the area around the schematic, and has a bigger node budget.
- Walk goals use a 0.6 smaller reach so arriving a little off the spot centre still lets the block be placed.
- Placement: blocks with a facing (stairs, doors, gates, chests, furnaces, glazed terracotta) are only clicked once the server already has the new rotation; slab/stair/trapdoor halves are checked against where the crosshair really is, and faces that would give the wrong half are not used; interactive blocks (chests, doors, buttons, ...) are never used as the block to click against; solid blocks are not placed into the space you stand in.

## Changes in 2.5.0
- Auto walk never turns the camera: it presses forward/back/strafe keys relative to where you look, so it can aim at a block and walk at the same time (no look-forward / look-back cycles). It keeps walking while it places blocks in reach.
- Lava: cells next to lava are not used for routes, and walking stops with "Lava ahead" if the next step or what is under it is lava.
- "Sneak at edges": crouches while walking when the ground ahead is missing, except on steps where the route itself goes down (drops, ladders, water).

## Changes in 2.4.0
- Schematic is now loaded into Litematica automatically (reflection, no hard dependency); the built-in outline preview is off by default (it caused the lag).
- Progress recount runs in slices instead of one big pass, and out-of-reach blocks are rejected cheaply before planning.

## Changes in 2.3.0
- Schematic builder walks to chests/barrels (within ~16 blocks) when it runs out of material, takes what is missing, then goes back to building. Unreachable chests are skipped.
- Path finder now handles doors and fence gates (opens them on the way, not iron doors), ladders and vines (climb up/down) and water (wades/swims, drops into water).
- Blocks that need a facing (stairs, doors, gates, chests, furnaces, glazed terracotta) are now planned from the direction you will be looking when you click: the builder picks a click face and standing spot where that direction matches, instead of failing after turning.

## Changes in 2.2.0
- GUI keeps its tab, scroll position, search text and unfolded cards when you close and reopen it.
- Neon look: pulsing halo around the window, enabled modules, toggles, slider fills, selected tab and the title.
- Schematic builder: ghost preview of the schematic, auto walk with a simple path finder (flat / 1 up / up to 3 down), walk target selection, route preview, and blocks that can't be clicked are parked instead of blocking the build.

## Changes in 2.1.1
- ChunkPos is read through helpers (U.cx / U.cz) so it compiles whether ChunkPos is a record or a class with fields.
- Module errors in onEnable/onDisable are now written to the log instead of being swallowed.
- Right Shift key edge detection no longer goes stale while the GUI is open.
