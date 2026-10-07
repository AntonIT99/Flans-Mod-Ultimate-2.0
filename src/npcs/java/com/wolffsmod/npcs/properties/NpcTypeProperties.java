package com.wolffsmod.npcs.properties;

import com.flansmodultimate.api.EntityTypeProperties;
import com.flansmodultimate.api.FlansEntityTypes;
import com.flansmodultimate.api.IContentType;
import com.wolffsmod.npcs.combat.NpcRangedAttack;
import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.properties.NpcTypeProperty.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataAI;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/** Per-NPC inheritance cache. Only the live NPC owns gameplay state; its selected model supplies definitions. */
public final class NpcTypeProperties
{
    private final NpcPropertyOverrides overrides = new NpcPropertyOverrides();
    private IContentType lastType;
    private int lastFlags = -1;
    private boolean lastNative;
    private int nextInspection;
    @Nullable
    private EntityTypeProperties type;
    private Map<NpcTypeProperty, Tag> mapped = Map.of();
    private float loadingHealthFraction = -1F;

    public void refresh(EntityNPCInterface npc)
    {
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        IContentType selected = options.enabled(Feature.TYPE_PROPERTIES) && npc instanceof EntityCustomNpc custom
            && custom.modelData.getEntity(npc) instanceof FlanModelEntity model ? model.getInfoType() : null;
        boolean nativeAttack = NpcRangedAttack.usesFlanProjectile(options, ItemStackWrapper.MCItem(npc.inventory.getProjectile()));
        int flags = flags(options);
        if (selected != lastType || flags != lastFlags || nativeAttack != lastNative || npc.tickCount >= nextInspection)
        {
            lastType = selected;
            lastFlags = flags;
            lastNative = nativeAttack;
            nextInspection = npc.tickCount + 20;
            type = Optional.ofNullable(selected)
                    .flatMap(value -> FlansEntityTypes.getProperties(value, options.enabled(Feature.SECONDARY_BANK), options.enabled(Feature.MODEL_MUZZLES)))
                    .orElse(null);
            double maxHealth = Attributes.MAX_HEALTH instanceof RangedAttribute attribute ? attribute.getMaxValue() : Integer.MAX_VALUE;
            mapped = Optional.ofNullable(type).map(properties -> NpcTypeMapping.create(properties, options, nativeAttack, maxHealth)).orElse(Map.of());
        }
        if (overrides.refresh(mapped, property -> NpcTypePropertyAccess.read(npc, property), (property, value) -> NpcTypePropertyAccess.write(npc, property, value))
            && !npc.level().isClientSide)
        {
            npc.updateClient = true;
            npc.updateAI = true;
        }
    }

    public Map<NpcTypeProperty, Tag> values()
    {
        return overrides.values();
    }

    public Optional<EntityTypeProperties> type()
    {
        return Optional.ofNullable(type);
    }

    public boolean nativeTiming()
    {
        return lastNative && type != null && type.shootDelay().isPresent();
    }

    public boolean usesNativeProjectile()
    {
        return lastNative && type != null;
    }

    public double shotDelay()
    {
        return Optional.ofNullable(type).map(properties -> properties.shootDelay().orElse(1D)).orElse(1D);
    }

    public void loaded(Component component)
    {
        overrides.loaded(component);
        lastFlags = -1;
    }

    public void beforeStatsLoad(EntityNPCInterface npc)
    {
        loadingHealthFraction = values().containsKey(NpcTypeProperty.HEALTH) ? NpcTypeHealth.fraction(npc.getHealth(), npc.getMaxHealth()) : -1F;
    }

    public void afterStatsLoad(EntityNPCInterface npc)
    {
        if (loadingHealthFraction >= 0F)
            npc.setHealth(loadingHealthFraction * npc.getMaxHealth());
        loadingHealthFraction = -1F;
    }

    public void saveDefaults(Component component, CompoundTag tag)
    {
        overrides.saveDefaults(component, tag);
    }

    public CompoundTag syncDefaults(EntityNPCInterface npc)
    {
        CompoundTag tag = new CompoundTag();
        ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().save(tag);
        for (NpcTypeProperty property : NpcTypeProperty.values())
            tag.put(property.name(), overrides.stored(property, key -> NpcTypePropertyAccess.read(npc, key)).copy());
        return tag;
    }

    public void readSyncedDefaults(EntityNPCInterface npc, CompoundTag tag)
    {
        float fraction = NpcTypeHealth.fraction(npc.getHealth(), npc.getMaxHealth());
        ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().load(tag);
        for (Component component : Component.values())
            loaded(component);
        for (NpcTypeProperty property : NpcTypeProperty.values())
        {
            Tag value = tag.get(property.name());
            if (value != null)
                NpcTypePropertyAccess.write(npc, property, value);
        }
        npc.setHealth(fraction * npc.getMaxHealth());
        refresh(npc);
    }

    public static NpcTypeProperties of(EntityNPCInterface npc)
    {
        return ((NpcWeaponSettings) npc.stats).wolffsmodnpcsTypeProperties();
    }

    public static Component component(Object data)
    {
        return data instanceof DataAI ? Component.AI : Component.ADVANCED;
    }

    private static int flags(NpcWeaponOptions options)
    {
        int flags = 0;
        for (Feature feature : Feature.values())
            if (options.enabled(feature))
                flags |= 1 << feature.ordinal();
        return flags;
    }
}
