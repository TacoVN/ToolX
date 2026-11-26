package com.toolx.mixin;

import com.toolx.EyeMeasurementController;
import com.toolx.PieChartController;
import com.toolx.gui.ThinModeHud;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	private float fov;

	@Shadow
	private float depthFar;

	@Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
	private void toolx$applyEyeMeasurementFov(float tickDelta, CallbackInfoReturnable<Float> cir) {
		if (EyeMeasurementController.isMeasuring()) {
			cir.setReturnValue(EyeMeasurementController.MEASURE_VFOV);
		}
	}

	@Inject(method = "createProjectionMatrixForCulling", at = @At("HEAD"), cancellable = true)
	private void toolx$narrowCullingForThinMode(CallbackInfoReturnable<Matrix4f> cir) {
		if (!EyeMeasurementController.isMeasuring()
			&& PieChartController.isThinCullingActive(this.minecraft)
			&& this.minecraft != null
			&& this.minecraft.options != null
			&& this.minecraft.getWindow() != null) {
			float physW = (float) this.minecraft.getWindow().getWidth();
			float physH = (float) this.minecraft.getWindow().getHeight();
			float aspect = (physW > 0.0F && physH > 0.0F)
				? Math.min(physW / physH, ThinModeHud.THIN_ASPECT)
				: ThinModeHud.THIN_ASPECT;
			float cullFov = Math.max(this.fov, (float) this.minecraft.options.fov().get());
			Matrix4f matrix = new Matrix4f();
			matrix.setPerspective(cullFov * 0.017453292F, aspect, 0.05F, this.depthFar);
			cir.setReturnValue(matrix);
		}
	}
}
