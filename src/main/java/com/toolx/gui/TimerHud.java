package com.toolx.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.toolx.TimerController;
import com.toolx.ToolXConfig;
import com.toolx.gui.ThinModeHud.DebugSpace;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.debug.DebugOptionsScreen;
import net.minecraft.client.gui.screens.options.InWorldGameRulesScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.telemetry.TelemetryInfoScreen;
import net.minecraft.world.level.GameType;
import org.jspecify.annotations.Nullable;

public final class TimerHud {
	private static final int MARGIN = 4;
	private static final int PAD = 3;
	private static final float TEXT_SCALE = 2.0F;
	private static final int TEXT_HEIGHT = 9;
	private static final double SNAP_THRESHOLD = 6.0;

	private static final int IDLE_TEXT_COLOR = 0xFFFFFFFF;
	private static final int RUNNING_TEXT_COLOR = 0xFFFFDE21;
	private static final int COMPLETED_TEXT_COLOR = 0xFF1FFF1F;
	private static final int OUTLINE_COLOR = 0xFF000000;
	private static final int IDLE_CHAT_BORDER_COLOR = 0x66FFFFFF;
	private static final int ACTIVE_BORDER_COLOR = 0xEEFFFFFF;

	private static boolean visible;
	private static int lastX;
	private static int lastY;
	private static int lastW;
	private static int lastH;
	private static int lastGuiW;
	private static int lastGuiH;
	private static float lastScale = 1.0F;

	private static boolean dragging;
	private static double dragOffsetX;
	private static double dragOffsetY;
	private static boolean chatHovered;

	private TimerHud() {
	}

	public static boolean isDragging() {
		return dragging;
	}

	public static boolean isHovered(double mouseX, double mouseY) {
		if (!visible || !shouldRender(Minecraft.getInstance())) {
			return false;
		}
		double safeScale = lastScale > 0.0F ? lastScale : 1.0F;
		double scaledX = mouseX / safeScale;
		double scaledY = mouseY / safeScale;
		return isInside(scaledX, scaledY, lastX, lastY, lastW, lastH);
	}

	public static void cancelDrag() {
		dragging = false;
	}

	public static boolean shouldRender(@Nullable Minecraft minecraft) {
		if (!ToolXConfig.get().timer
			|| minecraft == null
			|| minecraft.level == null
			|| minecraft.player == null
			|| minecraft.gameMode == null
			|| minecraft.gameMode.getPlayerMode() != GameType.SURVIVAL
			|| minecraft.getWindow() == null
			|| minecraft.options == null
			|| minecraft.gui == null) {
			return false;
		}
		if (StripTransition.isStripModeActive(minecraft) || StripTransition.easedProgress() > 0.0005F) {
			return false;
		}
		if (minecraft.debugEntries != null && minecraft.debugEntries.isOverlayVisible()) {
			return false;
		}
		Screen screen = minecraft.gui.screen();
		if (isOptionsScreen(screen) || screen instanceof LevelLoadingScreen) {
			return false;
		}
		return !(minecraft.gui.hud != null && minecraft.gui.hud.isHidden() && screen == null);
	}

	public static boolean isOptionsScreen(@Nullable Screen screen) {
		return screen instanceof OptionsScreen
			|| screen instanceof OptionsSubScreen
			|| screen instanceof InWorldGameRulesScreen
			|| screen instanceof PackSelectionScreen
			|| screen instanceof CreditsAndAttributionScreen
			|| screen instanceof TelemetryInfoScreen
			|| screen instanceof DebugOptionsScreen;
	}

	public static void extractRenderState(@Nullable Minecraft minecraft, GuiGraphicsExtractor graphics) {
		if (!shouldRender(minecraft)) {
			visible = false;
			dragging = false;
			chatHovered = false;
			return;
		}

		DebugSpace space = ThinModeHud.computeDebugSpace(minecraft, graphics);
		if (space == null) {
			visible = false;
			dragging = false;
			chatHovered = false;
			return;
		}

		String text = TimerController.formattedRta();
		Font font = minecraft.font;
		int textW = Math.max(1, font.width(text) - 1);
		int boxW = Math.round(textW * TEXT_SCALE) + PAD * 2;
		int boxH = Math.round(TEXT_HEIGHT * TEXT_SCALE) + PAD * 2;

		int minX = MARGIN;
		int minY = MARGIN;
		int maxX = Math.max(minX, space.debugWidth() - boxW - MARGIN);
		int maxY = Math.max(minY, space.debugHeight() - boxH - MARGIN);

		ToolXConfig config = ToolXConfig.get();
		double normX = Math.clamp(config.timerHudX, 0.0, 1.0);
		double normY = Math.clamp(config.timerHudY, 0.0, 1.0);
		int boxX = (int) Math.round(minX + normX * (maxX - minX));
		int boxY = (int) Math.round(minY + normY * (maxY - minY));

		visible = true;
		lastX = boxX;
		lastY = boxY;
		lastW = boxW;
		lastH = boxH;
		lastGuiW = space.debugWidth();
		lastGuiH = space.debugHeight();
		lastScale = space.scale();

		int textX = boxX + PAD;
		int textY = boxY + PAD;

		graphics.nextStratum();
		graphics.pose().pushMatrix();
		try {
			if (space.scale() != 1.0F) {
				graphics.pose().scale(space.scale(), space.scale());
			}
			graphics.pose().pushMatrix();
			try {
				graphics.pose().translate((float) textX, (float) textY);
				graphics.pose().scale(TEXT_SCALE, TEXT_SCALE);
				for (int dx = -1; dx <= 1; dx++) {
					for (int dy = -1; dy <= 1; dy++) {
						if (dx == 0 && dy == 0) {
							continue;
						}
						graphics.text(font, text, dx, dy, OUTLINE_COLOR, false);
					}
				}
				int textColor = TimerController.isCompleted()
					? COMPLETED_TEXT_COLOR
					: (TimerController.isRunning() ? RUNNING_TEXT_COLOR : IDLE_TEXT_COLOR);
				graphics.text(font, text, 0, 0, textColor, false);
			} finally {
				graphics.pose().popMatrix();
			}

			if (minecraft.gui.screen() instanceof ChatScreen) {
				boolean active = dragging || chatHovered;
				graphics.outline(boxX, boxY, boxW, boxH, active ? ACTIVE_BORDER_COLOR : IDLE_CHAT_BORDER_COLOR);
			}
		} finally {
			graphics.pose().popMatrix();
		}
	}

	public static boolean onChatMouseClicked(double mouseX, double mouseY, int button) {
		if (!visible || !shouldRender(Minecraft.getInstance())) {
			return false;
		}
		double safeScale = lastScale > 0.0F ? lastScale : 1.0F;
		double scaledX = mouseX / safeScale;
		double scaledY = mouseY / safeScale;
		if (!isInside(scaledX, scaledY, lastX, lastY, lastW, lastH)) {
			return false;
		}
		if (button == InputConstants.MOUSE_BUTTON_LEFT) {
			dragging = true;
			dragOffsetX = scaledX - lastX;
			dragOffsetY = scaledY - lastY;
			return true;
		}
		if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
			dragging = false;
			ToolXConfig config = ToolXConfig.get();
			config.timerHudX = 1.0;
			config.timerHudY = 0.5;
			config.save();
			return true;
		}
		return false;
	}

	public static void onChatRender(int mouseX, int mouseY) {
		if (!visible || !shouldRender(Minecraft.getInstance())) {
			dragging = false;
			chatHovered = false;
			return;
		}
		double safeScale = lastScale > 0.0F ? lastScale : 1.0F;
		double scaledMouseX = mouseX / safeScale;
		double scaledMouseY = mouseY / safeScale;
		chatHovered = !dragging
			&& !ThinModeHud.isDragging()
			&& !EyeMeasurementHud.isDragging()
			&& !ResourceTrackerHud.isDragging()
			&& !ThinModeHud.isHovered(mouseX, mouseY)
			&& !EyeMeasurementHud.isHovered(mouseX, mouseY)
			&& isInside(scaledMouseX, scaledMouseY, lastX, lastY, lastW, lastH);
		if (!dragging) {
			return;
		}

		int minX = MARGIN;
		int minY = MARGIN;
		int maxX = Math.max(minX, lastGuiW - lastW - MARGIN);
		int maxY = Math.max(minY, lastGuiH - lastH - MARGIN);

		double clampedX = snapAxis(Math.clamp(scaledMouseX - dragOffsetX, minX, maxX), minX, maxX);
		double clampedY = snapAxis(Math.clamp(scaledMouseY - dragOffsetY, minY, maxY), minY, maxY);

		ToolXConfig config = ToolXConfig.get();
		config.timerHudX = maxX > minX ? (clampedX - minX) / (maxX - minX) : 1.0;
		config.timerHudY = maxY > minY ? (clampedY - minY) / (maxY - minY) : 0.5;
		lastX = (int) Math.round(clampedX);
		lastY = (int) Math.round(clampedY);
	}

	private static double snapAxis(double value, int min, int max) {
		if (max <= min) {
			return min;
		}
		if (Math.abs(value - min) <= SNAP_THRESHOLD) {
			return min;
		}
		double center = (min + max) * 0.5;
		if (Math.abs(value - center) <= SNAP_THRESHOLD) {
			return center;
		}
		if (Math.abs(value - max) <= SNAP_THRESHOLD) {
			return max;
		}
		return value;
	}

	public static void endDrag() {
		if (dragging) {
			dragging = false;
			ToolXConfig.get().save();
		}
	}

	private static boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
		return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
	}
}
