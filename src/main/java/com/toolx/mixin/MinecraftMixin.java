package com.toolx.mixin;

import com.toolx.EyeMeasurementController;
import com.toolx.PieChartController;
import com.toolx.gui.StripTransition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.profiling.ContinuousProfiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.SingleTickProfiler;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Shadow
	@Final
	private ContinuousProfiler fpsPieProfiler;

	@Inject(method = "tick", at = @At("TAIL"))
	private void toolx$trackChunkChanges(CallbackInfo info) {
		Minecraft self = (Minecraft) (Object) this;
		PieChartController.onClientTick(self);
		EyeMeasurementController.onClientTick(self);
	}

	@Inject(method = "constructProfiler", at = @At("HEAD"))
	private void toolx$resetPieProfilerOnChunkChange(
		boolean shouldCollectFrameProfile,
		@Nullable SingleTickProfiler tickProfiler,
		CallbackInfoReturnable<ProfilerFiller> cir
	) {
		if (PieChartController.consumeReset() && shouldCollectFrameProfile && this.fpsPieProfiler.isEnabled()) {
			this.fpsPieProfiler.disable();
		}
	}

	@Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At("HEAD"))
	private void toolx$resetPieTrackingOnDisconnect(Screen screen, boolean keepResourcePacks, boolean stopSound, CallbackInfo info) {
		PieChartController.resetTracking((Minecraft) (Object) this);
		EyeMeasurementController.reset();
		StripTransition.snapOff();
	}

	@Inject(method = "clearClientLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"))
	private void toolx$resetPieTrackingOnClearLevel(Screen screen, CallbackInfo info) {
		PieChartController.resetTracking((Minecraft) (Object) this);
		EyeMeasurementController.reset();
		StripTransition.snapOff();
	}
}
