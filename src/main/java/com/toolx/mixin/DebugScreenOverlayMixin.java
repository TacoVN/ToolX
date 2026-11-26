package com.toolx.mixin;

import com.toolx.gui.ThinModeHud;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Unique
	private boolean toolx$clippedThinLines;

	@Redirect(
		method = "extractRenderState",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/debug/DebugScreenEntryList;isOverlayVisible()Z"
		)
	)
	private boolean toolx$hideDebugChartsHintInThinMode(DebugScreenEntryList entries) {
		if (ThinModeHud.shouldRenderThinLayout(this.minecraft)) {
			return false;
		}
		return entries.isOverlayVisible();
	}

	@Inject(method = "extractLines", at = @At("HEAD"))
	private void toolx$beginThinDebugLines(
		GuiGraphicsExtractor graphics,
		List<String> lines,
		boolean isLeft,
		int screenWidth,
		CallbackInfo info
	) {
		this.toolx$clippedThinLines = false;
		if (!ThinModeHud.shouldRenderThinLayout(this.minecraft)) {
			return;
		}
		ThinModeHud.DebugSpace space = ThinModeHud.computeDebugSpace(this.minecraft, graphics);
		if (space == null) {
			return;
		}
		graphics.enableScissor(space.stripLeft(), 0, space.stripRight(), space.debugHeight());
		int effectiveDebugScale = (space.debugScale() < space.guiScale() && space.debugScale() > 0)
			? space.debugScale()
			: space.guiScale();
		float stripLeftExact = (float) space.physLeftBarW() / (float) effectiveDebugScale;
		float f3Scale = ThinModeHud.thinF3ScaleRatio(this.minecraft);
		graphics.pose().pushMatrix();
		graphics.pose().translate(stripLeftExact, 0.0F);
		if (f3Scale < 1.0F) {
			graphics.pose().scale(f3Scale, f3Scale);
		}
		this.toolx$clippedThinLines = true;
	}

	@ModifyVariable(method = "extractLines", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int toolx$narrowDebugLineWidthForThinMode(int screenWidth) {
		return ThinModeHud.thinF3LogicalWidth(this.minecraft, screenWidth);
	}

	@Inject(method = "extractLines", at = @At("RETURN"))
	private void toolx$endThinDebugLines(
		GuiGraphicsExtractor graphics,
		List<String> lines,
		boolean isLeft,
		int screenWidth,
		CallbackInfo info
	) {
		if (this.toolx$clippedThinLines) {
			this.toolx$clippedThinLines = false;
			graphics.pose().popMatrix();
			graphics.disableScissor();
		}
	}
}
