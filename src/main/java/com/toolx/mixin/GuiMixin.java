package com.toolx.mixin;

import com.mojang.blaze3d.platform.Window;
import com.toolx.gui.TimerHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Gui.class)
public abstract class GuiMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

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
