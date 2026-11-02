package com.flansmodultimate.common.guns;

import com.flansmodultimate.api.projectiles.IArmorImpactListener;
import com.flansmodultimate.common.raytracing.hits.PartBodyHit;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Dispatches confirmed armour tests without depending on any addon. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ArmorImpactHandler
{
    private static final List<IArmorImpactListener> LISTENERS = new CopyOnWriteArrayList<>();

    public static void addListener(IArmorImpactListener listener)
    {
        if (!LISTENERS.contains(listener))
            LISTENERS.add(listener);
    }

    public static void removeListener(IArmorImpactListener listener)
    {
        LISTENERS.remove(listener);
    }

    public static void dispatch(@Nullable FiredShot shot, Entity target, String part, Vec3 outwardNormal, boolean blocked)
    {
        if (LISTENERS.isEmpty() || shot == null || !(target.level() instanceof ServerLevel))
            return;
        LivingEntity attacker = shot.getAttacker().orElse(null);
        if (attacker == null)
            return;
        var ammunition = ExternalProjectileSupport.armorPerformance(shot);
        for (IArmorImpactListener listener : LISTENERS)
            listener.onImpact(attacker, target, part, outwardNormal, ammunition, blocked);
    }

    public static void dispatchModel(FiredShot shot, Entity target, @Nullable PartBodyHit hit, Vec3 outwardNormal, boolean blocked)
    {
        dispatch(shot, target, hit == null ? "core" : hit.getPart().getShortName(), outwardNormal, blocked);
    }
}
