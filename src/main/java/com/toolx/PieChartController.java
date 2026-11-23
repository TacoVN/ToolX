package com.toolx;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class PieChartController {
	private static final long RESET_COOLDOWN_MS = 150L;

	private static int lastChunkX;
	private static int lastChunkZ;
	private static boolean hasLastChunk;
	private static @Nullable ResourceKey<Level> lastDimension;
	private static boolean pendingReset;
	private static long lastResetTimeMs;

	private PieChartController() {
	}

	public static void onClientTick(Minecraft minecraft) {
		if (!ToolXConfig.get().pieChartAutoReset) {
			hasLastChunk = false;
			lastDimension = null;
			pendingReset = false;
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
			long now = Util.getMillis();
			if (now - lastResetTimeMs >= RESET_COOLDOWN_MS) {
				pendingReset = true;
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

	public static void resetTracking() {
		hasLastChunk = false;
		lastDimension = null;
		pendingReset = false;
		lastResetTimeMs = 0L;
	}
}
