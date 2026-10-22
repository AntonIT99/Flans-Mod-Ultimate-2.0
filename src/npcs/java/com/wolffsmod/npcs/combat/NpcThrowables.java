package com.wolffsmod.npcs.combat;

import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.EventHooks;
import noppes.npcs.api.event.NpcEvent;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

import java.util.OptionalDouble;

/**
 * Fires vanilla throwables from the projectile slot as the real thing: potions splash, experience bottles burst,
 * fireworks explode with their stars and fire charges ignite. Custom NPCs' own projectile only carries the item's
 * picture and the NPC's damage, so those items would otherwise do none of this.
 *
 * <p>
 * Custom NPCs keeps the trigger, cadence, shot count, accuracy and indirect-fire choice. Other items, snowballs
 * included, stay with Custom NPCs, whose projectile deals the NPC's ranged damage.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcThrowables
{
    public static final float MIN_SPEED = 0.01F;
    public static final float MAX_SPEED = 10F;
    /** Vanilla inaccuracy of a perfectly inaccurate NPC; a witch throws at 8. */
    private static final float MAX_INACCURACY = 12F;

    /** Launch speed in blocks per tick, gravity per tick and the sound of a native throwable. */
    record Flight(float speed, double gravity, SoundEvent sound)
    {}

    /** @return whether the ammunition was handled here instead of by Custom NPCs */
    public static boolean fire(EntityNPCInterface npc, LivingEntity target, boolean highArc)
    {
        ItemStack ammunition = ammunition(npc);
        if (ammunition == null || !NpcEquipment.enabled(npc, Feature.NATIVE_THROWABLES))
            return false;
        Flight flight = flight(npc, ammunition);
        if (flight == null)
            return false;
        // The item is recognized, so Custom NPCs must not launch its own copy even when nothing can be thrown now.
        if (!(npc.level() instanceof ServerLevel level) || !level.getServer().isSameThread() || npc.isKilled() || target == null || !target.isAlive() || target.level() != npc.level())
            return true;
        NpcEvent.RangedLaunchedEvent event = new NpcEvent.RangedLaunchedEvent(npc.wrappedNPC, target, npc.stats.ranged.getStrength());
        Vec3 origin = npc.getEyePosition();
        if (launch(npc, level, ammunition, flight, origin, direction(npc, target, origin, flight, highArc)))
        {
            SoundEvent sound = npc.stats.ranged.getSoundEvent(0);
            npc.playSound(sound == null ? flight.sound() : sound, sound == null ? 1F : 2F, 1F);
            EventHooks.onNPCRangedLaunched(npc, event);
        }
        return true;
    }

    /** @return whether at least one of the NPC's shots entered the level */
    private static boolean launch(EntityNPCInterface npc, ServerLevel level, ItemStack ammunition, Flight flight, Vec3 origin, Vec3 direction)
    {
        float inaccuracy = inaccuracy(npc.stats.ranged.getAccuracy());
        int shots = Mth.clamp(npc.stats.ranged.getShotCount(), 1, 10);
        boolean launched = false;
        for (int i = 0; i < shots; i++)
        {
            Projectile projectile = create(npc, ammunition, origin, direction, inaccuracy);
            if (projectile == null)
                continue;
            if (!(projectile instanceof AbstractHurtingProjectile))
                projectile.shoot(direction.x, direction.y, direction.z, flight.speed(), inaccuracy);
            launched = level.addFreshEntity(projectile) || launched;
        }
        return launched;
    }

    private static Vec3 direction(EntityNPCInterface npc, LivingEntity target, Vec3 origin, Flight flight, boolean highArc)
    {
        Vec3 displacement = target.position().add(0D, target.getBbHeight() * 0.5D, 0D).subtract(origin);
        Vec3 lead = NpcEquipment.enabled(npc, Feature.LEAD_TARGET) ? target.getDeltaMovement() : Vec3.ZERO;
        double gravity = NpcEquipment.enabled(npc, Feature.BALLISTIC_AIM) ? flight.gravity() : 0D;
        return ProjectileAim.direction(displacement, lead, flight.speed(), gravity, 0.99D, highArc && gravity > 0D);
    }

    /** Custom NPCs accuracy runs from 0 to 100; a fully accurate NPC still throws with a hand's wobble of 1. */
    static float inaccuracy(int accuracy)
    {
        float miss = 1F - Mth.clamp(accuracy, 0, 100) / 100F;
        return 1F + miss * (MAX_INACCURACY - 1F);
    }

    @Nullable
    static Flight flight(ItemStack ammunition)
    {
        if (ammunition == null || ammunition.isEmpty())
            return null;
        if (ammunition.getItem() instanceof ThrowablePotionItem)
            return new Flight(0.75F, 0.05D, SoundEvents.WITCH_THROW);
        if (ammunition.is(Items.EXPERIENCE_BOTTLE))
            return new Flight(0.7F, 0.07D, SoundEvents.EXPERIENCE_BOTTLE_THROW);
        if (ammunition.is(Items.EGG))
            return new Flight(1.5F, 0.03D, SoundEvents.EGG_THROW);
        if (ammunition.is(Items.FIREWORK_ROCKET))
            return new Flight(1.6F, 0D, SoundEvents.FIREWORK_ROCKET_LAUNCH);
        if (ammunition.is(Items.FIRE_CHARGE))
            return new Flight(1F, 0D, SoundEvents.BLAZE_SHOOT);
        return null;
    }

    /** The supported item's unmodified launch speed, also used by readouts and imports. Powered fire charges have no throw speed. */
    public static OptionalDouble defaultSpeed(EntityNPCInterface npc)
    {
        ItemStack ammunition = ammunition(npc);
        if (ammunition == null || ammunition.is(Items.FIRE_CHARGE))
            return OptionalDouble.empty();
        Flight defaults = flight(ammunition);
        return defaults == null ? OptionalDouble.empty() : OptionalDouble.of(defaults.speed());
    }

    @Nullable
    private static ItemStack ammunition(EntityNPCInterface npc)
    {
        IItemStack slot = npc.inventory.getProjectile();
        return slot == null ? null : slot.getMCItemStack();
    }

    /** The stored Custom value; until one is edited/imported it starts at the current item's default. */
    public static float ownSpeed(EntityNPCInterface npc)
    {
        float own = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsCombat().getNativeThrowSpeed();
        return Float.isFinite(own) ? own : (float) defaultSpeed(npc).orElse(1D);
    }

    @Nullable
    static Flight flight(EntityNPCInterface npc, ItemStack ammunition)
    {
        Flight defaults = flight(ammunition);
        if (defaults == null || ammunition.is(Items.FIRE_CHARGE) || NpcEquipment.inherits(npc, EquipmentProperty.NATIVE_THROW_SPEED))
            return defaults;
        return new Flight(ownSpeed(npc), defaults.gravity(), defaults.sound());
    }

    @Nullable
    private static Projectile create(EntityNPCInterface npc, ItemStack ammunition, Vec3 origin, Vec3 direction, float inaccuracy)
    {
        ItemStack round = ammunition.copyWithCount(1);
        if (round.getItem() instanceof ThrowablePotionItem)
        {
            ThrownPotion potion = new ThrownPotion(npc.level(), npc);
            potion.setItem(round);
            return potion;
        }
        if (round.is(Items.EXPERIENCE_BOTTLE))
            return new ThrownExperienceBottle(npc.level(), npc);
        if (round.is(Items.EGG))
        {
            ThrownEgg egg = new ThrownEgg(npc.level(), npc);
            egg.setItem(round);
            return egg;
        }
        if (round.is(Items.FIREWORK_ROCKET))
            return new FireworkRocketEntity(npc.level(), round, npc, origin.x, origin.y - 0.15D, origin.z, true);
        if (round.is(Items.FIRE_CHARGE))
        {
            // Fireballs fly straight under their own power, so the NPC's inaccuracy is applied to the aim instead.
            double wobble = 0.0172275D * inaccuracy;
            Vec3 aim = direction.normalize().add(npc.getRandom().triangle(0D, wobble), npc.getRandom().triangle(0D, wobble), npc.getRandom().triangle(0D, wobble));
            SmallFireball fireball = new SmallFireball(npc.level(), npc, aim.x, aim.y, aim.z);
            fireball.setPos(origin.x, origin.y - 0.25D, origin.z);
            fireball.setItem(round);
            return fireball;
        }
        return null;
    }
}
