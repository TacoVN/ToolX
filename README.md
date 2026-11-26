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
  - **Eye Measurement:** Toggleable dual-box aim mode (`340:1080` un-stretched `2.0°` vFOV center strip, `~300ms` smooth strip transition, draggable `15..1 | 1..15` magnified EyeZoom box, 1:1 mouse scaling, scaled centered hotbar, `F3 + Esc` support, locked independently from radar modes) paired with `F3 + C` 8-ring Stronghold triangulation, Boat Eye `1.40625°` detection, compact 1-row icon HUD synced with Debug GUI Scale (9-point snap dragging), and `F3 + D` reset

## Configuration
ToolX can be configured in-game via **Options** ➡ **Credits & Attribution** ➡ **ToolX**
