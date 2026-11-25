package com.toolx.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.toolx.EyeMeasurementController;
import com.toolx.EyeMeasurementController.Result;
import com.toolx.ToolXConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class EyeMeasurementHud {
	private static final int MARGIN = 4;
	private static final int PADDING_X = 3;
	private static final int PADDING_Y = 3;
	private static final int ICON_SIZE = 16;
	private static final int SPEED_ICON_SIZE = 18;
	private static final int ICON_TEXT_GAP = 3;
	private static final int TEXT_HEIGHT = 9;
	private static final int COL_GAP = 10;
	private static final double SNAP_THRESHOLD = 6.0;

	private static final int HOVER_BORDER_COLOR = 0xCCFFFFFF;
	private static final int TEXT_BASE_COLOR = 0xFFFFFFFF;
	private static final int CROSSHAIR_INVERT_COLOR = 0xFFFFFFFF;
	private static final int CROSSHAIR_TIP_COLOR = 0xFFFF3B3B;

	private static final int COLOR_GREEN = 0x1FFF1F;
	private static final int COLOR_RED = 0xFF0B0B;
	private static final int COLOR_YELLOW = 0xFFDE21;

	private static final int CENTER_GAP_HALF = 3;
	private static final double RULER_MAX_DEG = 0.20;
	private static final double RULER_STEP_DEG = 0.05;
	private static final Identifier ENDER_EYE_TEXTURE = Identifier.withDefaultNamespace("textures/item/ender_eye.png");

	private static @Nullable ItemStack grassIcon;
	private static @Nullable ItemStack netherrackIcon;
	private static @Nullable Identifier speedSprite;

	private static boolean visible;
	private static int lastX;
	private static int lastY;
	private static int lastWidth;
	private static int lastHeight;
	private static int lastGuiWidth;
	private static int lastGuiHeight;
	private static float lastScale = 1.0F;

	private static boolean dragging;
	private static double dragOffsetX;
	private static double dragOffsetY;

	private EyeMeasurementHud() {
	}

	private static void ensureIcons() {
		if (grassIcon == null) {
			grassIcon = new ItemStack(Items.GRASS_BLOCK);
			netherrackIcon = new ItemStack(Items.NETHERRACK);
			speedSprite = Hud.getMobEffectSprite(MobEffects.SPEED);
		}
	}

	public static void extractRenderState(@Nullable Minecraft minecraft, GuiGraphicsExtractor graphics) {
		if (minecraft == null
			|| minecraft.player == null
			|| minecraft.level == null
			|| minecraft.getWindow() == null
			|| minecraft.options == null
			|| minecraft.gui == null
			|| minecraft.gui.hud == null) {
			visible = false;
			dragging = false;
			return;
		}

		if (minecraft.gui.hud.isHidden() && minecraft.gui.screen() == null) {
			visible = false;
			dragging = false;
			return;
		}

		ResourceKey<Level> dimension = minecraft.level.dimension();
		boolean inOverworld = Level.OVERWORLD.equals(dimension);
		boolean inNether = Level.NETHER.equals(dimension);

		if (EyeMeasurementController.isMeasuring() && inOverworld) {
			extractMeasureCrosshair(minecraft.getWindow(), graphics);
		}

		if (!inOverworld && !inNether) {
			visible = false;
			dragging = false;
			return;
		}

		Result result = EyeMeasurementController.lastResult();
		if (result == null) {
			visible = false;
			dragging = false;
			return;
		}

		ensureIcons();

		int targetBlockX = inOverworld ? result.owX() : result.netherX();
		int targetBlockZ = inOverworld ? result.owZ() : result.netherZ();
		double dx = (targetBlockX + 0.5) - minecraft.player.getX();
		double dz = (targetBlockZ + 0.5) - minecraft.player.getZ();
		boolean atTargetBlock = minecraft.player.getBlockX() == targetBlockX && minecraft.player.getBlockZ() == targetBlockZ;
		long distance = atTargetBlock ? 0L : Math.round(Math.hypot(dx, dz));

		int pct = Math.clamp(Math.round(result.accuracy() * 100.0), 0, 100);
		int pctColor = pct >= 90 ? COLOR_GREEN : (pct >= 50 ? COLOR_YELLOW : COLOR_RED);

		Component[] colTexts = {
			Component.literal(result.owX() + ", " + result.owZ()),
			Component.literal(result.netherX() + ", " + result.netherZ()),
			Component.literal(String.valueOf(distance)),
			colored(pct + "%", pctColor)
		};

		Font font = minecraft.font;
		int[] colWidths = new int[colTexts.length];
		int[] visualTextWidths = new int[colTexts.length];
		int totalColsWidth = 0;
		for (int i = 0; i < colTexts.length; i++) {
			visualTextWidths[i] = Math.max(0, font.width(colTexts[i]) - 1);
			int baseIconWidth = (i == 2) ? SPEED_ICON_SIZE : ICON_SIZE;
			colWidths[i] = Math.max(baseIconWidth, visualTextWidths[i]);
			totalColsWidth += colWidths[i];
		}

		int width = PADDING_X * 2 + totalColsWidth + COL_GAP * (colTexts.length - 1);
		int height = PADDING_Y * 2 + ICON_SIZE + ICON_TEXT_GAP + TEXT_HEIGHT;

		Window window = minecraft.getWindow();
		int guiScale = Math.max(1, window.getGuiScale());
		int rawDebugScale = minecraft.options.debugGuiScale().get();
		int debugScale;
		if (rawDebugScale == Options.DEBUG_GUI_SCALE_UNCHANGED) {
			debugScale = guiScale;
		} else if (rawDebugScale == Options.AUTO_GUI_SCALE) {
			debugScale = window.calculateScale(0, minecraft.isEnforceUnicode()) / 2;
		} else {
			debugScale = window.calculateScale(rawDebugScale, minecraft.isEnforceUnicode());
		}

		boolean scaled = debugScale < guiScale && debugScale > 0;
		float scale = scaled ? (float) debugScale / (float) guiScale : 1.0F;
		int guiWidth = scaled ? window.getWidth() / debugScale : graphics.guiWidth();
		int guiHeight = scaled ? window.getHeight() / debugScale : graphics.guiHeight();
		int minX = MARGIN;
		int minY = MARGIN;
		int maxX = Math.max(minX, guiWidth - width - MARGIN);
		int maxY = Math.max(minY, guiHeight - height - MARGIN);

		ToolXConfig config = ToolXConfig.get();
		double hudX = Math.clamp(config.eyeMeasurementHudX, 0.0, 1.0);
		double hudY = Math.clamp(config.eyeMeasurementHudY, 0.0, 1.0);
		int boxX = (int) Math.round(minX + hudX * (maxX - minX));
		int boxY = (int) Math.round(minY + hudY * (maxY - minY));

		lastX = boxX;
		lastY = boxY;
		lastWidth = width;
		lastHeight = height;
		lastGuiWidth = guiWidth;
		lastGuiHeight = guiHeight;
		lastScale = scale;
		visible = true;

		graphics.pose().pushMatrix();
		try {
			if (scaled) {
				graphics.pose().scale(scale, scale);
			}

			int iconY = boxY + PADDING_Y;
			int textY = iconY + ICON_SIZE + ICON_TEXT_GAP;
			int colX = boxX + PADDING_X;

			for (int i = 0; i < colTexts.length; i++) {
				if (i == 2) {
					int speedX = colX + Math.round((colWidths[i] - SPEED_ICON_SIZE) / 2.0F) - 1;
					graphics.blitSprite(RenderPipelines.GUI_TEXTURED, speedSprite, speedX, iconY - 2, SPEED_ICON_SIZE, SPEED_ICON_SIZE);
				} else {
					int iconX = colX + Math.round((colWidths[i] - ICON_SIZE) / 2.0F);
					switch (i) {
						case 0 -> graphics.fakeItem(grassIcon, iconX, iconY);
						case 1 -> graphics.fakeItem(netherrackIcon, iconX, iconY);
						case 3 -> graphics.blit(RenderPipelines.GUI_TEXTURED, ENDER_EYE_TEXTURE, iconX, iconY, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
					}
				}

				int textX = colX + Math.round((colWidths[i] - visualTextWidths[i]) / 2.0F);
				graphics.text(font, colTexts[i], textX, textY, TEXT_BASE_COLOR);
				colX += colWidths[i] + COL_GAP;
			}
		} finally {
			graphics.pose().popMatrix();
		}
	}

	private static void extractMeasureCrosshair(Window window, GuiGraphicsExtractor graphics) {
		int guiScale = Math.max(1, window.getGuiScale());
		float invScale = 1.0F / (float) guiScale;
		int physW = window.getWidth();
		int physH = window.getHeight();
		int cx = physW / 2;
		int cy = physH / 2;

		double halfFovRad = Math.toRadians(EyeMeasurementController.MEASURE_VFOV * 0.5);
		double pxPerRad = (physH * 0.5) * EyeMeasurementController.HORIZONTAL_STRETCH / Math.tan(halfFovRad);
		int maxReachX = (int) Math.round(Math.tan(Math.toRadians(RULER_MAX_DEG)) * pxPerRad);
		maxReachX = Math.clamp(maxReachX, 24, Math.max(24, cx - 8));
		int halfSpanY = Math.clamp(physH / 3, 40, Math.max(40, cy - 12));

		graphics.pose().pushMatrix();
		try {
			if (guiScale != 1) {
				graphics.pose().scale(invScale, invScale);
			}

			graphics.fill(RenderPipelines.GUI_INVERT, cx, cy - halfSpanY, cx + 1, cy - CENTER_GAP_HALF, CROSSHAIR_INVERT_COLOR);
			graphics.fill(RenderPipelines.GUI_INVERT, cx, cy + CENTER_GAP_HALF + 1, cx + 1, cy + halfSpanY + 1, CROSSHAIR_INVERT_COLOR);

			int tipLen = Math.min(14, Math.max(6, halfSpanY / 6));
			graphics.fill(cx, cy - halfSpanY, cx + 1, cy - halfSpanY + tipLen, CROSSHAIR_TIP_COLOR);
			graphics.fill(cx, cy + halfSpanY + 1 - tipLen, cx + 1, cy + halfSpanY + 1, CROSSHAIR_TIP_COLOR);

			graphics.fill(RenderPipelines.GUI_INVERT, cx - maxReachX, cy, cx - CENTER_GAP_HALF, cy + 1, CROSSHAIR_INVERT_COLOR);
			graphics.fill(RenderPipelines.GUI_INVERT, cx + CENTER_GAP_HALF + 1, cy, cx + maxReachX + 1, cy + 1, CROSSHAIR_INVERT_COLOR);

			int stepCount = (int) Math.round(RULER_MAX_DEG / RULER_STEP_DEG);
			for (int i = 1; i <= stepCount; i++) {
				int dx = (int) Math.round(Math.tan(Math.toRadians(i * RULER_STEP_DEG)) * pxPerRad);
				if (dx <= CENTER_GAP_HALF + 2 || dx > maxReachX) {
					continue;
				}
				int halfTick = (i % 2 == 0) ? 6 : 3;
				graphics.fill(RenderPipelines.GUI_INVERT, cx - dx, cy - halfTick, cx - dx + 1, cy + halfTick + 1, CROSSHAIR_INVERT_COLOR);
				graphics.fill(RenderPipelines.GUI_INVERT, cx + dx, cy - halfTick, cx + dx + 1, cy + halfTick + 1, CROSSHAIR_INVERT_COLOR);
			}
		} finally {
			graphics.pose().popMatrix();
		}
	}

	public static boolean onChatMouseClicked(double mouseX, double mouseY, int button) {
		double safeScale = lastScale > 0.0F ? lastScale : 1.0F;
		double scaledMouseX = mouseX / safeScale;
		double scaledMouseY = mouseY / safeScale;
		if (!visible || !isInsideBox(scaledMouseX, scaledMouseY)) {
			return false;
		}

		if (button == InputConstants.MOUSE_BUTTON_LEFT) {
			dragging = true;
			dragOffsetX = scaledMouseX - lastX;
			dragOffsetY = scaledMouseY - lastY;
			return true;
		}

		if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
			dragging = false;
			ToolXConfig config = ToolXConfig.get();
			config.eyeMeasurementHudX = 0.0;
			config.eyeMeasurementHudY = 0.5;
			config.save();
			return true;
		}

		return false;
	}

	public static void onChatRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		if (!visible) {
			dragging = false;
			return;
		}

		double safeScale = lastScale > 0.0F ? lastScale : 1.0F;
		double scaledMouseX = mouseX / safeScale;
		double scaledMouseY = mouseY / safeScale;

		if (dragging) {
			int minX = MARGIN;
			int minY = MARGIN;
			int maxX = Math.max(minX, lastGuiWidth - lastWidth - MARGIN);
			int maxY = Math.max(minY, lastGuiHeight - lastHeight - MARGIN);

			double clampedX = snapAxis(Math.clamp(scaledMouseX - dragOffsetX, minX, maxX), minX, maxX);
			double clampedY = snapAxis(Math.clamp(scaledMouseY - dragOffsetY, minY, maxY), minY, maxY);

			ToolXConfig config = ToolXConfig.get();
			config.eyeMeasurementHudX = maxX > minX ? (clampedX - minX) / (maxX - minX) : 0.0;
			config.eyeMeasurementHudY = maxY > minY ? (clampedY - minY) / (maxY - minY) : 0.5;
			lastX = (int) Math.round(clampedX);
			lastY = (int) Math.round(clampedY);
		}

		if (dragging || isInsideBox(scaledMouseX, scaledMouseY)) {
			graphics.pose().pushMatrix();
			try {
				if (lastScale != 1.0F) {
					graphics.pose().scale(lastScale, lastScale);
				}
				graphics.outline(lastX, lastY, lastWidth, lastHeight, HOVER_BORDER_COLOR);
			} finally {
				graphics.pose().popMatrix();
			}
		}
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

	public static void cancelDrag() {
		dragging = false;
	}

	private static boolean isInsideBox(double mouseX, double mouseY) {
		return mouseX >= lastX
			&& mouseX <= lastX + lastWidth
			&& mouseY >= lastY
			&& mouseY <= lastY + lastHeight;
	}

	private static Component colored(String text, int rgb) {
		return Component.literal(text).withStyle(style -> style.withColor(rgb));
	}
}
