package com.flansmodultimate.client;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.platform.PlatformEnvironment;
import com.mojang.logging.LogUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Optional EntityCulling integration. EntityCulling hides entities whose culling box is hidden behind blocks, and
 * by default also skips the client tick of every entity that is hidden or was outside the view last frame, keeping
 * only a vanilla entity's position bookkeeping. Its exemption for vehicles with passengers never covers a driveable,
 * whose riders sit on separate seat entities. Several Flan entities do work in their client tick that has to go on
 * while nobody looks at them:
 * <ul>
 *     <li>Driveables interpolate onto the server state or predict their driver's movement, play their engine loops
 *     and emit damage and configured particles. A frozen driveable also keeps the culling box of where it stopped,
 *     so it could stay hidden after driving into view.</li>
 *     <li>Seats and wheels follow their driveable only in their own tick and ignore their movement packets, so their
 *     interaction and hit boxes would be left behind.</li>
 *     <li>Bullets play flyby sounds, emit trails and send the guidance of manually guided missiles.</li>
 *     <li>Grenades move and emit trail and smoke particles, so smoke thrown behind cover would never rise.</li>
 * </ul>
 * Bullets and aircraft are exempt from culling as well. EntityCulling samples culling boxes only every few ticks by
 * default and they outrun them, so they would appear late; aircraft are mostly in open sky anyway, where culling
 * rarely saves any drawing. AA guns and deployed guns need nothing: their culling boxes exceed EntityCulling's size
 * limit, and manned ones have a passenger.
 *
 * <p>EntityCulling has no API for these exemptions. Its entity type whitelists are public fields, extended
 * reflectively, so it is neither a compile nor a runtime dependency. Client only.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EntityCullingCompat
{
    public static final String MOD_ID = "entityculling";
    static final String MOD_BASE = "dev.tr7zw.entityculling.EntityCullingModBase";
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Registers the exemptions once, when loading completes. EntityCulling creates its instance during client setup. */
    public static void init()
    {
        if (!PlatformEnvironment.isModLoaded(MOD_ID))
            return;
        try
        {
            Object instance = Class.forName(MOD_BASE, true, EntityCullingCompat.class.getClassLoader()).getField("instance").get(null);
            if (instance == null)
                throw new IllegalStateException("EntityCulling is not initialized");
            exempt(instance,
                List.of(FlansMod.bulletEntity.get(), FlansMod.planeEntity.get(), ApocalypseContent.flyByPlane.get()),
                List.of(FlansMod.vehicleEntity.get(), FlansMod.mechaEntity.get(), ApocalypseContent.aiMecha.get(),
                    FlansMod.seatEntity.get(), FlansMod.wheelEntity.get(), FlansMod.grenadeEntity.get()));
        }
        catch (ReflectiveOperationException | LinkageError | RuntimeException ex)
        {
            LOGGER.warn("EntityCulling is loaded, but its whitelists are unavailable; Flan's Mod driveables and projectiles may freeze while unseen", ex);
        }
    }

    /**
     * Adds {@code unculled} to EntityCulling's entity whitelist, which exempts from culling and tick culling, and
     * {@code ticked} to its tick culling whitelist. That list is read since June 2025 and was named
     * {@code tickCullWhistelist} until April 2026. Before, it went unread beside a misspelled
     * {@code entityWhistelist}, so ticked types join the entity whitelist there.
     */
    static void exempt(Object instance, Collection<?> unculled, Collection<?> ticked) throws ReflectiveOperationException
    {
        Set<Object> tickWhitelist = whitelist(instance, "tickCullWhitelists");
        Set<Object> entityWhitelist = whitelist(instance, "entityWhitelist");
        if (entityWhitelist == null)
        {
            entityWhitelist = whitelist(instance, "entityWhistelist");
            if (entityWhitelist == null)
                throw new NoSuchFieldException("entityWhitelist");
        }
        else if (tickWhitelist == null)
        {
            tickWhitelist = whitelist(instance, "tickCullWhistelist");
        }
        entityWhitelist.addAll(unculled);
        (tickWhitelist != null ? tickWhitelist : entityWhitelist).addAll(ticked);
    }

    /** The public whitelist field of that name, or null when this EntityCulling release has none. */
    @Nullable
    @SuppressWarnings("unchecked")
    private static Set<Object> whitelist(Object instance, String name) throws IllegalAccessException
    {
        try
        {
            return (Set<Object>)instance.getClass().getField(name).get(instance);
        }
        catch (NoSuchFieldException ex)
        {
            return null;
        }
    }
}
