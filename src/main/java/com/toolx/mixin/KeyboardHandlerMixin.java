package com.toolx.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.toolx.PieChartController;
import com.toolx.ToolXConfig;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
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

	@Inject(method = "keyPress", at = @At("TAIL"))
	private void toolx$handleRadarKey(long handle, int action, KeyEvent event, CallbackInfo info) {
		if (handle == 0L || handle != this.minecraft.getWindow().handle() || action != 1) {
			return;
		}
		if (this.minecraft.options.keyDebugModifier.isDown()) {
			return;
		}
		PieChartController.handleRadarKey(this.minecraft, InputConstants.getKey(event));
	}
}
