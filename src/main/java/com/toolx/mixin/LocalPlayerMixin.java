package com.toolx.mixin;

import com.toolx.ToolXConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Abilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
	@Redirect(
		method = "aiStep",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Abilities;getFlyingSpeed()F"
		)
	)
	private float toolx$scaleVerticalFlightSpeed(Abilities abilities) {
		float baseSpeed = abilities.getFlyingSpeed();
		int multiplier = ToolXConfig.get().flightSpeed;
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer self = (LocalPlayer) (Object) this;
		if (multiplier >= 2
			&& minecraft != null
			&& minecraft.hasSingleplayerServer()
			&& abilities.flying
			&& !self.isPassenger()) {
			return baseSpeed * multiplier;
		}
		return baseSpeed;
	}
}
