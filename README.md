# ToolX
ToolX is a simple client-side utility mod for Vanilla Minecraft.

## Features
- **F3 + F:** Cycle render distance on the fly (`F3 + F` to increase, `F3 + Shift + F` to decrease) **(removed in 1.19+)**
- **Pie Chart:**
  - **2D Mode:** Render the profiler pie chart as a flat 2D circle
  - **Auto Reset:** Reset pie chart profiler data automatically when entering a new chunk
  - **Endar:** Toggle FOV to `30`, open the pie chart at `root.tick.entities`, and enable `Auto Reset`
  - **Piedar:** Open the pie chart at `root.tick.blockEntities`, lock pie chart `0-9` navigation, enable chunk borders (`F3 + G`), and enable `Auto Reset`
  - **Preemptive Navigation:** Toggle FOV to `30`, open the pie chart at `root.frame.extract.level`, lock pie chart `0-9` navigation, enable chunk borders (`F3 + G`), and enable `Auto Reset`
- **MCSR:**
  - **Eye Measurement:** Toggleable horizontal-stretch aim mode (`30°` vertical FOV, `16×` horizontal stretch, 1:1 mouse scaling, `1px` invert crosshair + degree ticks, `F3 + Esc` support) paired with `F3 + C` 8-ring Stronghold triangulation, Boat Eye `1.40625°` detection, compact 1-row icon HUD synced with Debug GUI Scale (9-point snap dragging), and `F3 + D` reset

## Configuration
ToolX can be configured in-game via **Options** ➡ **Credits & Attribution** ➡ **ToolX**
