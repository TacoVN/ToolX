package com.toolx.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.toolx.gui.ThinModeHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.GuiProfilerChartRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.gui.pip.GuiProfilerChartRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ResultField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiProfilerChartRenderer.class)
public abstract class GuiProfilerChartRendererMixin {
	@Unique
	private static final float RADIUS_2D = 57.5F;

	@Inject(method = "renderToTexture", at = @At("HEAD"), cancellable = true)
	private void toolx$render2DChartInThinMode(
		GuiProfilerChartRenderState chartState,
		PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector,
		CallbackInfo info
	) {
		if (!ThinModeHud.shouldRenderThinLayout(Minecraft.getInstance())) {
			return;
		}

		double totalPercentage = 0.0;
		for (ResultField result : chartState.chartData()) {
			double slicePercentage = result.percentage;
			if (slicePercentage <= 0.0) {
				continue;
			}
			double currentPercentage = totalPercentage;
			totalPercentage += slicePercentage;
			int steps = Math.max(1, Mth.ceil(slicePercentage * 2.4));
			int color = ARGB.opaque(result.getColor());
			submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.debugTriangleFan(), (pose, buffer) -> {
				buffer.addVertex(pose, 0.0F, 0.0F, 0.0F).setColor(color);

				for (int j = steps; j >= 0; j--) {
					double dir = (currentPercentage + slicePercentage * j / steps) * (Math.PI * 2.0) / 100.0;
					float xx = (float) (Math.sin(dir) * RADIUS_2D);
					float yy = (float) (Math.cos(dir) * RADIUS_2D);
					buffer.addVertex(pose, xx, yy, 0.0F).setColor(color);
				}
			});
		}

		info.cancel();
	}
}
