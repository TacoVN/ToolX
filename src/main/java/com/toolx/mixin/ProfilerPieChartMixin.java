package com.toolx.mixin;

import com.toolx.ToolXConfig;
import net.minecraft.client.gui.components.debugchart.ProfilerPieChart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ProfilerPieChart.class)
public abstract class ProfilerPieChartMixin {
	@Unique
	private static final int CHART_HALF_SIZE_Y_2D = 115;

	@ModifyConstant(method = "extractRenderState", constant = @Constant(intValue = 62))
	private int toolx$expandChartHalfHeightFor2D(int original) {
		return ToolXConfig.get().pieChart2DMode ? CHART_HALF_SIZE_Y_2D : original;
	}
}
