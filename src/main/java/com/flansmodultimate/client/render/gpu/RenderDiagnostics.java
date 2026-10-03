package com.flansmodultimate.client.render.gpu;

/** Opt-in render-thread counters. No timers or per-draw allocations. */
public final class RenderDiagnostics
{
    static boolean enabled;
    static long hits, misses, uploads, uploadBytes, evictions, throttled;
    static long draws, ranges, vertices, submittedParts, culledParts, fallbackVertices, excludedScopes;
    private static long shadowDriveables, shadowImpostors, shadowParts, shadowCulledParts, shadowEntitiesSkipped;

    private RenderDiagnostics() {}

    public static void start() { reset(); enabled = true; }
    public static void stop() { enabled = false; }
    public static void reset()
    {
        hits = misses = uploads = uploadBytes = evictions = throttled = 0;
        draws = ranges = vertices = submittedParts = culledParts = fallbackVertices = excludedScopes = 0;
        shadowDriveables = shadowImpostors = shadowParts = shadowCulledParts = shadowEntitiesSkipped = 0;
    }

    /** A driveable in a shader pack's shadow pass, drawn or left out because the view shows its impostor. */
    public static void countShadowDriveable(boolean impostor)
    {
        if (!enabled) return;
        if (impostor) shadowImpostors++;
        else shadowDriveables++;
    }

    /** A rigid part sized for a shader pack's shadow pass. */
    public static void countShadowPart(boolean culled)
    {
        if (!enabled) return;
        shadowParts++;
        if (culled) shadowCulledParts++;
    }

    /** A projectile or proxy entity left out of a shader pack's shadow pass. */
    public static void countShadowEntitySkipped()
    {
        if (enabled) shadowEntitiesSkipped++;
    }

    public static String report()
    {
        return "Flan render counters (" + (enabled ? "recording" : "stopped") + "): cache hits/misses "
            + hits + "/" + misses + ", uploads " + uploads + " (" + uploadBytes / 1024 + " KiB), evictions "
            + evictions + ", upload deferrals " + throttled + "; GPU calls/ranges " + draws + "/" + ranges
            + ", GPU vertices " + vertices + "; parts/size-culled " + submittedParts + "/" + culledParts
            + ", CPU fallback vertices " + fallbackVertices + ", excluded scopes " + excludedScopes
            + "; shadow pass driveables drawn/impostors left out " + shadowDriveables + "/" + shadowImpostors
            + ", parts/size-culled " + shadowParts + "/" + shadowCulledParts
            + ", projectiles and proxies left out " + shadowEntitiesSkipped
            + ". " + GpuModelCache.status();
    }
}
