package com.toolx.mixin;

import com.toolx.EyeMeasurementController;
import com.toolx.KeyBindsController;
import com.toolx.PieChartController;
import com.toolx.TimerController;
import com.toolx.gui.ResourceTrackerHud;
import com.toolx.gui.StripTransition;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.util.profiling.ContinuousProfiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.SingleTickProfiler;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelStorageSource;
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
		TimerController.onClientTick(self);
		KeyBindsController.syncWindowsKeySuppression(self);
	}

	@Inject(
		method = "doWorldLoad",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/Minecraft;disconnectWithProgressScreen()V",
			shift = At.Shift.AFTER
		)
	)
	private void toolx$prepareTimerOnSingleplayerWorldLoad(
		LevelStorageSource.LevelStorageAccess levelSourceAccess,
		PackRepository packRepository,
		WorldStem worldStem,
		Optional<GameRules> gameRules,
		boolean newWorld,
		CallbackInfo info
	) {
		TimerController.onSingleplayerWorldLoad(levelSourceAccess.getLevelId(), newWorld);
	}

	@Inject(method = "setLevel", at = @At("TAIL"))
	private void toolx$armTimerOnClientLevelJoined(ClientLevel level, CallbackInfo info) {
		TimerController.onClientLevelJoined((Minecraft) (Object) this);
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
		TimerController.onDisconnect();
		ResourceTrackerHud.resetState();
		KeyBindsController.resetTransientState();
		KeyBindsController.releaseWindowsKeySuppression((Minecraft) (Object) this);
		StripTransition.snapOff();
	}

	@Inject(method = "clearClientLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"))
	private void toolx$resetPieTrackingOnClearLevel(Screen screen, CallbackInfo info) {
		PieChartController.resetTracking((Minecraft) (Object) this);
		EyeMeasurementController.reset();
		TimerController.onDisconnect();
		ResourceTrackerHud.resetState();
		KeyBindsController.resetTransientState();
		KeyBindsController.releaseWindowsKeySuppression((Minecraft) (Object) this);
		StripTransition.snapOff();
	}
}
