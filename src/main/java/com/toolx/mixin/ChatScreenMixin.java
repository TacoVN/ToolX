package com.toolx.mixin;

import com.toolx.gui.EyeMeasurementHud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void toolx$onChatMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		if (EyeMeasurementHud.onChatMouseClicked(event.x(), event.y(), event.button())) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void toolx$onChatRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo info) {
		EyeMeasurementHud.onChatRender(graphics, mouseX, mouseY);
	}

	@Inject(method = "removed", at = @At("HEAD"))
	private void toolx$onChatClosed(CallbackInfo info) {
		EyeMeasurementHud.endDrag();
	}
}
