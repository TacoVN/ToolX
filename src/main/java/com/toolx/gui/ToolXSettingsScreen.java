package com.toolx.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.toolx.KeyBindsController;
import com.toolx.PieChartController;
import com.toolx.TimerController;
import com.toolx.ToolXConfig;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractSliderButton;
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
		PREEMPTIVE_NAVIGATION,
		PIE_ZERO,
		BACKSPACE
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
		this.addKeyBindsSection(config);
		this.toolXList.refreshKeyBinds();
	}

	@Override
	protected void addContents() {
		this.toolXList = this.layout.addToContents(new ToolXOptionsList(this.minecraft, this.width, this));
		this.list = this.toolXList;
		this.addOptions();
	}

	private void addGeneralSection(ToolXConfig config) {
		boolean multiplayerLocked = this.minecraft != null
			&& (this.minecraft.level != null || this.minecraft.getConnection() != null)
			&& !this.minecraft.hasSingleplayerServer();
		this.addToggleSection(
			"General",
			createToggle("F3 + F", config.f3F, value -> config.f3F = value, config),
			new FlightSpeedSlider(config, multiplayerLocked),
			createToggle("F3 Debug Chart", config.f3DebugChart, value -> config.f3DebugChart = value, config)
		);
	}

	private void addPieChartSection(ToolXConfig config) {
		this.addToggleSection(
			"Pie Chart",
			createToggle("Auto Reset", config.pieChartAutoReset, value -> {
				config.pieChartAutoReset = value;
				PieChartController.syncSavedAutoReset(value);
			}, config),
			createToggle("Better Navigation", config.pieChartBetterNavigation, value -> {
				config.pieChartBetterNavigation = value;
				if (!value) {
					PieChartController.clearBetterNavigation();
				}
			}, config)
		);
		this.toolXList.addKeyBind(
			Component.literal("Endar"),
			InputConstants.UNKNOWN,
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
			InputConstants.UNKNOWN,
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
			InputConstants.UNKNOWN,
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
			}, config),
			createToggle("Resource Tracker", config.resourceTracker, value -> {
				config.resourceTracker = value;
				if (!value) {
					ResourceTrackerHud.resetState();
				}
			}, config)
		);
		this.toolXList.addKeyBind(
			Component.literal("Eye Measurement"),
			InputConstants.UNKNOWN,
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

	private void addKeyBindsSection(ToolXConfig config) {
		this.addHeader("Key Binds");
		this.toolXList.addKeyBind(
			Component.literal("Traverse Back"),
			KeyBindsController.DEFAULT_PIE_ZERO_KEY,
			config::pieZeroKey,
			() -> this.selectedKeyBind == KeyBindTarget.PIE_ZERO,
			() -> {
				this.selectedKeyBind = KeyBindTarget.PIE_ZERO;
				this.toolXList.refreshKeyBinds();
			},
			() -> {
				this.selectedKeyBind = null;
				config.setPieZeroKey(KeyBindsController.DEFAULT_PIE_ZERO_KEY);
				config.save();
				this.toolXList.refreshKeyBinds();
			}
		);
		this.toolXList.addKeyBind(
			Component.literal("Backspace"),
			KeyBindsController.DEFAULT_BACKSPACE_KEY,
			config::backspaceKey,
			() -> this.selectedKeyBind == KeyBindTarget.BACKSPACE,
			() -> {
				this.selectedKeyBind = KeyBindTarget.BACKSPACE;
				this.toolXList.refreshKeyBinds();
			},
			() -> {
				this.selectedKeyBind = null;
				config.setBackspaceKey(KeyBindsController.DEFAULT_BACKSPACE_KEY);
				config.save();
				this.toolXList.refreshKeyBinds();
			}
		);
	}

	private void addToggleSection(String title, AbstractWidget... widgets) {
		this.addHeader(title);
		this.list.addSmall(List.of(widgets));
	}

	private static final class FlightSpeedSlider extends AbstractSliderButton {
		private static final int MAX_SPEED = 10;
		private static final int MAX_INDEX = MAX_SPEED - 1;
		private final ToolXConfig config;
		private final boolean locked;

		private FlightSpeedSlider(ToolXConfig config, boolean locked) {
			super(
				0,
				0,
				150,
				20,
				labelFor(locked ? 1 : config.flightSpeed),
				locked ? 0.0 : indexToSliderValue(speedToIndex(config.flightSpeed))
			);
			this.config = config;
			this.locked = locked;
			if (locked) {
				this.active = false;
			}
		}

		@Override
		protected void updateMessage() {
			this.setMessage(labelFor(this.locked ? 1 : this.config.flightSpeed));
		}

		@Override
		protected void applyValue() {
			if (this.locked) {
				return;
			}
			int nextSpeed = sliderValueToSpeed(this.value);
			if (this.config.flightSpeed != nextSpeed) {
				this.config.flightSpeed = nextSpeed;
				this.config.save();
			}
		}

		@Override
		public boolean keyPressed(KeyEvent event) {
			if (this.locked) {
				return false;
			}
			if (this.canChangeValue && (event.isLeft() || event.isRight())) {
				int currentIndex = speedToIndex(this.config.flightSpeed);
				int nextIndex = Math.clamp(currentIndex + (event.isLeft() ? -1 : 1), 0, MAX_INDEX);
				this.setValue(indexToSliderValue(nextIndex));
				return true;
			}
			return super.keyPressed(event);
		}

		private static int indexToSpeed(int index) {
			return Math.clamp(index, 0, MAX_INDEX) + 1;
		}

		private static int speedToIndex(int speed) {
			return Math.clamp(speed, 1, MAX_SPEED) - 1;
		}

		private static double indexToSliderValue(int index) {
			return (double) Math.clamp(index, 0, MAX_INDEX) / MAX_INDEX;
		}

		private static int sliderValueToSpeed(double value) {
			return indexToSpeed((int) Math.round(Math.clamp(value, 0.0, 1.0) * MAX_INDEX));
		}

		private static Component labelFor(int speed) {
			return Component.literal("Flight Speed: " + Math.clamp(speed, 1, MAX_SPEED) + "x");
		}
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
		} else if (target == KeyBindTarget.PIE_ZERO) {
			config.setPieZeroKey(key);
		} else if (target == KeyBindTarget.BACKSPACE) {
			config.setBackspaceKey(key);
		}
		config.save();
		this.toolXList.refreshKeyBinds();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.selectedKeyBind != null) {
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
