package com.toolx.mixin;

import com.toolx.KeyBindsController;
import com.toolx.gui.EyeMeasurementHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(
		method = "render",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V")
	)
	private void toolx$captureEyeZoomTexture(CallbackInfo info) {
		EyeMeasurementHud.captureEyeZoomFrame(this.minecraft);
	}

	@Inject(method = "close", at = @At("HEAD"))
	private void toolx$closeEyeZoomTexture(CallbackInfo info) {
		EyeMeasurementHud.closeZoomResources();
		KeyBindsController.releaseWindowsKeySuppression(this.minecraft);
	}
}
