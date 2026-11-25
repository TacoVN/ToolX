package com.toolx.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.toolx.EyeMeasurementController;
import com.toolx.PieChartController;
import com.toolx.gui.EyeMeasurementHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	private double accumulatedDX;

	@Shadow
	private double accumulatedDY;

	@Inject(method = "turnPlayer", at = @At("HEAD"))
	private void toolx$scaleMouseDuringEyeMeasurement(double movementTime, CallbackInfo info) {
		if (EyeMeasurementController.isMeasuring()) {
			this.accumulatedDX *= EyeMeasurementController.mouseYawScale(this.minecraft);
			this.accumulatedDY *= EyeMeasurementController.mousePitchScale(this.minecraft);
		}
	}

	@Inject(method = "onButton", at = @At("TAIL"))
	private void toolx$handleRadarMouse(long handle, MouseButtonInfo rawButtonInfo, int action, CallbackInfo info) {
		if (handle == 0L || handle != this.minecraft.getWindow().handle()) {
			return;
		}
		if (action == 0 && rawButtonInfo.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			EyeMeasurementHud.endDrag();
			return;
		}
		if (action != 1 || this.minecraft.options.keyDebugModifier.isDown()) {
			return;
		}
		InputConstants.Key key = InputConstants.Type.MOUSE.getOrCreate(rawButtonInfo.button());
		PieChartController.handleRadarKey(this.minecraft, key);
		EyeMeasurementController.handleMeasureKey(this.minecraft, key);
	}
}
