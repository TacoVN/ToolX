# ToolX
ToolX is a simple client-side utility mod for Vanilla Minecraft.

## Features
- **F3 + F:** Cycle render distance on the fly (`F3 + F` to increase, `F3 + Shift + F` to decrease) **(removed in 1.19+)**
- **Pie Chart:**
  - **Auto Reset:** Reset pie chart profiler data automatically when entering a new chunk
  - **Endar:** Switch to MCSR Thin Mode (`340:1080`, `~300ms` smooth strip transition, auto-scaled clipped F3, draggable `E:` counter & borderless `2D` pie chart), open the pie chart at `root.tick.entities`, and enable `Auto Reset`
  - **Piedar:** Open the pie chart at `root.tick.blockEntities`, lock pie chart `0-9` navigation, enable chunk borders (`F3 + G`), and enable `Auto Reset`
  - **Preemptive Navigation:** Switch to MCSR Thin Mode (`340:1080`, `~300ms` smooth strip transition, auto-scaled clipped F3, draggable `E:` counter & borderless `2D` pie chart), open the pie chart at `root.frame.extract.level`, lock pie chart `0-9` navigation, enable chunk borders (`F3 + G`), and enable `Auto Reset`
- **MCSR:**
  - **Timer:** Real-time speedrun RTA display (`Any%`, `3 Decimals`, active only in `GameType.SURVIVAL`, `First Input` start on New Worlds excluding `F3` / Chat / Command / non-gameplay keys, idle `00:00.000` visible on Old Worlds, red `#FF0B0B` before start -> white `#FFFFFF` while running -> green `#1FFF1F` on `WinScreen`, continuous fair RTA, hidden in F3 Debug Overlay / Options / Thin Mode / Eye Measurement, `2x` Debug GUI Scale outlined vanilla font without background, draggable in Chat, default middle-right)
  - **Resource Tracker:** Toggleable MCSR inventory HUD (`OFF` by default; `Obsidian`, `Fire Resistance`, `Ender Pearl`, `Blaze Rod`, `Eye of Ender`, `Explosive` with Bed icon), active only in Survival (`GameType.SURVIVAL`), recursively scans inventory, cursor stack, open Crafting grid (`2x2` / `3x3`), `Bundle`, and Shulker/container components with strict obtainable `26.3` rules (`Potions`/`Splash Potions`/`Enchanted Golden Apple` only for Fire Resistance; Beds + TNT + `min(Respawn Anchor, Glowstone)` for Explosives; excludes lingering potions, totems, tipped arrows, suspicious stew, TNT minecart, and end crystals), uses crisp 2D item textures, renders cleanly under open container UIs / tooltips via `HudMixin`, hidden in Options / Thin Mode / Eye Measurement / F1, synced with `Debug GUI Scale`, draggable in Chat, default centered between bottom of Inventory and selected item name
  - **Eye Measurement:** Toggleable dual-box aim mode (`340:1080` un-stretched `2.0°` vFOV center strip, `~300ms` smooth strip transition, draggable `15..1 | 1..15` magnified EyeZoom box, 1:1 mouse scaling, scaled centered hotbar, `F3 + Esc` support, locked independently from radar modes) paired with `F3 + C` 8-ring Stronghold triangulation, Boat Eye `1.40625°` detection, compact 1-row icon HUD synced with Debug GUI Scale (9-point snap dragging), and `F3 + D` reset

## Configuration
ToolX can be configured in-game via **Options** ➡ **Credits & Attribution** ➡ **ToolX**
