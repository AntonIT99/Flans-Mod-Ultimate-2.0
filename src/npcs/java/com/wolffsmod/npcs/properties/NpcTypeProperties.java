package com.wolffsmod.npcs.properties;

import com.flansmodultimate.api.*;
import com.wolffsmod.npcs.combat.*;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.model.FlanModelKind;
import com.wolffsmod.npcs.properties.NpcTypeProperty.Component;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataAI;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/** Per-NPC inheritance cache. Only the live NPC owns gameplay state; its selected model supplies definitions. */
public final class NpcTypeProperties
{
    private final NpcPropertyOverrides overrides = new NpcPropertyOverrides();
    private IContentType lastType;
    private long lastFlags = -1L;
    private boolean lastNative;
    private int nextInspection;
    @Nullable
    private EntityTypeProperties type;
    /** Every value the model offers, including those the NPC keeps its own value for. */
    private Map<NpcTypeProperty, Tag> inherited = Map.of();
    /** The inherited values actually applied to the NPC. */
    private Map<NpcTypeProperty, Tag> mapped = Map.of();
    @Nullable
    private ReloadCycle cycle;
    private float loadingHealthFraction = -1F;

    public void refresh(EntityNPCInterface npc)
    {
        NpcWeaponSettings settings = (NpcWeaponSettings) npc.stats;
        NpcWeaponOptions options = settings.wolffsmodnpcsWeaponOptions();
        NpcCombatSettings combat = settings.wolffsmodnpcsCombat();
        IContentType selected = options.enabled(Feature.TYPE_PROPERTIES) && npc instanceof EntityCustomNpc custom && custom.modelData.getEntity(npc) instanceof FlanModelEntity model
            ? model.getInfoType()
            : null;
        ItemStack ammunition = ItemStackWrapper.MCItem(npc.inventory.getProjectile());
        boolean nativeAttack = NpcRangedAttack.usesFlanProjectile(options, ammunition);
        long flags = flags(options) | (long) combat.customSignature() << 32;
        if (selected != lastType || flags != lastFlags || nativeAttack != lastNative || npc.tickCount >= nextInspection)
        {
            lastType = selected;
            lastFlags = flags;
            lastNative = nativeAttack;
            nextInspection = npc.tickCount + 20;
            inspect(options, combat, selected, ammunition);
        }
        boolean server = !npc.level().isClientSide;
        boolean changed = server && carryOutImports(npc, combat);
        changed |= overrides.refresh(mapped, property -> NpcTypePropertyAccess.read(npc, property), (property, value) -> NpcTypePropertyAccess.write(npc, property, value));
        if (changed && server)
        {
            npc.updateClient = true;
            npc.updateAI = true;
        }
    }

    /** Reads what the selected model offers with the NPC's switches and ammunition, and which of it the NPC inherits. */
    private void inspect(NpcWeaponOptions options, NpcCombatSettings combat, @Nullable IContentType selected, ItemStack ammunition)
    {
        boolean secondary = options.enabled(Feature.SECONDARY_BANK);
        type = Optional.ofNullable(selected).flatMap(value -> FlansEntityTypes.getProperties(value, secondary, options.enabled(Feature.MODEL_MUZZLES))).orElse(null);
        boolean followsCycle = type != null && lastNative && options.enabled(Feature.RELOAD_CYCLE) && type.shootDelay().isPresent();
        cycle = followsCycle ? FlansEntityTypes.getReloadCycle(selected, secondary, ammunition).orElse(null) : null;
        double maxHealth = Attributes.MAX_HEALTH instanceof RangedAttribute attribute ? attribute.getMaxValue() : Integer.MAX_VALUE;
        Map<NpcTypeProperty, Tag> offered = new EnumMap<>(NpcTypeProperty.class);
        if (type != null)
            offered.putAll(NpcTypeMapping.create(type, options, lastNative, maxHealth, cycle));
        // Vehicle engines have their own presentation controller; mecha stomps remain ordinary inherited footsteps.
        if (FlanModelKind.of(selected) == FlanModelKind.VEHICLE || FlanModelKind.of(selected) == FlanModelKind.PLANE)
        {
            offered.remove(NpcTypeProperty.STEP_SOUND);
            offered.remove(NpcTypeProperty.IDLE_SOUND);
        }
        inherited = Map.copyOf(offered);
        offered.keySet().removeAll(combat.customProperties());
        mapped = Map.copyOf(offered);
    }

    /**
     * Carries out the imports the editor asked for. A model value becomes the NPC's own value; equipment statistics are
     * written to the NPC's own values with the overlay lifted, so that they do not land in an inherited value and vanish.
     */
    private boolean carryOutImports(EntityNPCInterface npc, NpcCombatSettings combat)
    {
        if (!combat.hasRequests())
            return false;
        for (NpcTypeProperty property : combat.takePropertyImports())
        {
            Tag value = inherited.get(property);
            if (value != null)
                overrides.adopt(property, value, key -> NpcTypePropertyAccess.read(npc, key), (key, tag) -> NpcTypePropertyAccess.write(npc, key, tag));
        }
        Set<NpcCombatSettings.EquipmentImport> equipment = combat.takeEquipmentImports();
        if (!equipment.isEmpty())
        {
            overrides.suspend((key, tag) -> NpcTypePropertyAccess.write(npc, key, tag));
            equipment.forEach(kind -> NpcEquipmentImport.apply(npc, kind));
        }
        return true;
    }

    public Map<NpcTypeProperty, Tag> values()
    {
        return overrides.values();
    }

    /** @return every value the selected model offers, whether or not the NPC currently inherits it */
    public Map<NpcTypeProperty, Tag> inherited()
    {
        return inherited;
    }

    /** @return whether the model offers this property but the NPC keeps its own value for it */
    public static boolean usesOwnValue(EntityNPCInterface npc, NpcTypeProperty property)
    {
        NpcWeaponSettings settings = (NpcWeaponSettings) npc.stats;
        return settings.wolffsmodnpcsCombat().isCustom(property) && settings.wolffsmodnpcsTypeProperties().inherited.containsKey(property);
    }

    public Optional<EntityTypeProperties> type()
    {
        return Optional.ofNullable(type);
    }

    /** @return the model's reload cycle with the NPC's ammunition, while the NPC follows it */
    public Optional<ReloadCycle> reloadCycle()
    {
        return nativeTiming() ? Optional.ofNullable(cycle) : Optional.empty();
    }

    /** @return the model the NPC inherits from, or null without one */
    @Nullable
    public IContentType model()
    {
        return lastType;
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
        lastFlags = -1L;
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
        NpcPresentation.of(npc).save(tag);
        ((NpcWeaponSettings) npc.stats).wolffsmodnpcsCombat().save(tag);
        for (NpcTypeProperty property : NpcTypeProperty.values())
            tag.put(property.name(), overrides.stored(property, key -> NpcTypePropertyAccess.read(npc, key)).copy());
        return tag;
    }

    public void readSyncedDefaults(EntityNPCInterface npc, CompoundTag tag)
    {
        float fraction = NpcTypeHealth.fraction(npc.getHealth(), npc.getMaxHealth());
        ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().load(tag);
        NpcPresentation.of(npc).load(tag);
        ((NpcWeaponSettings) npc.stats).wolffsmodnpcsCombat().load(tag);
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

    private static long flags(NpcWeaponOptions options)
    {
        long flags = 0L;
        for (Feature feature : Feature.values())
            if (options.enabled(feature))
                flags |= 1L << feature.ordinal();
        return flags;
    }
}
