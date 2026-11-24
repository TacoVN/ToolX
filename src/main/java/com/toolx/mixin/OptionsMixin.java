package com.toolx.mixin;

import com.toolx.PieChartController;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public abstract class OptionsMixin {
	@Unique
	private boolean toolx$restoredFovForSave;

	@Inject(method = "save", at = @At("HEAD"))
	private void toolx$restoreFovBeforeSave(CallbackInfo info) {
		this.toolx$restoredFovForSave = PieChartController.prepareOptionsSave((Options) (Object) this);
	}

	@Inject(method = "save", at = @At("RETURN"))
	private void toolx$reapplyRadarFovAfterSave(CallbackInfo info) {
		if (this.toolx$restoredFovForSave) {
			this.toolx$restoredFovForSave = false;
			PieChartController.finishOptionsSave((Options) (Object) this);
		}
	}
}
