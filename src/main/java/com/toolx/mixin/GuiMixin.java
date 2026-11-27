package com.toolx.mixin;

import com.mojang.blaze3d.platform.Window;
import com.toolx.EyeMeasurementController;
import com.toolx.KeyBindsController;
import com.toolx.PieChartController;
import com.toolx.gui.TimerHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	private @Nullable Screen screen;

	@Inject(method = "setScreen", at = @At("TAIL"))
	private void toolx$onScreenChanged(Screen requested, CallbackInfo info) {
		PieChartController.onScreenChanged(this.minecraft, this.screen);
		EyeMeasurementController.onScreenChanged(this.screen);
		KeyBindsController.syncWindowsKeySuppression(this.minecraft);
	}

	@Redirect(
		method = "extractRenderState",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;applyCursor(Lcom/mojang/blaze3d/platform/Window;)V"
		)
	)
	private void toolx$renderTimerBeforeCursor(GuiGraphicsExtractor graphics, Window window) {
		TimerHud.extractRenderState(this.minecraft, graphics);
		graphics.applyCursor(window);
	}
}
