package com.wolffsmod.npcs.properties;

import com.flansmodultimate.api.HeldItemSettings;
import com.flansmodultimate.api.IContentType;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.model.*;
import lombok.Getter;
import lombok.Setter;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;

import java.util.Optional;

/** Per-NPC presentation, independent of combat and model-stat inheritance. */
@Getter
public final class NpcPresentation
{
    private static final String HURT_FLASH = "HurtFlash";
    private static final String DEATH_ROTATION = "DeathRotation";
    private static final String DISPLAY_MODEL = "DisplayModel";

    public static final String NBT_KEY = "WolffsModPresentation";
    @Setter
    private boolean hurtFlash = true;
    @Setter
    private boolean deathRotation = true;
    private final NpcVehicleSounds vehicleSounds = new NpcVehicleSounds();
    private String displayModel = "";
    private boolean modelInitialized;
    private HeldItemSettings mainHand = HeldItemSettings.DEFAULT;
    private HeldItemSettings offHand = HeldItemSettings.DEFAULT;

    public HeldItemSettings hand(InteractionHand hand)
    {
        return hand == InteractionHand.MAIN_HAND ? mainHand : offHand;
    }

    public void hand(InteractionHand hand, HeldItemSettings settings)
    {
        if (hand == InteractionHand.MAIN_HAND)
            mainHand = settings;
        else
            offHand = settings;
    }

    public void load(CompoundTag root)
    {
        CompoundTag tag = root.getCompound(NBT_KEY);
        hurtFlash = !tag.contains(HURT_FLASH, Tag.TAG_BYTE) || tag.getBoolean(HURT_FLASH);
        deathRotation = !tag.contains(DEATH_ROTATION, Tag.TAG_BYTE) || tag.getBoolean(DEATH_ROTATION);
        displayModel = tag.getString(DISPLAY_MODEL);
        modelInitialized = tag.contains(DISPLAY_MODEL, Tag.TAG_STRING);
        vehicleSounds.load(tag);
        mainHand = HeldItemSettings.load(tag.getCompound("MainHand"));
        offHand = HeldItemSettings.load(tag.getCompound("OffHand"));
    }

    public void save(CompoundTag root)
    {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(HURT_FLASH, hurtFlash);
        tag.putBoolean(DEATH_ROTATION, deathRotation);
        if (modelInitialized)
            tag.putString(DISPLAY_MODEL, displayModel);
        vehicleSounds.save(tag);
        tag.put("MainHand", mainHand.save());
        tag.put("OffHand", offHand.save());
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
        NpcPresentation settings = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsPresentation();
        String model = "";
        boolean flan = false;
        if (npc instanceof EntityCustomNpc custom && custom.modelData.getEntityName() != null)
        {
            model = custom.modelData.getEntityName().toString();
            flan = BuiltInRegistries.ENTITY_TYPE.get(custom.modelData.getEntityName()) instanceof FlanModelEntityType;
        }
        if (settings.selectModel(model, flan) && !npc.level().isClientSide)
            npc.updateClient = true;
        return settings;
    }

    /** Applies selection defaults once per model, preserving explicit edits and reloads for that model. */
    boolean selectModel(String model, boolean flan)
    {
        if (modelInitialized && displayModel.equals(model))
            return false;
        if (modelInitialized || flan)
        {
            hurtFlash = !flan;
            deathRotation = !flan;
        }
        displayModel = model;
        modelInitialized = true;
        return true;
    }
}
