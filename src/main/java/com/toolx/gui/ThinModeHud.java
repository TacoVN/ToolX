package com.toolx.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.toolx.PieChartController;
import com.toolx.ToolXConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

public final class ThinModeHud {
	public static final float THIN_ASPECT = 340.0F / 1080.0F;
	public static final double LEFT_STRIP_SNAP = -1.0;
	public static final double RIGHT_STRIP_SNAP = -2.0;

	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int IDLE_CHAT_BORDER_COLOR = 0x66FFFFFF;
	private static final int ACTIVE_BORDER_COLOR = 0xEEFFFFFF;

	private static final int MARGIN = 4;
	private static final int COUNTER_PAD = 2;
	private static final float COUNTER_TEXT_SCALE = 2.0F;
	private static final int COUNTER_GAP = 8;
	private static final int STRIP_GAP = 12;
	private static final double SNAP_THRESHOLD = 6.0;

	public record DebugSpace(
		int guiScale,
		int debugScale,
		float scale,
		int debugWidth,
		int debugHeight,
		int physLeftBarW,
		int stripLeft,
		int stripRight
	) {
	}

	private enum DragTarget {
		NONE,
		C_COUNTER,
		E_COUNTER,
		PIE
	}

	private static boolean cVisible;
	private static int cX;
	private static int cY;
	private static int cW;
	private static int cH;

	private static boolean eVisible;
	private static int eX;
	private static int eY;
	private static int eW;
	private static int eH;

	private static boolean pieVisible;
	private static int pieX;
	private static int pieY;
	private static int pieW;
	private static int pieH;

	private static @Nullable DebugSpace lastSpace;
	private static DragTarget dragTarget = DragTarget.NONE;
	private static double dragOffsetX;
	private static double dragOffsetY;
	private static double lastChatMouseX = -1.0;
	private static double lastChatMouseY = -1.0;

	private ThinModeHud() {
	}

	public static boolean isDragging() {
		return dragTarget != DragTarget.NONE;
	}

	public static boolean isHovered(double mouseX, double mouseY) {
		if (lastSpace == null || !shouldRenderThinLayout(Minecraft.getInstance())) {
			return false;
		}
		double safeScale = lastSpace.scale() > 0.0F ? lastSpace.scale() : 1.0F;
		double scaledX = mouseX / safeScale;
		double scaledY = mouseY / safeScale;
		return (cVisible && isInside(scaledX, scaledY, cX, cY, cW, cH))
			|| (eVisible && isInside(scaledX, scaledY, eX, eY, eW, eH))
			|| (pieVisible && isInside(scaledX, scaledY, pieX, pieY, pieW, pieH));
	}

	public static void resetState() {
		cVisible = false;
		eVisible = false;
		pieVisible = false;
		dragTarget = DragTarget.NONE;
		lastSpace = null;
	}

	public static void markPieChartHidden() {
		pieVisible = false;
		if (dragTarget == DragTarget.PIE) {
			dragTarget = DragTarget.NONE;
		}
	}

	public static boolean shouldRenderThinLayout(@Nullable Minecraft minecraft) {
		if (!PieChartController.isThinModeActive()
			|| minecraft == null
			|| minecraft.level == null
			|| minecraft.player == null
			|| minecraft.getWindow() == null
			|| minecraft.options == null
			|| minecraft.gui == null) {
			return false;
		}
		Screen screen = minecraft.gui.screen();
		if (screen != null
			&& !(screen instanceof ChatScreen)
			&& !(screen instanceof PauseScreen pause && !pause.showsPauseMenu())) {
			return false;
		}
		if (minecraft.gui.hud != null && minecraft.gui.hud.isHidden()) {
			return false;
		}
		Window window = minecraft.getWindow();
		int physW = window.getWidth();
		int physH = window.getHeight();
		return physW > 0 && physH > 0 && Math.round(physH * THIN_ASPECT) < physW;
	}

	public static @Nullable DebugSpace computeDebugSpace(Minecraft minecraft, GuiGraphicsExtractor graphics) {
		Window window = minecraft.getWindow();
		if (window == null || minecraft.options == null) {
			return null;
		}
		int guiScale = Math.max(1, window.getGuiScale());
		int debugScale = resolveDebugScale(minecraft, window, guiScale);
		boolean scaled = debugScale < guiScale && debugScale > 0;
		int debugWidth = scaled ? Math.max(1, window.getWidth() / debugScale) : Math.max(1, graphics.guiWidth());
		int debugHeight = scaled ? Math.max(1, window.getHeight() / debugScale) : Math.max(1, graphics.guiHeight());
		return buildDebugSpace(window, guiScale, debugScale, debugWidth, debugHeight);
	}

	public static DebugSpace computeDebugSpace(Minecraft minecraft, int debugWidth, int debugHeight) {
		Window window = minecraft.getWindow();
		int guiScale = Math.max(1, window.getGuiScale());
		int debugScale = resolveDebugScale(minecraft, window, guiScale);
		return buildDebugSpace(window, guiScale, debugScale, Math.max(1, debugWidth), Math.max(1, debugHeight));
	}

	public static float thinF3ScaleRatio(@Nullable Minecraft minecraft) {
		if (!shouldRenderThinLayout(minecraft)) {
			return 1.0F;
		}
		Window window = minecraft.getWindow();
		int physThinW = Math.round(window.getHeight() * THIN_ASPECT);
		int guiScale = Math.max(1, window.getGuiScale());
		int debugScale = resolveDebugScale(minecraft, window, guiScale);
		int effectiveDebugScale = (debugScale < guiScale && debugScale > 0) ? debugScale : guiScale;
		int targetF3Scale = Math.min(effectiveDebugScale, Math.max(1, physThinW / 320));
		return (float) targetF3Scale / (float) effectiveDebugScale;
	}

	public static int thinF3LogicalWidth(@Nullable Minecraft minecraft, int debugWidth) {
		if (!shouldRenderThinLayout(minecraft)) {
			return debugWidth;
		}
		Window window = minecraft.getWindow();
		int physThinW = Math.round(window.getHeight() * THIN_ASPECT);
		int guiScale = Math.max(1, window.getGuiScale());
		int debugScale = resolveDebugScale(minecraft, window, guiScale);
		int effectiveDebugScale = (debugScale < guiScale && debugScale > 0) ? debugScale : guiScale;
		int targetF3Scale = Math.min(effectiveDebugScale, Math.max(1, physThinW / 320));
		return Math.max(1, physThinW / targetF3Scale);
	}

	private static int resolveDebugScale(Minecraft minecraft, Window window, int guiScale) {
		int rawDebugScale = minecraft.options.debugGuiScale().get();
		if (rawDebugScale == Options.DEBUG_GUI_SCALE_UNCHANGED) {
			return guiScale;
		}
		if (rawDebugScale == Options.AUTO_GUI_SCALE) {
			return window.calculateScale(0, minecraft.isEnforceUnicode()) / 2;
		}
		return window.calculateScale(rawDebugScale, minecraft.isEnforceUnicode());
	}

	private static DebugSpace buildDebugSpace(Window window, int guiScale, int debugScale, int debugWidth, int debugHeight) {
		boolean scaled = debugScale < guiScale && debugScale > 0;
		float scale = scaled ? (float) debugScale / (float) guiScale : 1.0F;
		int physW = Math.max(1, window.getWidth());
		int physH = Math.max(1, window.getHeight());
		int physThinW = Math.round(physH * THIN_ASPECT);
		int physLeftBarW = Math.max(0, (physW - physThinW) / 2);
		int physRightBarX = Math.min(physW, physLeftBarW + physThinW);
		int stripLeft = Math.round((float) physLeftBarW * debugWidth / (float) physW);
		int stripRight = Math.round((float) physRightBarX * debugWidth / (float) physW);
		return new DebugSpace(
			guiScale,
			debugScale,
			scale,
			debugWidth,
			debugHeight,
			physLeftBarW,
			stripLeft,
			stripRight
		);
	}

	public static void extractCounters(@Nullable Minecraft minecraft, GuiGraphicsExtractor graphics) {
		if (!shouldRenderThinLayout(minecraft) || minecraft.levelExtractor == null) {
			cVisible = false;
			eVisible = false;
			if (dragTarget == DragTarget.C_COUNTER || dragTarget == DragTarget.E_COUNTER) {
				dragTarget = DragTarget.NONE;
			}
			return;
		}

		String cText = parseCounterPrefix(minecraft.levelExtractor.sectionStatistics());
		String eText = parseCounterPrefix(minecraft.levelExtractor.entityStatistics());
		if (cText == null && eText == null) {
			cVisible = false;
			eVisible = false;
			if (dragTarget == DragTarget.C_COUNTER || dragTarget == DragTarget.E_COUNTER) {
				dragTarget = DragTarget.NONE;
			}
			return;
		}

		DebugSpace space = computeDebugSpace(minecraft, graphics);
		if (space == null) {
			cVisible = false;
			eVisible = false;
			return;
		}

		int boxH = Math.round(9 * COUNTER_TEXT_SCALE) + COUNTER_PAD * 2;
		int minX = MARGIN;
		int minY = MARGIN;
		int maxY = Math.max(minY, space.debugHeight() - boxH - MARGIN);

		int pairH = boxH * 2 + COUNTER_GAP;
		int pairTopY = (space.debugHeight() - pairH) / 2;
		int defaultCY = Math.clamp(pairTopY, minY, maxY);
		int defaultEY = Math.clamp(defaultCY + boxH + COUNTER_GAP, minY, maxY);

		lastSpace = space;
		ToolXConfig config = ToolXConfig.get();

		if (cText != null) {
			int calcCW = counterBoxWidth(minecraft, cText);
			int maxCX = Math.max(minX, space.debugWidth() - calcCW - MARGIN);
			int boxX = resolveCounterX(config.thinCCounterX, calcCW, minX, maxCX, space);
			int boxY = config.thinCCounterY < 0.0
				? defaultCY
				: (int) Math.round(minY + Math.clamp(config.thinCCounterY, 0.0, 1.0) * (maxY - minY));
			cVisible = true;
			cX = boxX;
			cY = boxY;
			cW = calcCW;
			cH = boxH;
			renderCounterBox(minecraft, graphics, space, cText, boxX, boxY, calcCW);
		} else {
			cVisible = false;
			if (dragTarget == DragTarget.C_COUNTER) {
				dragTarget = DragTarget.NONE;
			}
		}

		if (eText != null) {
			int calcEW = counterBoxWidth(minecraft, eText);
			int maxEX = Math.max(minX, space.debugWidth() - calcEW - MARGIN);
			int boxX = resolveCounterX(config.thinECounterX, calcEW, minX, maxEX, space);
			int boxY = config.thinECounterY < 0.0
				? defaultEY
				: (int) Math.round(minY + Math.clamp(config.thinECounterY, 0.0, 1.0) * (maxY - minY));
			eVisible = true;
			eX = boxX;
			eY = boxY;
			eW = calcEW;
			eH = boxH;
			renderCounterBox(minecraft, graphics, space, eText, boxX, boxY, calcEW);
		} else {
			eVisible = false;
			if (dragTarget == DragTarget.E_COUNTER) {
				dragTarget = DragTarget.NONE;
			}
		}
	}

	private static int resolveCounterX(double savedX, int boxW, int minX, int maxX, DebugSpace space) {
		if (savedX <= -1.5) {
			return Math.clamp(space.stripRight() + STRIP_GAP, minX, maxX);
		}
		if (savedX < 0.0) {
			return Math.clamp(space.stripLeft() - boxW - STRIP_GAP, minX, maxX);
		}
		return (int) Math.round(minX + Math.clamp(savedX, 0.0, 1.0) * (maxX - minX));
	}

	private static @Nullable String parseCounterPrefix(@Nullable String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		int firstSpace = raw.indexOf(' ');
		int end = firstSpace >= 0 ? raw.indexOf(' ', firstSpace + 1) : -1;
		String prefix = (end >= 0 ? raw.substring(0, end) : raw).trim();
		while (prefix.endsWith(",")) {
			prefix = prefix.substring(0, prefix.length() - 1).trim();
		}
		return prefix.isEmpty() ? null : prefix;
	}

	private static int counterBoxWidth(Minecraft minecraft, String text) {
		int textWidth = Math.max(1, minecraft.font.width(text) - 1);
		return Math.round(textWidth * COUNTER_TEXT_SCALE) + COUNTER_PAD * 2;
	}

	private static void renderCounterBox(
		Minecraft minecraft,
		GuiGraphicsExtractor graphics,
		DebugSpace space,
		String text,
		int boxX,
		int boxY,
		int boxW
	) {
		float slideX = StripTransition.horizontalSlideOffset(boxX, boxW, space.debugWidth());
		graphics.pose().pushMatrix();
		try {
			if (space.scale() != 1.0F) {
				graphics.pose().scale(space.scale(), space.scale());
			}
			graphics.pose().translate((float) (boxX + COUNTER_PAD) + slideX, (float) (boxY + COUNTER_PAD));
			graphics.pose().scale(COUNTER_TEXT_SCALE, COUNTER_TEXT_SCALE);
			graphics.text(minecraft.font, text, 0, 0, TEXT_COLOR);
		} finally {
			graphics.pose().popMatrix();
		}
	}

	public static boolean beginPieChartOffset(
		GuiGraphicsExtractor graphics,
		int origX,
		int origY,
		int boxW,
		int boxH,
		int debugWidth,
		int debugHeight
	) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!shouldRenderThinLayout(minecraft)) {
			markPieChartHidden();
			return false;
		}

		DebugSpace space = computeDebugSpace(minecraft, debugWidth, debugHeight);
		int minX = MARGIN;
		int minY = MARGIN;
		int maxX = Math.max(minX, space.debugWidth() - boxW - MARGIN);
		int maxY = Math.max(minY, space.debugHeight() - boxH - MARGIN);

		ToolXConfig config = ToolXConfig.get();
		int defaultX = Math.round((space.stripRight() + space.debugWidth() - boxW) * 0.5F);
		int targetX = config.thinPieChartX < 0.0
			? Math.clamp(defaultX, minX, maxX)
			: (int) Math.round(minX + Math.clamp(config.thinPieChartX, 0.0, 1.0) * (maxX - minX));
		int targetY = config.thinPieChartY < 0.0
			? (int) Math.round((minY + maxY) * 0.5)
			: (int) Math.round(minY + Math.clamp(config.thinPieChartY, 0.0, 1.0) * (maxY - minY));

		pieVisible = true;
		pieX = targetX;
		pieY = targetY;
		pieW = boxW;
		pieH = boxH;
		lastSpace = space;

		float slideX = StripTransition.horizontalSlideOffset(targetX, boxW, space.debugWidth());
		graphics.pose().pushMatrix();
		graphics.pose().translate((float) (targetX - origX) + slideX, (float) (targetY - origY));
		return true;
	}

	public static void endPieChartOffset(GuiGraphicsExtractor graphics) {
		graphics.pose().popMatrix();
		Minecraft minecraft = Minecraft.getInstance();
		if (pieVisible && minecraft.gui != null && minecraft.gui.screen() instanceof ChatScreen) {
			boolean active = dragTarget == DragTarget.PIE
				|| (dragTarget == DragTarget.NONE
					&& !EyeMeasurementHud.isDragging()
					&& !TimerHud.isDragging()
					&& !ResourceTrackerHud.isDragging()
					&& lastChatMouseX >= 0.0
					&& !(cVisible && isInside(lastChatMouseX, lastChatMouseY, cX, cY, cW, cH))
					&& !(eVisible && isInside(lastChatMouseX, lastChatMouseY, eX, eY, eW, eH))
					&& isInside(lastChatMouseX, lastChatMouseY, pieX, pieY, pieW, pieH));
			graphics.nextStratum();
			graphics.outline(pieX, pieY, pieW, pieH, active ? ACTIVE_BORDER_COLOR : IDLE_CHAT_BORDER_COLOR);
		}
	}

	public static boolean onChatMouseClicked(double mouseX, double mouseY, int button) {
		if (lastSpace == null || !shouldRenderThinLayout(Minecraft.getInstance())) {
			return false;
		}
		double safeScale = lastSpace.scale() > 0.0F ? lastSpace.scale() : 1.0F;
		double scaledX = mouseX / safeScale;
		double scaledY = mouseY / safeScale;

		DragTarget hit = DragTarget.NONE;
		if (cVisible && isInside(scaledX, scaledY, cX, cY, cW, cH)) {
			hit = DragTarget.C_COUNTER;
		} else if (eVisible && isInside(scaledX, scaledY, eX, eY, eW, eH)) {
			hit = DragTarget.E_COUNTER;
		} else if (pieVisible && isInside(scaledX, scaledY, pieX, pieY, pieW, pieH)) {
			hit = DragTarget.PIE;
		}

		if (hit == DragTarget.NONE) {
			return false;
		}

		if (button == InputConstants.MOUSE_BUTTON_LEFT) {
			dragTarget = hit;
			int hitX = switch (hit) {
				case C_COUNTER -> cX;
				case E_COUNTER -> eX;
				case PIE -> pieX;
				default -> 0;
			};
			int hitY = switch (hit) {
				case C_COUNTER -> cY;
				case E_COUNTER -> eY;
				case PIE -> pieY;
				default -> 0;
			};
			dragOffsetX = scaledX - hitX;
			dragOffsetY = scaledY - hitY;
			return true;
		}

		if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
			dragTarget = DragTarget.NONE;
			ToolXConfig config = ToolXConfig.get();
			if (hit == DragTarget.C_COUNTER) {
				config.thinCCounterX = LEFT_STRIP_SNAP;
				config.thinCCounterY = -1.0;
			} else if (hit == DragTarget.E_COUNTER) {
				config.thinECounterX = LEFT_STRIP_SNAP;
				config.thinECounterY = -1.0;
			} else {
				config.thinPieChartX = -1.0;
				config.thinPieChartY = -1.0;
			}
			config.save();
			return true;
		}

		return false;
	}

	public static void onChatRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		if (!shouldRenderThinLayout(Minecraft.getInstance()) || lastSpace == null) {
			dragTarget = DragTarget.NONE;
			lastChatMouseX = -1.0;
			lastChatMouseY = -1.0;
			return;
		}

		float scale = lastSpace.scale() > 0.0F ? lastSpace.scale() : 1.0F;
		double scaledMouseX = mouseX / (double) scale;
		double scaledMouseY = mouseY / (double) scale;
		lastChatMouseX = scaledMouseX;
		lastChatMouseY = scaledMouseY;

		if (dragTarget == DragTarget.C_COUNTER && cVisible) {
			applyDrag(scaledMouseX, scaledMouseY, cW, cH, DragTarget.C_COUNTER);
		} else if (dragTarget == DragTarget.E_COUNTER && eVisible) {
			applyDrag(scaledMouseX, scaledMouseY, eW, eH, DragTarget.E_COUNTER);
		} else if (dragTarget == DragTarget.PIE && pieVisible) {
			applyDrag(scaledMouseX, scaledMouseY, pieW, pieH, DragTarget.PIE);
		}

		boolean canHover = dragTarget == DragTarget.NONE
			&& !EyeMeasurementHud.isDragging()
			&& !TimerHud.isDragging()
			&& !ResourceTrackerHud.isDragging();
		boolean cHovered = canHover && cVisible && isInside(scaledMouseX, scaledMouseY, cX, cY, cW, cH);
		boolean eHovered = canHover && !cHovered && eVisible && isInside(scaledMouseX, scaledMouseY, eX, eY, eW, eH);

		if (cVisible) {
			boolean active = dragTarget == DragTarget.C_COUNTER || cHovered;
			drawChatOutline(graphics, scale, cX, cY, cW, cH, active);
		}
		if (eVisible) {
			boolean active = dragTarget == DragTarget.E_COUNTER || eHovered;
			drawChatOutline(graphics, scale, eX, eY, eW, eH, active);
		}
	}

	private static void drawChatOutline(GuiGraphicsExtractor graphics, float scale, int x, int y, int w, int h, boolean active) {
		graphics.pose().pushMatrix();
		try {
			if (scale != 1.0F) {
				graphics.pose().scale(scale, scale);
			}
			graphics.outline(x, y, w, h, active ? ACTIVE_BORDER_COLOR : IDLE_CHAT_BORDER_COLOR);
		} finally {
			graphics.pose().popMatrix();
		}
	}

	private static void applyDrag(double scaledMouseX, double scaledMouseY, int boxW, int boxH, DragTarget target) {
		DebugSpace space = lastSpace;
		if (space == null) {
			return;
		}
		int minX = MARGIN;
		int minY = MARGIN;
		int maxX = Math.max(minX, space.debugWidth() - boxW - MARGIN);
		int maxY = Math.max(minY, space.debugHeight() - boxH - MARGIN);

		double clampedX = snapX(Math.clamp(scaledMouseX - dragOffsetX, minX, maxX), minX, maxX, boxW, space);
		double clampedY = target == DragTarget.PIE
			? snapPieY(Math.clamp(scaledMouseY - dragOffsetY, minY, maxY), minY, maxY)
			: snapCounterY(Math.clamp(scaledMouseY - dragOffsetY, minY, maxY), minY, maxY, boxH, space);

		double normX = maxX > minX ? (clampedX - minX) / (maxX - minX) : 0.0;
		double normY = maxY > minY ? (clampedY - minY) / (maxY - minY) : 0.5;

		ToolXConfig config = ToolXConfig.get();
		if (target == DragTarget.PIE) {
			config.thinPieChartX = normX;
			config.thinPieChartY = normY;
			pieX = (int) Math.round(clampedX);
			pieY = (int) Math.round(clampedY);
			return;
		}

		double leftStripAnchor = Math.clamp(space.stripLeft() - boxW - STRIP_GAP, minX, maxX);
		double rightStripAnchor = Math.clamp(space.stripRight() + STRIP_GAP, minX, maxX);
		double storedX;
		if (Math.abs(clampedX - leftStripAnchor) < 0.5) {
			storedX = LEFT_STRIP_SNAP;
		} else if (Math.abs(clampedX - rightStripAnchor) < 0.5) {
			storedX = RIGHT_STRIP_SNAP;
		} else {
			storedX = normX;
		}

		if (target == DragTarget.C_COUNTER) {
			config.thinCCounterX = storedX;
			config.thinCCounterY = normY;
			cX = (int) Math.round(clampedX);
			cY = (int) Math.round(clampedY);
		} else {
			config.thinECounterX = storedX;
			config.thinECounterY = normY;
			eX = (int) Math.round(clampedX);
			eY = (int) Math.round(clampedY);
		}
	}

	private static double snapX(double value, int min, int max, int boxW, DebugSpace space) {
		if (max <= min) {
			return min;
		}
		double[] anchors = {
			min,
			(min + max) * 0.5,
			max,
			(space.stripLeft() - boxW) * 0.5,
			space.stripLeft() - boxW - STRIP_GAP,
			space.stripRight() + STRIP_GAP,
			(space.stripRight() + space.debugWidth() - boxW) * 0.5
		};
		for (double anchor : anchors) {
			double clampedAnchor = Math.clamp(anchor, min, max);
			if (Math.abs(value - clampedAnchor) <= SNAP_THRESHOLD) {
				return clampedAnchor;
			}
		}
		return value;
	}

	private static double snapCounterY(double value, int min, int max, int boxH, DebugSpace space) {
		if (max <= min) {
			return min;
		}
		int pairH = boxH * 2 + COUNTER_GAP;
		int pairTopY = (space.debugHeight() - pairH) / 2;
		int defaultCY = Math.clamp(pairTopY, min, max);
		int defaultEY = Math.clamp(defaultCY + boxH + COUNTER_GAP, min, max);
		double[] anchors = {
			min,
			defaultCY,
			(min + max) * 0.5,
			defaultEY,
			max
		};
		for (double anchor : anchors) {
			if (Math.abs(value - anchor) <= SNAP_THRESHOLD) {
				return anchor;
			}
		}
		return value;
	}

	private static double snapPieY(double value, int min, int max) {
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
		if (dragTarget != DragTarget.NONE) {
			dragTarget = DragTarget.NONE;
			ToolXConfig.get().save();
		}
	}

	private static boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
		return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
	}
}
