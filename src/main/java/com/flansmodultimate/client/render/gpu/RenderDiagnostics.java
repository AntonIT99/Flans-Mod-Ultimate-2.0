package com.flansmodultimate.client.render.gpu;

/** Opt-in render-thread counters. No timers or per-draw allocations. */
public final class RenderDiagnostics
{
    static boolean enabled;
    static long hits, misses, uploads, uploadBytes, evictions, throttled;
    static long draws, ranges, vertices, submittedParts, culledParts, fallbackVertices, excludedScopes;

    private RenderDiagnostics() {}

    public static void start() { reset(); enabled = true; }
    public static void stop() { enabled = false; }
    public static void reset()
    {
        hits = misses = uploads = uploadBytes = evictions = throttled = 0;
        draws = ranges = vertices = submittedParts = culledParts = fallbackVertices = excludedScopes = 0;
    }

    public static String report()
    {
        return "Flan render counters (" + (enabled ? "recording" : "stopped") + "): cache hits/misses "
            + hits + "/" + misses + ", uploads " + uploads + " (" + uploadBytes / 1024 + " KiB), evictions "
            + evictions + ", upload deferrals " + throttled + "; GPU calls/ranges " + draws + "/" + ranges
            + ", GPU vertices " + vertices + "; parts/size-culled " + submittedParts + "/" + culledParts
            + ", CPU fallback vertices " + fallbackVertices + ", excluded scopes " + excludedScopes
            + ". " + GpuModelCache.status();
    }
}
