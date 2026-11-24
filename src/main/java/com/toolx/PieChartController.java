package com.toolx;

import com.mojang.blaze3d.platform.InputConstants;
import com.toolx.mixin.DebugScreenEntryListAccessor;
import com.toolx.mixin.DebugScreenOverlayAccessor;
import com.toolx.mixin.ProfilerPieChartAccessor;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.components.debug.DebugScreenProfile;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class PieChartController {
	private static final long RESET_COOLDOWN_MS = 150L;
	private static final int RADAR_FOV = 30;
	private static final String ENDAR_PIE_PATH =
		"root" + ProfileResults.PATH_SEPARATOR + "tick" + ProfileResults.PATH_SEPARATOR + "entities";
	private static final String PIEDAR_PIE_PATH =
		"root" + ProfileResults.PATH_SEPARATOR + "tick" + ProfileResults.PATH_SEPARATOR + "blockEntities";
	private static final String PREEMPTIVE_PIE_PATH =
		"root"
			+ ProfileResults.PATH_SEPARATOR
			+ "frame"
			+ ProfileResults.PATH_SEPARATOR
			+ "extract"
			+ ProfileResults.PATH_SEPARATOR
			+ "level";

	private enum RadarMode {
		NONE,
		ENDAR,
		PIEDAR,
		PREEMPTIVE
	}

	private static int lastChunkX;
	private static int lastChunkZ;
	private static boolean hasLastChunk;
	private static @Nullable ResourceKey<Level> lastDimension;
	private static boolean pendingReset;
	private static boolean deferredChunkReset;
	private static long lastResetTimeMs;

	private static RadarMode activeRadarMode = RadarMode.NONE;
	private static int savedFov = 70;
	private static boolean savedAutoReset = true;
	private static boolean savedOverlayVisible;
	private static boolean savedRenderProfilerChart;
	private static String savedProfilerTreePath = "root";
	private static boolean savedChunkBordersPresent;
	private static @Nullable DebugScreenEntryStatus savedChunkBordersStatus;
	private static @Nullable DebugScreenProfile savedDebugProfile;

	private PieChartController() {
	}

	public static boolean isPieChartLocked() {
		return activeRadarMode == RadarMode.PIEDAR || activeRadarMode == RadarMode.PREEMPTIVE;
	}

	public static boolean isBoundToDigit(int digit) {
		if (digit < 0 || digit > 9) {
			return false;
		}
		ToolXConfig config = ToolXConfig.get();
		return matchesDigit(config.endarKey(), digit)
			|| matchesDigit(config.piedarKey(), digit)
			|| matchesDigit(config.preemptiveNavigationKey(), digit);
	}

	private static boolean matchesDigit(InputConstants.Key key, int digit) {
		if (key == null || key.equals(InputConstants.UNKNOWN)) {
			return false;
		}
		return key.getNumericKeyValue().isPresent() && key.getNumericKeyValue().getAsInt() == digit;
	}

	public static void syncSavedAutoReset(boolean value) {
		if (activeRadarMode != RadarMode.NONE) {
			savedAutoReset = value;
		}
	}

	public static boolean persistedAutoReset(boolean currentValue) {
		return activeRadarMode != RadarMode.NONE ? savedAutoReset : currentValue;
	}

	private static boolean isRadarFovActive() {
		return activeRadarMode == RadarMode.ENDAR || activeRadarMode == RadarMode.PREEMPTIVE;
	}

	public static boolean prepareOptionsSave(@Nullable Options options) {
		if (!isRadarFovActive() || options == null) {
			return false;
		}
		int currentFov = options.fov().get();
		if (currentFov != RADAR_FOV) {
			savedFov = currentFov;
			return false;
		}
		if (savedFov == RADAR_FOV) {
			return false;
		}
		options.fov().set(savedFov);
		return true;
	}

	public static void finishOptionsSave(@Nullable Options options) {
		if (options != null && isRadarFovActive()) {
			options.fov().set(RADAR_FOV);
		}
	}

	public static void handleRadarKey(Minecraft minecraft, InputConstants.Key pressedKey) {
		if (minecraft == null
			|| minecraft.level == null
			|| minecraft.player == null
			|| minecraft.gui == null
			|| minecraft.gui.screen() != null
			|| minecraft.gui.overlay() != null
			|| minecraft.debugEntries == null
			|| minecraft.getDebugOverlay() == null) {
			return;
		}

		ToolXConfig config = ToolXConfig.get();
		InputConstants.Key endarKey = config.endarKey();
		InputConstants.Key piedarKey = config.piedarKey();
		InputConstants.Key preemptiveKey = config.preemptiveNavigationKey();

		RadarMode targetMode = null;
		if (!endarKey.equals(InputConstants.UNKNOWN) && endarKey.equals(pressedKey)) {
			targetMode = RadarMode.ENDAR;
		} else if (!piedarKey.equals(InputConstants.UNKNOWN) && piedarKey.equals(pressedKey)) {
			targetMode = RadarMode.PIEDAR;
		} else if (!preemptiveKey.equals(InputConstants.UNKNOWN) && preemptiveKey.equals(pressedKey)) {
			targetMode = RadarMode.PREEMPTIVE;
		}

		if (targetMode == null) {
			return;
		}

		if (activeRadarMode == targetMode) {
			deactivateRadar(minecraft, true);
		} else {
			DebugScreenOverlay debugOverlay = minecraft.getDebugOverlay();
			DebugScreenEntryListAccessor entriesAccessor = (DebugScreenEntryListAccessor) minecraft.debugEntries;
			Map<Identifier, DebugScreenEntryStatus> allStatuses = entriesAccessor.toolx$getAllStatuses();

			if (activeRadarMode == RadarMode.NONE) {
				savedFov = minecraft.options.fov().get();
				savedAutoReset = config.pieChartAutoReset;
				savedOverlayVisible = minecraft.debugEntries.isOverlayVisible();
				savedRenderProfilerChart = ((DebugScreenOverlayAccessor) debugOverlay).toolx$isRenderProfilerChart();
				String currentPath = ((ProfilerPieChartAccessor) debugOverlay.getProfilerPieChart()).toolx$getProfilerTreePath();
				savedProfilerTreePath = currentPath != null ? currentPath : "root";
				savedChunkBordersPresent = allStatuses.containsKey(DebugScreenEntries.CHUNK_BORDERS);
				savedChunkBordersStatus = allStatuses.get(DebugScreenEntries.CHUNK_BORDERS);
				savedDebugProfile = entriesAccessor.toolx$getProfile();
			}

			boolean wasProfilerChartVisible = debugOverlay.showProfilerChart();
			activeRadarMode = targetMode;

			if (targetMode == RadarMode.ENDAR || targetMode == RadarMode.PREEMPTIVE) {
				minecraft.options.fov().set(RADAR_FOV);
			} else {
				minecraft.options.fov().set(savedFov);
			}

			config.pieChartAutoReset = true;

			boolean chunkBordersChanged;
			if (targetMode == RadarMode.PIEDAR || targetMode == RadarMode.PREEMPTIVE) {
				chunkBordersChanged = allStatuses.put(DebugScreenEntries.CHUNK_BORDERS, DebugScreenEntryStatus.ALWAYS_ON)
					!= DebugScreenEntryStatus.ALWAYS_ON;
			} else {
				chunkBordersChanged = restoreChunkBorders(allStatuses, entriesAccessor);
			}

			if (!minecraft.debugEntries.isOverlayVisible()) {
				minecraft.debugEntries.setOverlayVisible(true);
			} else if (chunkBordersChanged) {
				minecraft.debugEntries.rebuildCurrentList();
			}

			((DebugScreenOverlayAccessor) debugOverlay).toolx$setRenderProfilerChart(true);
			((ProfilerPieChartAccessor) debugOverlay.getProfilerPieChart()).toolx$setProfilerTreePath(pathFor(targetMode));

			ChunkPos currentChunk = minecraft.player.chunkPosition();
			lastChunkX = currentChunk.x();
			lastChunkZ = currentChunk.z();
			lastDimension = minecraft.level.dimension();
			hasLastChunk = true;
			deferredChunkReset = false;

			if (wasProfilerChartVisible) {
				long now = Util.getMillis();
				if (now - lastResetTimeMs >= RESET_COOLDOWN_MS) {
					pendingReset = true;
					lastResetTimeMs = now;
				}
			}
		}
	}

	private static boolean restoreChunkBorders(
		Map<Identifier, DebugScreenEntryStatus> allStatuses,
		DebugScreenEntryListAccessor entriesAccessor
	) {
		DebugScreenEntryStatus previousStatus = allStatuses.get(DebugScreenEntries.CHUNK_BORDERS);
		DebugScreenEntryStatus restoredStatus;
		if (savedChunkBordersPresent && savedChunkBordersStatus != null) {
			allStatuses.put(DebugScreenEntries.CHUNK_BORDERS, savedChunkBordersStatus);
			restoredStatus = savedChunkBordersStatus;
		} else {
			allStatuses.remove(DebugScreenEntries.CHUNK_BORDERS);
			restoredStatus = null;
		}
		DebugScreenProfile previousProfile = entriesAccessor.toolx$getProfile();
		entriesAccessor.toolx$setProfile(savedDebugProfile);
		return previousStatus != restoredStatus || previousProfile != savedDebugProfile;
	}

	private static String pathFor(RadarMode mode) {
		return switch (mode) {
			case ENDAR -> ENDAR_PIE_PATH;
			case PIEDAR -> PIEDAR_PIE_PATH;
			case PREEMPTIVE -> PREEMPTIVE_PIE_PATH;
			case NONE -> "root";
		};
	}

	private static void deactivateRadar(@Nullable Minecraft minecraft, boolean restoreOverlayState) {
		activeRadarMode = RadarMode.NONE;
		ToolXConfig.get().pieChartAutoReset = savedAutoReset;
		if (!savedAutoReset) {
			hasLastChunk = false;
			lastDimension = null;
			pendingReset = false;
			deferredChunkReset = false;
		}
		if (minecraft == null) {
			return;
		}

		if (minecraft.options != null) {
			minecraft.options.fov().set(savedFov);
		}

		boolean overlayVisible = minecraft.debugEntries != null && minecraft.debugEntries.isOverlayVisible();

		DebugScreenOverlay debugOverlay = minecraft.getDebugOverlay();
		if (debugOverlay != null) {
			((ProfilerPieChartAccessor) debugOverlay.getProfilerPieChart())
				.toolx$setProfilerTreePath(savedProfilerTreePath != null ? savedProfilerTreePath : "root");
			if (restoreOverlayState || !overlayVisible) {
				((DebugScreenOverlayAccessor) debugOverlay).toolx$setRenderProfilerChart(savedRenderProfilerChart);
			}
		}

		if (minecraft.debugEntries != null) {
			DebugScreenEntryListAccessor entriesAccessor = (DebugScreenEntryListAccessor) minecraft.debugEntries;
			boolean chunkBordersChanged = restoreChunkBorders(entriesAccessor.toolx$getAllStatuses(), entriesAccessor);
			if (restoreOverlayState && overlayVisible != savedOverlayVisible) {
				minecraft.debugEntries.setOverlayVisible(savedOverlayVisible);
			} else if (chunkBordersChanged) {
				minecraft.debugEntries.rebuildCurrentList();
			}
		}
	}

	public static void onClientTick(Minecraft minecraft) {
		if (activeRadarMode != RadarMode.NONE
			&& minecraft != null
			&& minecraft.debugEntries != null
			&& minecraft.getDebugOverlay() != null) {
			boolean overlayVisible = minecraft.debugEntries.isOverlayVisible();
			boolean profilerVisible = ((DebugScreenOverlayAccessor) minecraft.getDebugOverlay()).toolx$isRenderProfilerChart();
			if (!overlayVisible || !profilerVisible) {
				deactivateRadar(minecraft, false);
			}
		}

		if (!ToolXConfig.get().pieChartAutoReset) {
			hasLastChunk = false;
			lastDimension = null;
			pendingReset = false;
			deferredChunkReset = false;
			return;
		}

		if (minecraft == null || minecraft.player == null || minecraft.level == null) {
			return;
		}

		ChunkPos currentChunk = minecraft.player.chunkPosition();
		int cx = currentChunk.x();
		int cz = currentChunk.z();
		ResourceKey<Level> dim = minecraft.level.dimension();

		if (hasLastChunk && (cx != lastChunkX || cz != lastChunkZ || dim != lastDimension)) {
			deferredChunkReset = true;
		}

		if (deferredChunkReset) {
			long now = Util.getMillis();
			if (now - lastResetTimeMs >= RESET_COOLDOWN_MS) {
				pendingReset = true;
				deferredChunkReset = false;
				lastResetTimeMs = now;
			}
		}

		lastChunkX = cx;
		lastChunkZ = cz;
		lastDimension = dim;
		hasLastChunk = true;
	}

	public static boolean consumeReset() {
		if (pendingReset) {
			pendingReset = false;
			return true;
		}
		return false;
	}

	public static void resetTracking(@Nullable Minecraft minecraft) {
		if (activeRadarMode != RadarMode.NONE) {
			deactivateRadar(minecraft, true);
		}
		hasLastChunk = false;
		lastDimension = null;
		pendingReset = false;
		deferredChunkReset = false;
		lastResetTimeMs = 0L;
	}
}
