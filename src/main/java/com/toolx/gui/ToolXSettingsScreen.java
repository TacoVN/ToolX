package com.toolx.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.toolx.PieChartController;
import com.toolx.TimerController;
import com.toolx.ToolXConfig;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public class ToolXSettingsScreen extends OptionsSubScreen {
	private static final Component TITLE = Component.literal("ToolX Settings");
	private ToolXOptionsList toolXList;
	private @Nullable KeyBindTarget selectedKeyBind;

	private enum KeyBindTarget {
		EYE_MEASUREMENT,
		ENDAR,
		PIEDAR,
		PREEMPTIVE_NAVIGATION
	}

	public ToolXSettingsScreen(Screen lastScreen, Options options) {
		super(lastScreen, options, TITLE);
	}

	@Override
	protected void addOptions() {
		ToolXConfig config = ToolXConfig.get();
		this.addGeneralSection(config);
		this.addPieChartSection(config);
		this.addMcsrSection(config);
		this.toolXList.refreshKeyBinds();
	}

	@Override
	protected void addContents() {
		this.toolXList = this.layout.addToContents(new ToolXOptionsList(this.minecraft, this.width, this));
		this.list = this.toolXList;
		this.addOptions();
	}

	private void addGeneralSection(ToolXConfig config) {
		this.addToggleSection(
			"General",
			createToggle("F3 + F", config.f3F, value -> config.f3F = value, config)
		);
	}

	private void addPieChartSection(ToolXConfig config) {
		this.addToggleSection(
			"Pie Chart",
			createToggle("Auto Reset", config.pieChartAutoReset, value -> {
				config.pieChartAutoReset = value;
				PieChartController.syncSavedAutoReset(value);
			}, config)
		);
		this.toolXList.addKeyBind(
			Component.literal("Endar"),
			config::endarKey,
			() -> this.selectedKeyBind == KeyBindTarget.ENDAR,
			() -> {
				this.selectedKeyBind = KeyBindTarget.ENDAR;
				this.toolXList.refreshKeyBinds();
			},
			() -> {
				this.selectedKeyBind = null;
				config.setEndarKey(InputConstants.UNKNOWN);
				config.save();
				this.toolXList.refreshKeyBinds();
			}
		);
		this.toolXList.addKeyBind(
			Component.literal("Piedar"),
			config::piedarKey,
			() -> this.selectedKeyBind == KeyBindTarget.PIEDAR,
			() -> {
				this.selectedKeyBind = KeyBindTarget.PIEDAR;
				this.toolXList.refreshKeyBinds();
			},
			() -> {
				this.selectedKeyBind = null;
				config.setPiedarKey(InputConstants.UNKNOWN);
				config.save();
				this.toolXList.refreshKeyBinds();
			}
		);
		this.toolXList.addKeyBind(
			Component.literal("Preemptive Navigation"),
			config::preemptiveNavigationKey,
			() -> this.selectedKeyBind == KeyBindTarget.PREEMPTIVE_NAVIGATION,
			() -> {
				this.selectedKeyBind = KeyBindTarget.PREEMPTIVE_NAVIGATION;
				this.toolXList.refreshKeyBinds();
			},
			() -> {
				this.selectedKeyBind = null;
				config.setPreemptiveNavigationKey(InputConstants.UNKNOWN);
				config.save();
				this.toolXList.refreshKeyBinds();
			}
		);
	}

	private void addMcsrSection(ToolXConfig config) {
		this.addToggleSection(
			"MCSR",
			createToggle("Timer", config.timer, value -> {
				config.timer = value;
				TimerController.onToggleChanged(value);
			}, config)
		);
		this.toolXList.addKeyBind(
			Component.literal("Eye Measurement"),
			config::eyeMeasurementKey,
			() -> this.selectedKeyBind == KeyBindTarget.EYE_MEASUREMENT,
			() -> {
				this.selectedKeyBind = KeyBindTarget.EYE_MEASUREMENT;
				this.toolXList.refreshKeyBinds();
			},
			() -> {
				this.selectedKeyBind = null;
				config.setEyeMeasurementKey(InputConstants.UNKNOWN);
				config.save();
				this.toolXList.refreshKeyBinds();
			}
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

	private void applyKeyBind(InputConstants.Key key) {
		ToolXConfig config = ToolXConfig.get();
		KeyBindTarget target = this.selectedKeyBind;
		this.selectedKeyBind = null;
		if (target == KeyBindTarget.EYE_MEASUREMENT) {
			config.setEyeMeasurementKey(key);
		} else if (target == KeyBindTarget.ENDAR) {
			config.setEndarKey(key);
		} else if (target == KeyBindTarget.PIEDAR) {
			config.setPiedarKey(key);
		} else if (target == KeyBindTarget.PREEMPTIVE_NAVIGATION) {
			config.setPreemptiveNavigationKey(key);
		}
		config.save();
		this.toolXList.refreshKeyBinds();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.selectedKeyBind != null) {
			if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
				this.selectedKeyBind = null;
				this.toolXList.refreshKeyBinds();
				return super.mouseClicked(event, doubleClick);
			}
			this.applyKeyBind(InputConstants.Type.MOUSE.getOrCreate(event.button()));
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.selectedKeyBind != null) {
			this.applyKeyBind(event.isEscape() ? InputConstants.UNKNOWN : InputConstants.getKey(event));
			return true;
		}
		return super.keyPressed(event);
	}
}
