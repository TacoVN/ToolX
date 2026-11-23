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
	public boolean pieChart2DMode = true;
	public boolean pieChartAutoReset = true;
	public String endarKey = InputConstants.UNKNOWN.getName();
	public String piedarKey = InputConstants.UNKNOWN.getName();
	public String preemptiveNavigationKey = InputConstants.UNKNOWN.getName();
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
		if (instance.endarKey == null || instance.endarKey.isBlank()) {
			instance.endarKey = InputConstants.UNKNOWN.getName();
		}
		if (instance.piedarKey == null || instance.piedarKey.isBlank()) {
			instance.piedarKey = InputConstants.UNKNOWN.getName();
		}
		if (instance.preemptiveNavigationKey == null || instance.preemptiveNavigationKey.isBlank()) {
			instance.preemptiveNavigationKey = InputConstants.UNKNOWN.getName();
		}
		instance.refreshCachedKeys();
	}

	private void refreshCachedKeys() {
		this.cachedEndarKey = parseKey(this.endarKey);
		this.cachedPiedarKey = parseKey(this.piedarKey);
		this.cachedPreemptiveNavigationKey = parseKey(this.preemptiveNavigationKey);
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
