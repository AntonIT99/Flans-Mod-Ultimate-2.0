package com.wolffsmod.npcs.tactics;

import com.flansmodultimate.api.content.IContentType;
import com.wolffsmod.npcs.combat.NpcArmorEngagement;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Short-lived movement intent, independent of the attack target and retained across AI task rebuilds. Server thread. */
@Getter
@Setter
public final class NpcSafetyState
{
    private final NpcArmorEngagement armorEngagement = new NpcArmorEngagement();
    public enum Action
    {
        NONE, EVADE, RETREAT, HIDE, PEEK, FLANK, SPREAD
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
