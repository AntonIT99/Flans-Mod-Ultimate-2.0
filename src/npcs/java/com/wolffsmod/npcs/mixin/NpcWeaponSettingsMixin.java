package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.combat.*;
import com.wolffsmod.npcs.crew.NpcCrewSettings;
import com.wolffsmod.npcs.diag.NpcServerSettings;
import com.wolffsmod.npcs.properties.NpcPresentation;
import com.wolffsmod.npcs.properties.NpcTypeProperties;
import com.wolffsmod.npcs.properties.NpcTypeProperty.Component;
import com.wolffsmod.npcs.sim.NpcTuning;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataRanged;
import noppes.npcs.entity.data.DataStats;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.nbt.CompoundTag;

/**
 * Every hook into the Custom NPCs stats data.
 *
 * <ul>
 * <li>The mod's per-NPC settings use Custom NPCs' existing permission-checked stats menu and normal persistence.</li>
 * <li>A newly created NPC starts with the server's aggro and firing ranges, gunfight distances by default instead of Custom
 * NPCs' melee-scale ones; a saved NPC keeps the ranges it was saved with until an operator applies the server's ranges to
 * every NPC, and remembers whether it has taken them. It remembers the same of the server's search intervals.</li>
 * </ul>
 */
@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(value = DataStats.class, remap = false)
public abstract class NpcWeaponSettingsMixin implements NpcWeaponSettings
{
    @Unique
    private static final String WOLFFSMODNPCS_RANGE_GENERATION = "WolffsModRangeGeneration";
    @Unique
    private static final String WOLFFSMODNPCS_SEARCH_GENERATION = "WolffsModSearchGeneration";

    @Shadow
    public int aggroRange;
    @Shadow
    public DataRanged ranged;
    @Shadow
    private EntityNPCInterface npc;

    @Unique
    private final NpcWeaponOptions wolffsmodnpcsOptions = new NpcWeaponOptions();

    @Unique
    private final NpcTypeProperties wolffsmodnpcsProperties = new NpcTypeProperties();

    @Unique
    private final NpcPresentation wolffsmodnpcsPresentation = new NpcPresentation();

    @Unique
    private final NpcCombatSettings wolffsmodnpcsCombat = new NpcCombatSettings();
    @Unique
    private final NpcPartSettings wolffsmodnpcsParts = new NpcPartSettings();
    @Unique
    private final NpcTuning wolffsmodnpcsTuning = new NpcTuning();
    @Unique
    private final NpcCrewSettings wolffsmodnpcsCrew = new NpcCrewSettings();
    @Unique
    private int wolffsmodnpcsRangeGeneration;
    @Unique
    private int wolffsmodnpcsSearchGeneration;

    // Defaults

    /** Runs only for a new NPC: loading a saved one overwrites both ranges and the generation with the saved values. */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void wolffsmodnpcsDefaultRanges(EntityNPCInterface owner, CallbackInfo callback)
    {
        NpcServerSettings settings = NpcServerSettings.of(owner);
        aggroRange = settings == null ? NpcServerSettings.DEFAULT_AGGRO_RANGE : settings.getAggroRange();
        ranged.setRange(settings == null ? NpcServerSettings.DEFAULT_FIRING_RANGE : settings.getFiringRange());
        // Off the server thread the settings cannot be read: the NPC then takes the server's ranges at its first tick.
        wolffsmodnpcsRangeGeneration = settings == null ? -1 : settings.getRangeGeneration();
        // A new NPC has no intervals of its own to give up.
        wolffsmodnpcsSearchGeneration = settings == null ? -1 : settings.getSearchGeneration();
    }

    // Persistence

    @Inject(method = "readToNBT", at = @At("HEAD"))
    private void wolffsmodnpcsRetainHealth(CompoundTag tag, CallbackInfo callback)
    {
        wolffsmodnpcsProperties.beforeStatsLoad(npc);
    }

    @Inject(method = "readToNBT", at = @At("TAIL"))
    private void wolffsmodnpcsReadWeapons(CompoundTag compound, CallbackInfo callback)
    {
        wolffsmodnpcsOptions.load(compound);
        wolffsmodnpcsPresentation.load(compound);
        wolffsmodnpcsCombat.load(compound);
        wolffsmodnpcsParts.load(compound);
        wolffsmodnpcsTuning.load(compound);
        wolffsmodnpcsCrew.load(compound);
        wolffsmodnpcsRangeGeneration = compound.getInt(WOLFFSMODNPCS_RANGE_GENERATION);
        wolffsmodnpcsSearchGeneration = compound.getInt(WOLFFSMODNPCS_SEARCH_GENERATION);
        wolffsmodnpcsProperties.loaded(Component.STATS);
        wolffsmodnpcsProperties.afterStatsLoad(npc);
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void wolffsmodnpcsSaveWeapons(CompoundTag tag, CallbackInfoReturnable<CompoundTag> callback)
    {
        wolffsmodnpcsOptions.save(callback.getReturnValue());
        wolffsmodnpcsPresentation.save(callback.getReturnValue());
        wolffsmodnpcsCombat.save(callback.getReturnValue());
        wolffsmodnpcsParts.save(callback.getReturnValue());
        wolffsmodnpcsTuning.save(callback.getReturnValue());
        wolffsmodnpcsCrew.save(callback.getReturnValue());
        callback.getReturnValue().putInt(WOLFFSMODNPCS_RANGE_GENERATION, wolffsmodnpcsRangeGeneration);
        callback.getReturnValue().putInt(WOLFFSMODNPCS_SEARCH_GENERATION, wolffsmodnpcsSearchGeneration);
        wolffsmodnpcsProperties.saveDefaults(Component.STATS, callback.getReturnValue());
    }

    @Override
    public NpcWeaponOptions wolffsmodnpcsWeaponOptions()
    {
        return wolffsmodnpcsOptions;
    }

    @Override
    public NpcTypeProperties wolffsmodnpcsTypeProperties()
    {
        return wolffsmodnpcsProperties;
    }

    @Override
    public NpcPresentation wolffsmodnpcsPresentation()
    {
        return wolffsmodnpcsPresentation;
    }

    @Override
    public NpcCombatSettings wolffsmodnpcsCombat()
    {
        return wolffsmodnpcsCombat;
    }

    @Override
    public NpcPartSettings wolffsmodnpcsParts()
    {
        return wolffsmodnpcsParts;
    }

    @Override
    public NpcTuning wolffsmodnpcsTuning()
    {
        return wolffsmodnpcsTuning;
    }

    @Override
    public NpcCrewSettings wolffsmodnpcsCrew()
    {
        return wolffsmodnpcsCrew;
    }

    @Override
    public int wolffsmodnpcsRangeGeneration()
    {
        return wolffsmodnpcsRangeGeneration;
    }

    @Override
    public void wolffsmodnpcsRangeGeneration(int generation)
    {
        wolffsmodnpcsRangeGeneration = generation;
    }

    @Override
    public int wolffsmodnpcsSearchGeneration()
    {
        return wolffsmodnpcsSearchGeneration;
    }

    @Override
    public void wolffsmodnpcsSearchGeneration(int generation)
    {
        wolffsmodnpcsSearchGeneration = generation;
    }
}
