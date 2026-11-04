package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.DriveableKind;
import com.flansmodultimate.api.driveables.IDriveableType;
import com.flansmodultimate.api.driveables.WeaponMuzzle;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.flansmodultimate.api.projectiles.ProjectileParameters;
import com.flansmodultimate.api.projectiles.ProjectileShot;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import com.wolffsmod.npcs.sim.*;
import com.wolffsmod.npcs.sim.NpcTuning.AircraftRole;
import com.wolffsmod.npcs.teams.TeamsBots;
import lombok.Getter;
import noppes.npcs.ai.selector.NPCAttackSelector;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/** Aircraft missions and loaded weapon capabilities, shared by generated bots and authored flying NPCs. Server thread. */
public final class NpcAirCombat
{
    private static final long INTERVAL = 20L;
    /** Share of its search range within which a fighter still leaves a ground target for an aircraft that threatens nobody. */
    private static final double INTERCEPTION_SHARE = 0.5D;

    /** Reported destination and target priority; a patrol with no contact has priority 4. */
    public record Mission(Vec3 position, int priority)
    {}
    private static final double UNUSABLE = -10000D;

    private record Bank(boolean guns, boolean bombs)
    {
        boolean usable(boolean airborne)
        {
            return guns || bombs && !airborne;
        }
    }

    @Nullable
    private IContentType model;
    private long checkedAt = Long.MIN_VALUE;
    private Bank primary = new Bank(false, false);
    private Bank secondary = new Bank(false, false);
    @Getter @Nullable
    private Vec3 patrol;

    public static boolean aircraft(Entity entity)
    {
        Entity platform = com.wolffsmod.npcs.tactics.NpcVehicleThreats.platform(entity);
        if (platform instanceof EntityNPCInterface npc)
            // Target-goal constructors ask for ranges before Custom NPCs installs the AI data.
            return npc.ais != null && NpcFlight.type(npc) != NpcTuning.Flight.NORMAL;
        return platform != null && FlansEntityTypes.getDriveableType(platform).filter(type -> type.getDriveableKind() == DriveableKind.PLANE && !type.isShip()).isPresent();
    }

    public static boolean airborne(Entity entity)
    {
        return aircraft(entity) && NpcBombTargets.airborne(com.wolffsmod.npcs.tactics.NpcVehicleThreats.platform(entity));
    }

    public static boolean active(EntityNPCInterface npc)
    {
        // Target-goal constructors ask for ranges before Custom NPCs installs the AI data.
        return npc.ais != null && NpcFlight.type(npc) != NpcTuning.Flight.NORMAL;
    }

    public static NpcAirCombat of(EntityNPCInterface npc)
    {
        return NpcVehicleState.of(npc).getFlight().getAirCombat();
    }

    public boolean canIntercept()
    {
        return primary.guns() || secondary.guns();
    }

    public AircraftRole role(EntityNPCInterface npc)
    {
        AircraftRole selected = NpcTuning.of(npc).aircraftRole();
        return selected == AircraftRole.AUTO ? automatic(primary.guns() || secondary.guns(), primary.bombs() || secondary.bombs(), NpcFlight.type(npc) == NpcTuning.Flight.HELICOPTER) : selected;
    }

    static AircraftRole automatic(boolean guns, boolean bombs, boolean rotorcraft)
    {
        if (bombs && !guns)
            return AircraftRole.BOMBER;
        // A gunship supports the ground, but as an attacker rather than a bomber, which ranks every aircraft last and takes
        // no notice of one going for its own side. It carries guns to turn on that one, and nothing it drops.
        return rotorcraft ? AircraftRole.ATTACKER : AircraftRole.FIGHTER;
    }

    /** Interception outranks ordinary air combat; a bomber keeps its ground mission while its independent gunners defend it. */
    public int rank(EntityNPCInterface npc, LivingEntity candidate)
    {
        boolean air = airborne(candidate);
        boolean threat = air && (TeamsBots.reportedAirThreat(npc, candidate) || candidate instanceof Mob mob && mob.getTarget() != null && NpcDrivers.ally(npc, mob.getTarget()));
        return rank(role(npc), air, threat, npc.distanceToSqr(candidate) <= Mth.square(interceptionReach(npc)));
    }

    /**
     * @param withinReach
     *            whether the candidate is near enough for a fighter to hope to catch it, as {@link #interceptionReach} says
     * @return the candidate's priority, the lowest number first
     */
    static int rank(AircraftRole role, boolean airborne, boolean threat, boolean withinReach)
    {
        // A fighter chases an aircraft that threatens nobody only as far as it could catch it. Beyond that a ground target it
        // could be supporting now comes first, rather than the fighter crossing the battle after a contact it never reaches.
        if (airborne && !threat && role == AircraftRole.FIGHTER && !withinReach)
            return priority(role, false, false) + 1;
        return priority(role, airborne, threat);
    }

    /** @return blocks within which a fighter leaves a ground target for an aircraft that threatens nobody */
    static double interceptionReach(EntityNPCInterface npc)
    {
        return NpcTargetSearch.range(npc) * INTERCEPTION_SHARE;
    }

    public static int priority(AircraftRole role, boolean airborne, boolean threat)
    {
        if (role == AircraftRole.BOMBER)
            return airborne ? 3 : 0;
        // A ground-attack aircraft keeps its ground mission, but still turns on an aircraft going for its own side.
        if (role == AircraftRole.ATTACKER)
            return airborne ? (threat ? 1 : 2) : 0;
        if (!airborne)
            return 2;
        return threat ? 0 : 1;
    }

    /** Reassesses all distance bands once a second, allowing a fighter to leave a ground attack for an air threat. */
    public void tick(EntityNPCInterface npc)
    {
        if (!(npc.level() instanceof ServerLevel level) || npc.isKilled() || !npc.isAlive() || npc.isNoAi() || npc.isPassenger() || NpcDormancy.dormant(npc))
            return;
        long now = level.getGameTime();
        if (checkedAt != Long.MIN_VALUE && now - checkedAt < INTERVAL)
            return;
        checkedAt = now;
        refresh(npc, definition(npc));
        Mission mission = TeamsBots.airMission(npc).orElse(null);
        patrol = mission == null ? null : mission.position();
        LivingEntity kept = npc.getTarget();
        LivingEntity best = find(npc, NpcCombatants.of(level), kept);
        if (mission != null && mission.priority() < (best == null ? 4 : rank(npc, best)))
        {
            // Leave a lower-priority local fight to investigate a reported air threat, without tracking an unseen entity.
            if (kept != null)
                npc.setTarget(null);
            return;
        }
        if (best != null)
        {
            if (best != kept)
                npc.setTarget(best);
            TeamsBots.reportContact(npc, best);
        }
        else if (kept != null && !canAttack(kept))
            npc.setTarget(null);
    }

    @Nullable
    private LivingEntity find(EntityNPCInterface npc, NpcCombatants combatants, @Nullable LivingEntity kept)
    {
        Map<Integer, Integer> assigned = assignments(npc, combatants);
        NPCAttackSelector enemies = new NPCAttackSelector(npc);
        LivingEntity best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        double range = NpcTargetSearch.range(npc);
        for (int i = 0; i < combatants.size(); i++)
        {
            LivingEntity candidate = combatants.get(i).entity();
            if (!eligible(npc, candidate, enemies, range))
                continue;
            double score = score(npc, candidate, assigned.getOrDefault(candidate.getId(), 0), candidate == kept);
            if (score > bestScore && NpcSight.sees(npc, candidate))
            {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private double score(EntityNPCInterface npc, LivingEntity candidate, int assigned, boolean kept)
    {
        double score = -rank(npc, candidate) * 1_000_000D - npc.distanceToSqr(candidate) - assigned * 2048D;
        if (patrol != null && !airborne(candidate))
            score -= Math.min(262144D, candidate.position().subtract(patrol).horizontalDistanceSqr());
        if (NpcBombTargets.worthBombing(candidate))
            score += 8192D;
        return kept ? score + 1024D : score;
    }

    private boolean eligible(EntityNPCInterface npc, LivingEntity candidate, NPCAttackSelector enemies, double range)
    {
        return candidate != npc && candidate.isAlive() && !candidate.isSpectator() && !(candidate instanceof EntityNPCInterface other && other.isKilled())
            && npc.distanceToSqr(candidate) <= range * range && enemies.isEntityApplicable(candidate) && canAttack(candidate);
    }

    private static Map<Integer, Integer> assignments(EntityNPCInterface npc, NpcCombatants combatants)
    {
        Map<Integer, Integer> assigned = new HashMap<>();
        for (int i = 0; i < combatants.npcSize(); i++)
        {
            EntityNPCInterface ally = combatants.npc(i).npc();
            if (ally != null && ally != npc && ally.isAlive() && !ally.isKilled() && active(ally) && ally.getTarget() != null && NpcDrivers.ally(npc, ally))
                assigned.merge(ally.getTarget().getId(), 1, Integer::sum);
        }
        return assigned;
    }

    private boolean canAttack(LivingEntity target)
    {
        boolean air = airborne(target);
        return primary.usable(air) || secondary.usable(air);
    }

    /** Chooses actual supplied guns for interception, bombs for a bomber's ground mission, and useful ordnance for CAS. */
    public boolean bank(EntityNPCInterface npc, @Nullable IContentType definition, boolean configured)
    {
        if (npc.level().isClientSide || !active(npc) || definition == null)
            return configured;
        if (definition != model)
            refresh(npc, definition);
        LivingEntity target = npc.getTarget();
        if (target == null)
            return configured;
        boolean air = airborne(target);
        boolean bombing = !air && (role(npc) == AircraftRole.BOMBER || NpcBombTargets.worthBombing(target));
        double first = bankScore(primary.guns(), primary.bombs(), air, bombing);
        double second = bankScore(secondary.guns(), secondary.bombs(), air, bombing);
        return first == second ? configured : second > first;
    }

    static double bankScore(boolean guns, boolean bombs, boolean airborne, boolean bombing)
    {
        if (airborne)
            return guns ? 100D : UNUSABLE;
        if (bombing && bombs)
            return 100D;
        if (guns)
            return 50D;
        return bombs ? 25D : UNUSABLE;
    }

    private void refresh(EntityNPCInterface npc, @Nullable IContentType definition)
    {
        model = definition;
        primary = definition == null ? new Bank(NpcEquipment.fightsAtRange(npc), false) : capabilities(npc, definition, false);
        secondary = capabilities(npc, definition, true);
    }

    private static Bank capabilities(EntityNPCInterface npc, @Nullable IContentType definition, boolean secondary)
    {
        boolean guns = false;
        boolean bombs = false;
        if (definition instanceof IDriveableType)
        {
            for (WeaponMuzzle muzzle : FlansProjectiles.getMuzzles(definition, secondary, 0, false))
            {
                IContentType weapon = muzzle.weapon().orElse(null);
                ItemStack ammo = weapon == null ? NpcAmmunition.bankTemplate(npc, definition, secondary) : NpcAmmunition.driverGunTemplate(npc, weapon);
                ProjectileShot shot = ammo.isEmpty()
                    ? null
                    : FlansProjectiles.prepare(npc, ammo, ItemStack.EMPTY, weapon, definition, secondary, new ProjectileParameters(0, true, 0F, 0F, 1F)).orElse(null);
                if (shot != null)
                {
                    bombs |= shot.dropped();
                    guns |= !shot.dropped();
                }
            }
        }
        return new Bank(guns, bombs);
    }

    @Nullable
    private static IContentType definition(EntityNPCInterface npc)
    {
        var model = NpcModels.flan(npc);
        return model == null ? null : model.getInfoType();
    }
}
