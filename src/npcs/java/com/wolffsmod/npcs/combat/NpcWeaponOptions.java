package com.wolffsmod.npcs.combat;

import lombok.Getter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.EnumSet;

/** Per-NPC switches; absent keys in existing saves keep the useful defaults enabled. */
public final class NpcWeaponOptions
{
    public static final String NBT_KEY = "WolffsModWeapons";
    private final EnumSet<Feature> enabled = EnumSet.noneOf(Feature.class);

    public enum Feature
    {
        ITEM_ARMOR(true), ITEM_WEAPONS(true), ARMOR_ANIMATIONS(true), WEAPON_ANIMATIONS(true), TYPE_PROPERTIES(true), PROJECTILES(true), GRENADES(true), MODEL_MUZZLES(true), ALTERNATE_BARRELS(
            true), MODEL_WEAPON_STATS(true), FIRE_SOUNDS(true), SHOOT_PARTICLES(true), LEAD_TARGET(true), BALLISTIC_AIM(true), SECONDARY_BANK(false), RELOAD_SOUNDS(true), THROW_SOUNDS(
                true), MODEL_SOUNDS(true), MELEE_SOUNDS(true), VEHICLE_DAMAGE(true), MODEL_ARMOUR(true), INVULNERABLE(false), FLAN_DAMAGE_TYPES(true), RELOAD_CYCLE(true), DUAL_WIELD(
                    true), GUN_SHOT_ANIMATION(true), GUN_RELOAD_ANIMATION(true), GUN_MELEE_ANIMATION(true), NATIVE_THROWABLES(true), GRADUAL_AIM(true), FIRE_WHEN_ALIGNED(true), TURRET_LOCKED(
                        false), PASSENGER_GUNS(true), TURRET_MUZZLES(true), ADVANCED_LEAD(true), HULL_FACES_TRAVEL(true), MISALIGNED_THROTTLE(true), RIDER_AT_SEAT(true), EYE_AT_MUZZLE(
                            true), ANIMATED_MODEL(true), DOORS_OPEN(false), GEAR_UP(false), WINGS_FOLDED(false), ENGINE_RUNNING(true), FULL_THROTTLE(false), MODEL_CULLING(true), PROGRESSIVE_SEARCH(
                                true), ADAPTIVE_PATH_RANGE(true), STAY_ACTIVE(false), HULL_ASSISTS_AIM(true), DRIVING(true), DRIVE_REVERSE(true), PATH_DEBUG(false), CONSUME_AMMO(false), PART_HITBOXES(
                                    true), PART_HEALTH(true), PART_EFFECTS(true), PART_REPAIR(true), HITBOX_OUTLINES(false), SOLID_HITBOXES(true), ARMOR_POTION_EFFECTS(true), MANNED_GUNS_FIRE(
                                        true), MELEE_DUAL_WIELD(false), DRIVE_DRIVEABLES(
                                            true), RETURN_HOME_TELEPORT(true), ADVANCED_SEARCH_INTERVALS(false), STAGGER_NEAR_SEARCH(false), DRIVER_GUNS(true), MUZZLE_FLASHES(true),
        /** Flan ammunition in the projectile slot is only fired by a held gun or model that takes it, as ammunition from the inventory always is. */
        FITTING_SLOT_AMMO(false),
        /**
         * An NPC with infinite ammunition may fall back on every round its weapon, or its model's selected bank, is issued
         * with by default, beside what it carries, and chooses between them to suit its target. Turning it off holds the NPC
         * to the rounds it was actually given.
         */
        DEFAULT_AMMO(true),
        /** A vehicle or mecha whose definition squashes mobs runs over everything in its way but its allies. */
        RUN_OVER(true),
        /** Landing gear that retracts while the aircraft flies and comes down near the ground; the gear row shows it as a third choice beside down and up. */
        GEAR_DYNAMIC(true),
        /** Shows what the NPC aims at: its line to the target, the point it leads to, its ranges and a label above its head. */
        AIM_DEBUG(false),
        /**
         * A ground vehicle whose main weapon cannot lower or raise itself onto its target backs away until it can, and one
         * whose line of fire is blocked drives on, round what blocks it, until it has a clear shot.
         */
        MANOEUVRE_TO_FIRE(true),
        /**
         * A vehicle, aircraft or ship keeps out of the reach of its own explosive rounds: it holds its fire at a target
         * too close for them and manoeuvres back to where it can fire them safely.
         */
        BLAST_STANDOFF(true),
        /**
         * A ship turns its bow onto its target for weapons fixed to its hull, such as torpedo tubes, and turns its side
         * to it for its turrets once it has none of those left to fire.
         */
        SHIP_WEAPON_BEARING(true),
        /**
         * A submarine chooses its own depth: it runs at the depth it was given, comes up to periscope depth to fight, and
         * goes deep for a moment after it is hit. Without it the submarine holds its running depth whatever happens.
         */
        SUBMARINE_TRIM(true),
        /** Each manned seat gun picks its own target within its own arc, whatever the turret and the other guns fire at. */
        GUNNER_TARGETS(true),
        /** A seat gun nobody sits behind aims and fires by itself, as a crew member would man it, credited to the vehicle. */
        UNMANNED_GUNS_FIRE(false),
        /** A vehicle or mecha model climbs steps as high as its model does; a ground vehicle never jumps like a mob, and a mecha jumps as its model does. */
        STEP_CLIMB(true),
        /** A ground vehicle model whose definition has a complete real-world profile slows down climbing steep grades, as the vehicle does. */
        SLOPE_PHYSICS(true),
        /** A plane or helicopter model flies at its definition's speeds, climb rate and control rates. */
        MODEL_FLIGHT(true),
        /** Timed maintenance, carrier servicing, buoyancy recovery and fuel-backed nanorepair; manual tool healing remains separate. */
        AUTOMATIC_REPAIRS(true),
        /**
         * An NPC with Return to start that is left away from home while it does not simulate, outside the simulation
         * distance or resting beyond its dormant range, is sent straight back once no player is near enough to see it go.
         */
        RETURN_WHEN_INACTIVE(false),
        /**
         * Flan rounds hit an NPC drawn with the player model limb by limb, as they hit players: headshots and the other
         * hit zones scale their damage, and the armour piece of the part struck resists them.
         */
        LIMB_HITBOXES(true),
        /**
         * An NPC holding a ranged weapon fires as far as the weapon's spread still hits a man-sized target, within a limit for
         * its kind of weapon, instead of at its own firing range, and notices enemies at least that far out.
         */
        WEAPON_FIRING_RANGE(false),
        /**
         * An NPC on foot that an enemy beyond its reach is shooting at gets out of that enemy's sight, then moves up on it
         * from cover to cover, instead of walking up to it in the open.
         */
        OUTRANGED_COVER(true),
        /** The NPC's hits leave a player at least half a heart, one of its human limits. */
        NON_LETHAL(false),
        /**
         * Rounds of the enemy passing close to an NPC on foot suppress it, as near misses suppress players: it aims worse,
         * and once pinned down it stops advancing until the fire lets up.
         */
        SUPPRESSIBLE(false),
        /** An NPC on foot looks at the enemies its weapons suit first: an anti-tank soldier at armour, any other at infantry. */
        SUITED_TARGETS(true),
        /** Takes cover under effective fire and while reloading, including from enemies within firing range. */
        TACTICAL_COVER(true),
        /** Breaks contact with protected vehicles the weapons carried cannot usefully engage. */
        AVOID_UNWINNABLE_FIGHTS(true),
        /** Steps out of a moving vehicle's predicted path. */
        VEHICLE_EVASION(true),
        /** Chooses a carried weapon and compatible ammunition to suit the target, with either ammunition policy. */
        TACTICAL_WEAPON_SWITCH(true),
        /** Approaches protected vehicles from a reachable side when carrying an anti-armour weapon. */
        ADAPTIVE_FLANKING(true),
        /** Leaves space between nearby allies when choosing combat positions. */
        ALLY_SPACING(true),
        /** Ground vehicles choose firing positions and weapons together instead of closing for the main gun alone. */
        ADAPTIVE_VEHICLE_COMBAT(true),
        /** Seat gunners look for threats even while the vehicle has no main target. */
        DEFENSIVE_GUNNERS(true),
        /** Only players its faction is friendly to see the NPC's name tag. */
        ALLY_NAME_TAGS(false),
        /** Retains movement orders in combat and alternates short infantry advances with aimed fire. */
        COMBAT_ADVANCE(true),
        /** Fires from a position while a nearby ally crosses toward the same enemy. */
        COVERING_FIRE(true),
        /** A soldier places carried deployable guns and mans free deployable or AA emplacements during combat. */
        MAN_EMPLACEMENTS(true),
        /** Allied players may deploy from a vehicle spawner into this NPC's seat in a real Flan driveable. */
        PLAYER_SEAT_DEPLOYMENT(true),
        /** Occasionally crouches or crawls when holding a firing position or approaching nearby cover. */
        TACTICAL_POSTURES(true),
        /** A halted tracked ground vehicle turns its angled front armour toward the nearest dangerous enemy vehicle. */
        FRONTAL_ARMOR_ANGLING(true),
        /**
         * The motion of a vehicle's hull throws its guns off: rounds fired while it moves and turns scatter, and, without a
         * stabiliser, it stands still for a moment to shoot before driving on again.
         */
        UNSTEADY_GUNS(true),
        /** The vehicle's guns are stabilised: they stay on target while it moves, so it neither scatters its rounds nor stops to fire. */
        GUN_STABILISER(false),
        /**
         * The body stays after the death animation, for the Corpse time tuning value, when the NPC does not respawn. Teams bots
         * follow the Teams corpses rule instead.
         */
        KEEP_CORPSE(false),
        /**
         * A vehicle with no weapon of its own follows the Transport tactic rather than Custom NPCs' own AI, which would drive
         * it at the enemy. Off, it drives as Custom NPCs says; the Transport tactic can still be chosen for it by hand.
         */
        TRANSPORT_TACTIC(true);

        /** Whether the switch is on for an NPC whose save does not mention it. */
        @Getter
        private final boolean defaultEnabled;

        Feature(boolean defaultEnabled)
        {
            this.defaultEnabled = defaultEnabled;
        }
    }

    public NpcWeaponOptions()
    {
        load(new CompoundTag());
    }

    public boolean enabled(Feature feature)
    {
        return enabled.contains(feature);
    }

    public void set(Feature feature, boolean value)
    {
        if (value)
            enabled.add(feature);
        else
            enabled.remove(feature);
    }

    public void load(CompoundTag root)
    {
        CompoundTag tag = root.getCompound(NBT_KEY);
        for (Feature feature : Feature.values())
        {
            boolean fallback = feature.defaultEnabled;
            // Split the former switch without changing saved NPC audio preferences.
            if (isSoundFeature(feature) && tag.contains("FLAN_SOUNDS", Tag.TAG_BYTE))
                fallback = tag.getBoolean("FLAN_SOUNDS");
            // The model weapon switch was saved under its earlier name.
            if (feature == Feature.MODEL_WEAPON_STATS && tag.contains("WEAPON_STATS", Tag.TAG_BYTE))
                fallback = tag.getBoolean("WEAPON_STATS");
            // Gear an NPC saved before dynamic gear existed keeps it up if it was put up; gear left down becomes dynamic.
            if (feature == Feature.GEAR_DYNAMIC && tag.contains(Feature.GEAR_UP.name(), Tag.TAG_BYTE))
                fallback = !tag.getBoolean(Feature.GEAR_UP.name());
            set(feature, tag.contains(feature.name(), Tag.TAG_BYTE) ? tag.getBoolean(feature.name()) : fallback);
        }
    }

    private static boolean isSoundFeature(Feature feature)
    {
        return feature == Feature.FIRE_SOUNDS || feature == Feature.RELOAD_SOUNDS || feature == Feature.THROW_SOUNDS || feature == Feature.MODEL_SOUNDS || feature == Feature.MELEE_SOUNDS;
    }

    public void save(CompoundTag root)
    {
        CompoundTag tag = new CompoundTag();
        for (Feature feature : Feature.values())
            tag.putBoolean(feature.name(), enabled(feature));
        root.put(NBT_KEY, tag);
    }
}
