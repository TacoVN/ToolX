package com.toolx.gui;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class ToolXOptionsList extends OptionsList {
	private static final int HEADER_HEIGHT = 14;
	private static final int HEADER_TEXT_OFFSET = -4;
	private static final int FIRST_HEADER_TEXT_OFFSET = -3;
	private final OptionsSubScreen screen;
	private boolean firstHeader = true;

	public ToolXOptionsList(Minecraft minecraft, int width, OptionsSubScreen screen) {
		super(minecraft, width, screen);
		this.screen = screen;
	}

	@Override
	public void addHeader(Component text) {
		boolean first = this.firstHeader;
		this.addEntry(new CompactHeaderEntry(text, first), HEADER_HEIGHT);
		this.firstHeader = false;
	}

	public void addKeyBind(
		Component label,
		Supplier<InputConstants.Key> getter,
		BooleanSupplier isSelected,
		Runnable onSelect,
		Runnable onReset
	) {
		KeyBindEntry entry = new KeyBindEntry(label, getter, isSelected, onSelect, onReset);
		this.addEntry(entry);
	}

	public void refreshKeyBinds() {
		for (OptionsList.AbstractEntry child : this.children()) {
			if (child instanceof KeyBindEntry entry) {
				entry.refreshEntry();
			}
		}
	}

	private final class CompactHeaderEntry extends OptionsList.AbstractEntry {
		private final FocusableTextWidget widget;
		private final boolean first;

		private CompactHeaderEntry(Component text, boolean first) {
			this.first = first;
			this.widget = FocusableTextWidget.builder(text, ToolXOptionsList.this.screen.getFont())
				.alwaysShowBorder(false)
				.backgroundFill(FocusableTextWidget.BackgroundFill.ON_FOCUS)
				.build();
		}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
			int offset = this.first ? FIRST_HEADER_TEXT_OFFSET : HEADER_TEXT_OFFSET;
			this.widget.setPosition(ToolXOptionsList.this.screen.width / 2 - this.widget.getWidth() / 2, this.getContentY() + offset);
			this.widget.extractRenderState(graphics, mouseX, mouseY, a);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of(this.widget);
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of(this.widget);
		}
	}

	public final class KeyBindEntry extends OptionsList.AbstractEntry {
		private static final Component RESET_BUTTON_TITLE = Component.translatable("controls.reset");
		private final Component label;
		private final Supplier<InputConstants.Key> getter;
		private final BooleanSupplier isSelected;
		private final Button changeButton;
		private final Button resetButton;
		private boolean hasCollision;

		private KeyBindEntry(
			Component label,
			Supplier<InputConstants.Key> getter,
			BooleanSupplier isSelected,
			Runnable onSelect,
			Runnable onReset
		) {
			this.label = label;
			this.getter = getter;
			this.isSelected = isSelected;
			this.changeButton = Button.builder(label, button -> onSelect.run())
				.bounds(0, 0, 75, 20)
				.createNarration(defaultNarrationSupplier -> {
					InputConstants.Key current = getter.get();
					return current.equals(InputConstants.UNKNOWN)
						? Component.translatable("narrator.controls.unbound", label)
						: Component.translatable("narrator.controls.bound", label, defaultNarrationSupplier.get());
				})
				.build();
			this.resetButton = Button.builder(RESET_BUTTON_TITLE, button -> onReset.run())
				.bounds(0, 0, 50, 20)
				.createNarration(defaultNarrationSupplier -> Component.translatable("narrator.controls.reset", label))
				.build();
			this.refreshEntry();
		}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
			int rowLeft = ToolXOptionsList.this.screen.width / 2 - 155;
			int rowRight = rowLeft + 310;
			int buttonY = this.getContentY();

			int resetButtonX = rowRight - this.resetButton.getWidth();
			this.resetButton.setPosition(resetButtonX, buttonY);
			this.resetButton.extractRenderState(graphics, mouseX, mouseY, a);

			int changeButtonX = resetButtonX - 5 - this.changeButton.getWidth();
			this.changeButton.setPosition(changeButtonX, buttonY);
			this.changeButton.extractRenderState(graphics, mouseX, mouseY, a);

			graphics.text(ToolXOptionsList.this.minecraft.font, this.label, rowLeft, buttonY + (20 - 9) / 2, -1);

			if (this.hasCollision) {
				int stripeLeft = this.changeButton.getX() - 6;
				graphics.fill(stripeLeft, buttonY, stripeLeft + 3, buttonY + 20, -256);
			}
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of(this.changeButton, this.resetButton);
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of(this.changeButton, this.resetButton);
		}

		public void refreshEntry() {
			InputConstants.Key current = this.getter.get();
			this.resetButton.active = !current.equals(InputConstants.UNKNOWN);
			this.hasCollision = false;
			MutableComponent tooltip = Component.empty();

			if (!current.equals(InputConstants.UNKNOWN)) {
				for (KeyMapping otherKey : ToolXOptionsList.this.minecraft.options.keyMappings) {
					if (otherKey.saveString().equals(current.getName())) {
						if (this.hasCollision) {
							tooltip.append(", ");
						}

						this.hasCollision = true;
						tooltip.append(Component.translatable(otherKey.getName()));
					}
				}

				for (OptionsList.AbstractEntry child : ToolXOptionsList.this.children()) {
					if (child != this && child instanceof KeyBindEntry otherEntry && otherEntry.getter.get().equals(current)) {
						if (this.hasCollision) {
							tooltip.append(", ");
						}

						this.hasCollision = true;
						tooltip.append(otherEntry.label);
					}
				}
			}

			if (this.hasCollision) {
				this.changeButton.setTooltip(Tooltip.create(Component.translatable("controls.keybinds.duplicateKeybinds", tooltip)));
			} else {
				this.changeButton.setTooltip(null);
			}

			this.updateChangeButtonMessage();
		}

		private void updateChangeButtonMessage() {
			Component message = this.getter.get().getDisplayName();
			if (this.hasCollision) {
				message = Component.literal("[ ").append(message.copy().withStyle(ChatFormatting.WHITE)).append(" ]").withStyle(ChatFormatting.YELLOW);
			}

			if (this.isSelected.getAsBoolean()) {
				message = Component.literal("> ")
					.append(message.copy().withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE))
					.append(" <")
					.withStyle(ChatFormatting.YELLOW);
			}

			this.changeButton.setMessage(message);
		}
	}
}
