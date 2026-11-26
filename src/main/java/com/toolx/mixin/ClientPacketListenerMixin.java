package com.toolx.mixin;

import com.toolx.TimerController;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
	@Inject(method = "handleGameEvent", at = @At("TAIL"))
	private void toolx$completeAnyPercentOnWinGame(ClientboundGameEventPacket packet, CallbackInfo info) {
		if (packet.getEvent() == ClientboundGameEventPacket.WIN_GAME) {
			TimerController.onAnyPercentComplete();
		}
	}
}
