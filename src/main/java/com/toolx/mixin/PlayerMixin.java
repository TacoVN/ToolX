package com.toolx.mixin;

import com.toolx.ToolXConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {
	@Inject(method = "getFlyingSpeed", at = @At("RETURN"), cancellable = true)
	private void toolx$scaleFlightSpeed(CallbackInfoReturnable<Float> cir) {
		if (!((Object) this instanceof LocalPlayer)) {
			return;
		}
		int multiplier = ToolXConfig.get().flightSpeed;
		Minecraft minecraft = Minecraft.getInstance();
		Player self = (Player) (Object) this;
		if (multiplier >= 2
			&& minecraft != null
			&& minecraft.hasSingleplayerServer()
			&& self.getAbilities().flying
			&& !self.isPassenger()) {
			cir.setReturnValue(cir.getReturnValueF() * multiplier);
		}
	}
}
