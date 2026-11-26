package com.toolx;

import com.toolx.gui.TimerHud;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.level.GameType;
import org.jspecify.annotations.Nullable;

public final class TimerController {
	private enum TimerStatus {
		NONE,
		IDLE,
		RUNNING,
		COMPLETED
	}

	private static TimerStatus status = TimerStatus.NONE;
	private static @Nullable String activeWorldKey;
	private static long startNanos;
	private static long finalElapsedNanos;

	private TimerController() {
	}

	public static void onSingleplayerWorldLoad(@Nullable String levelId, boolean newWorld) {
		String key = levelId != null && !levelId.isBlank() ? "sp:" + levelId : null;
		if (newWorld && key != null) {
			resetAndArm(key);
		} else if (key == null || !key.equals(activeWorldKey)) {
			clear();
		}
	}

	public static void onClientLevelJoined(@Nullable Minecraft minecraft) {
		if (minecraft == null || minecraft.getSingleplayerServer() == null) {
			clear();
		}
	}

	public static void onDisconnect() {
		TimerHud.cancelDrag();
	}

	public static void onToggleChanged(boolean enabled) {
		if (!enabled) {
			TimerHud.cancelDrag();
		}
	}

	public static boolean isRunning() {
		return status == TimerStatus.RUNNING;
	}

	public static boolean isCompleted() {
		return status == TimerStatus.COMPLETED;
	}

	private static boolean isSurvivalMode(@Nullable Minecraft minecraft) {
		return minecraft != null
			&& minecraft.gameMode != null
			&& minecraft.gameMode.getPlayerMode() == GameType.SURVIVAL;
	}

	public static void onFirstInput(@Nullable Minecraft minecraft) {
		if (status != TimerStatus.IDLE) {
			return;
		}
		if (minecraft == null
			|| minecraft.level == null
			|| minecraft.player == null
			|| minecraft.gui == null
			|| minecraft.gui.screen() != null
			|| minecraft.gui.overlay() != null) {
			return;
		}
		if (!isSurvivalMode(minecraft)) {
			clear();
			return;
		}
		startNanos = System.nanoTime();
		status = TimerStatus.RUNNING;
	}

	public static void onClientTick(@Nullable Minecraft minecraft) {
		if (minecraft != null
			&& minecraft.level != null
			&& minecraft.player != null
			&& minecraft.gameMode != null
			&& minecraft.gameMode.getPlayerMode() != GameType.SURVIVAL) {
			if (status == TimerStatus.IDLE || status == TimerStatus.RUNNING) {
				clear();
			}
			return;
		}
		if (status != TimerStatus.IDLE || minecraft == null || minecraft.options == null) {
			return;
		}
		Options options = minecraft.options;
		if (options.keyUp.isDown()
			|| options.keyDown.isDown()
			|| options.keyLeft.isDown()
			|| options.keyRight.isDown()
			|| options.keyJump.isDown()
			|| options.keyShift.isDown()
			|| options.keySprint.isDown()
			|| options.keyAttack.isDown()
			|| options.keyUse.isDown()
			|| options.keyInventory.isDown()) {
			onFirstInput(minecraft);
		}
	}

	public static void onAnyPercentComplete() {
		if (status == TimerStatus.RUNNING) {
			finalElapsedNanos = Math.max(0L, System.nanoTime() - startNanos);
			status = TimerStatus.COMPLETED;
		}
	}

	public static String formattedRta() {
		long totalNanos = switch (status) {
			case RUNNING -> Math.max(0L, System.nanoTime() - startNanos);
			case COMPLETED -> finalElapsedNanos;
			default -> 0L;
		};
		long ms = Math.max(0L, totalNanos / 1_000_000L);
		long hours = ms / 3_600_000L;
		long minutes = (ms / 60_000L) % 60L;
		long seconds = (ms / 1_000L) % 60L;
		long millis = ms % 1_000L;
		if (hours > 0L) {
			return String.format(Locale.ROOT, "%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
		}
		return String.format(Locale.ROOT, "%02d:%02d.%03d", minutes, seconds, millis);
	}

	private static void resetAndArm(String worldKey) {
		activeWorldKey = worldKey;
		startNanos = 0L;
		finalElapsedNanos = 0L;
		status = TimerStatus.IDLE;
	}

	private static void clear() {
		activeWorldKey = null;
		startNanos = 0L;
		finalElapsedNanos = 0L;
		status = TimerStatus.NONE;
	}
}
