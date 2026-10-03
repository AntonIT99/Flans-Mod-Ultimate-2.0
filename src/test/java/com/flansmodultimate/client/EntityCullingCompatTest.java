package com.flansmodultimate.client;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The whitelist fields of EntityCulling's mod instance across its releases. */
class EntityCullingCompatTest
{
    private static final List<String> UNCULLED = List.of("bullet", "plane");
    private static final List<String> TICKED = List.of("vehicle", "seat");

    /** April 2026 on. Like the real instance, the fields are inherited from the base class. */
    public static class CurrentBase
    {
        public Set<Object> entityWhitelist = new HashSet<>();
        public Set<Object> tickCullWhitelists = new HashSet<>();
    }

    public static class Current extends CurrentBase {}

    /** June 2025 to April 2026. */
    public static class Mid2025
    {
        public Set<Object> entityWhitelist = new HashSet<>();
        public Set<Object> tickCullWhistelist = new HashSet<>();
    }

    /** Before June 2025: the tick list exists but is never read. */
    public static class Early2025
    {
        public Set<Object> entityWhistelist = new HashSet<>();
        public Set<Object> tickCullWhistelist = new HashSet<>();
    }

    public static class Unknown {}

    @Test
    void currentReleaseKeepsTickedTypesCullable() throws ReflectiveOperationException
    {
        Current instance = new Current();
        EntityCullingCompat.exempt(instance, UNCULLED, TICKED);
        assertEquals(Set.copyOf(UNCULLED), instance.entityWhitelist);
        assertEquals(Set.copyOf(TICKED), instance.tickCullWhitelists);
    }

    @Test
    void mid2025ReleaseUsesTheMisspelledTickList() throws ReflectiveOperationException
    {
        Mid2025 instance = new Mid2025();
        EntityCullingCompat.exempt(instance, UNCULLED, TICKED);
        assertEquals(Set.copyOf(UNCULLED), instance.entityWhitelist);
        assertEquals(Set.copyOf(TICKED), instance.tickCullWhistelist);
    }

    @Test
    void earlyReleaseExemptsTickedTypesThroughTheEntityList() throws ReflectiveOperationException
    {
        Early2025 instance = new Early2025();
        EntityCullingCompat.exempt(instance, UNCULLED, TICKED);
        assertEquals(Set.of("bullet", "plane", "vehicle", "seat"), instance.entityWhistelist);
        assertTrue(instance.tickCullWhistelist.isEmpty());
    }

    @Test
    void releaseWithoutEntityWhitelistIsRejected()
    {
        assertThrows(NoSuchFieldException.class, () -> EntityCullingCompat.exempt(new Unknown(), UNCULLED, TICKED));
    }
}
