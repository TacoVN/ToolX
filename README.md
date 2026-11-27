# ToolX
ToolX is a simple client-side utility mod for Vanilla Minecraft.

## Features
- **General:**
  - **F3 + F:** Cycle render distance on the fly (`F3 + F` to increase, `F3 + Shift + F` to decrease) **(removed in 1.19+)**
  - **Flight Speed:** Singleplayer-only slider (`1x` by default, up to `10x`, temporarily locked at `1x` while connected to Multiplayer without overwriting your Singleplayer value) multiplying Creative, Spectator, and similar creative-flight horizontal and vertical speed (stacks cleanly with Spectator scroll wheel speed adjustments)
  - **F3 Debug Chart:** Toggle visibility of the F3 `Debug charts:` hint lines (`OFF` by default hides the hint across F3 & Thin Mode; `ON` restores vanilla)
- **Pie Chart:**
  - **Auto Reset:** Reset pie chart profiler data automatically when entering a new chunk
  - **Better Navigation:** Hold `Ctrl` + type digits (`10`, `11`, `57`, etc.) to navigate Pie Chart entries beyond `9` across all Pie Chart modes (including `Endar`, `Piedar`, and `Preemptive Navigation`), auto-jumping immediately once no longer prefix match is possible (or on releasing `Ctrl`), while preventing accidental hotbar slot swaps (`ON` by default)
  - **Endar:** Switch to MCSR Thin Mode (`340:1080`, `~300ms` smooth strip transition, hides Closed Captions, auto-scaled clipped F3, draggable `C:` & `E:` counters & borderless `2D` pie chart), open the pie chart at `root.tick.entities`, and enable `Auto Reset`
  - **Piedar:** Open the pie chart at `root.tick.blockEntities`, lock pie chart `0-9` navigation, enable chunk borders (`F3 + G`), and enable `Auto Reset`
  - **Preemptive Navigation:** Switch to MCSR Thin Mode (`340:1080`, `~300ms` smooth strip transition, hides Closed Captions, auto-scaled clipped F3, draggable `C:` & `E:` counters & borderless `2D` pie chart), open the pie chart at `root.frame.extract.level`, lock pie chart `0-9` navigation, enable chunk borders (`F3 + G`), and enable `Auto Reset`
- **MCSR:**
  - **Timer:** Real-time speedrun RTA display (`OFF` by default; `Any%`, `3 Decimals`, active only in `GameType.SURVIVAL`, SpeedRunIGT-style `First Input` start on New Worlds triggered by gameplay keybinds, grabbed mouse look, gameplay mouse buttons, and hotbar scroll, idle `00:00.000` visible on Old Worlds, white `#FFFFFF` before start -> yellow `#FFDE21` while running -> green `#1FFF1F` on `WinScreen`, continuous fair RTA, hidden in F3 Debug Overlay / Options / Thin Mode / Eye Measurement, `2x` Debug GUI Scale outlined vanilla font without background, draggable in Chat, default middle-right)
  - **Resource Tracker:** Toggleable MCSR inventory HUD (`OFF` by default; `Obsidian`, `Fire Resistance`, `Ender Pearl`, `Blaze Rod`, `Eye of Ender`, `Explosive` with Bed icon), active only in Survival (`GameType.SURVIVAL`), recursively scans inventory, cursor stack, open Crafting grid (`2x2` / `3x3`), `Bundle`, and Shulker/container components with strict obtainable `26.3` rules (`Potions`/`Splash Potions`/`Enchanted Golden Apple` only for Fire Resistance; Beds + TNT + `min(Respawn Anchor, Glowstone)` for Explosives; excludes lingering potions, totems, tipped arrows, suspicious stew, TNT minecart, and end crystals), uses crisp 2D item textures, renders cleanly under open container UIs / tooltips via `HudMixin`, hidden in Options / Thin Mode / Eye Measurement / F1, synced with `Debug GUI Scale`, draggable in Chat, default centered between bottom of Inventory and selected item name
  - **Eye Measurement:** Toggleable dual-box aim mode (`340:1080` un-stretched `2.0°` vFOV center strip, `~300ms` smooth strip transition, hides F3 Debug Overlay & Closed Captions while measuring, draggable `15..1 | 1..15` magnified EyeZoom box, 1:1 mouse scaling, scaled centered hotbar, `F3 + Esc` support, locked independently from radar modes) paired with standalone `F3 + C` 8-ring Stronghold triangulation (works both with and without the Eye Measurement zoom key bound), Boat Eye `1.40625°` detection, balanced 5-column 1-row HUD (`Overworld`, `Nether`, `Distance`, static Compass `Angle` with left/right turn offset, `Accuracy%`) synced with Debug GUI Scale (9-point snap dragging), and `F3 + D` reset
- **Key Binds:**
  - **Traverse Back:** Remappable key/mouse bind for stepping up `[0]` in the Pie Chart (defaults to `0`)
  - **Backspace:** Remappable key/mouse bind for triggering `Backspace` in focused text fields/screens such as Recipe Book search and Chat (defaults to `Backspace`; temporarily suppresses the Windows Start key only while the Recipe Book is visibly open when bound to `Win`)

## Configuration
ToolX can be configured in-game via **Options** ➡ **Credits & Attribution** ➡ **ToolX**
