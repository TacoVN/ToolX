package com.toolx.mixin;

import com.mojang.blaze3d.platform.Window;
import com.toolx.EyeMeasurementController;
import com.toolx.PieChartController;
import com.toolx.gui.EyeMeasurementHud;
import com.toolx.gui.StripTransition;
import com.toolx.gui.ThinModeHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Unique
	private boolean toolx$scaledHotbar;

	@Inject(
		method = "extractRenderState",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/Hud;extractCrosshair(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"
		)
	)
	private void toolx$renderModeOverlays(
		GuiGraphicsExtractor graphics,
		DeltaTracker deltaTracker,
		CallbackInfo info
	) {
		StripTransition.extractSidebars(this.minecraft, graphics);
	}

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

	@Inject(method = "extractHotbarAndDecorations", at = @At("HEAD"))
	private void toolx$fitHotbarInCenterStrip(
		GuiGraphicsExtractor graphics,
		DeltaTracker deltaTracker,
		CallbackInfo info
	) {
		this.toolx$scaledHotbar = false;
		float scale = this.toolx$computeStripHotbarScale();
		if (scale < 0.999F) {
			float cx = graphics.guiWidth() * 0.5F;
			float bottom = (float) graphics.guiHeight();
			graphics.pose().pushMatrix();
			graphics.pose().translate(cx, bottom);
			graphics.pose().scale(scale, scale);
			graphics.pose().translate(-cx, -bottom);
			this.toolx$scaledHotbar = true;
		}
	}

	@Inject(method = "extractHotbarAndDecorations", at = @At("RETURN"))
	private void toolx$restoreHotbarTransform(
		GuiGraphicsExtractor graphics,
		DeltaTracker deltaTracker,
		CallbackInfo info
	) {
		if (this.toolx$scaledHotbar) {
			this.toolx$scaledHotbar = false;
			graphics.pose().popMatrix();
		}
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void toolx$renderToolxHud(
		GuiGraphicsExtractor graphics,
		DeltaTracker deltaTracker,
		CallbackInfo info
	) {
		if (this.minecraft.gui != null
			&& this.minecraft.gui.hud != null
			&& this.minecraft.gui.hud.isHidden()) {
			StripTransition.snapOff();
			PieChartController.syncThinCulling(this.minecraft);
		}
		ThinModeHud.extractECounter(this.minecraft, graphics);
		EyeMeasurementHud.extractRenderState(this.minecraft, graphics);
	}

	@Unique
	private float toolx$computeStripHotbarScale() {
		float eased = StripTransition.easedProgress();
		if (eased <= 0.0005F) {
			return 1.0F;
		}

		Window window = this.minecraft.getWindow();
		if (window == null || window.getHeight() <= 0) {
			return 1.0F;
		}
		float stripGuiW = (float) Math.round(window.getHeight() * ThinModeHud.THIN_ASPECT)
			/ (float) Math.max(1, window.getGuiScale());
		boolean hasOffhand = this.minecraft.player != null && !this.minecraft.player.getOffhandItem().isEmpty();
		float targetW = hasOffhand ? 244.0F : 186.0F;
		float targetScale = Math.clamp(stripGuiW / targetW, 0.25F, 1.0F);
		return 1.0F + (targetScale - 1.0F) * eased;
	}
}
