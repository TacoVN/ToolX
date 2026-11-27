package com.toolx.mixin;

import com.toolx.KeyBindsController;
import com.toolx.PieChartController;
import com.toolx.gui.StripTransition;
import com.toolx.gui.ThinModeHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.debugchart.ProfilerPieChart;
import net.minecraft.util.profiling.ProfileResults;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProfilerPieChart.class)
public abstract class ProfilerPieChartMixin {
	@Unique
	private static final int CHART_HALF_SIZE_Y_2D = 64;

	@Shadow
	private @Nullable ProfileResults profilerPieChartResults;

	@Unique
	private boolean toolx$pushedThinPie;

	@Unique
	private int toolx$debugWidth;

	@Unique
	private int toolx$debugHeight;

	@ModifyConstant(method = "extractRenderState", constant = @Constant(intValue = 62))
	private int toolx$expandChartHalfHeightFor2D(int original) {
		return ThinModeHud.shouldRenderThinLayout(Minecraft.getInstance()) ? CHART_HALF_SIZE_Y_2D : original;
	}

	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void toolx$prepareThinPieRender(GuiGraphicsExtractor graphics, int width, int height, CallbackInfo info) {
		this.toolx$pushedThinPie = false;
		this.toolx$debugWidth = width;
		this.toolx$debugHeight = height;
		Minecraft mc = Minecraft.getInstance();
		if (mc == null || mc.getDebugOverlay() == null || !mc.getDebugOverlay().showProfilerChart()) {
			ThinModeHud.markPieChartHidden();
			info.cancel();
			return;
		}
		if (!ThinModeHud.shouldRenderThinLayout(mc) && StripTransition.easedProgress() > 0.0005F && !PieChartController.isRadarActive()) {
			ThinModeHud.markPieChartHidden();
			info.cancel();
			return;
		}
		if (this.profilerPieChartResults == null) {
			ThinModeHud.markPieChartHidden();
		}
	}

	@Redirect(
		method = "extractRenderState",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V")
	)
	private void toolx$repositionAndStripPieBackground(
		GuiGraphicsExtractor graphics,
		int x0,
		int y0,
		int x1,
		int y1,
		int color
	) {
		if (ThinModeHud.shouldRenderThinLayout(Minecraft.getInstance())) {
			this.toolx$pushedThinPie = ThinModeHud.beginPieChartOffset(
				graphics,
				x0,
				y0,
				x1 - x0,
				y1 - y0,
				this.toolx$debugWidth,
				this.toolx$debugHeight
			);
			return;
		}
		ThinModeHud.markPieChartHidden();
		graphics.fill(x0, y0, x1, y1, color);
	}

	@Inject(method = "extractRenderState", at = @At("RETURN"))
	private void toolx$finishThinPieRender(GuiGraphicsExtractor graphics, int width, int height, CallbackInfo info) {
		if (this.toolx$pushedThinPie) {
			this.toolx$pushedThinPie = false;
			ThinModeHud.endPieChartOffset(graphics);
		}
	}

	@Inject(method = "profilerPieChartKeyPress", at = @At("HEAD"), cancellable = true)
	private void toolx$lockPieChartNavigation(int key, CallbackInfo info) {
		if (PieChartController.isDispatchingBetterNavigation()) {
			return;
		}
		if (PieChartController.isPieChartLocked()) {
			info.cancel();
			return;
		}
		if (KeyBindsController.isDispatchingPieZero()) {
			return;
		}
		if (!KeyBindsController.allowVanillaPieChartKey(key) || PieChartController.isBoundToDigit(key)) {
			info.cancel();
		}
	}
}
