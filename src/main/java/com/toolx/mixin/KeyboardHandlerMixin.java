package com.toolx.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.toolx.EyeMeasurementController;
import com.toolx.KeyBindsController;
import com.toolx.PieChartController;
import com.toolx.TimerController;
import com.toolx.ToolXConfig;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	private void debugFeedbackComponent(Component component) {
	}

	@Inject(method = "handleDebugKeys", at = @At("TAIL"), cancellable = true)
	private void toolx$handleCycleRenderDistance(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
		if (Boolean.TRUE.equals(cir.getReturnValue()) || !ToolXConfig.get().f3F || event.key() != InputConstants.KEY_F) {
			return;
		}

		Options options = this.minecraft.options;
		OptionInstance<Integer> renderDistance = options.renderDistance();

		int min = 2;
		int max = 32;
		if (renderDistance.values() instanceof OptionInstance.IntRangeBase range) {
			min = range.minInclusive();
			max = range.maxInclusive();
		}

		int current = renderDistance.get();
		int next = Mth.clamp(current + (event.hasShiftDown() ? -1 : 1), min, max);
		if (next != current) {
			renderDistance.set(next);
			options.save();
		}

		this.debugFeedbackComponent(
			Component.translatableWithFallback("debug.cycle_renderdistance.message", "Render Distance: %s", renderDistance.get())
		);
		cir.setReturnValue(true);
	}

	@Inject(method = "handleDebugKeys", at = @At("TAIL"))
	private void toolx$handleEyeMeasurementDebugKeys(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
		if (!Boolean.TRUE.equals(cir.getReturnValue())) {
			return;
		}
		if (this.minecraft.options.keyDebugClearChat.matches(event)) {
			EyeMeasurementController.reset();
			return;
		}
		if (this.minecraft.options.keyDebugCopyLocation.matches(event)
			&& this.minecraft.player != null
			&& !this.minecraft.player.isReducedDebugInfo()) {
			EyeMeasurementController.onCopyLocation(this.minecraft);
		}
	}

	@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
	private void toolx$handleBetterNavigationKey(long handle, int action, KeyEvent event, CallbackInfo info) {
		if (handle == 0L || handle != this.minecraft.getWindow().handle()) {
			return;
		}
		if (action == 0) {
			PieChartController.onKeyReleased(this.minecraft, event);
			return;
		}
		if ((action == 1 || action == -1) && PieChartController.handleBetterNavigationKey(this.minecraft, action, event)) {
			info.cancel();
		}
	}

	@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
	private void toolx$handleBoundBackspaceKey(long handle, int action, KeyEvent event, CallbackInfo info) {
		if (handle != 0L
			&& handle == this.minecraft.getWindow().handle()
			&& (action == 1 || action == -1)
			&& KeyBindsController.handleScreenBackspaceKey(this.minecraft, event)) {
			info.cancel();
		}
	}

	@Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
	private void toolx$suppressCharForBoundBackspace(long handle, CharacterEvent event, CallbackInfo info) {
		if (handle != 0L
			&& handle == this.minecraft.getWindow().handle()
			&& KeyBindsController.consumeSuppressedChar()) {
			info.cancel();
		}
	}

	@Inject(method = "keyPress", at = @At("TAIL"))
	private void toolx$handleRadarKey(long handle, int action, KeyEvent event, CallbackInfo info) {
		if (handle == 0L || handle != this.minecraft.getWindow().handle() || action != 1) {
			return;
		}
		InputConstants.Key key = InputConstants.getKey(event);
		if (!this.minecraft.options.keyDebugModifier.matches(event)
			&& !this.minecraft.options.keyDebugModifier.isDown()
			&& TimerController.isGameplayKey(this.minecraft.options, key)) {
			TimerController.onFirstInput(this.minecraft);
		}
		if (this.minecraft.options.keyDebugModifier.isDown()) {
			return;
		}
		if (!PieChartController.handleRadarKey(this.minecraft, key)
			&& !KeyBindsController.handlePieZeroKey(this.minecraft, key)) {
			EyeMeasurementController.handleMeasureKey(this.minecraft, key);
		}
	}
}
