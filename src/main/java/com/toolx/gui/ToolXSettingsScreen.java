package com.toolx.gui;

import com.toolx.ToolXConfig;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

public class ToolXSettingsScreen extends OptionsSubScreen {
	private static final Component TITLE = Component.literal("ToolX Settings");

	public ToolXSettingsScreen(Screen lastScreen, Options options) {
		super(lastScreen, options, TITLE);
	}

	@Override
	protected void addOptions() {
		ToolXConfig config = ToolXConfig.get();
		this.addGeneralSection(config);
	}

	@Override
	protected void addContents() {
		this.list = this.layout.addToContents(new ToolXOptionsList(this.minecraft, this.width, this));
		this.addOptions();
	}

	private void addGeneralSection(ToolXConfig config) {
		this.addToggleSection(
			"General",
			createToggle("F3 + F", config.f3F, value -> config.f3F = value, config)
		);
	}

	private void addToggleSection(String title, AbstractWidget... widgets) {
		this.addHeader(title);
		this.list.addSmall(List.of(widgets));
	}

	private CycleButton<Boolean> createToggle(String label, boolean initialValue, Consumer<Boolean> setter, ToolXConfig config) {
		return CycleButton.onOffBuilder(initialValue).create(Component.literal(label), (button, value) -> {
			setter.accept(value);
			config.save();
		});
	}

	private void addHeader(String title) {
		this.list.addHeader(Component.literal(title).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD));
	}

	@Override
	public void removed() {
		super.removed();
		ToolXConfig.get().save();
	}
}
