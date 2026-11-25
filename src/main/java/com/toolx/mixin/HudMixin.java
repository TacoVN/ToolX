package com.toolx.mixin;

import com.toolx.EyeMeasurementController;
import com.toolx.gui.EyeMeasurementHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
	private void toolx$hideVanillaCrosshairDuringMeasure(
		GuiGraphicsExtractor graphics,
		DeltaTracker deltaTracker,
		CallbackInfo info
	) {
		if (EyeMeasurementController.isMeasuring()) {
			info.cancel();
		}
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void toolx$renderEyeMeasurementHud(
		GuiGraphicsExtractor graphics,
		DeltaTracker deltaTracker,
		CallbackInfo info
	) {
		EyeMeasurementHud.extractRenderState(this.minecraft, graphics);
	}
}
