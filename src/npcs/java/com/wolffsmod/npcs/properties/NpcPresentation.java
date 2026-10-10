package com.wolffsmod.npcs.properties;

import com.flansmodultimate.api.IContentType;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.model.FlanModelKind;
import lombok.Getter;
import lombok.Setter;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.Optional;

/** Per-NPC presentation, independent of combat and model-stat inheritance. */
@Getter
public final class NpcPresentation
{
    private static final String HURT_FLASH = "HurtFlash";
    private static final String DEATH_ROTATION = "DeathRotation";

    public static final String NBT_KEY = "WolffsModPresentation";
    @Setter
    private boolean hurtFlash = true;
    @Setter
    private boolean deathRotation = true;
    private final NpcVehicleSounds vehicleSounds = new NpcVehicleSounds();

    public void load(CompoundTag root)
    {
        CompoundTag tag = root.getCompound(NBT_KEY);
        hurtFlash = !tag.contains(HURT_FLASH, Tag.TAG_BYTE) || tag.getBoolean(HURT_FLASH);
        deathRotation = !tag.contains(DEATH_ROTATION, Tag.TAG_BYTE) || tag.getBoolean(DEATH_ROTATION);
        vehicleSounds.load(tag);
    }

    public void save(CompoundTag root)
    {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(HURT_FLASH, hurtFlash);
        tag.putBoolean(DEATH_ROTATION, deathRotation);
        vehicleSounds.save(tag);
        root.put(NBT_KEY, tag);
    }

    public static Optional<IContentType> vehicleType(EntityNPCInterface npc)
    {
        if (npc instanceof EntityCustomNpc custom && custom.modelData.getEntity(npc) instanceof FlanModelEntity model)
        {
            FlanModelKind kind = FlanModelKind.of(model.getInfoType());
            if (kind == FlanModelKind.VEHICLE || kind == FlanModelKind.PLANE)
                return Optional.of(model.getInfoType());
        }
        return Optional.empty();
    }

    public static NpcPresentation of(EntityNPCInterface npc)
    {
        return ((NpcWeaponSettings) npc.stats).wolffsmodnpcsPresentation();
    }
}
