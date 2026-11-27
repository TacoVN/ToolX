package com.toolx.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.toolx.EyeMeasurementController;
import com.toolx.KeyBindsController;
import com.toolx.PieChartController;
import com.toolx.TimerController;
import com.toolx.gui.EyeMeasurementHud;
import com.toolx.gui.ResourceTrackerHud;
import com.toolx.gui.ThinModeHud;
import com.toolx.gui.TimerHud;
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
		if (((MouseHandler) (Object) this).isMouseGrabbed()
			&& this.minecraft.isWindowActive()
			&& (this.accumulatedDX != 0.0 || this.accumulatedDY != 0.0)) {
			TimerController.onFirstInput(this.minecraft);
		}
		if (EyeMeasurementController.isMeasuring()) {
			this.accumulatedDX *= EyeMeasurementController.mouseYawScale(this.minecraft);
			this.accumulatedDY *= EyeMeasurementController.mousePitchScale(this.minecraft);
		}
	}

	@Inject(
		method = "onScroll",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Inventory;setSelectedSlot(I)V"
		)
	)
	private void toolx$triggerFirstInputOnHotbarScroll(long handle, double xoffset, double yoffset, CallbackInfo info) {
		TimerController.onFirstInput(this.minecraft);
	}

	@Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
	private void toolx$handleBoundBackspaceMouse(long handle, MouseButtonInfo rawButtonInfo, int action, CallbackInfo info) {
		if (handle != 0L
			&& handle == this.minecraft.getWindow().handle()
			&& action == 1
			&& KeyBindsController.handleScreenBackspaceMouse(this.minecraft, rawButtonInfo)) {
			info.cancel();
		}
	}

	@Inject(method = "onButton", at = @At("TAIL"))
	private void toolx$handleRadarMouse(long handle, MouseButtonInfo rawButtonInfo, int action, CallbackInfo info) {
		if (handle == 0L || handle != this.minecraft.getWindow().handle()) {
			return;
		}
		if (action == 0 && rawButtonInfo.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			ThinModeHud.endDrag();
			EyeMeasurementHud.endDrag();
			TimerHud.endDrag();
			ResourceTrackerHud.endDrag();
			return;
		}
		if (action != 1) {
			return;
		}
		KeyBindsController.syncWindowsKeySuppression(this.minecraft);
		InputConstants.Key key = InputConstants.Type.MOUSE.getOrCreate(rawButtonInfo.button());
		if (!this.minecraft.options.keyDebugModifier.isDown()
			&& TimerController.isGameplayKey(this.minecraft.options, key)) {
			TimerController.onFirstInput(this.minecraft);
		}
		if (this.minecraft.options.keyDebugModifier.isDown()) {
			return;
		}
		if (!PieChartController.handleRadarKey(this.minecraft, key)
			&& !PieChartController.handleBetterNavigationMouse(this.minecraft, key)
			&& !KeyBindsController.handlePieZeroKey(this.minecraft, key)) {
			EyeMeasurementController.handleMeasureKey(this.minecraft, key);
		}
	}
}
