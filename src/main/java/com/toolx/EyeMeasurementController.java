package com.toolx;

import com.mojang.blaze3d.platform.InputConstants;
import com.toolx.gui.EyeMeasurementHud;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class EyeMeasurementController {
	public static final float MEASURE_VFOV = 2.0F;

	private static final double SIGMA_NORMAL_DEG = 0.10;
	private static final double SIGMA_STRETCH_DEG = 0.03;
	private static final double SIGMA_BOAT_DEG = 0.005;
	private static final double BOAT_GRID_STEP_DEG = 1.40625;
	private static final double BOAT_GRID_TOLERANCE_DEG = 0.01;
	private static final float MAX_MEASURE_PITCH_DEG = 31.0F;
	private static final double BIOME_SNAP_CHUNKS = 7.0;
	private static final double MAX_BIOME_SNAP_CHUNKS = 10.0;
	private static final double SAME_SPOT_RADIUS_SQ = 2.0 * 2.0;
	private static final double RESET_DISTANCE_SQ = 1500.0 * 1500.0;
	private static final int MAX_RAYS = 8;
	private static final double MAX_MULTI_RAY_ERROR_DEG = 1.5;
	private static final double CONE_HALF_ANGLE_DEG = 1.5;
	private static final double CONE_SCAN_STEP = 0.7;
	private static final int MAX_CONE_HALF_WIDTH = 120;
	private static final int INTERSECTION_SEARCH_RADIUS = 6;
	private static final int RADIAL_SAMPLES = 5;

	private record StrongholdRing(int count, double rMin, double rMax) {
	}

	public record EyeRay(double startX, double startZ, double dirX, double dirZ, double sigmaDeg) {
	}

	public record Result(
		int owX,
		int owZ,
		int netherX,
		int netherZ,
		double accuracy,
		double maxErrorDeg
	) {
	}

	private record CandidateScore(int chunkX, int chunkZ, double logScore, double maxErrorDeg) {
	}

	private static final StrongholdRing[] RINGS = {
		new StrongholdRing(3, 88.0, 168.0),
		new StrongholdRing(6, 280.0, 360.0),
		new StrongholdRing(10, 472.0, 552.0),
		new StrongholdRing(15, 664.0, 744.0),
		new StrongholdRing(21, 856.0, 936.0),
		new StrongholdRing(28, 1048.0, 1128.0),
		new StrongholdRing(36, 1240.0, 1320.0),
		new StrongholdRing(9, 1432.0, 1512.0)
	};

	private static boolean measuring;
	private static final List<EyeRay> RECORDED_RAYS = new ArrayList<>();
	private static @Nullable Result lastResult;

	private EyeMeasurementController() {
	}

	public static boolean isMeasuring() {
		return measuring;
	}

	public static double mousePitchScale(@Nullable Minecraft minecraft) {
		double baseFov = (minecraft != null && minecraft.options != null) ? minecraft.options.fov().get() : 70.0;
		return MEASURE_VFOV / Math.max(30.0, baseFov);
	}

	public static double mouseYawScale(@Nullable Minecraft minecraft) {
		return mousePitchScale(minecraft);
	}

	public static void reset() {
		measuring = false;
		RECORDED_RAYS.clear();
		lastResult = null;
		EyeMeasurementHud.cancelDrag();
	}

	public static @Nullable Result lastResult() {
		return lastResult;
	}

	public static void onClientTick(@Nullable Minecraft minecraft) {
		if (!measuring) {
			return;
		}
		if (minecraft == null
			|| minecraft.level == null
			|| minecraft.player == null
			|| minecraft.gui == null
			|| !Level.OVERWORLD.equals(minecraft.level.dimension())
			|| !isAllowedWhileMeasuring(minecraft.gui.screen())) {
			measuring = false;
		}
	}

	public static void handleMeasureKey(@Nullable Minecraft minecraft, InputConstants.Key pressedKey) {
		if (minecraft == null
			|| minecraft.level == null
			|| minecraft.player == null
			|| minecraft.gui == null
			|| minecraft.gui.overlay() != null
			|| !isAllowedScreen(minecraft.gui.screen())
			|| !Level.OVERWORLD.equals(minecraft.level.dimension())) {
			return;
		}

		InputConstants.Key boundKey = ToolXConfig.get().eyeMeasurementKey();
		if (boundKey.equals(InputConstants.UNKNOWN) || !boundKey.equals(pressedKey)) {
			return;
		}

		if (!measuring) {
			if (PieChartController.isRadarActive()) {
				return;
			}
			measuring = true;
		} else {
			measuring = false;
		}
	}

	public static void onCopyLocation(@Nullable Minecraft minecraft) {
		if (ToolXConfig.get().eyeMeasurementKey().equals(InputConstants.UNKNOWN)
			|| minecraft == null
			|| minecraft.level == null
			|| minecraft.player == null
			|| minecraft.gui == null
			|| minecraft.gui.overlay() != null
			|| !isAllowedScreen(minecraft.gui.screen())
			|| !Level.OVERWORLD.equals(minecraft.level.dimension())) {
			return;
		}

		if (minecraft.player.getXRot() > MAX_MEASURE_PITCH_DEG) {
			return;
		}

		boolean usedStretch = measuring;
		measuring = false;

		boolean boatAligned = usedStretch
			&& minecraft.player.getVehicle() instanceof AbstractBoat boat
			&& isBoatGridAligned(boat.getYRot());
		double sigma = boatAligned ? SIGMA_BOAT_DEG : (usedStretch ? SIGMA_STRETCH_DEG : SIGMA_NORMAL_DEG);

		double x = minecraft.player.getX();
		double z = minecraft.player.getZ();
		double yawRad = Math.toRadians(minecraft.player.getYRot());
		double dirX = -Math.sin(yawRad);
		double dirZ = Math.cos(yawRad);
		recordAndSolve(new EyeRay(x, z, dirX, dirZ, sigma));
	}

	private static boolean isAllowedScreen(@Nullable Screen screen) {
		return screen == null || (screen instanceof PauseScreen pause && !pause.showsPauseMenu());
	}

	private static boolean isAllowedWhileMeasuring(@Nullable Screen screen) {
		return isAllowedScreen(screen) || screen instanceof ChatScreen;
	}

	public static boolean isBoatGridAligned(double boatYawDeg) {
		return Math.abs(Math.IEEEremainder(boatYawDeg, BOAT_GRID_STEP_DEG)) <= BOAT_GRID_TOLERANCE_DEG;
	}

	private static void recordAndSolve(EyeRay newRay) {
		int matchedSpotIndex = -1;
		for (int i = 0; i < RECORDED_RAYS.size(); i++) {
			EyeRay existing = RECORDED_RAYS.get(i);
			if (distSq(newRay.startX(), newRay.startZ(), existing.startX(), existing.startZ()) <= SAME_SPOT_RADIUS_SQ) {
				matchedSpotIndex = i;
				break;
			}
		}

		if (matchedSpotIndex >= 0) {
			RECORDED_RAYS.set(matchedSpotIndex, newRay);
		} else {
			if (!RECORDED_RAYS.isEmpty()) {
				EyeRay first = RECORDED_RAYS.getFirst();
				if (distSq(newRay.startX(), newRay.startZ(), first.startX(), first.startZ()) > RESET_DISTANCE_SQ) {
					RECORDED_RAYS.clear();
				} else if (RECORDED_RAYS.size() >= MAX_RAYS) {
					RECORDED_RAYS.removeFirst();
				}
			}
			RECORDED_RAYS.add(newRay);
		}

		Result solved = solve(RECORDED_RAYS);
		double maxSigma = 0.0;
		for (EyeRay ray : RECORDED_RAYS) {
			if (ray.sigmaDeg() > maxSigma) {
				maxSigma = ray.sigmaDeg();
			}
		}
		double consistencyLimit = Math.min(MAX_MULTI_RAY_ERROR_DEG, Math.max(0.35, 8.0 * maxSigma));
		if (RECORDED_RAYS.size() >= 2 && (solved == null || solved.maxErrorDeg() > consistencyLimit)) {
			RECORDED_RAYS.clear();
			RECORDED_RAYS.add(newRay);
			solved = solve(RECORDED_RAYS);
		}

		if (solved != null) {
			lastResult = solved;
		}
	}

	public static @Nullable Result solve(@Nullable List<EyeRay> rays) {
		if (rays == null || rays.isEmpty()) {
			return null;
		}

		IntersectionPoint intersection = rays.size() >= 2 ? findBestIntersection(rays) : null;

		Set<Long> candidateKeys = new HashSet<>();
		collectRingConeCandidates(rays.getLast(), candidateKeys);
		if (intersection != null) {
			int centerCx = (int) Math.round(intersection.x() / 16.0);
			int centerCz = (int) Math.round(intersection.z() / 16.0);
			for (int dx = -INTERSECTION_SEARCH_RADIUS; dx <= INTERSECTION_SEARCH_RADIUS; dx++) {
				for (int dz = -INTERSECTION_SEARCH_RADIUS; dz <= INTERSECTION_SEARCH_RADIUS; dz++) {
					candidateKeys.add(packChunk(centerCx + dx, centerCz + dz));
				}
			}
		}

		if (candidateKeys.isEmpty()) {
			return null;
		}

		EyeRay first = rays.getFirst();
		double firstChunkX = first.startX() / 16.0;
		double firstChunkZ = first.startZ() / 16.0;
		double firstOriginRadius = Math.hypot(firstChunkX, firstChunkZ);

		List<CandidateScore> scores = new ArrayList<>();
		for (long key : candidateKeys) {
			int cx = unpackChunkX(key);
			int cz = unpackChunkZ(key);
			double targetX = cx * 16.0;
			double targetZ = cz * 16.0;

			boolean inFrontOfAll = true;
			double logLikelihood = 0.0;
			double maxErr = 0.0;
			for (EyeRay ray : rays) {
				double dx = targetX - ray.startX();
				double dz = targetZ - ray.startZ();
				double forward = dx * ray.dirX() + dz * ray.dirZ();
				if (forward <= 0.0) {
					inFrontOfAll = false;
					break;
				}
				double errDeg = Math.toDegrees(Math.abs(Math.atan2(ray.dirX() * dz - ray.dirZ() * dx, forward)));
				if (errDeg > maxErr) {
					maxErr = errDeg;
				}
				double ratio = errDeg / ray.sigmaDeg();
				logLikelihood -= 0.5 * ratio * ratio;
			}

			if (!inFrontOfAll) {
				continue;
			}

			double prior = computeChunkPrior(cx, cz, firstChunkX, firstChunkZ, firstOriginRadius);
			if (prior <= 0.0) {
				continue;
			}
			scores.add(new CandidateScore(cx, cz, Math.log(prior) + logLikelihood, maxErr));
		}

		if (scores.isEmpty()) {
			return null;
		}

		scores.sort(Comparator.comparingDouble(CandidateScore::logScore).reversed());
		CandidateScore best = scores.getFirst();
		double maxLog = best.logScore();

		double totalWeight = 0.0;
		for (CandidateScore score : scores) {
			totalWeight += Math.exp(score.logScore() - maxLog);
		}
		double accuracy = totalWeight > 0.0 ? Math.clamp(1.0 / totalWeight, 0.0, 1.0) : 1.0;

		int owChunkX = best.chunkX();
		int owChunkZ = best.chunkZ();
		int owX = owChunkX * 16 + 4;
		int owZ = owChunkZ * 16 + 4;
		int netherX = owChunkX * 2;
		int netherZ = owChunkZ * 2;
		return new Result(
			owX,
			owZ,
			netherX,
			netherZ,
			accuracy,
			best.maxErrorDeg()
		);
	}

	private record IntersectionPoint(double x, double z) {
	}

	private static @Nullable IntersectionPoint findBestIntersection(List<EyeRay> rays) {
		double bestAbsDet = 0.0;
		double bestX = 0.0;
		double bestZ = 0.0;
		for (int i = 0; i < rays.size(); i++) {
			EyeRay a = rays.get(i);
			for (int j = i + 1; j < rays.size(); j++) {
				EyeRay b = rays.get(j);
				double det = a.dirX() * b.dirZ() - a.dirZ() * b.dirX();
				double absDet = Math.abs(det);
				if (absDet < 1e-4 || absDet <= bestAbsDet) {
					continue;
				}
				double dx = b.startX() - a.startX();
				double dz = b.startZ() - a.startZ();
				double tA = (dx * b.dirZ() - dz * b.dirX()) / det;
				double tB = (dx * a.dirZ() - dz * a.dirX()) / det;
				if (tA > 0.0 && tB > 0.0 && tA < 30000.0 && tB < 30000.0) {
					bestAbsDet = absDet;
					bestX = a.startX() + tA * a.dirX();
					bestZ = a.startZ() + tA * a.dirZ();
				}
			}
		}
		return bestAbsDet > 0.0 ? new IntersectionPoint(bestX, bestZ) : null;
	}

	private static void collectRingConeCandidates(EyeRay ray, Set<Long> outKeys) {
		double originX = ray.startX() / 16.0;
		double originZ = ray.startZ() / 16.0;
		double tanHalfAngle = Math.tan(Math.toRadians(CONE_HALF_ANGLE_DEG));
		double perpX = -ray.dirZ();
		double perpZ = ray.dirX();

		for (StrongholdRing ring : RINGS) {
			double outerR = ring.rMax() + MAX_BIOME_SNAP_CHUNKS;
			double innerR = Math.max(0.0, ring.rMin() - MAX_BIOME_SNAP_CHUNKS);
			double bDot = originX * ray.dirX() + originZ * ray.dirZ();
			double cOuter = originX * originX + originZ * originZ - outerR * outerR;
			double discOuter = bDot * bDot - cOuter;
			if (discOuter < 0.0) {
				continue;
			}
			double sqrtOuter = Math.sqrt(discOuter);
			double tMax = -bDot + sqrtOuter;
			if (tMax <= 0.0) {
				continue;
			}
			double tMin = Math.max(0.5, -bDot - sqrtOuter);

			double cInner = originX * originX + originZ * originZ - innerR * innerR;
			double discInner = bDot * bDot - cInner;
			if (discInner > 0.0) {
				double sqrtInner = Math.sqrt(discInner);
				double tHoleStart = -bDot - sqrtInner;
				double tHoleEnd = -bDot + sqrtInner;
				scanRaySegment(originX, originZ, ray.dirX(), ray.dirZ(), perpX, perpZ, tanHalfAngle, tMin, Math.min(tMax, tHoleStart), outKeys);
				scanRaySegment(originX, originZ, ray.dirX(), ray.dirZ(), perpX, perpZ, tanHalfAngle, Math.max(tMin, tHoleEnd), tMax, outKeys);
			} else {
				scanRaySegment(originX, originZ, ray.dirX(), ray.dirZ(), perpX, perpZ, tanHalfAngle, tMin, tMax, outKeys);
			}
		}
	}

	private static void scanRaySegment(
		double originX,
		double originZ,
		double dirX,
		double dirZ,
		double perpX,
		double perpZ,
		double tanHalfAngle,
		double tStart,
		double tEnd,
		Set<Long> outKeys
	) {
		if (tEnd <= tStart) {
			return;
		}
		for (double t = tStart; t <= tEnd + 0.5; t += CONE_SCAN_STEP) {
			double clampedT = Math.min(t, tEnd);
			double cxBase = originX + clampedT * dirX;
			double czBase = originZ + clampedT * dirZ;
			int halfSpan = Math.min(MAX_CONE_HALF_WIDTH, (int) Math.ceil(clampedT * tanHalfAngle) + 2);
			for (int s = -halfSpan; s <= halfSpan; s++) {
				int cx = (int) Math.round(cxBase + s * perpX);
				int cz = (int) Math.round(czBase + s * perpZ);
				outKeys.add(packChunk(cx, cz));
			}
		}
	}

	private static double computeChunkPrior(int cx, int cz, double originCx, double originCz, double originR) {
		double r = Math.hypot(cx, cz);
		StrongholdRing ownerRing = null;
		double bestOwnerDensity = 0.0;
		double totalRingDensity = 0.0;
		for (StrongholdRing ring : RINGS) {
			double radialDensity = ringRadialDensity(r, ring);
			if (radialDensity > 0.0) {
				double circumference = Math.max(1.0, 2.0 * Math.PI * Math.max(r, 1.0));
				double contrib = (ring.count() * radialDensity) / circumference;
				totalRingDensity += contrib;
				if (contrib > bestOwnerDensity) {
					bestOwnerDensity = contrib;
					ownerRing = ring;
				}
			}
		}

		if (totalRingDensity <= 0.0) {
			return 0.0;
		}

		double distFromOrigin = Math.hypot(cx - originCx, cz - originCz);
		double nearestSurvival = 1.0;
		for (StrongholdRing ring : RINGS) {
			int otherCount = (ring == ownerRing) ? (ring.count() - 1) : ring.count();
			if (otherCount <= 0) {
				continue;
			}
			double avgFracInside = 0.0;
			for (int s = 0; s < RADIAL_SAMPLES; s++) {
				double sampleR = ring.rMin() + (ring.rMax() - ring.rMin()) * (s + 0.5) / RADIAL_SAMPLES;
				avgFracInside += circleIntersectionFraction(originR, sampleR, distFromOrigin);
			}
			avgFracInside /= RADIAL_SAMPLES;
			double ringSurvival = Math.pow(Math.max(0.0, 1.0 - avgFracInside), otherCount);
			nearestSurvival *= Math.max(1e-4, ringSurvival);
		}

		return Math.max(1e-12, totalRingDensity * nearestSurvival);
	}

	private static double circleIntersectionFraction(double d0, double rRing, double maxDist) {
		if (maxDist <= Math.abs(rRing - d0)) {
			return 0.0;
		}
		if (maxDist >= rRing + d0) {
			return 1.0;
		}
		if (d0 <= 1e-6 || rRing <= 1e-6) {
			return maxDist >= rRing ? 1.0 : 0.0;
		}
		double cosAlpha = Math.clamp((rRing * rRing + d0 * d0 - maxDist * maxDist) / (2.0 * rRing * d0), -1.0, 1.0);
		return Math.acos(cosAlpha) / Math.PI;
	}

	private static double ringRadialDensity(double r, StrongholdRing ring) {
		double span = ring.rMax() - ring.rMin();
		if (span <= 0.0) {
			return 0.0;
		}
		if (r >= ring.rMin() && r <= ring.rMax()) {
			return 1.0 / span;
		}
		double delta = r < ring.rMin() ? (ring.rMin() - r) : (r - ring.rMax());
		if (delta > MAX_BIOME_SNAP_CHUNKS) {
			return 0.0;
		}
		if (delta <= BIOME_SNAP_CHUNKS) {
			double norm = delta / (BIOME_SNAP_CHUNKS * 0.4);
			return (1.0 / span) * Math.exp(-0.5 * norm * norm) * 0.65;
		}
		double norm = delta / (BIOME_SNAP_CHUNKS * 0.45);
		return (1.0 / span) * Math.exp(-0.5 * norm * norm) * 0.25;
	}

	private static double distSq(double ax, double az, double bx, double bz) {
		double dx = ax - bx;
		double dz = az - bz;
		return dx * dx + dz * dz;
	}

	private static long packChunk(int cx, int cz) {
		return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
	}

	private static int unpackChunkX(long key) {
		return (int) (key >> 32);
	}

	private static int unpackChunkZ(long key) {
		return (int) key;
	}
}
