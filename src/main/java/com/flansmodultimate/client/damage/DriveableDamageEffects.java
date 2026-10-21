package com.flansmodultimate.client.damage;

import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.damage.DamageVisualCurves;
import com.flansmodultimate.common.driveables.parts.PartBodies;
import com.flansmodultimate.common.driveables.parts.PartBody;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.config.ModCommonConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * How damage and destruction look on everything drawn as a driveable: smoke, fire, steam and sparks from damaged
 * parts, the death each kind of machine dies, pieces thrown off, and wrecks burning out. Client only.
 *
 * <p>
 * Driveables report their own part changes and their wrecking. Entities that only wear a part body, such as NPCs drawn
 * as vehicles, are watched here instead: the body they carry on the client already holds the part state their server
 * sends, so their parts smoke, are thrown off and die exactly like a driveable's, with nothing more sent for it.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DriveableDamageEffects
{
    /** What was last seen of each entity wearing a part body. */
    private static final Map<Entity, Watched> WATCHED = new WeakHashMap<>();
    /** Parts thrown off entities that are not driveables, which their models stop drawing. */
    private static final Map<Entity, Set<EnumDriveablePart>> DETACHED = new WeakHashMap<>();
    /** The crackle each burning wreck is playing. */
    private static final Map<Driveable, WreckFireSound> FIRE_SOUNDS = new WeakHashMap<>();

    private static final class Watched
    {
        private final EnumSet<EnumDriveablePart> broken = EnumSet.noneOf(EnumDriveablePart.class);
        private boolean seen;
        private boolean dead;
        private PartBodyEffectSource source;
    }

    /** Gives off the driveable's damage effects for this tick. */
    public static void tick(Driveable driveable)
    {
        DriveableEffectSource source = DriveableEffectSource.of(driveable);
        if (source != null)
            DamageEffectEmitter.tick(source);
        if (driveable.isWreck())
            keepFireSound(driveable);
    }

    /** Shows parts shot off the driveable. */
    public static void partsDestroyed(Driveable driveable, List<EnumDriveablePart> parts)
    {
        DriveableEffectSource source = DriveableEffectSource.of(driveable);
        if (source != null)
            DestructionSequences.partsLost(source, parts);
    }

    /** Plays the driveable's death as it becomes a wreck. */
    public static void wrecked(Driveable driveable)
    {
        DriveableEffectSource source = DriveableEffectSource.of(driveable);
        if (source != null)
            DestructionSequences.start(source);
    }

    /** Shows a wreck blown apart by explosives, just before it is removed. */
    public static void brokenUp(Driveable driveable)
    {
        DriveableEffectSource source = DriveableEffectSource.of(driveable);
        if (source != null)
            DestructionSequences.breakUp(source);
    }

    /** Leaves a scorch mark on the ground below the point. */
    public static void scorch(Vec3 position, float radius)
    {
        DestructionSequences.scorch(position, radius);
    }

    /** @return how hard the driveable's wreck burns now, for its sound and its heat, or 0 when it is no wreck */
    public static float wreckFireStrength(Driveable driveable)
    {
        int age = driveable.getWreckAge();
        if (age < 0)
            return 0F;
        return DamageVisualCurves.wreckFire(age, ModCommonConfig.driveableWreckLifetimeTicks()) * driveable.getWreckLoad().fireStrength();
    }

    /** @return how hot the driveable shows on a thermal sight, from 0 to 1 */
    public static float thermalHeat(Driveable driveable)
    {
        int age = driveable.getWreckAge();
        if (age < 0)
            return 1F;
        return Math.max(DamageVisualCurves.wreckHeat(age, ModCommonConfig.driveableWreckLifetimeTicks()) * (0.5F + 0.5F * driveable.getWreckLoad().fireStrength()), DamageVisualCurves.MIN_WRECK_HEAT);
    }

    private static void keepFireSound(Driveable wreck)
    {
        WreckFireSound playing = FIRE_SOUNDS.get(wreck);
        if (playing != null && !playing.isStopped() || wreckFireStrength(wreck) <= 0F)
            return;
        WreckFireSound sound = new WreckFireSound(wreck);
        FIRE_SOUNDS.put(wreck, sound);
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    /** Advances deaths, pieces and the entities wearing part bodies. Client tick. */
    public static void tickClient()
    {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
        {
            clear();
            return;
        }
        DestructionSequences.tick();
        DetachedPieces.tick();
        watchPartBodies(level);
    }

    /** Draws the pieces thrown off. Render thread, after the entities. */
    public static void render(PoseStack poseStack, Camera camera, float partialTick)
    {
        DetachedPieces.render(poseStack, camera, partialTick);
    }

    /** @return whether a destruction effect has thrown the part off an entity that is not a driveable */
    public static boolean isCosmeticallyDetached(Entity entity, EnumDriveablePart part)
    {
        Set<EnumDriveablePart> parts = DETACHED.get(entity);
        return parts != null && parts.contains(part);
    }

    /** Stops drawing a part a destruction effect has thrown off the entity. */
    static void detach(Entity entity, EnumDriveablePart part)
    {
        if (entity instanceof Driveable driveable)
            driveable.detachCosmetically(part);
        else
            DETACHED.computeIfAbsent(entity, ignored -> EnumSet.noneOf(EnumDriveablePart.class)).add(part);
    }

    /** Forgets every effect in play, as when leaving a world. */
    public static void clear()
    {
        WATCHED.clear();
        DETACHED.clear();
        FIRE_SOUNDS.clear();
        DestructionSequences.clear();
        DetachedPieces.clear();
    }

    private static void watchPartBodies(ClientLevel level)
    {
        for (Entity wearer : PartBodies.wearers(level))
        {
            PartBody body = PartBodies.of(wearer);
            PartBodyEffectSource source = body == null ? null : PartBodyEffectSource.of(wearer, body);
            if (source != null)
                watch(wearer, source);
        }
        findDeaths(level);
    }

    /** Gives off the wearer's damage effects and shows what it has lost since the last tick. */
    private static void watch(Entity wearer, PartBodyEffectSource source)
    {
        Watched watched = WATCHED.computeIfAbsent(wearer, ignored -> new Watched());
        watched.source = source;
        DamageEffectEmitter.tick(source);
        List<EnumDriveablePart> lost = new ArrayList<>();
        for (DriveablePart part : source.parts())
        {
            if (!part.isDestroyed())
            {
                if (watched.broken.remove(part.getType()))
                    restore(wearer, part.getType());
            }
            else if (watched.broken.add(part.getType()))
                lost.add(part.getType());
        }
        // What the body showed when first seen is how it already was, not something happening now.
        if (watched.seen)
        {
            boolean coreLost = lost.remove(EnumDriveablePart.CORE);
            if (!lost.isEmpty())
                DestructionSequences.partsLost(source, lost);
            if (coreLost)
                die(watched);
        }
        watched.seen = true;
    }

    /** The dying are no longer counted as wearers, so their deaths are found here. */
    private static void findDeaths(ClientLevel level)
    {
        Iterator<Map.Entry<Entity, Watched>> entries = WATCHED.entrySet().iterator();
        while (entries.hasNext())
        {
            Map.Entry<Entity, Watched> entry = entries.next();
            Entity entity = entry.getKey();
            Watched watched = entry.getValue();
            if (entity.isRemoved() || entity.level() != level)
                entries.remove();
            else if (entity instanceof LivingEntity living && living.isDeadOrDying())
                die(watched);
            else if (watched.dead && !watched.broken.contains(EnumDriveablePart.CORE))
            {
                // Brought back, as a respawned NPC is: whole again.
                watched.dead = false;
                DETACHED.remove(entity);
            }
        }
    }

    private static void die(Watched watched)
    {
        if (watched.dead || watched.source == null)
            return;
        watched.dead = true;
        DestructionSequences.start(watched.source);
    }

    private static void restore(Entity entity, EnumDriveablePart part)
    {
        Set<EnumDriveablePart> parts = DETACHED.get(entity);
        if (parts != null)
            parts.remove(part);
    }
}
