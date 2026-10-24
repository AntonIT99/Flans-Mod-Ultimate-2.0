package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.equipment.EquippedWeaponProperties;
import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.api.projectiles.DetonationRadii;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import com.wolffsmod.npcs.platform.NpcPlatform;
import com.wolffsmod.npcs.sim.NpcModels;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Throwing;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import com.wolffsmod.npcs.sim.TacticKind;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.phys.AABB;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Which throwables an NPC uses and when, by the strategy chosen for each kind: Flan grenades, throwable Flan guns and native
 * throwables. The projectile slot holds the item of a kind the NPC prefers, and the inventory supplies the rest. A kind thrown
 * as the main weapon is thrown whenever the NPC holds no ranged weapon in hand; the other strategies throw it now and then
 * beside the NPC's other attacks, through {@link NpcThrowGoal}, and only from far enough off that the throw harms neither the
 * NPC nor its allies. Only an NPC without a content-pack model chooses: a model fires its projectile slot as it always has.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcThrowing
{
    /** Blocks within which an NPC throws each kind now and then. */
    private static final double GRENADE_REACH = 20D;
    private static final double THROWN_WEAPON_REACH = 20D;
    private static final double NATIVE_REACH = 16D;
    /** Blocks a thrower keeps beyond the reach of its grenade, and the least distance it throws one from whatever that reach. */
    private static final double GRENADE_MARGIN = 2D;
    private static final double MIN_GRENADE_DISTANCE = 4D;
    /** Share of a grenade's fragment radius a thrower keeps out of: fragments that far off seldom hit, and do little. */
    private static final double FRAGMENT_SHARE = 0.5D;
    /** Blocks round where it lands that a splash potion or its lingering cloud reaches, and that a firework's stars reach. */
    private static final double SPLASH_RADIUS = 4D;
    private static final double FIREWORK_RADIUS = 5D;
    /** The least distance from its target at which an NPC throws something that harms nothing round where it lands. */
    private static final double MIN_THROW_DISTANCE = 3D;
    /** Health and armor from which a lone target is worth a grenade. */
    private static final float TOUGH_HEALTH = 40F;
    private static final int TOUGH_ARMOR = 15;

    /** The kinds of throwable that each have a strategy, with the switch that lets the NPC use them as the real thing. */
    public enum Kind
    {
        GRENADE(Value.GRENADE_USE, Feature.GRENADES), THROWN_WEAPON(Value.THROWN_WEAPON_USE, Feature.ITEM_WEAPONS), NATIVE(Value.NATIVE_THROWABLE_USE, Feature.NATIVE_THROWABLES);

        private final Value strategy;
        private final Feature feature;

        Kind(Value strategy, Feature feature)
        {
            this.strategy = strategy;
            this.feature = feature;
        }
    }

    /** A carried throwable the NPC has chosen to throw now and then, and its kind. */
    public record Choice(ItemStack stack, Kind kind)
    {}

    /** @return the kind of throwable the item is, or null for an item no strategy is for */
    @Nullable
    public static Kind kind(ItemStack stack)
    {
        if (stack.isEmpty())
            return null;
        if (FlansProjectiles.isGrenade(stack))
            return Kind.GRENADE;
        if (FlansEquipment.getWeaponProperties(stack, ItemStack.EMPTY).filter(EquippedWeaponProperties::throwable).isPresent())
            return Kind.THROWN_WEAPON;
        return NpcThrowables.flight(stack) != null ? Kind.NATIVE : null;
    }

    public static Throwing strategy(EntityNPCInterface npc, Kind kind)
    {
        return NpcTuning.of(npc).throwing(kind.strategy);
    }

    /**
     * The grenade or throwable Flan gun the main hand throws while it holds no ranged weapon: the projectile slot's, or else the
     * first in the inventory of a kind thrown as the main weapon. A model throws only its projectile slot's. Either side.
     *
     * @return the item, or an empty stack when there is none
     */
    public static ItemStack mainThrownWeapon(EntityNPCInterface npc)
    {
        ItemStack projectile = NpcAmmunition.projectile(npc);
        Kind kind = thrownFromHand(npc, projectile);
        boolean model = NpcModels.flan(npc) != null;
        if (kind != null && (model || strategy(npc, kind).mainWeapon(true)))
            return projectile;
        if (model)
            return ItemStack.EMPTY;
        return firstCarried(npc, carried ->
        {
            Kind carriedKind = thrownFromHand(npc, carried);
            return carriedKind != null && strategy(npc, carriedKind).mainWeapon(false);
        });
    }

    /** @return the kind of a grenade, while grenades are thrown as the real thing, or of a throwable Flan gun; else null */
    @Nullable
    private static Kind thrownFromHand(EntityNPCInterface npc, ItemStack stack)
    {
        Kind kind = kind(stack);
        if (kind == Kind.GRENADE)
            return NpcEquipment.enabled(npc, Feature.GRENADES) ? kind : null;
        return kind == Kind.THROWN_WEAPON ? kind : null;
    }

    /**
     * What Custom NPCs' ranged attack fires for an NPC without an equipped ranged weapon: the projectile slot, unless it holds a
     * throwable of a kind only thrown now and then, or else the first native throwable that harms, or grenade, in the inventory
     * of a kind thrown as the main weapon. A model always fires its projectile slot. Either side.
     *
     * @return the item, or an empty stack when there is none
     */
    public static ItemStack mainProjectile(EntityNPCInterface npc)
    {
        ItemStack projectile = NpcAmmunition.projectile(npc);
        if (NpcModels.flan(npc) != null)
            return projectile;
        if (!projectile.isEmpty())
        {
            Kind kind = managed(npc, projectile);
            if (kind == null || strategy(npc, kind).mainWeapon(true))
                return projectile;
        }
        return firstCarried(npc, carried ->
        {
            Kind kind = managed(npc, carried);
            boolean fired = kind == Kind.NATIVE && offensive(carried) || kind == Kind.GRENADE && NpcEquipment.enabled(npc, Feature.PROJECTILES);
            return fired && strategy(npc, kind).mainWeapon(false);
        });
    }

    /** @return the kind of the item while its switch is on, or null for an item Custom NPCs fires as it is */
    @Nullable
    private static Kind managed(EntityNPCInterface npc, ItemStack stack)
    {
        Kind kind = kind(stack);
        return kind != null && NpcEquipment.enabled(npc, kind.feature) ? kind : null;
    }

    private static ItemStack firstCarried(EntityNPCInterface npc, Predicate<ItemStack> wanted)
    {
        for (ItemStack carried : NpcAmmunition.inventory(npc))
            if (!carried.isEmpty() && wanted.test(carried))
                return carried;
        return ItemStack.EMPTY;
    }

    /** @return whether the NPC throws a kind now and then: a quick test, before its inventory is looked through */
    public static boolean throwsOccasionally(EntityNPCInterface npc)
    {
        for (Kind kind : Kind.values())
            if (NpcEquipment.enabled(npc, kind.feature) && strategy(npc, kind).occasional())
                return true;
        return false;
    }

    /**
     * Chooses what the NPC throws now and then at its target, if anything: of each kind it throws so and may throw again, the
     * first item it carries that is worth throwing from where it stands, the projectile slot's first. Only an NPC that fights
     * on foot, and retaliates, throws so. Server thread.
     *
     * @param readyAt
     *            game time from which each kind, by ordinal, may be thrown again
     */
    public static Optional<Choice> occasionalThrow(EntityNPCInterface npc, LivingEntity target, long[] readyAt, long now)
    {
        if (npc.ais.onAttack != 0 || TacticKind.of(npc) != TacticKind.STANDARD || NpcDrivers.holdsFire(npc) || !npc.getSensing().hasLineOfSight(target))
            return Optional.empty();
        double distance = npc.distanceTo(target);
        for (Kind kind : Kind.values())
        {
            Throwing strategy = strategy(npc, kind);
            if (!strategy.occasional() || now < readyAt[kind.ordinal()] || !NpcEquipment.enabled(npc, kind.feature) || distance > reach(kind))
                continue;
            Optional<ItemStack> chosen = carried(npc, kind).filter(stack -> worthThrowing(npc, target, stack, kind, strategy, distance)).findFirst();
            if (chosen.isPresent())
                return Optional.of(new Choice(chosen.get(), kind));
        }
        return Optional.empty();
    }

    /** @return whether a throw that has begun may go on: the item is still carried and the target in sight, and neither the NPC nor an ally is now where it lands */
    public static boolean stillWorthThrowing(EntityNPCInterface npc, LivingEntity target, Choice choice)
    {
        ItemStack stack = choice.stack();
        if (stack.isEmpty() || !target.isAlive() || target.level() != npc.level() || !carries(npc, stack) || !npc.getSensing().hasLineOfSight(target))
            return false;
        double distance = npc.distanceTo(target);
        return distance <= reach(choice.kind()) + MIN_THROW_DISTANCE && keepsClear(npc, target, stack, distance);
    }

    /**
     * @return how long, in ticks, the NPC waits after a throw before it throws the kind again; spread by a quarter either way,
     *         so that a squad does not throw as one, and 0 for a kind it does not throw now and then
     */
    public static int cooldown(EntityNPCInterface npc, Kind kind)
    {
        int ticks = switch (strategy(npc, kind))
        {
            case FREQUENT -> 100;
            case MODERATE -> 300;
            case SPARING -> 600;
            case RANGED_SLOT, MAIN_WEAPON -> 0;
        };
        return ticks * (75 + npc.getRandom().nextInt(51)) / 100;
    }

    /** @return whether the target is too close to throw the grenade at without the thrower being caught in it */
    public static boolean tooClose(EntityNPCInterface npc, LivingEntity target, ItemStack grenade)
    {
        return npc.getEyePosition().distanceTo(target.getEyePosition()) < safeDistance(grenade);
    }

    /** @return the least distance in blocks from its target at which the NPC throws the item: beyond what it harms where it lands */
    static double safeDistance(ItemStack stack)
    {
        double hazard = hazard(stack);
        if (FlansProjectiles.isGrenade(stack))
            return Math.max(MIN_GRENADE_DISTANCE, hazard + GRENADE_MARGIN);
        return hazard > 0D ? hazard + 1D : MIN_THROW_DISTANCE;
    }

    /** @return blocks round where it lands within which the item harms, as far as a thrower needs to keep out of it */
    static double hazard(ItemStack stack)
    {
        if (FlansProjectiles.isProjectile(stack))
            return FlansProjectiles.getDetonationRadii(stack).map(NpcThrowing::grenadeHazard).orElse(0D);
        if (stack.getItem() instanceof ThrowablePotionItem)
            return harmful(stack) ? SPLASH_RADIUS : 0D;
        if (stack.is(Items.FIREWORK_ROCKET))
            return NpcPlatform.fireworkExplodes(stack) ? FIREWORK_RADIUS : 0D;
        return 0D;
    }

    private static double grenadeHazard(DetonationRadii radii)
    {
        return Math.max(Math.max(radii.explosion(), radii.blast()), Math.max(radii.fire(), radii.fragments() * FRAGMENT_SHARE));
    }

    /** @return whether a native throwable from the inventory is worth throwing at an enemy at all: a harmful potion, a fire charge or a firework with stars */
    static boolean offensive(ItemStack stack)
    {
        if (stack.getItem() instanceof ThrowablePotionItem)
            return harmful(stack);
        if (stack.is(Items.FIREWORK_ROCKET))
            return NpcPlatform.fireworkExplodes(stack);
        return stack.is(Items.FIRE_CHARGE);
    }

    private static boolean harmful(ItemStack potion)
    {
        return NpcPlatform.potionEffects(potion).stream().anyMatch(effect -> effect.getEffect().getCategory() == MobEffectCategory.HARMFUL);
    }

    private static double reach(Kind kind)
    {
        return switch (kind)
        {
            case GRENADE -> GRENADE_REACH;
            case THROWN_WEAPON -> THROWN_WEAPON_REACH;
            case NATIVE -> NATIVE_REACH;
        };
    }

    /** The items of the kind the NPC carries: the projectile slot's first, then the inventory's in slot order, which only offers native throwables that harm. */
    private static Stream<ItemStack> carried(EntityNPCInterface npc, Kind kind)
    {
        ItemStack projectile = NpcAmmunition.projectile(npc);
        Stream<ItemStack> preferred = kind(projectile) == kind ? Stream.of(projectile) : Stream.empty();
        return Stream.concat(preferred, NpcAmmunition.inventory(npc).stream().filter(stack -> kind(stack) == kind && (kind != Kind.NATIVE || offensive(stack))));
    }

    private static boolean carries(EntityNPCInterface npc, ItemStack stack)
    {
        return NpcAmmunition.projectile(npc) == stack || NpcAmmunition.inventory(npc).stream().anyMatch(carried -> carried == stack);
    }

    private static boolean worthThrowing(EntityNPCInterface npc, LivingEntity target, ItemStack stack, Kind kind, Throwing strategy, double distance)
    {
        if (!keepsClear(npc, target, stack, distance))
            return false;
        return switch (kind)
        {
            case GRENADE -> tough(target) || enemiesAround(npc, target, Math.max(MIN_GRENADE_DISTANCE, hazard(stack))) >= groupSize(strategy);
            case NATIVE -> useful(target, stack);
            case THROWN_WEAPON -> true;
        };
    }

    /** @return whether the NPC is beyond the reach of the item where it lands, and no ally of the NPC is within it */
    private static boolean keepsClear(EntityNPCInterface npc, LivingEntity target, ItemStack stack, double distance)
    {
        if (distance < safeDistance(stack))
            return false;
        double hazard = hazard(stack);
        return hazard <= 0D || around(npc, target, hazard, other -> other != npc && NpcDrivers.ally(npc, other)) == 0;
    }

    /** @return how many enemies a grenade must be able to catch together to be worth throwing at a target that is not tough */
    private static int groupSize(Throwing strategy)
    {
        return switch (strategy)
        {
            case FREQUENT, RANGED_SLOT, MAIN_WEAPON -> 1;
            case MODERATE -> 2;
            case SPARING -> 3;
        };
    }

    /** @return whether a lone target is worth a grenade: it has much health or armor, is a vehicle, or rides in one */
    private static boolean tough(LivingEntity target)
    {
        return target.getMaxHealth() >= TOUGH_HEALTH || target.getArmorValue() >= TOUGH_ARMOR || NpcModels.flan(target) != null
            || target.getVehicle() != null && !(target.getVehicle() instanceof LivingEntity);
    }

    private static int enemiesAround(EntityNPCInterface npc, LivingEntity target, double radius)
    {
        return around(npc, target, radius, other -> other == target || other != npc && NpcDrivers.hostile(npc, other));
    }

    private static int around(EntityNPCInterface npc, LivingEntity target, double radius, Predicate<LivingEntity> counted)
    {
        AABB area = target.getBoundingBox().inflate(radius);
        double reach = radius * radius;
        return npc.level().getEntitiesOfClass(LivingEntity.class, area, other -> other.isAlive() && other.distanceToSqr(target) <= reach && counted.test(other)).size();
    }

    /** @return whether a native throwable would do the target any good to throw: a potion that is not already working on it, fire at what can burn */
    private static boolean useful(LivingEntity target, ItemStack stack)
    {
        if (stack.getItem() instanceof ThrowablePotionItem)
            return NpcPlatform.potionEffects(stack).stream()
                .anyMatch(effect -> effect.getEffect().getCategory() == MobEffectCategory.HARMFUL && (effect.getEffect().isInstantenous() || !target.hasEffect(effect.getEffect())));
        if (stack.is(Items.FIRE_CHARGE))
            return !target.fireImmune() && !target.isOnFire();
        return true;
    }
}
