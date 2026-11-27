package com.toolx;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import com.toolx.gui.ThinModeHud;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ToolXConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger("ToolX");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static ToolXConfig instance;

	public boolean f3F = true;
	public int flightSpeed = 1;
	public String eyeMeasurementKey = InputConstants.UNKNOWN.getName();
	public double eyeMeasurementHudX = 0.0;
	public double eyeMeasurementHudY = 0.5;
	public double eyeZoomBoxX = -1.0;
	public double eyeZoomBoxY = -1.0;
	public boolean pieChartAutoReset = true;
	public String endarKey = InputConstants.UNKNOWN.getName();
	public String piedarKey = InputConstants.UNKNOWN.getName();
	public String preemptiveNavigationKey = InputConstants.UNKNOWN.getName();
	public boolean timer = false;
	public double timerHudX = 1.0;
	public double timerHudY = 0.5;
	public boolean resourceTracker = false;
	public double resourceTrackerHudX = -1.0;
	public double resourceTrackerHudY = -1.0;
	public double thinCCounterX = -1.0;
	public double thinCCounterY = -1.0;
	public double thinECounterX = -1.0;
	public double thinECounterY = -1.0;
	public double thinPieChartX = -1.0;
	public double thinPieChartY = -1.0;
	public String pieZeroKey = KeyBindsController.DEFAULT_PIE_ZERO_KEY.getName();
	public String backspaceKey = KeyBindsController.DEFAULT_BACKSPACE_KEY.getName();
	public boolean f3DebugChart = false;
	private transient InputConstants.Key cachedEyeMeasurementKey = InputConstants.UNKNOWN;
	private transient InputConstants.Key cachedEndarKey = InputConstants.UNKNOWN;
	private transient InputConstants.Key cachedPiedarKey = InputConstants.UNKNOWN;
	private transient InputConstants.Key cachedPreemptiveNavigationKey = InputConstants.UNKNOWN;
	private transient InputConstants.Key cachedPieZeroKey = KeyBindsController.DEFAULT_PIE_ZERO_KEY;
	private transient InputConstants.Key cachedBackspaceKey = KeyBindsController.DEFAULT_BACKSPACE_KEY;

	public static ToolXConfig get() {
		if (instance == null) {
			load();
		}
		return instance;
	}

	public static void load() {
		Path path = path();
		ToolXConfig loaded = null;

		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				loaded = GSON.fromJson(reader, ToolXConfig.class);
			} catch (IOException | RuntimeException exception) {
				LOGGER.warn("Could not read {}", path, exception);
			}
		}

		instance = loaded != null ? loaded : new ToolXConfig();
		if (instance.eyeMeasurementKey == null || instance.eyeMeasurementKey.isBlank()) {
			instance.eyeMeasurementKey = InputConstants.UNKNOWN.getName();
		}
		if (!Double.isFinite(instance.eyeMeasurementHudX)) {
			instance.eyeMeasurementHudX = 0.0;
		} else {
			instance.eyeMeasurementHudX = Math.clamp(instance.eyeMeasurementHudX, 0.0, 1.0);
		}
		if (!Double.isFinite(instance.eyeMeasurementHudY)) {
			instance.eyeMeasurementHudY = 0.5;
		} else {
			instance.eyeMeasurementHudY = Math.clamp(instance.eyeMeasurementHudY, 0.0, 1.0);
		}
		if (instance.endarKey == null || instance.endarKey.isBlank()) {
			instance.endarKey = InputConstants.UNKNOWN.getName();
		}
		if (instance.piedarKey == null || instance.piedarKey.isBlank()) {
			instance.piedarKey = InputConstants.UNKNOWN.getName();
		}
		if (instance.preemptiveNavigationKey == null || instance.preemptiveNavigationKey.isBlank()) {
			instance.preemptiveNavigationKey = InputConstants.UNKNOWN.getName();
		}
		if (instance.pieZeroKey == null || instance.pieZeroKey.isBlank()) {
			instance.pieZeroKey = KeyBindsController.DEFAULT_PIE_ZERO_KEY.getName();
		}
		if (instance.backspaceKey == null || instance.backspaceKey.isBlank()) {
			instance.backspaceKey = KeyBindsController.DEFAULT_BACKSPACE_KEY.getName();
		}
		instance.flightSpeed = sanitizeFlightSpeed(instance.flightSpeed);
		instance.thinCCounterX = sanitizeThinCounterX(instance.thinCCounterX);
		instance.thinCCounterY = sanitizeOptionalAxis(instance.thinCCounterY);
		instance.thinECounterX = sanitizeThinCounterX(instance.thinECounterX);
		instance.thinECounterY = sanitizeOptionalAxis(instance.thinECounterY);
		instance.thinPieChartX = sanitizeOptionalAxis(instance.thinPieChartX);
		instance.thinPieChartY = sanitizeOptionalAxis(instance.thinPieChartY);
		instance.eyeZoomBoxX = sanitizeOptionalAxis(instance.eyeZoomBoxX);
		instance.eyeZoomBoxY = sanitizeOptionalAxis(instance.eyeZoomBoxY);
		instance.timerHudX = Double.isFinite(instance.timerHudX) ? Math.clamp(instance.timerHudX, 0.0, 1.0) : 1.0;
		instance.timerHudY = Double.isFinite(instance.timerHudY) ? Math.clamp(instance.timerHudY, 0.0, 1.0) : 0.5;
		instance.resourceTrackerHudX = sanitizeOptionalAxis(instance.resourceTrackerHudX);
		instance.resourceTrackerHudY = sanitizeOptionalAxis(instance.resourceTrackerHudY);
		instance.refreshCachedKeys();
	}

	private static int sanitizeFlightSpeed(int value) {
		return Math.clamp(value, 1, 10);
	}

	private static double sanitizeThinCounterX(double value) {
		if (!Double.isFinite(value)) {
			return ThinModeHud.LEFT_STRIP_SNAP;
		}
		if (Math.abs(value - ThinModeHud.RIGHT_STRIP_SNAP) < 0.001) {
			return ThinModeHud.RIGHT_STRIP_SNAP;
		}
		if (value < 0.0) {
			return ThinModeHud.LEFT_STRIP_SNAP;
		}
		return Math.clamp(value, 0.0, 1.0);
	}

	private static double sanitizeOptionalAxis(double value) {
		if (!Double.isFinite(value) || value < 0.0) {
			return -1.0;
		}
		return Math.clamp(value, 0.0, 1.0);
	}

	private void refreshCachedKeys() {
		this.cachedEyeMeasurementKey = parseKey(this.eyeMeasurementKey);
		this.cachedEndarKey = parseKey(this.endarKey);
		this.cachedPiedarKey = parseKey(this.piedarKey);
		this.cachedPreemptiveNavigationKey = parseKey(this.preemptiveNavigationKey);
		this.cachedPieZeroKey = parseKey(this.pieZeroKey);
		this.cachedBackspaceKey = parseKey(this.backspaceKey);
	}

	public InputConstants.Key eyeMeasurementKey() {
		return this.cachedEyeMeasurementKey != null ? this.cachedEyeMeasurementKey : InputConstants.UNKNOWN;
	}

	public void setEyeMeasurementKey(InputConstants.Key key) {
		InputConstants.Key resolved = key != null ? key : InputConstants.UNKNOWN;
		this.eyeMeasurementKey = resolved.getName();
		this.cachedEyeMeasurementKey = resolved;
	}

	public InputConstants.Key endarKey() {
		return this.cachedEndarKey != null ? this.cachedEndarKey : InputConstants.UNKNOWN;
	}

	public void setEndarKey(InputConstants.Key key) {
		InputConstants.Key resolved = key != null ? key : InputConstants.UNKNOWN;
		this.endarKey = resolved.getName();
		this.cachedEndarKey = resolved;
	}

	public InputConstants.Key piedarKey() {
		return this.cachedPiedarKey != null ? this.cachedPiedarKey : InputConstants.UNKNOWN;
	}

	public void setPiedarKey(InputConstants.Key key) {
		InputConstants.Key resolved = key != null ? key : InputConstants.UNKNOWN;
		this.piedarKey = resolved.getName();
		this.cachedPiedarKey = resolved;
	}

	public InputConstants.Key preemptiveNavigationKey() {
		return this.cachedPreemptiveNavigationKey != null ? this.cachedPreemptiveNavigationKey : InputConstants.UNKNOWN;
	}

	public void setPreemptiveNavigationKey(InputConstants.Key key) {
		InputConstants.Key resolved = key != null ? key : InputConstants.UNKNOWN;
		this.preemptiveNavigationKey = resolved.getName();
		this.cachedPreemptiveNavigationKey = resolved;
	}

	public InputConstants.Key pieZeroKey() {
		return this.cachedPieZeroKey != null ? this.cachedPieZeroKey : KeyBindsController.DEFAULT_PIE_ZERO_KEY;
	}

	public void setPieZeroKey(InputConstants.Key key) {
		InputConstants.Key resolved = key != null ? key : InputConstants.UNKNOWN;
		this.pieZeroKey = resolved.getName();
		this.cachedPieZeroKey = resolved;
	}

	public InputConstants.Key backspaceKey() {
		return this.cachedBackspaceKey != null ? this.cachedBackspaceKey : KeyBindsController.DEFAULT_BACKSPACE_KEY;
	}

	public void setBackspaceKey(InputConstants.Key key) {
		InputConstants.Key resolved = key != null ? key : InputConstants.UNKNOWN;
		this.backspaceKey = resolved.getName();
		this.cachedBackspaceKey = resolved;
	}

	private static InputConstants.Key parseKey(String name) {
		if (name == null || name.isBlank()) {
			return InputConstants.UNKNOWN;
		}
		try {
			return InputConstants.getKey(name);
		} catch (IllegalArgumentException exception) {
			return InputConstants.UNKNOWN;
		}
	}

	public void save() {
		Path path = path();

		try {
			Files.createDirectories(path.getParent());
			boolean originalAutoReset = this.pieChartAutoReset;
			this.pieChartAutoReset = PieChartController.persistedAutoReset(originalAutoReset);
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(this, writer);
			} finally {
				this.pieChartAutoReset = originalAutoReset;
			}
		} catch (IOException exception) {
			LOGGER.warn("Could not save {}", path, exception);
		}
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("toolx.json");
	}
}
