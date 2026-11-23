package com.toolx;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
	}

	public void save() {
		Path path = path();

		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException exception) {
			LOGGER.warn("Could not save {}", path, exception);
		}
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("toolx.json");
	}
}
