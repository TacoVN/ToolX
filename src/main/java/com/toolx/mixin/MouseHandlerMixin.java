package com.toolx.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.toolx.PieChartController;
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

	@Inject(method = "onButton", at = @At("TAIL"))
	private void toolx$handleRadarMouse(long handle, MouseButtonInfo rawButtonInfo, int action, CallbackInfo info) {
		if (handle == 0L || handle != this.minecraft.getWindow().handle() || action != 1) {
			return;
		}
		if (this.minecraft.options.keyDebugModifier.isDown()) {
			return;
		}
		PieChartController.handleRadarKey(this.minecraft, InputConstants.Type.MOUSE.getOrCreate(rawButtonInfo.button()));
	}
}
