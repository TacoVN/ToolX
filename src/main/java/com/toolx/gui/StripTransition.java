package com.toolx.gui;

import com.mojang.blaze3d.platform.Window;
import com.toolx.PieChartController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

public final class StripTransition {
	private static final double DURATION_MS = 300.0;
	private static final int SIDEBAR_COLOR = 0xFF000000;

	private static float progress;
	private static long lastFrameNanos;

	private StripTransition() {
	}

	public static void snapOff() {
		progress = 0.0F;
		lastFrameNanos = 0L;
	}

	public static boolean isSettled() {
		return progress >= 0.999F;
	}

	public static boolean isStripModeActive(@Nullable Minecraft minecraft) {
		return ThinModeHud.shouldRenderThinLayout(minecraft)
			|| EyeMeasurementHud.shouldRenderMeasureLayout(minecraft);
	}

	public static void update(@Nullable Minecraft minecraft) {
		if (minecraft == null
			|| minecraft.level == null
			|| minecraft.player == null
			|| minecraft.getWindow() == null
			|| minecraft.gui == null) {
			snapOff();
			PieChartController.syncThinCulling(minecraft);
			return;
		}

		Screen screen = minecraft.gui.screen();
		boolean blockedByScreen = screen != null
			&& !(screen instanceof ChatScreen)
			&& !(screen instanceof PauseScreen pause && !pause.showsPauseMenu());
		boolean hiddenHud = minecraft.gui.hud != null && minecraft.gui.hud.isHidden();
		if (blockedByScreen || hiddenHud) {
			snapOff();
			PieChartController.syncThinCulling(minecraft);
			return;
		}

		boolean targetActive = isStripModeActive(minecraft);
		long now = System.nanoTime();
		if (lastFrameNanos == 0L) {
			lastFrameNanos = now;
		}
		double dtMs = Math.clamp((now - lastFrameNanos) / 1_000_000.0, 0.0, 50.0);
		lastFrameNanos = now;

		float step = (float) (dtMs / DURATION_MS);
		if (targetActive) {
			progress = Math.min(1.0F, progress + step);
		} else {
			progress = Math.max(0.0F, progress - step);
		}
		PieChartController.syncThinCulling(minecraft);
	}

	public static float easedProgress() {
		float t = Math.clamp(progress, 0.0F, 1.0F);
		if (t < 0.5F) {
			return 4.0F * t * t * t;
		}
		return 1.0F - (float) Math.pow(-2.0F * t + 2.0F, 3.0) * 0.5F;
	}

	public static float horizontalSlideOffset(int boxX, int boxW, int screenW) {
		float inv = 1.0F - easedProgress();
		if (inv <= 0.0005F) {
			return 0.0F;
		}
		if (boxX + boxW * 0.5F < screenW * 0.5F) {
			return -(boxX + boxW + 8.0F) * inv;
		}
		return (screenW - boxX + 8.0F) * inv;
	}

	public static void extractSidebars(@Nullable Minecraft minecraft, GuiGraphicsExtractor graphics) {
		update(minecraft);
		float eased = easedProgress();
		if (eased <= 0.0005F || minecraft == null || minecraft.getWindow() == null) {
			return;
		}

		Window window = minecraft.getWindow();
		int guiScale = Math.max(1, window.getGuiScale());
		float invScale = 1.0F / (float) guiScale;
		int physW = window.getWidth();
		int physH = window.getHeight();
		int physThinW = Math.round(physH * ThinModeHud.THIN_ASPECT);
		if (physW <= 0 || physH <= 0 || physThinW >= physW) {
			return;
		}

		int fullLeftW = Math.max(0, (physW - physThinW) / 2);
		int fullRightW = Math.max(0, physW - (fullLeftW + physThinW));
		int curLeftW = Math.round(fullLeftW * eased);
		int curRightX = physW - Math.round(fullRightW * eased);

		graphics.pose().pushMatrix();
		try {
			if (guiScale != 1) {
				graphics.pose().scale(invScale, invScale);
			}
			if (curLeftW > 0) {
				graphics.fill(0, 0, curLeftW, physH, SIDEBAR_COLOR);
			}
			if (curRightX < physW) {
				graphics.fill(curRightX, 0, physW, physH, SIDEBAR_COLOR);
			}
		} finally {
			graphics.pose().popMatrix();
		}
		graphics.nextStratum();
	}
}
