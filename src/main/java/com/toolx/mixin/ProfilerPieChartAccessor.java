package com.toolx.mixin;

import net.minecraft.client.gui.components.debugchart.ProfilerPieChart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ProfilerPieChart.class)
public interface ProfilerPieChartAccessor {
	@Accessor("profilerTreePath")
	String toolx$getProfilerTreePath();

	@Accessor("profilerTreePath")
	void toolx$setProfilerTreePath(String path);
}
