package com.toolx;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
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
	public String eyeMeasurementKey = InputConstants.UNKNOWN.getName();
	public double eyeMeasurementHudX = 0.0;
	public double eyeMeasurementHudY = 0.5;
	public double eyeZoomBoxX = -1.0;
	public double eyeZoomBoxY = -1.0;
	public boolean pieChartAutoReset = true;
	public String endarKey = InputConstants.UNKNOWN.getName();
	public String piedarKey = InputConstants.UNKNOWN.getName();
	public String preemptiveNavigationKey = InputConstants.UNKNOWN.getName();
	public boolean timer = true;
	public double timerHudX = 1.0;
	public double timerHudY = 0.0;
	public double thinECounterX = -1.0;
	public double thinECounterY = -1.0;
	public double thinPieChartX = -1.0;
	public double thinPieChartY = -1.0;
	private transient InputConstants.Key cachedEyeMeasurementKey = InputConstants.UNKNOWN;
	private transient InputConstants.Key cachedEndarKey = InputConstants.UNKNOWN;
	private transient InputConstants.Key cachedPiedarKey = InputConstants.UNKNOWN;
	private transient InputConstants.Key cachedPreemptiveNavigationKey = InputConstants.UNKNOWN;

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
		instance.thinECounterX = sanitizeOptionalAxis(instance.thinECounterX);
		instance.thinECounterY = sanitizeOptionalAxis(instance.thinECounterY);
		instance.thinPieChartX = sanitizeOptionalAxis(instance.thinPieChartX);
		instance.thinPieChartY = sanitizeOptionalAxis(instance.thinPieChartY);
		instance.eyeZoomBoxX = sanitizeOptionalAxis(instance.eyeZoomBoxX);
		instance.eyeZoomBoxY = sanitizeOptionalAxis(instance.eyeZoomBoxY);
		instance.timerHudX = Double.isFinite(instance.timerHudX) ? Math.clamp(instance.timerHudX, 0.0, 1.0) : 1.0;
		instance.timerHudY = Double.isFinite(instance.timerHudY) ? Math.clamp(instance.timerHudY, 0.0, 1.0) : 0.0;
		instance.refreshCachedKeys();
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
