package com.toolx;

import com.mojang.blaze3d.platform.InputConstants;
import com.toolx.gui.StripTransition;
import com.toolx.gui.ThinModeHud;
import com.toolx.mixin.DebugScreenEntryListAccessor;
import com.toolx.mixin.DebugScreenOverlayAccessor;
import com.toolx.mixin.ProfilerPieChartAccessor;
import com.toolx.mixin.SectionOcclusionGraphAccessor;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.components.debug.DebugScreenProfile;
import net.minecraft.client.gui.components.debugchart.ProfilerPieChart;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.ResultField;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class PieChartController {
	private static final long RESET_COOLDOWN_MS = 150L;
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
	private static boolean lastThinCullingActive;
	private static boolean savedAutoReset = true;
	private static boolean savedOverlayVisible;
	private static boolean savedRenderProfilerChart;
	private static String savedProfilerTreePath = "root";
	private static boolean savedChunkBordersPresent;
	private static @Nullable DebugScreenEntryStatus savedChunkBordersStatus;
	private static @Nullable DebugScreenProfile savedDebugProfile;
	private static int betterNavBuffer;
	private static boolean dispatchingBetterNav;

	private PieChartController() {
	}

	public static boolean isPieChartLocked() {
		return activeRadarMode == RadarMode.PIEDAR || activeRadarMode == RadarMode.PREEMPTIVE;
	}

	public static boolean isDispatchingBetterNavigation() {
		return dispatchingBetterNav;
	}

	public static void clearBetterNavigation() {
		betterNavBuffer = 0;
	}

	public static void onKeyReleased(@Nullable Minecraft minecraft, KeyEvent event) {
		if (betterNavBuffer <= 0
			|| (event.key() != InputConstants.KEY_LCONTROL && event.key() != InputConstants.KEY_RCONTROL)) {
			return;
		}
		boolean otherCtrlDown = (event.key() != InputConstants.KEY_LCONTROL && InputConstants.isKeyDown(InputConstants.KEY_LCONTROL))
			|| (event.key() != InputConstants.KEY_RCONTROL && InputConstants.isKeyDown(InputConstants.KEY_RCONTROL));
		if (otherCtrlDown) {
			return;
		}
		int target = betterNavBuffer;
		betterNavBuffer = 0;
		if (canUseBetterNavigation(minecraft)) {
			dispatchPieKey(minecraft, target);
		}
	}

	public static boolean handleBetterNavigationKey(@Nullable Minecraft minecraft, int action, KeyEvent event) {
		if (!canUseBetterNavigation(minecraft)) {
			return false;
		}
		if (event.isEscape() || minecraft.options.keyDebugModifier.matches(event)) {
			clearBetterNavigation();
			return false;
		}
		if (!event.hasControlDown()) {
			clearBetterNavigation();
			return false;
		}
		InputConstants.Key pressed = InputConstants.getKey(event);
		if (isRadarOrEyeKey(pressed)) {
			return false;
		}
		int digit = resolveBetterNavDigit(pressed);
		if (digit < 0) {
			return false;
		}
		if (action != 1) {
			return true;
		}
		processBetterNavDigit(minecraft, digit);
		return true;
	}

	public static boolean handleBetterNavigationMouse(@Nullable Minecraft minecraft, InputConstants.Key pressed) {
		if (!canUseBetterNavigation(minecraft) || !isCtrlHeld()) {
			return false;
		}
		InputConstants.Key pieZero = ToolXConfig.get().pieZeroKey();
		if (!pieZero.equals(InputConstants.UNKNOWN) && pieZero.equals(pressed)) {
			processBetterNavDigit(minecraft, 0);
			return true;
		}
		return false;
	}

	private static boolean canUseBetterNavigation(@Nullable Minecraft minecraft) {
		return ToolXConfig.get().pieChartBetterNavigation
			&& minecraft != null
			&& minecraft.level != null
			&& minecraft.player != null
			&& minecraft.gui != null
			&& minecraft.gui.overlay() == null
			&& isAllowedRadarScreen(minecraft.gui.screen())
			&& minecraft.options != null
			&& !minecraft.options.keyDebugModifier.isDown()
			&& minecraft.getDebugOverlay() != null
			&& minecraft.getDebugOverlay().showProfilerChart();
	}

	private static boolean isCtrlHeld() {
		return InputConstants.isKeyDown(InputConstants.KEY_LCONTROL)
			|| InputConstants.isKeyDown(InputConstants.KEY_RCONTROL);
	}

	private static boolean isRadarOrEyeKey(InputConstants.Key key) {
		ToolXConfig config = ToolXConfig.get();
		return (!config.endarKey().equals(InputConstants.UNKNOWN) && config.endarKey().equals(key))
			|| (!config.piedarKey().equals(InputConstants.UNKNOWN) && config.piedarKey().equals(key))
			|| (!config.preemptiveNavigationKey().equals(InputConstants.UNKNOWN) && config.preemptiveNavigationKey().equals(key))
			|| (!config.eyeMeasurementKey().equals(InputConstants.UNKNOWN) && config.eyeMeasurementKey().equals(key));
	}

	private static int resolveBetterNavDigit(InputConstants.Key pressed) {
		if (pressed.getNumericKeyValue().isPresent()) {
			int d = pressed.getNumericKeyValue().getAsInt();
			if (d == 0 && betterNavBuffer == 0 && !KeyBindsController.allowVanillaPieChartKey(0)) {
				return -1;
			}
			return d;
		}
		InputConstants.Key pieZero = ToolXConfig.get().pieZeroKey();
		if (!pieZero.equals(InputConstants.UNKNOWN) && pieZero.equals(pressed)) {
			return 0;
		}
		return -1;
	}

	private static void processBetterNavDigit(Minecraft minecraft, int digit) {
		if (digit == 0 && betterNavBuffer == 0) {
			dispatchPieKey(minecraft, 0);
			return;
		}
		int maxEntries = currentPieEntryCount(minecraft);
		int candidate = betterNavBuffer * 10 + digit;
		if (maxEntries <= 0 || candidate > maxEntries) {
			betterNavBuffer = 0;
			return;
		}
		if (candidate * 10 > maxEntries) {
			betterNavBuffer = 0;
			dispatchPieKey(minecraft, candidate);
		} else {
			betterNavBuffer = candidate;
		}
	}

	private static int currentPieEntryCount(Minecraft minecraft) {
		if (minecraft.getDebugOverlay() == null) {
			return 0;
		}
		ProfilerPieChart pieChart = minecraft.getDebugOverlay().getProfilerPieChart();
		ProfilerPieChartAccessor accessor = (ProfilerPieChartAccessor) pieChart;
		ProfileResults results = accessor.toolx$getProfilerPieChartResults();
		if (results == null) {
			return 0;
		}
		String path = accessor.toolx$getProfilerTreePath();
		List<ResultField> entries = results.getTimes(path != null ? path : "root");
		return Math.max(0, entries.size() - 1);
	}

	private static void dispatchPieKey(Minecraft minecraft, int target) {
		if (minecraft.getDebugOverlay() == null) {
			return;
		}
		dispatchingBetterNav = true;
		try {
			minecraft.getDebugOverlay().getProfilerPieChart().profilerPieChartKeyPress(target);
		} finally {
			dispatchingBetterNav = false;
		}
	}

	public static boolean isBoundToDigit(int digit) {
		if (digit < 0 || digit > 9) {
			return false;
		}
		ToolXConfig config = ToolXConfig.get();
		InputConstants.Key pieZero = config.pieZeroKey();
		return matchesDigit(config.eyeMeasurementKey(), digit)
			|| matchesDigit(config.endarKey(), digit)
			|| matchesDigit(config.piedarKey(), digit)
			|| matchesDigit(config.preemptiveNavigationKey(), digit)
			|| (!pieZero.equals(KeyBindsController.DEFAULT_PIE_ZERO_KEY) && matchesDigit(pieZero, digit));
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

	public static boolean isRadarActive() {
		return activeRadarMode != RadarMode.NONE;
	}

	public static boolean isThinModeActive() {
		return activeRadarMode == RadarMode.ENDAR || activeRadarMode == RadarMode.PREEMPTIVE;
	}

	public static boolean isThinCullingActive(@Nullable Minecraft minecraft) {
		return ThinModeHud.shouldRenderThinLayout(minecraft) && StripTransition.isSettled();
	}

	public static void syncThinCulling(@Nullable Minecraft minecraft) {
		boolean thinCullingNow = isThinCullingActive(minecraft);
		if (thinCullingNow != lastThinCullingActive) {
			lastThinCullingActive = thinCullingNow;
			refreshFrustum(minecraft);
		}
	}

	private static void refreshFrustum(@Nullable Minecraft minecraft) {
		if (minecraft != null && minecraft.levelRenderer != null) {
			SectionOcclusionGraph graph = minecraft.levelRenderer.sectionOcclusionGraph();
			graph.invalidate();
			((SectionOcclusionGraphAccessor) graph).toolx$getNeedsFrustumUpdate().set(true);
		}
	}

	public static boolean handleRadarKey(Minecraft minecraft, InputConstants.Key pressedKey) {
		if (minecraft == null
			|| minecraft.level == null
			|| minecraft.player == null
			|| minecraft.gui == null
			|| !isAllowedRadarScreen(minecraft.gui.screen())
			|| minecraft.gui.overlay() != null
			|| minecraft.debugEntries == null
			|| minecraft.getDebugOverlay() == null) {
			return false;
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

		if (targetMode == null || EyeMeasurementController.isMeasuring()) {
			return false;
		}

		if (activeRadarMode == targetMode) {
			deactivateRadar(minecraft, true);
		} else {
			DebugScreenOverlay debugOverlay = minecraft.getDebugOverlay();
			DebugScreenEntryListAccessor entriesAccessor = (DebugScreenEntryListAccessor) minecraft.debugEntries;
			Map<Identifier, DebugScreenEntryStatus> allStatuses = entriesAccessor.toolx$getAllStatuses();

			if (activeRadarMode == RadarMode.NONE) {
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
			syncThinCulling(minecraft);

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
		return true;
	}

	private static boolean isAllowedRadarScreen(@Nullable Screen screen) {
		return screen == null || (screen instanceof PauseScreen pause && !pause.showsPauseMenu());
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

	public static void onScreenChanged(@Nullable Minecraft minecraft, @Nullable Screen screen) {
		if (!isAllowedRadarScreen(screen)) {
			clearBetterNavigation();
		}
		if (activeRadarMode != RadarMode.NONE && !isAllowedWhileRadarActive(screen)) {
			deactivateRadar(minecraft, true);
			StripTransition.snapOff();
		}
	}

	private static boolean isAllowedWhileRadarActive(@Nullable Screen screen) {
		return screen == null
			|| screen instanceof ChatScreen
			|| (screen instanceof PauseScreen pause && !pause.showsPauseMenu());
	}

	private static void deactivateRadar(@Nullable Minecraft minecraft, boolean restoreOverlayState) {
		activeRadarMode = RadarMode.NONE;
		clearBetterNavigation();
		ThinModeHud.resetState();
		syncThinCulling(minecraft);
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

		if (debugOverlay != null && !debugOverlay.showProfilerChart()) {
			debugOverlay.getProfilerPieChart().setPieChartResults(null);
		}
	}

	public static void onClientTick(Minecraft minecraft) {
		if (activeRadarMode != RadarMode.NONE
			&& minecraft != null
			&& minecraft.gui != null
			&& !isAllowedWhileRadarActive(minecraft.gui.screen())) {
			deactivateRadar(minecraft, true);
			StripTransition.snapOff();
		}

		if (betterNavBuffer > 0 && (!canUseBetterNavigation(minecraft) || !isCtrlHeld())) {
			clearBetterNavigation();
		}

		syncThinCulling(minecraft);

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

		boolean chartVisible = minecraft.getDebugOverlay() != null && minecraft.getDebugOverlay().showProfilerChart();
		if (!chartVisible) {
			pendingReset = false;
			deferredChunkReset = false;
			lastChunkX = cx;
			lastChunkZ = cz;
			lastDimension = dim;
			hasLastChunk = true;
			return;
		}

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
		} else {
			ThinModeHud.resetState();
		}
		clearBetterNavigation();
		hasLastChunk = false;
		lastDimension = null;
		pendingReset = false;
		deferredChunkReset = false;
		lastResetTimeMs = 0L;
	}
}
