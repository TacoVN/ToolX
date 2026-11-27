package com.toolx.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.toolx.ToolXConfig;
import com.toolx.gui.ThinModeHud.DebugSpace;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import org.jspecify.annotations.Nullable;

public final class ResourceTrackerHud {
	private static final int MARGIN = 4;
	private static final int PADDING_X = 3;
	private static final int PADDING_Y = 3;
	private static final int ICON_SIZE = 16;
	private static final int EFFECT_ICON_SIZE = 18;
	private static final int ICON_TEXT_GAP = 3;
	private static final int TEXT_HEIGHT = 9;
	private static final int MIN_COL_WIDTH = 24;
	private static final int COL_GAP = 4;
	private static final int MAX_CONTAINER_DEPTH = 4;
	private static final double SNAP_THRESHOLD = 6.0;

	private static final float INVENTORY_HALF_HEIGHT_GUI = 83.0F;
	private static final float SELECTED_ITEM_OFFSET_GUI = 59.0F;

	private static final int TEXT_ACTIVE_COLOR = 0xFFFFFFFF;
	private static final int TEXT_ZERO_COLOR = 0xFFAAAAAA;
	private static final int IDLE_CHAT_BORDER_COLOR = 0x66FFFFFF;
	private static final int ACTIVE_BORDER_COLOR = 0xEEFFFFFF;

	private static final Identifier ENDER_PEARL_TEXTURE = Identifier.withDefaultNamespace("textures/item/ender_pearl.png");
	private static final Identifier BLAZE_ROD_TEXTURE = Identifier.withDefaultNamespace("textures/item/blaze_rod.png");
	private static final Identifier ENDER_EYE_TEXTURE = Identifier.withDefaultNamespace("textures/item/ender_eye.png");

	private static @Nullable ItemStack obsidianIcon;
	private static @Nullable ItemStack bedIcon;
	private static @Nullable Identifier fireResSprite;

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

	private ResourceTrackerHud() {
	}

	private static final class Counts {
		long obsidian;
		long fireResistance;
		long enderPearls;
		long blazeRods;
		long enderEyes;
		long explosives;
		long respawnAnchors;
		long glowstoneBlocks;
	}

	public static boolean isDragging() {
		return dragging;
	}

	public static void resetState() {
		visible = false;
		dragging = false;
	}

	public static boolean shouldRender(@Nullable Minecraft minecraft) {
		if (!ToolXConfig.get().resourceTracker
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
		Screen screen = minecraft.gui.screen();
		if (TimerHud.isOptionsScreen(screen) || screen instanceof LevelLoadingScreen) {
			return false;
		}
		return !(minecraft.gui.hud != null && minecraft.gui.hud.isHidden() && screen == null);
	}

	public static void extractRenderState(@Nullable Minecraft minecraft, GuiGraphicsExtractor graphics) {
		if (!shouldRender(minecraft)) {
			visible = false;
			dragging = false;
			return;
		}

		DebugSpace space = ThinModeHud.computeDebugSpace(minecraft, graphics);
		if (space == null) {
			visible = false;
			dragging = false;
			return;
		}

		ensureIcons();

		Counts counts = scanPlayerResources(minecraft.player);
		int[] values = {
			clampCount(counts.obsidian),
			clampCount(counts.fireResistance),
			clampCount(counts.enderPearls),
			clampCount(counts.blazeRods),
			clampCount(counts.enderEyes),
			clampCount(counts.explosives + Math.min(counts.respawnAnchors, counts.glowstoneBlocks))
		};

		Font font = minecraft.font;
		String[] labels = new String[values.length];
		int[] textWidths = new int[values.length];
		int[] colWidths = new int[values.length];
		int totalColsWidth = 0;

		for (int i = 0; i < values.length; i++) {
			labels[i] = String.valueOf(values[i]);
			textWidths[i] = font.width(labels[i]);
			colWidths[i] = Math.max(MIN_COL_WIDTH, textWidths[i]);
			totalColsWidth += colWidths[i];
		}

		int boxW = PADDING_X * 2 + totalColsWidth + COL_GAP * (values.length - 1);
		int boxH = PADDING_Y * 2 + ICON_SIZE + ICON_TEXT_GAP + TEXT_HEIGHT;

		int minX = MARGIN;
		int minY = MARGIN;
		int maxX = Math.max(minX, space.debugWidth() - boxW - MARGIN);
		int maxY = Math.max(minY, space.debugHeight() - boxH - MARGIN);

		ToolXConfig config = ToolXConfig.get();
		int boxX = config.resourceTrackerHudX < 0.0
			? Math.clamp(Math.round((minX + maxX) * 0.5F), minX, maxX)
			: (int) Math.round(minX + Math.clamp(config.resourceTrackerHudX, 0.0, 1.0) * (maxX - minX));
		int boxY = config.resourceTrackerHudY < 0.0
			? computeDefaultY(space, graphics.guiHeight(), boxH, minY, maxY)
			: (int) Math.round(minY + Math.clamp(config.resourceTrackerHudY, 0.0, 1.0) * (maxY - minY));

		visible = true;
		lastX = boxX;
		lastY = boxY;
		lastW = boxW;
		lastH = boxH;
		lastGuiW = space.debugWidth();
		lastGuiH = space.debugHeight();
		lastScale = space.scale();

		graphics.pose().pushMatrix();
		try {
			if (space.scale() != 1.0F) {
				graphics.pose().scale(space.scale(), space.scale());
			}

			int iconY = boxY + PADDING_Y;
			int textY = iconY + ICON_SIZE + ICON_TEXT_GAP;
			int colX = boxX + PADDING_X;

			for (int i = 0; i < values.length; i++) {
				if (i == 1) {
					int spriteX = colX + (colWidths[i] - EFFECT_ICON_SIZE) / 2;
					graphics.blitSprite(
						RenderPipelines.GUI_TEXTURED,
						fireResSprite,
						spriteX,
						iconY - 1,
						EFFECT_ICON_SIZE,
						EFFECT_ICON_SIZE
					);
				} else {
					int iconX = colX + (colWidths[i] - ICON_SIZE) / 2;
					switch (i) {
						case 0 -> graphics.fakeItem(obsidianIcon, iconX, iconY);
						case 2 -> graphics.blit(RenderPipelines.GUI_TEXTURED, ENDER_PEARL_TEXTURE, iconX, iconY, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
						case 3 -> graphics.blit(RenderPipelines.GUI_TEXTURED, BLAZE_ROD_TEXTURE, iconX, iconY, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
						case 4 -> graphics.blit(RenderPipelines.GUI_TEXTURED, ENDER_EYE_TEXTURE, iconX, iconY, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
						case 5 -> graphics.fakeItem(bedIcon, iconX, iconY);
					}
				}

				int textX = colX + (colWidths[i] - textWidths[i]) / 2;
				int textColor = values[i] > 0 ? TEXT_ACTIVE_COLOR : TEXT_ZERO_COLOR;
				graphics.text(font, labels[i], textX, textY, textColor);
				colX += colWidths[i] + COL_GAP;
			}
		} finally {
			graphics.pose().popMatrix();
		}
	}

	private static int computeDefaultY(DebugSpace space, int normalGuiH, int boxH, int minY, int maxY) {
		float safeScale = space.scale() > 0.0F ? space.scale() : 1.0F;
		float halfBoxGui = boxH * safeScale * 0.5F;
		float invBottomGui = normalGuiH * 0.5F + INVENTORY_HALF_HEIGHT_GUI;
		float selectedItemTopGui = normalGuiH - SELECTED_ITEM_OFFSET_GUI;
		float centerGuiY = Math.min((invBottomGui + selectedItemTopGui) * 0.5F, selectedItemTopGui - 2.0F - halfBoxGui);
		int boxY = Math.round(centerGuiY / safeScale - boxH * 0.5F);
		return Math.clamp(boxY, minY, maxY);
	}

	private static void ensureIcons() {
		if (obsidianIcon == null) {
			obsidianIcon = new ItemStack(Items.OBSIDIAN);
			bedIcon = new ItemStack(Items.BED.white());
			fireResSprite = Hud.getMobEffectSprite(MobEffects.FIRE_RESISTANCE);
		}
	}

	private static Counts scanPlayerResources(LocalPlayer player) {
		Counts counts = new Counts();
		Inventory inventory = player.getInventory();
		int size = inventory.getContainerSize();
		for (int slot = 0; slot < size; slot++) {
			accumulate(inventory.getItem(slot), counts, 1L, 0);
		}
		if (player.containerMenu != null) {
			accumulate(player.containerMenu.getCarried(), counts, 1L, 0);
			for (Slot slot : player.containerMenu.slots) {
				if (slot.container instanceof CraftingContainer) {
					accumulate(slot.getItem(), counts, 1L, 0);
				}
			}
		}
		return counts;
	}

	private static void accumulate(@Nullable ItemInstance item, Counts counts, long multiplier, int depth) {
		if (item == null || item.count() <= 0 || multiplier <= 0L) {
			return;
		}

		long stackCount = (long) item.count() * multiplier;

		if (item.is(Items.OBSIDIAN)) {
			counts.obsidian += stackCount;
		} else if (item.is(Items.ENCHANTED_GOLDEN_APPLE)) {
			counts.fireResistance += stackCount;
		} else if ((item.is(Items.POTION) || item.is(Items.SPLASH_POTION)) && hasFireResistanceEffect(item)) {
			counts.fireResistance += stackCount;
		} else if (item.is(Items.ENDER_PEARL)) {
			counts.enderPearls += stackCount;
		} else if (item.is(Items.BLAZE_ROD)) {
			counts.blazeRods += stackCount;
		} else if (item.is(Items.ENDER_EYE)) {
			counts.enderEyes += stackCount;
		} else if (item.is(ItemTags.BEDS) || item.is(Items.TNT)) {
			counts.explosives += stackCount;
		} else if (item.is(Items.RESPAWN_ANCHOR)) {
			counts.respawnAnchors += stackCount;
		} else if (item.is(Items.GLOWSTONE)) {
			counts.glowstoneBlocks += stackCount;
		}

		if (depth >= MAX_CONTAINER_DEPTH) {
			return;
		}

		BundleContents bundle = item.get(DataComponents.BUNDLE_CONTENTS);
		if (bundle != null) {
			for (ItemStackTemplate child : bundle.items()) {
				accumulate(child, counts, stackCount, depth + 1);
			}
		}

		ItemContainerContents container = item.get(DataComponents.CONTAINER);
		if (container != null) {
			for (ItemStackTemplate child : container.nonEmptyItems()) {
				accumulate(child, counts, stackCount, depth + 1);
			}
		}
	}

	private static boolean hasFireResistanceEffect(ItemInstance item) {
		PotionContents contents = item.get(DataComponents.POTION_CONTENTS);
		if (contents == null) {
			return false;
		}
		for (MobEffectInstance effect : contents.getAllEffects()) {
			if (effect != null && MobEffects.FIRE_RESISTANCE.equals(effect.getEffect())) {
				return true;
			}
		}
		return false;
	}

	private static int clampCount(long value) {
		if (value <= 0L) {
			return 0;
		}
		return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
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
			config.resourceTrackerHudX = -1.0;
			config.resourceTrackerHudY = -1.0;
			config.save();
			return true;
		}
		return false;
	}

	public static void onChatRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		if (!visible || !shouldRender(Minecraft.getInstance())) {
			dragging = false;
			return;
		}
		double safeScale = lastScale > 0.0F ? lastScale : 1.0F;
		double scaledMouseX = mouseX / safeScale;
		double scaledMouseY = mouseY / safeScale;
		if (dragging) {
			int minX = MARGIN;
			int minY = MARGIN;
			int maxX = Math.max(minX, lastGuiW - lastW - MARGIN);
			int maxY = Math.max(minY, lastGuiH - lastH - MARGIN);

			double clampedX = snapAxis(Math.clamp(scaledMouseX - dragOffsetX, minX, maxX), minX, maxX);
			double clampedY = snapAxis(Math.clamp(scaledMouseY - dragOffsetY, minY, maxY), minY, maxY);

			ToolXConfig config = ToolXConfig.get();
			config.resourceTrackerHudX = maxX > minX ? (clampedX - minX) / (maxX - minX) : 0.5;
			config.resourceTrackerHudY = maxY > minY ? (clampedY - minY) / (maxY - minY) : 0.5;
			lastX = (int) Math.round(clampedX);
			lastY = (int) Math.round(clampedY);
		}

		graphics.pose().pushMatrix();
		try {
			if (lastScale != 1.0F) {
				graphics.pose().scale(lastScale, lastScale);
			}
			boolean active = dragging
				|| (!ThinModeHud.isDragging()
					&& !EyeMeasurementHud.isDragging()
					&& !TimerHud.isDragging()
					&& !ThinModeHud.isHovered(mouseX, mouseY)
					&& !EyeMeasurementHud.isHovered(mouseX, mouseY)
					&& !TimerHud.isHovered(mouseX, mouseY)
					&& isInside(scaledMouseX, scaledMouseY, lastX, lastY, lastW, lastH));
			graphics.outline(lastX, lastY, lastW, lastH, active ? ACTIVE_BORDER_COLOR : IDLE_CHAT_BORDER_COLOR);
		} finally {
			graphics.pose().popMatrix();
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

	private static boolean isInside(double px, double py, int x, int y, int width, int height) {
		return px >= x && px <= x + width && py >= y && py <= y + height;
	}
}
