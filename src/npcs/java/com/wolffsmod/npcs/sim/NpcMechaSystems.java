package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.entity.FlansEntitySounds;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.flansmodultimate.api.entity.SoundRange;
import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.api.equipment.MechaUpgrades;
import com.wolffsmod.npcs.combat.NpcAmmunition;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcMechaTools;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.properties.NpcTypeProperties;
import com.wolffsmod.npcs.properties.NpcTypeProperty;
import lombok.Getter;

import noppes.npcs.entity.EntityNPCInterface;

/** Server-owned passive addons, landing effects and walking-cycle audio for an NPC wearing a mecha model. */
public final class NpcMechaSystems
{
    @Getter
    private MechaUpgrades upgrades = MechaUpgrades.NONE;
    private float phase;
    private int soundDelay;
    private double descent;
    private boolean airborne;

    /** Refresh after inventory updates, before type inheritance and movement. */
    public void refresh(EntityNPCInterface npc)
    {
        upgrades = active(npc) ? FlansEquipment.getMechaUpgrades(NpcAmmunition.supplies(npc)) : MechaUpgrades.NONE;
    }

    public static boolean active(EntityNPCInterface npc)
    {
        return NpcEquipment.enabled(npc, Feature.TYPE_PROPERTIES) && NpcMechaTools.isMecha(npc);
    }

    public static boolean stomp(EntityNPCInterface npc)
    {
        var model = NpcModels.flan(npc);
        return active(npc) && model != null && NpcTypeProperties.of(npc).covers(NpcTypeProperty.STEP_SOUND) && FlansEntityTypes.getMechaGait(model.getInfoType()).isPresent();
    }

    /** Called once after server movement, so landing uses the last airborne vertical velocity. */
    public void tick(EntityNPCInterface npc)
    {
        if (!active(npc) || !npc.isAlive() || npc.isPassenger())
        {
            phase = 0F;
            soundDelay = 0;
            airborne = false;
            descent = 0D;
            return;
        }
        var model = NpcModels.flan(npc);
        if (model == null)
            return;
        if (airborne && npc.onGround())
            FlansEntityTypes.applyMechaLanding(npc, model.getInfoType(), upgrades.breakBlocksUponFalling(), descent);
        airborne = !npc.onGround();
        descent = airborne ? npc.getDeltaMovement().y : 0D;
        if (npc.tickCount % 20 == 0 && upgrades.autoRepair() > 0F)
            npc.heal(upgrades.autoRepair());
        if (soundDelay > 0)
            soundDelay--;
        boolean moving = npc.onGround() && npc.position().subtract(npc.xo, npc.yo, npc.zo).horizontalDistanceSqr() > 1.0E-4D;
        if (!moving || !stomp(npc))
        {
            phase = 0F;
            return;
        }
        FlansEntityTypes.getMechaGait(model.getInfoType()).ifPresent(gait ->
        {
            float previous = phase;
            phase = (phase + gait.speed()) % 1F;
            if (soundDelay == 0 && gait.crosses(previous, phase))
            {
                FlansEntitySounds.playClip(npc, gait.sound(), FlansEntitySounds.getRange(SoundRange.VEHICLE), false);
                soundDelay = gait.delay();
            }
        });
    }
}
