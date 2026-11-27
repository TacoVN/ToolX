package com.toolx.mixin;

import net.minecraft.client.gui.components.debugchart.ProfilerPieChart;
import net.minecraft.util.profiling.ProfileResults;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ProfilerPieChart.class)
public interface ProfilerPieChartAccessor {
	@Accessor("profilerPieChartResults")
	@Nullable ProfileResults toolx$getProfilerPieChartResults();

	@Accessor("profilerTreePath")
	String toolx$getProfilerTreePath();

	@Accessor("profilerTreePath")
	void toolx$setProfilerTreePath(String path);
}
