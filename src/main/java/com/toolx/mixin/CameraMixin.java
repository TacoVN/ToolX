package com.toolx.mixin;

import com.toolx.EyeMeasurementController;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
	private void toolx$applyEyeMeasurementFov(float tickDelta, CallbackInfoReturnable<Float> cir) {
		if (EyeMeasurementController.isMeasuring()) {
			cir.setReturnValue(EyeMeasurementController.MEASURE_VFOV);
		}
	}

	@ModifyVariable(method = "setupPerspective", at = @At("HEAD"), argsOnly = true, ordinal = 3)
	private float toolx$stretchEyeMeasurementWidth(float width) {
		if (EyeMeasurementController.isMeasuring()) {
			return width / EyeMeasurementController.HORIZONTAL_STRETCH;
		}
		return width;
	}
}
