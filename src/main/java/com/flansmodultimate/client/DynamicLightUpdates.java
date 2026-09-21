package com.flansmodultimate.client;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;

/** Client-owned refresh clock and ownership of temporary light blocks. */
public final class DynamicLightUpdates
{
    public interface Access
    {
        /** Zero for air, 1..15 for a light block, -1 for other/unloaded blocks. */
        int lightAt(long position);
        void setLight(long position, int light);
    }

    private Long2ByteMap placed = new Long2ByteOpenHashMap();
    private int ticksUntilRefresh;

    public static void request(Long2ByteMap requested, long position, int light)
    {
        int clamped = Math.max(0, Math.min(15, light));
        if (clamped > requested.get(position))
            requested.put(position, (byte) clamped);
    }

    public boolean tick(int interval)
    {
        if (ticksUntilRefresh > 0)
            --ticksUntilRefresh;
        if (ticksUntilRefresh > 0)
            return false;
        ticksUntilRefresh = Math.max(1, interval);
        return true;
    }

    public void apply(Long2ByteMap requested, Access access)
    {
        for (Long2ByteMap.Entry previous : placed.long2ByteEntrySet())
        {
            long position = previous.getLongKey();
            if (!requested.containsKey(position) && access.lightAt(position) == previous.getByteValue())
                access.setLight(position, 0);
        }

        Long2ByteMap next = new Long2ByteOpenHashMap();
        for (Long2ByteMap.Entry request : requested.long2ByteEntrySet())
        {
            long position = request.getLongKey();
            int current = access.lightAt(position);
            int desired = request.getByteValue();
            // Never take ownership of existing lights or overwrite a server block update.
            if (current != 0 && (!placed.containsKey(position) || current != placed.get(position)))
                continue;
            if (current != desired)
                access.setLight(position, desired);
            next.put(position, (byte) desired);
        }
        placed = next;
    }

    public void reset()
    {
        placed.clear();
        ticksUntilRefresh = 0;
    }
}
