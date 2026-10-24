package com.wolffsmod.npcs.properties;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.ReloadCycle;
import com.flansmodultimate.api.driveables.WeaponMuzzle;
import com.flansmodultimate.api.entity.EntityTypeProperties;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.flansmodultimate.api.equipment.MechaMeleeProperties;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.wolffsmod.npcs.combat.*;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.diag.NpcModelRanges;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.model.FlanModelKind;
import com.wolffsmod.npcs.platform.NpcPlatform;
import com.wolffsmod.npcs.properties.NpcTypeProperty.Component;
import com.wolffsmod.npcs.sim.NpcMobility;
import com.wolffsmod.npcs.sim.NpcTuning;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataAI;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/** Per-NPC inheritance cache. Only the live NPC owns gameplay state; its selected model supplies definitions. */
public final class NpcTypeProperties
{
    private final NpcPropertyOverrides overrides = new NpcPropertyOverrides();
    private IContentType lastType;
    private LivingEntity lastModel;
    private long lastFlags = -1L;
    private boolean lastNative;
    private Optional<MechaMeleeProperties> lastMelee = Optional.empty();
    private int nextInspection;
    @Nullable
    private EntityTypeProperties type;
    /** Every value the model offers, including those the NPC keeps its own value for. */
    private Map<NpcTypeProperty, Tag> inherited = Map.of();
    /** The inherited values actually applied to the NPC. */
    private Map<NpcTypeProperty, Tag> mapped = Map.of();
    /**
     * Every property the model decides while inherited: those it offers a value for, and those its mounts fire with values
     * of their own, too different for one Custom NPCs value.
     */
    private Set<NpcTypeProperty> supplied = Set.of();
    @Nullable
    private ReloadCycle cycle;
    private float loadingHealthFraction = -1F;

    public void refresh(EntityNPCInterface npc)
    {
        NpcWeaponSettings settings = (NpcWeaponSettings) npc.stats;
        NpcWeaponOptions options = settings.wolffsmodnpcsWeaponOptions();
        NpcCombatSettings combat = settings.wolffsmodnpcsCombat();
        LivingEntity model = options.enabled(Feature.TYPE_PROPERTIES) && npc instanceof EntityCustomNpc custom ? custom.modelData.getEntity(npc) : null;
        IContentType selected = model instanceof FlanModelEntity flan ? flan.getInfoType() : null;
        ItemStack ammunition = NpcAmmunition.modelTemplate(npc);
        boolean nativeAttack = NpcRangedAttack.usesFlanProjectile(options, ammunition);
        Optional<MechaMeleeProperties> melee = NpcMechaTools.properties(npc);
        // There are more switches than fit beside the signature, so the two are mixed rather than packed side by side.
        long flags = flags(options) * 31L + combat.customSignature();
        if (model != lastModel || selected != lastType || flags != lastFlags || nativeAttack != lastNative || !melee.equals(lastMelee) || npc.tickCount >= nextInspection)
        {
            lastModel = model;
            lastType = selected;
            lastFlags = flags;
            lastNative = nativeAttack;
            lastMelee = melee;
            nextInspection = npc.tickCount + 20;
            double modelHealth = model != null && !(model instanceof FlanModelEntity) ? ((NpcModelHealth) ((EntityCustomNpc) npc).modelData).wolffsmodnpcsModelMaxHealth() : Double.NaN;
            ItemStack reference = selected == null ? ammunition : NpcAmmunition.modelReference(npc, ammunition);
            inspect(options, combat, selected, reference, modelHealth, new ModelRanges(NpcModelRanges.aggro(npc), NpcModelRanges.firing(npc)));
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

    /**
     * Reads what the selected model offers with the NPC's switches and ammunition, and which of it the NPC inherits.
     *
     * @param reference
     *            the ammunition the model's statistics are read with: what the NPC fires, or else the model's default
     */
    private void inspect(NpcWeaponOptions options, NpcCombatSettings combat, @Nullable IContentType selected, ItemStack reference, double modelHealth, ModelRanges ranges)
    {
        boolean secondary = options.enabled(Feature.SECONDARY_BANK);
        type = Optional.ofNullable(selected).flatMap(value -> FlansEntityTypes.getProperties(value, secondary, options.enabled(Feature.MODEL_MUZZLES))).orElse(null);
        // Without a round of its own the NPC still bursts and reloads as the model's default ammunition would.
        boolean followsCycle = type != null && FlansProjectiles.isProjectile(reference) && options.enabled(Feature.RELOAD_CYCLE) && type.shootDelay().isPresent();
        cycle = followsCycle ? FlansEntityTypes.getReloadCycle(selected, secondary, reference).orElse(null) : null;
        double maxHealth = NpcPlatform.maxHealthLimit();
        Map<NpcTypeProperty, Tag> offered = new EnumMap<>(NpcTypeProperty.class);
        if (type != null)
            offered.putAll(NpcTypeMapping.create(type, options, lastNative, maxHealth, cycle));
        else if (Double.isFinite(modelHealth) && modelHealth > 0D)
            offered.put(NpcTypeProperty.HEALTH, IntTag.valueOf((int) Math.max(1D, Math.min(Math.ceil(modelHealth), Math.min(Integer.MAX_VALUE, maxHealth)))));
        FlanModelKind kind = FlanModelKind.of(selected);
        if (type != null && kind != null)
            offerMachineValues(offered, kind, selected, secondary, reference, options, ranges);
        if (type != null && kind == FlanModelKind.MECHA)
            offerMeleeValues(offered, lastMelee);
        // Vehicle engines have their own presentation controller; mecha stomps remain ordinary inherited footsteps.
        if (kind == FlanModelKind.VEHICLE || kind == FlanModelKind.PLANE)
        {
            offered.remove(NpcTypeProperty.STEP_SOUND);
            offered.remove(NpcTypeProperty.IDLE_SOUND);
        }
        inherited = Map.copyOf(offered);
        supplied = Set.copyOf(type == null ? inherited.keySet() : NpcTypeReadouts.create(type, inherited, options, lastNative).keySet());
        offered.keySet().removeAll(combat.customProperties());
        mapped = Map.copyOf(offered);
    }

    /**
     * What an AA gun, vehicle, plane or mecha lends the NPC wearing it beyond its definition: the server's model ranges, no
     * melee damage unless it is a mecha, an AA gun's standing still, and the velocity the round it fires with leaves at.
     */
    private void offerMachineValues(Map<NpcTypeProperty, Tag> offered, FlanModelKind kind, IContentType selected, boolean secondary, ItemStack reference, NpcWeaponOptions options, ModelRanges ranges)
    {
        int limit = NpcMobility.rangeLimit();
        offered.put(NpcTypeProperty.AGGRO_RANGE, IntTag.valueOf(Math.min(limit, ranges.aggro())));
        offered.put(NpcTypeProperty.RANGE, IntTag.valueOf(Math.min(limit, ranges.firing())));
        if (kind != FlanModelKind.MECHA)
            offered.put(NpcTypeProperty.MELEE_DAMAGE, IntTag.valueOf(0));
        if (kind == FlanModelKind.AA_GUN)
            offered.put(NpcTypeProperty.WALKING_SPEED, IntTag.valueOf(0));
        // Mounts firing at velocities of their own keep showing them side by side.
        boolean mixedMounts = type != null && type.weapons().size() > 1;
        if (!options.enabled(Feature.MODEL_WEAPON_STATS) || !FlansProjectiles.isProjectile(reference) || mixedMounts)
            return;
        IContentType weapon = FlansProjectiles.getMuzzles(selected, secondary, 0, false).stream().findFirst().flatMap(WeaponMuzzle::weapon).orElse(null);
        FlansProjectiles.getMuzzleVelocity(reference, weapon, selected, secondary)
            .ifPresent(velocity -> offered.put(NpcTypeProperty.SPEED, IntTag.valueOf((int) Mth.clamp(Math.ceil(velocity * 10D), 0D, 100D))));
    }

    /** A sword supplies all three melee fields together; removing it restores zero damage and the saved cadence/reach. */
    static void offerMeleeValues(Map<NpcTypeProperty, Tag> offered, Optional<MechaMeleeProperties> melee)
    {
        offered.put(NpcTypeProperty.MELEE_DAMAGE, IntTag.valueOf(melee.map(value -> Mth.ceil(value.damage())).orElse(0)));
        offered.remove(NpcTypeProperty.MELEE_REACH);
        offered.remove(NpcTypeProperty.MELEE_DELAY);
        melee.ifPresent(value ->
        {
            offered.put(NpcTypeProperty.MELEE_REACH, FloatTag.valueOf((float) value.reach()));
            offered.put(NpcTypeProperty.MELEE_DELAY, IntTag.valueOf(value.delay()));
        });
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
        Set<EquipmentProperty> equipment = combat.takeEquipmentImports();
        if (!equipment.isEmpty())
        {
            overrides.suspend((key, tag) -> NpcTypePropertyAccess.write(npc, key, tag));
            equipment.forEach(property -> NpcEquipmentImport.apply(npc, property));
        }
        return true;
    }

    public Map<NpcTypeProperty, Tag> values()
    {
        return overrides.values();
    }

    /** @return the NPC's own saved value, which the model's value may currently cover */
    public Tag own(EntityNPCInterface npc, NpcTypeProperty property)
    {
        return overrides.stored(property, key -> NpcTypePropertyAccess.read(npc, key));
    }

    /** Sets the NPC's own saved value without writing into a value the model still covers. */
    public void setOwn(EntityNPCInterface npc, NpcTypeProperty property, Tag value)
    {
        overrides.setStored(property, value, (key, tag) -> NpcTypePropertyAccess.write(npc, key, tag));
    }

    /**
     * Copies the model's value into the NPC's own and makes the property custom, at once. The editor calls this on its copy
     * of the NPC, which it sends to the server like any other edit. A property the model does not offer is left alone.
     */
    public void importNow(EntityNPCInterface npc, NpcTypeProperty property)
    {
        Tag value = inherited.get(property);
        if (value == null)
            return;
        ((NpcWeaponSettings) npc.stats).wolffsmodnpcsCombat().setCustom(property, true);
        overrides.adopt(property, value, key -> NpcTypePropertyAccess.read(npc, key), (key, tag) -> NpcTypePropertyAccess.write(npc, key, tag));
    }

    /**
     * Copies what the equipment offers for the statistic into the NPC's own values at once, with the model's overlay lifted
     * so that the copy is not written into a value the model covers; the next refresh lays the overlay again. A statistic
     * the equipment does not offer is left alone.
     */
    public void importNow(EntityNPCInterface npc, EquipmentProperty property)
    {
        overrides.suspend((key, tag) -> NpcTypePropertyAccess.write(npc, key, tag));
        if (NpcEquipmentImport.apply(npc, property))
            ((NpcWeaponSettings) npc.stats).wolffsmodnpcsCombat().setCustom(property, true);
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
        return settings.wolffsmodnpcsCombat().isCustom(property) && settings.wolffsmodnpcsTypeProperties().supplied.contains(property);
    }

    /** @return every property the model decides while inherited, including values its mounts differ in */
    public Set<NpcTypeProperty> supplied()
    {
        return supplied;
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
        ((NpcWeaponSettings) npc.stats).wolffsmodnpcsParts().save(tag);
        NpcTuning.of(npc).save(tag);
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
        ((NpcWeaponSettings) npc.stats).wolffsmodnpcsParts().load(tag);
        NpcTuning.of(npc).load(tag);
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

    /** The aggro and firing ranges, in blocks, a model lends the NPC. */
    private record ModelRanges(int aggro, int firing)
    {}
}
