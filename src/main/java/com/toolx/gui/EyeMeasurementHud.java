package com.toolx.gui;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.toolx.EyeMeasurementController;
import com.toolx.EyeMeasurementController.Result;
import com.toolx.ToolXConfig;
import com.toolx.gui.ThinModeHud.DebugSpace;
import com.toolx.mixin.GuiGraphicsExtractorInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
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
	private static final int EYE_ZOOM_COLS = 30;
	private static final float EYE_ZOOM_CROP_STRIP_FRAC = 0.80F;
	private static final float EYE_ZOOM_CROP_HEIGHT_FRAC = 0.40F;

	private static final int MARGIN = 4;
	private static final int PADDING_X = 3;
	private static final int PADDING_Y = 3;
	private static final int ICON_SIZE = 16;
	private static final int SPEED_ICON_SIZE = 18;
	private static final int ICON_TEXT_GAP = 3;
	private static final int TEXT_HEIGHT = 9;
	private static final int COL_GAP = 10;
	private static final double SNAP_THRESHOLD = 6.0;

	private static final int IDLE_CHAT_BORDER_COLOR = 0x66FFFFFF;
	private static final int HOVER_BORDER_COLOR = 0xEEFFFFFF;
	private static final int TEXT_BASE_COLOR = 0xFFFFFFFF;
	private static final int CROSSHAIR_INVERT_COLOR = 0xFFFFFFFF;
	private static final int ZOOM_RULER_BG = 0x90000000;
	private static final int ZOOM_RULER_TICK_COLOR = 0x55FFFFFF;
	private static final int ZOOM_CENTER_LINE = 0xFFFFFFFF;

	private static final int COLOR_GREEN = 0x1FFF1F;
	private static final int COLOR_RED = 0xFF0B0B;
	private static final int COLOR_YELLOW = 0xFFDE21;

	private static final int CENTER_CROSSHAIR_HALF = 4;
	private static final Identifier ENDER_EYE_TEXTURE = Identifier.withDefaultNamespace("textures/item/ender_eye.png");

	private enum DragTarget {
		NONE,
		HUD,
		EYE_ZOOM
	}

	private static @Nullable ItemStack grassIcon;
	private static @Nullable ItemStack netherrackIcon;
	private static @Nullable Identifier speedSprite;

	private static @Nullable GpuTexture eyeZoomTexture;
	private static @Nullable GpuTextureView eyeZoomTextureView;
	private static @Nullable GpuFormat eyeZoomFormat;
	private static int eyeZoomTexW;
	private static int eyeZoomTexH;

	private static boolean visible;
	private static int lastX;
	private static int lastY;
	private static int lastWidth;
	private static int lastHeight;

	private static boolean zoomVisible;
	private static int zoomX;
	private static int zoomY;
	private static int zoomW;
	private static int zoomH;

	private static int lastGuiWidth;
	private static int lastGuiHeight;
	private static int lastStripLeft;
	private static int lastStripRight;
	private static float lastScale = 1.0F;

	private static DragTarget dragTarget = DragTarget.NONE;
	private static double dragOffsetX;
	private static double dragOffsetY;

	private EyeMeasurementHud() {
	}

	public static boolean isDragging() {
		return dragTarget != DragTarget.NONE;
	}

	public static boolean isHovered(double mouseX, double mouseY) {
		if (!visible && !zoomVisible) {
			return false;
		}
		double safeScale = lastScale > 0.0F ? lastScale : 1.0F;
		double scaledX = mouseX / safeScale;
		double scaledY = mouseY / safeScale;
		return (zoomVisible && isInside(scaledX, scaledY, zoomX, zoomY, zoomW, zoomH))
			|| (visible && isInside(scaledX, scaledY, lastX, lastY, lastWidth, lastHeight));
	}

	private static void ensureIcons() {
		if (grassIcon == null) {
			grassIcon = new ItemStack(Items.GRASS_BLOCK);
			netherrackIcon = new ItemStack(Items.NETHERRACK);
			speedSprite = Hud.getMobEffectSprite(MobEffects.SPEED);
		}
	}

	public static boolean shouldRenderMeasureLayout(@Nullable Minecraft minecraft) {
		if (!EyeMeasurementController.isMeasuring()
			|| minecraft == null
			|| minecraft.level == null
			|| minecraft.player == null
			|| minecraft.getWindow() == null
			|| minecraft.gui == null
			|| !Level.OVERWORLD.equals(minecraft.level.dimension())) {
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
		return physW > 0 && physH > 0 && Math.round(physH * ThinModeHud.THIN_ASPECT) < physW;
	}

	public static void captureEyeZoomFrame(@Nullable Minecraft minecraft) {
		if (!EyeMeasurementController.isMeasuring()) {
			if (eyeZoomTexture != null || eyeZoomTextureView != null) {
				closeZoomResources();
			}
			return;
		}
		if (!shouldRenderMeasureLayout(minecraft)) {
			return;
		}
		RenderTarget mainTarget = minecraft.gameRenderer != null ? minecraft.gameRenderer.mainRenderTarget() : null;
		GpuTexture src = mainTarget != null ? mainTarget.getColorTexture() : null;
		if (src == null || src.isClosed()) {
			return;
		}
		int srcW = src.getWidth(0);
		int srcH = src.getHeight(0);
		if (srcW <= 0 || srcH <= 0) {
			return;
		}

		GpuFormat format = src.getFormat();
		if (eyeZoomTexture == null
			|| eyeZoomTexture.isClosed()
			|| eyeZoomTextureView == null
			|| !format.equals(eyeZoomFormat)
			|| eyeZoomTexW != srcW
			|| eyeZoomTexH != srcH) {
			closeZoomResources();
			GpuDevice device = RenderSystem.getDevice();
			eyeZoomTexture = device.createTexture(
				"toolx_eye_zoom",
				GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
				format,
				srcW,
				srcH,
				1,
				1
			);
			eyeZoomTextureView = device.createTextureView(eyeZoomTexture);
			eyeZoomFormat = format;
			eyeZoomTexW = srcW;
			eyeZoomTexH = srcH;
		}

		// Copy full framebuffer with zero offsets to avoid GlCommandEncoder's srcX1/srcY1 blit bug, then crop center via UVs.
		CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
		encoder.copyTextureToTexture(src, eyeZoomTexture, 0, 0, 0, 0, 0, srcW, srcH);
	}

	public static void closeZoomResources() {
		if (eyeZoomTextureView != null) {
			eyeZoomTextureView.close();
			eyeZoomTextureView = null;
		}
		if (eyeZoomTexture != null) {
			eyeZoomTexture.close();
			eyeZoomTexture = null;
		}
		eyeZoomFormat = null;
		eyeZoomTexW = 0;
		eyeZoomTexH = 0;
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
			zoomVisible = false;
			dragTarget = DragTarget.NONE;
			return;
		}

		if ((minecraft.gui.hud.isHidden() && minecraft.gui.screen() == null)
			|| TimerHud.isOptionsScreen(minecraft.gui.screen())) {
			visible = false;
			zoomVisible = false;
			dragTarget = DragTarget.NONE;
			return;
		}

		ResourceKey<Level> dimension = minecraft.level.dimension();
		boolean inOverworld = Level.OVERWORLD.equals(dimension);
		boolean inNether = Level.NETHER.equals(dimension);
		if (!inOverworld && !inNether) {
			visible = false;
			zoomVisible = false;
			dragTarget = DragTarget.NONE;
			return;
		}

		DebugSpace space = ThinModeHud.computeDebugSpace(minecraft, graphics);
		if (space == null) {
			visible = false;
			zoomVisible = false;
			dragTarget = DragTarget.NONE;
			return;
		}
		Window window = minecraft.getWindow();
		float scale = space.scale();
		boolean scaled = scale < 1.0F;
		int guiWidth = space.debugWidth();
		int guiHeight = space.debugHeight();
		int stripLeft = space.stripLeft();
		int stripRight = space.stripRight();

		lastGuiWidth = guiWidth;
		lastGuiHeight = guiHeight;
		lastStripLeft = stripLeft;
		lastStripRight = stripRight;
		lastScale = scale;

		if (shouldRenderMeasureLayout(minecraft)) {
			extractMeasureCrosshair(window, graphics);
			extractEyeZoomBox(minecraft, graphics, guiWidth, guiHeight, stripLeft, scale, scaled);
		} else {
			zoomVisible = false;
			if (dragTarget == DragTarget.EYE_ZOOM) {
				dragTarget = DragTarget.NONE;
			}
		}

		Result result = EyeMeasurementController.lastResult();
		if (result == null) {
			visible = false;
			if (dragTarget == DragTarget.HUD) {
				dragTarget = DragTarget.NONE;
			}
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

	private static void extractEyeZoomBox(
		Minecraft minecraft,
		GuiGraphicsExtractor graphics,
		int guiWidth,
		int guiHeight,
		int stripLeft,
		float scale,
		boolean scaled
	) {
		if (eyeZoomTextureView == null
			|| eyeZoomTexture == null
			|| eyeZoomTexture.isClosed()
			|| eyeZoomTexW <= 0
			|| eyeZoomTexH <= 0) {
			zoomVisible = false;
			if (dragTarget == DragTarget.EYE_ZOOM) {
				dragTarget = DragTarget.NONE;
			}
			return;
		}

		int availLeft = Math.max(0, stripLeft - MARGIN);
		int colW = Math.max(6, Math.min(20, availLeft / EYE_ZOOM_COLS));
		int boxW = colW * EYE_ZOOM_COLS;
		int boxH = Math.max(40, Math.round(boxW * 0.42F));

		int minX = 0;
		int minY = 0;
		int maxX = Math.max(minX, guiWidth - boxW);
		int maxY = Math.max(minY, guiHeight - boxH);

		ToolXConfig config = ToolXConfig.get();
		int boxX = config.eyeZoomBoxX < 0.0
			? minX
			: (int) Math.round(minX + Math.clamp(config.eyeZoomBoxX, 0.0, 1.0) * (maxX - minX));
		int boxY = config.eyeZoomBoxY < 0.0
			? minY
			: (int) Math.round(minY + Math.clamp(config.eyeZoomBoxY, 0.0, 1.0) * (maxY - minY));

		zoomVisible = true;
		zoomX = boxX;
		zoomY = boxY;
		zoomW = boxW;
		zoomH = boxH;

		float cropW = Math.min((float) eyeZoomTexW, Math.round(eyeZoomTexH * ThinModeHud.THIN_ASPECT * EYE_ZOOM_CROP_STRIP_FRAC));
		float cropH = Math.min((float) eyeZoomTexH, Math.round(eyeZoomTexH * EYE_ZOOM_CROP_HEIGHT_FRAC));
		float u0 = ((float) eyeZoomTexW - cropW) * 0.5F / (float) eyeZoomTexW;
		float u1 = ((float) eyeZoomTexW + cropW) * 0.5F / (float) eyeZoomTexW;
		float vTop = 0.5F + (cropH * 0.5F) / (float) eyeZoomTexH;
		float vBottom = 0.5F - (cropH * 0.5F) / (float) eyeZoomTexH;

		GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
		int midY = boxY + boxH / 2;
		int centerLineX = boxX + (EYE_ZOOM_COLS / 2) * colW;
		float slideX = StripTransition.horizontalSlideOffset(boxX, boxW, guiWidth);

		graphics.pose().pushMatrix();
		try {
			if (scaled) {
				graphics.pose().scale(scale, scale);
			}
			if (slideX != 0.0F) {
				graphics.pose().translate(slideX, 0.0F);
			}

			((GuiGraphicsExtractorInvoker) graphics).toolx$innerBlit(
				RenderPipelines.GUI_OPAQUE_TEXTURED_BACKGROUND,
				eyeZoomTextureView,
				sampler,
				boxX,
				boxY,
				boxX + boxW,
				boxY + boxH,
				u0,
				u1,
				vTop,
				vBottom,
				0xFFFFFFFF
			);

			Font font = minecraft.font;
			int twoDigitWidth = Math.max(1, font.width("15") - 1);
			float textScale = Math.min(0.85F, Math.max(0.30F, (colW - 3.5F) / (float) twoDigitWidth));
			int bandHalf = Math.max(4, Math.round(4.5F * textScale) + 2);

			graphics.fill(boxX, midY - bandHalf, boxX + boxW, midY + bandHalf, ZOOM_RULER_BG);
			for (int i = 1; i < EYE_ZOOM_COLS; i++) {
				if (i == EYE_ZOOM_COLS / 2) {
					continue;
				}
				int tickX = boxX + i * colW;
				graphics.fill(tickX, midY + bandHalf - 2, tickX + 1, midY + bandHalf, ZOOM_RULER_TICK_COLOR);
			}
			graphics.fill(centerLineX, boxY, centerLineX + 1, boxY + boxH, ZOOM_CENTER_LINE);

			int halfCols = EYE_ZOOM_COLS / 2;
			for (int i = 0; i < EYE_ZOOM_COLS; i++) {
				int number = i < halfCols ? (halfCols - i) : (i - halfCols + 1);
				String label = Integer.toString(number);
				int visualW = Math.max(1, font.width(label) - 1);
				float colCenterX = boxX + (i + 0.5F) * colW;
				float drawX = colCenterX - (visualW * textScale) * 0.5F;
				float drawY = midY - (7.0F * textScale) * 0.5F;

				graphics.pose().pushMatrix();
				graphics.pose().translate(drawX, drawY);
				if (textScale != 1.0F) {
					graphics.pose().scale(textScale, textScale);
				}
				graphics.text(font, label, 0, 0, TEXT_BASE_COLOR);
				graphics.pose().popMatrix();
			}
		} finally {
			graphics.pose().popMatrix();
		}
	}

	private static void extractMeasureCrosshair(Window window, GuiGraphicsExtractor graphics) {
		int guiScale = Math.max(1, window.getGuiScale());
		float invScale = 1.0F / (float) guiScale;
		int cx = window.getWidth() / 2;
		int cy = window.getHeight() / 2;

		graphics.pose().pushMatrix();
		try {
			if (guiScale != 1) {
				graphics.pose().scale(invScale, invScale);
			}
			graphics.fill(RenderPipelines.GUI_INVERT, cx, cy - CENTER_CROSSHAIR_HALF, cx + 1, cy + CENTER_CROSSHAIR_HALF + 1, CROSSHAIR_INVERT_COLOR);
			graphics.fill(RenderPipelines.GUI_INVERT, cx - CENTER_CROSSHAIR_HALF, cy, cx + CENTER_CROSSHAIR_HALF + 1, cy + 1, CROSSHAIR_INVERT_COLOR);
		} finally {
			graphics.pose().popMatrix();
		}
	}

	public static boolean onChatMouseClicked(double mouseX, double mouseY, int button) {
		double safeScale = lastScale > 0.0F ? lastScale : 1.0F;
		double scaledMouseX = mouseX / safeScale;
		double scaledMouseY = mouseY / safeScale;

		DragTarget hit = DragTarget.NONE;
		if (zoomVisible && isInside(scaledMouseX, scaledMouseY, zoomX, zoomY, zoomW, zoomH)) {
			hit = DragTarget.EYE_ZOOM;
		} else if (visible && isInside(scaledMouseX, scaledMouseY, lastX, lastY, lastWidth, lastHeight)) {
			hit = DragTarget.HUD;
		}
		if (hit == DragTarget.NONE) {
			return false;
		}

		if (button == InputConstants.MOUSE_BUTTON_LEFT) {
			dragTarget = hit;
			dragOffsetX = scaledMouseX - (hit == DragTarget.EYE_ZOOM ? zoomX : lastX);
			dragOffsetY = scaledMouseY - (hit == DragTarget.EYE_ZOOM ? zoomY : lastY);
			return true;
		}

		if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
			dragTarget = DragTarget.NONE;
			ToolXConfig config = ToolXConfig.get();
			if (hit == DragTarget.EYE_ZOOM) {
				config.eyeZoomBoxX = -1.0;
				config.eyeZoomBoxY = -1.0;
			} else {
				config.eyeMeasurementHudX = 0.0;
				config.eyeMeasurementHudY = 0.5;
			}
			config.save();
			return true;
		}

		return false;
	}

	public static void onChatRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		if (!visible && !zoomVisible) {
			dragTarget = DragTarget.NONE;
			return;
		}

		double safeScale = lastScale > 0.0F ? lastScale : 1.0F;
		double scaledMouseX = mouseX / safeScale;
		double scaledMouseY = mouseY / safeScale;

		if (dragTarget == DragTarget.EYE_ZOOM && zoomVisible) {
			applyDrag(scaledMouseX, scaledMouseY, zoomW, zoomH, true);
		} else if (dragTarget == DragTarget.HUD && visible) {
			applyDrag(scaledMouseX, scaledMouseY, lastWidth, lastHeight, false);
		}

		graphics.pose().pushMatrix();
		try {
			if (lastScale != 1.0F) {
				graphics.pose().scale(lastScale, lastScale);
			}
			boolean blockedByHigher = ThinModeHud.isHovered(mouseX, mouseY);
			boolean zoomActive = false;
			if (zoomVisible) {
				zoomActive = dragTarget == DragTarget.EYE_ZOOM
					|| (dragTarget == DragTarget.NONE
						&& !blockedByHigher
						&& !ThinModeHud.isDragging()
						&& !TimerHud.isDragging()
						&& !ResourceTrackerHud.isDragging()
						&& isInside(scaledMouseX, scaledMouseY, zoomX, zoomY, zoomW, zoomH));
				graphics.outline(zoomX, zoomY, zoomW, zoomH, zoomActive ? HOVER_BORDER_COLOR : IDLE_CHAT_BORDER_COLOR);
			}
			if (visible) {
				boolean hudActive = dragTarget == DragTarget.HUD
					|| (dragTarget == DragTarget.NONE
						&& !blockedByHigher
						&& !zoomActive
						&& !ThinModeHud.isDragging()
						&& !TimerHud.isDragging()
						&& !ResourceTrackerHud.isDragging()
						&& isInside(scaledMouseX, scaledMouseY, lastX, lastY, lastWidth, lastHeight));
				graphics.outline(lastX, lastY, lastWidth, lastHeight, hudActive ? HOVER_BORDER_COLOR : IDLE_CHAT_BORDER_COLOR);
			}
		} finally {
			graphics.pose().popMatrix();
		}
	}

	private static void applyDrag(double scaledMouseX, double scaledMouseY, int boxW, int boxH, boolean isZoomBox) {
		int minX = isZoomBox ? 0 : MARGIN;
		int minY = isZoomBox ? 0 : MARGIN;
		int maxX = Math.max(minX, lastGuiWidth - boxW - (isZoomBox ? 0 : MARGIN));
		int maxY = Math.max(minY, lastGuiHeight - boxH - (isZoomBox ? 0 : MARGIN));

		double clampedX = snapX(Math.clamp(scaledMouseX - dragOffsetX, minX, maxX), minX, maxX, boxW);
		double clampedY = snapAxis(Math.clamp(scaledMouseY - dragOffsetY, minY, maxY), minY, maxY);

		double normX = maxX > minX ? (clampedX - minX) / (maxX - minX) : 0.0;
		double normY = maxY > minY ? (clampedY - minY) / (maxY - minY) : (isZoomBox ? 0.0 : 0.5);

		ToolXConfig config = ToolXConfig.get();
		if (isZoomBox) {
			config.eyeZoomBoxX = normX;
			config.eyeZoomBoxY = normY;
			zoomX = (int) Math.round(clampedX);
			zoomY = (int) Math.round(clampedY);
		} else {
			config.eyeMeasurementHudX = normX;
			config.eyeMeasurementHudY = normY;
			lastX = (int) Math.round(clampedX);
			lastY = (int) Math.round(clampedY);
		}
	}

	private static double snapX(double value, int min, int max, int boxW) {
		if (max <= min) {
			return min;
		}
		double[] anchors = {
			min,
			(min + max) * 0.5,
			max,
			lastStripLeft - boxW,
			lastStripRight
		};
		for (double anchor : anchors) {
			double clampedAnchor = Math.clamp(anchor, min, max);
			if (Math.abs(value - clampedAnchor) <= SNAP_THRESHOLD) {
				return clampedAnchor;
			}
		}
		return value;
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
		if (dragTarget != DragTarget.NONE) {
			dragTarget = DragTarget.NONE;
			ToolXConfig.get().save();
		}
	}

	public static void cancelDrag() {
		dragTarget = DragTarget.NONE;
	}

	private static boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
		return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
	}

	private static Component colored(String text, int rgb) {
		return Component.literal(text).withStyle(style -> style.withColor(rgb));
	}
}
