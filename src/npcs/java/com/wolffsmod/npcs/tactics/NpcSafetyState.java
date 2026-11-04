package com.wolffsmod.npcs.tactics;

import com.flansmodultimate.api.content.IContentType;
import com.wolffsmod.npcs.combat.NpcAmmoSelection;
import com.wolffsmod.npcs.combat.NpcArmorEngagement;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/** Short-lived movement intent, independent of the attack target and retained across AI task rebuilds. Server thread. */
@Getter
@Setter
public final class NpcSafetyState
{
    private final NpcArmorEngagement armorEngagement = new NpcArmorEngagement();
    private final NpcInfantryAirDefense.Memory airDefense = new NpcInfantryAirDefense.Memory();
    public enum Action
    {
        NONE, EVADE, RETREAT, HIDE, PEEK, FLANK, SPREAD, ADVANCE, FIRE
    }

    private Action action = Action.NONE;
    @Nullable
    private Vec3 destination;
    @Nullable
    private Vec3 anchor;
    private long nextSearch;
    private long until;
    private long urgentUntil;
    private long weaponChangedAt = Long.MIN_VALUE / 2;
    @Nullable
    private ItemStack selectedWeapon;
    private long flankReadyAt;
    private long collisionCheckedAt = Long.MIN_VALUE / 2;
    @Nullable
    private Vec3 evasion;
    @Nullable
    private NpcCover.Cover cover;
    private long coverReadyAt;
    @Nullable
    private NpcCombatIntent advanceIntent;
    private long advanceReadyAt;
    @Nullable
    private IContentType bankModel;
    private long bankCheckedAt = Long.MIN_VALUE / 2;
    private long bankChangedAt = Long.MIN_VALUE / 2;
    private boolean secondary;
    private long policyAt = Long.MIN_VALUE;
    private NpcTacticalProfile policy = NpcTacticalProfile.INHERIT;
    @Nullable
    private Vec3 firingPosition;
    private int firingTarget = -1;
    private long firingPositionAt = Long.MIN_VALUE / 2;
    @Nullable
    private LivingEntity gunnerTarget;
    private long gunnerCheckedAt = Long.MIN_VALUE / 2;
    /** One remembered round choice per weapon, bounded by the weapons the NPC's model and equipment hold. */
    private final Map<NpcAmmoSelection.Key, NpcAmmoSelection> ammoSelections = new HashMap<>();

    /** @return the remembered round choice of one weapon, created empty the first time it is asked for */
    public NpcAmmoSelection ammoSelection(NpcAmmoSelection.Key weapon)
    {
        return ammoSelections.computeIfAbsent(weapon, ignored -> new NpcAmmoSelection());
    }

    public boolean active(long now)
    {
        return action != Action.NONE && now < until;
    }

    public void clear()
    {
        action = Action.NONE;
        destination = null;
        until = 0L;
        cover = null;
    }
}
