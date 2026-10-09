package com.flansmodultimate.client.render.gpu;

import com.flansmodultimate.platform.render.VertexWriterPlatform;
import com.flansmodultimate.util.FlansLog;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Opt-in render-thread counters. No allocations; clocks are read only while recording, per driveable and per GPU draw. */
public final class RenderDiagnostics
{
    static boolean enabled;
    static long hits, misses, uploads, uploadBytes, evictions, workingSetEvictions, throttled;
    static long draws, immediateDraws, paletteEntries, stateSetups, ranges, vertices, submittedParts, culledParts, fallbackVertices, excludedScopes;
    private static long shadowDriveables, shadowImpostors, shadowParts, shadowCulledParts, shadowEntitiesSkipped;
    private static long bulkWrites, bulkVertices;
    private static long frames, startNanos, stopNanos;
    private static long driveables, driveableNanos, drawNanos, lookupNanos, paletteNanos, glDrawNanos;
    /** Caller stacks of geometry changes logged per recording, to name what keeps invalidating geometry. */
    private static final int LOGGED_GEOMETRY_CHANGES = 3;
    private static long geometryChanges, faceRevalidations, scannedRevalidations, cpuParts, cpuVertices;
    private static final Map<String, long[]> cpuPartReasons = new LinkedHashMap<>();
    private static long cachedArrayParts, uncachedArrayParts;
    private static final Map<String, long[]> uncachedArrayReasons = new LinkedHashMap<>();

    private RenderDiagnostics()
    {}

    public static void start()
    {
        reset();
        enabled = true;
    }

    public static void stop()
    {
        if (enabled)
            stopNanos = System.nanoTime();
        enabled = false;
    }

    public static void reset()
    {
        hits = misses = uploads = uploadBytes = evictions = workingSetEvictions = throttled = 0;
        draws = immediateDraws = paletteEntries = stateSetups = ranges = vertices = submittedParts = culledParts = fallbackVertices = excludedScopes = 0;
        shadowDriveables = shadowImpostors = shadowParts = shadowCulledParts = shadowEntitiesSkipped = 0;
        bulkWrites = bulkVertices = 0;
        frames = peakBytes = peakMeshes = 0;
        driveables = driveableNanos = drawNanos = lookupNanos = paletteNanos = glDrawNanos = 0;
        geometryChanges = faceRevalidations = scannedRevalidations = cpuParts = cpuVertices = 0;
        cpuPartReasons.clear();
        cachedArrayParts = uncachedArrayParts = 0;
        uncachedArrayReasons.clear();
        startNanos = stopNanos = System.nanoTime();
    }

    private static long peakBytes, peakMeshes;

    /** Resident size after an upload; the peak shows saturation even after evictions or a shrink. */
    static void countResident(long bytes, int meshes)
    {
        if (!enabled)
            return;
        peakBytes = Math.max(peakBytes, bytes);
        peakMeshes = Math.max(peakMeshes, meshes);
    }

    static String peakUsage(long maximumBytes)
    {
        if (peakBytes == 0)
            return ""; // No uploads recorded: the current figure is the peak
        return String.format(Locale.ROOT, "; recorded peak %d meshes, %d KiB (%.0f%%)", peakMeshes, peakBytes / 1024, 100D * peakBytes / maximumBytes);
    }

    public static boolean recording()
    {
        return enabled;
    }

    /** A change of the global geometry epoch, which makes every part re-validate its polygons once. */
    static void countGeometryChange()
    {
        if (!enabled)
            return;
        if (geometryChanges++ < LOGGED_GEOMETRY_CHANGES)
            FlansLog.log.info("Flan render counters: geometry change while recording", new Throwable("geometry change caller"));
    }

    /** A part re-validating its polygons instead of reusing them; scanned when it also checks every vertex. */
    public static void countFaceRevalidation(boolean scanned)
    {
        if (!enabled)
            return;
        faceRevalidations++;
        if (scanned)
            scannedRevalidations++;
    }

    /** Parts of one array drawn from its compact cache. */
    public static void countCachedArrayParts(int parts)
    {
        if (enabled)
            cachedArrayParts += parts;
    }

    /** A part of a cached array drawn by the normal per-part path instead, and why. */
    public static void countUncachedArrayPart(String reason)
    {
        if (!enabled)
            return;
        uncachedArrayParts++;
        uncachedArrayReasons.computeIfAbsent(reason == null ? "unknown" : reason, ignored -> new long[1])[0]++;
    }

    /** A part offered the GPU path that still drew on the CPU, and why. */
    public static void countCpuPart(String reason)
    {
        if (!enabled)
            return;
        cpuParts++;
        cpuPartReasons.computeIfAbsent(reason, ignored -> new long[1])[0]++;
    }

    /** A vertex written directly to the CPU fallback by a part the GPU path could not take. */
    static void countCpuVertex()
    {
        if (enabled)
            cpuVertices++;
    }

    private static String cpuReasons(double frames)
    {
        return reasons(cpuPartReasons, frames);
    }

    private static String reasons(Map<String, long[]> counts, double frames)
    {
        StringBuilder text = new StringBuilder();
        for (var entry : counts.entrySet())
            text.append(text.length() == 0 ? " (" : ", ").append(entry.getKey()).append(' ').append(String.format(Locale.ROOT, "%.1f", entry.getValue()[0] / frames));
        return text.length() == 0 ? "" : text.append(')').toString();
    }

    /** A clock reading to pass to a matching count, or 0 while not recording. */
    public static long startTimer()
    {
        return enabled ? System.nanoTime() : 0L;
    }

    /** CPU time of one driveable render call: animation state, LOD, part traversal and its GPU draws. */
    public static void countDriveableTime(long started)
    {
        if (!enabled || started == 0L)
            return;
        driveables++;
        driveableNanos += System.nanoTime() - started;
    }

    /** CPU time of one GPU model draw: mesh lookup, uploads, flushes, state setup and the GL calls. */
    /** Mesh cache lookup, including any upload, of one GPU draw. */
    static void countLookupTime(long started)
    {
        if (enabled && started != 0L)
            lookupNanos += System.nanoTime() - started;
    }

    /** Palette packing and upload of one GPU draw; returns a clock reading for the GL draw that follows. */
    static long countPaletteTime(long started)
    {
        if (!enabled || started == 0L)
            return 0L;
        long now = System.nanoTime();
        paletteNanos += now - started;
        return now;
    }

    /** Vertex array bind and draw calls of one GPU draw. */
    static void countGlDrawTime(long started)
    {
        if (enabled && started != 0L)
            glDrawNanos += System.nanoTime() - started;
    }

    /** Size-culled indices drawn anyway between visible ranges of a batch; 0 disables bridging. */
    public static int bridgedGap()
    {
        return VisibleRanges.bridgedGap;
    }

    public static void setBridgedGap(int indices)
    {
        VisibleRanges.bridgedGap = Math.max(0, indices);
    }

    static void countDrawTime(long started)
    {
        if (enabled && started != 0L)
            drawNanos += System.nanoTime() - started;
    }

    /** One rendered frame while recording, the divisor of the per-frame averages. */
    static void countFrame()
    {
        if (enabled)
            frames++;
    }

    /** A driveable in a shader pack's shadow pass, drawn or left out because the view shows its impostor. */
    public static void countShadowDriveable(boolean impostor)
    {
        if (!enabled)
            return;
        if (impostor)
            shadowImpostors++;
        else
            shadowDriveables++;
    }

    /** A rigid part sized for a shader pack's shadow pass. */
    public static void countShadowPart(boolean culled)
    {
        if (!enabled)
            return;
        shadowParts++;
        if (culled)
            shadowCulledParts++;
    }

    /** A projectile or proxy entity left out of a shader pack's shadow pass. */
    public static void countShadowEntitySkipped()
    {
        if (enabled)
            shadowEntitiesSkipped++;
    }

    /** A batch of model vertices handed to Sodium's or Embeddium's bulk vertex writer. */
    public static void countBulkWrite(int vertexCount)
    {
        if (!enabled)
            return;
        bulkWrites++;
        bulkVertices += vertexCount;
    }

    private static String bulkWriter()
    {
        String renderer = VertexWriterPlatform.rendererName();
        return renderer == null ? "no Sodium or Embeddium writer" : renderer;
    }

    private static String perFrame()
    {
        double seconds = ((enabled ? System.nanoTime() : stopNanos) - startNanos) / 1E9;
        if (frames == 0)
            return String.format(Locale.ROOT, "%.1f s, no frames", seconds);
        double n = frames;
        return String.format(Locale.ROOT,
            "%d frames in %.1f s (%.0f FPS, %.2f ms); per frame: %.1f driveables using %.2f ms of CPU, "
                + "%.2f ms of it and of held guns in GPU model draw submission (mesh lookup %.2f, palette upload %.2f, GL draws %.2f); "
                + "GPU calls %.1f (%.1f render state setups, %.1f palette entries per call), ranges %.1f (bridging gaps up to %d indices), "
                + "GPU vertices %.0f, parts %.0f (%.0f size-culled), CPU fallback vertices %.0f, uploads %.2f; " + "part arrays: %.0f parts cached, %.0f by the normal path%s; "
                + "CPU-path parts %.1f%s writing %.0f vertices; geometry changes %.2f, part re-validations %.0f " + "(%.0f scanning every vertex)",
            frames, seconds, seconds > 0 ? n / seconds : 0, seconds * 1000 / n, driveables / n, driveableNanos / 1E6 / n, drawNanos / 1E6 / n, lookupNanos / 1E6 / n, paletteNanos / 1E6 / n,
            glDrawNanos / 1E6 / n, draws / n, stateSetups / n, (double) paletteEntries / Math.max(1, immediateDraws), ranges / n, VisibleRanges.bridgedGap, vertices / n, submittedParts / n,
            culledParts / n, fallbackVertices / n, uploads / n, cachedArrayParts / n, uncachedArrayParts / n, reasons(uncachedArrayReasons, n), cpuParts / n, cpuReasons(n), cpuVertices / n,
            geometryChanges / n, faceRevalidations / n, scannedRevalidations / n);
    }

    public static String report()
    {
        return "Flan render counters (" + (enabled ? "recording" : "stopped") + ", " + perFrame() + "): cache hits/misses " + hits + "/" + misses + ", uploads " + uploads + " (" + uploadBytes / 1024
            + " KiB), evictions " + evictions + " (" + workingSetEvictions + " of meshes drawn in the last two frames), upload deferrals " + throttled + "; GPU calls/ranges " + draws + "/" + ranges
            + ", GPU vertices " + vertices + "; parts/size-culled " + submittedParts + "/" + culledParts + ", CPU fallback vertices " + fallbackVertices + ", excluded scopes " + excludedScopes
            + "; shadow pass driveables drawn/impostors left out " + shadowDriveables + "/" + shadowImpostors + ", parts/size-culled " + shadowParts + "/" + shadowCulledParts
            + ", projectiles and proxies left out " + shadowEntitiesSkipped + "; bulk vertex writes/vertices " + bulkWrites + "/" + bulkVertices + " (" + bulkWriter() + ")" + ". "
            + GpuModelCache.status();
    }
}
