package com.wolffsmod.npcs.properties;

import com.flansmodultimate.api.*;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.*;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.mixin.ModelDataMixin;
import com.wolffsmod.npcs.model.FlanModelEntity;
import noppes.npcs.ModelData;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.data.DataInventory;
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcModelHealthTest
{
    private final NpcWeaponOptions options = new NpcWeaponOptions();
    private final NpcCombatSettings combat = new NpcCombatSettings();
    private final NpcTypeProperties properties = new NpcTypeProperties();
    private float health = 10F;

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    private EntityCustomNpc npc(double modelHealth)
    {
        EntityCustomNpc npc = mock(EntityCustomNpc.class);
        npc.modelData = mock(ModelData.class, withSettings().extraInterfaces(NpcModelHealth.class));
        npc.stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
        npc.stats.maxHealth = 20;
        npc.inventory = new DataInventory(npc);
        NpcWeaponSettings settings = (NpcWeaponSettings) npc.stats;
        when(settings.wolffsmodnpcsWeaponOptions()).thenReturn(options);
        when(settings.wolffsmodnpcsCombat()).thenReturn(combat);
        Level level = mock(Level.class);
        when(npc.level()).thenReturn(level);
        when(npc.getMaxHealth()).thenAnswer(invocation -> (float) npc.stats.maxHealth);
        when(npc.getHealth()).thenAnswer(invocation -> health);
        doAnswer(invocation ->
        {
            npc.stats.maxHealth = invocation.getArgument(0);
            return null;
        }).when(npc.stats).setMaxHealth(anyInt());
        doAnswer(invocation ->
        {
            health = invocation.getArgument(0);
            return null;
        }).when(npc).setHealth(anyFloat());
        model(npc, modelHealth);
        return npc;
    }

    private static void model(EntityCustomNpc npc, double maximum)
    {
        LivingEntity model = mock(LivingEntity.class);
        // Custom NPCs already replaced the detached entity's maximum; inheritance must use the captured value.
        when(model.getMaxHealth()).thenReturn(20F);
        when(npc.modelData.getEntity(npc)).thenReturn(model);
        when(((NpcModelHealth) npc.modelData).wolffsmodnpcsModelMaxHealth()).thenReturn(maximum);
    }

    @Test
    void inheritsOnlyHealthFromAnyLivingModelAndPreservesDamageWhenTheModelChanges()
    {
        EntityCustomNpc npc = npc(80D);
        properties.refresh(npc);
        assertEquals(Map.of(NpcTypeProperty.HEALTH, IntTag.valueOf(80)), properties.inherited());
        assertEquals(Set.of(NpcTypeProperty.HEALTH), properties.supplied());
        assertTrue(properties.type().isEmpty());
        assertEquals(40F, health);
        properties.refresh(npc);
        assertEquals(40F, health);
        model(npc, 100D);
        // A new entity model is noticed immediately, without waiting for the periodic inspection.
        properties.refresh(npc);
        assertEquals(100, npc.stats.maxHealth);
        assertEquals(50F, health);
        CompoundTag saved = new CompoundTag();
        properties.saveDefaults(NpcTypeProperty.Component.STATS, saved);
        assertEquals(20, saved.getInt("MaxHealth"));
    }

    @Test
    void customImportDisableAndNoModelKeepTheExistingOverlaySemantics()
    {
        EntityCustomNpc npc = npc(80D);
        combat.setCustom(NpcTypeProperty.HEALTH, true);
        properties.refresh(npc);
        assertEquals(20, npc.stats.maxHealth);
        assertTrue(properties.values().isEmpty());
        properties.importNow(npc, NpcTypeProperty.HEALTH);
        assertEquals(80, npc.stats.maxHealth);
        assertEquals(40F, health);
        combat.setCustom(NpcTypeProperty.HEALTH, false);
        properties.refresh(npc);
        model(npc, 100D);
        properties.refresh(npc);
        options.set(Feature.TYPE_PROPERTIES, false);
        properties.refresh(npc);
        assertEquals(80, npc.stats.maxHealth);
        assertEquals(40F, health);
        options.set(Feature.TYPE_PROPERTIES, true);
        properties.refresh(npc);
        when(npc.modelData.getEntity(npc)).thenReturn(null);
        properties.refresh(npc);
        assertEquals(80, npc.stats.maxHealth);
        assertEquals(40F, health);
        assertTrue(properties.inherited().isEmpty());
    }

    @Test
    void roundsUpFractionalHealthClampsToTheAttributeLimitAndRejectsInvalidSources()
    {
        EntityCustomNpc npc = npc(30.5D);
        properties.refresh(npc);
        assertEquals(31, npc.stats.maxHealth);
        model(npc, 5000D);
        properties.refresh(npc);
        assertEquals(1024, npc.stats.maxHealth);
        for (double invalid : new double[]{Double.NaN, Double.POSITIVE_INFINITY, 0D, -1D})
        {
            model(npc, invalid);
            properties.refresh(npc);
            assertEquals(20, npc.stats.maxHealth);
            assertTrue(properties.inherited().isEmpty());
        }
    }

    @Test
    void modelHealthReadoutsWorkWithoutAFlanDefinition()
    {
        assertEquals(Map.of(NpcTypeProperty.HEALTH, "80"), NpcTypeReadouts.create(null, Map.of(NpcTypeProperty.HEALTH, IntTag.valueOf(80)), options, false));
    }

    @Test
    @SuppressWarnings("java:S3011") // Exercise the injection at the boundary where Custom NPCs overwrites model health.
    void capturesTheModelMaximumBeforeTheNpcMaximumReplacesIt() throws ReflectiveOperationException
    {
        ModelDataMixin data = new ModelDataMixin()
        {};
        assertTrue(Double.isNaN(data.wolffsmodnpcsModelMaxHealth()));
        AttributeInstance attribute = new AttributeInstance(Attributes.MAX_HEALTH, changed ->
        {
        });
        attribute.setBaseValue(80D);
        var capture = ModelDataMixin.class.getDeclaredMethod("wolffsmodnpcsCaptureModelHealth", AttributeInstance.class, double.class);
        capture.setAccessible(true);
        capture.invoke(data, attribute, 20D);
        assertEquals(80D, data.wolffsmodnpcsModelMaxHealth());
        assertEquals(20D, attribute.getValue());
        attribute.setBaseValue(100D);
        capture.invoke(data, attribute, 30D);
        assertEquals(100D, data.wolffsmodnpcsModelMaxHealth());
        assertEquals(30D, attribute.getValue());
    }

    @Test
    void flanModelsStillUseTheirDefinitionHealthInsteadOfTheDetachedEntityAttribute()
    {
        EntityCustomNpc npc = npc(80D);
        FlanModelEntity model = mock(FlanModelEntity.class);
        IContentType definition = mock(IContentType.class);
        when(model.getInfoType()).thenReturn(definition);
        when(npc.modelData.getEntity(npc)).thenReturn(model);
        EntityTypeProperties type = mock(EntityTypeProperties.class);
        when(type.health()).thenReturn(OptionalDouble.of(200D));
        try (var inspection = mockStatic(FlansEntityTypes.class))
        {
            inspection.when(() -> FlansEntityTypes.getProperties(eq(definition), anyBoolean(), anyBoolean())).thenReturn(Optional.of(type));
            properties.refresh(npc);
        }
        assertEquals(200, npc.stats.maxHealth);
        assertEquals(100F, health);
    }
}
