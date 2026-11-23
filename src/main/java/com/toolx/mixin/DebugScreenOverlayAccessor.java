package com.toolx.mixin;

import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DebugScreenOverlay.class)
public interface DebugScreenOverlayAccessor {
	@Accessor("renderProfilerChart")
	boolean toolx$isRenderProfilerChart();

	@Accessor("renderProfilerChart")
	void toolx$setRenderProfilerChart(boolean value);
}
