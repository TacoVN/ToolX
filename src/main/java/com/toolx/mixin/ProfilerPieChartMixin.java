package com.toolx.mixin;

import com.toolx.PieChartController;
import com.toolx.ToolXConfig;
import net.minecraft.client.gui.components.debugchart.ProfilerPieChart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProfilerPieChart.class)
public abstract class ProfilerPieChartMixin {
	@Unique
	private static final int CHART_HALF_SIZE_Y_2D = 115;

	@ModifyConstant(method = "extractRenderState", constant = @Constant(intValue = 62))
	private int toolx$expandChartHalfHeightFor2D(int original) {
		return ToolXConfig.get().pieChart2DMode ? CHART_HALF_SIZE_Y_2D : original;
	}

	@Inject(method = "profilerPieChartKeyPress", at = @At("HEAD"), cancellable = true)
	private void toolx$lockPieChartNavigation(int key, CallbackInfo info) {
		if (PieChartController.isPieChartLocked() || PieChartController.isBoundToDigit(key)) {
			info.cancel();
		}
	}
}
