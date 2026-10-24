package com.wolffsmod.npcs.combat;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.properties.NpcTypeProperties;
import com.wolffsmod.npcs.sim.NpcTuning;
import noppes.npcs.EventHooks;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataInventory;
import noppes.npcs.entity.data.DataRanged;
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcNativeThrowSpeedTest
{
    private final NpcWeaponOptions options = new NpcWeaponOptions();
    private final NpcCombatSettings combat = new NpcCombatSettings();
    private final NpcTypeProperties properties = new NpcTypeProperties();

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    static Stream<Arguments> throwableDefaults()
    {
        return Stream.of(Arguments.of(Items.SPLASH_POTION, 0.75F), Arguments.of(Items.LINGERING_POTION, 0.75F), Arguments.of(Items.EXPERIENCE_BOTTLE, 0.7F), Arguments.of(Items.EGG, 1.5F),
            Arguments.of(Items.FIREWORK_ROCKET, 1.6F));
    }

    @ParameterizedTest
    @MethodSource("throwableDefaults")
    void itemDefaultsRemainLiveUntilACustomOverrideIsChosen(Item item, float speed)
    {
        ItemStack round = new ItemStack(item);
        EntityNPCInterface npc = npc(round);
        assertEquals(speed, launchSpeed(npc, round));
        assertTrue(EquipmentValues.inUse(npc, EquipmentProperty.NATIVE_THROW_SPEED));
        assertEquals(speed, NpcThrowables.ownSpeed(npc));
        combat.setNativeThrowSpeed(2.25F);
        assertEquals(speed, launchSpeed(npc, round));
        combat.setCustom(EquipmentProperty.NATIVE_THROW_SPEED, true);
        assertEquals(2.25F, launchSpeed(npc, round));
        options.set(Feature.ITEM_WEAPONS, false);
        assertEquals(2.25F, launchSpeed(npc, round));
        combat.setCustom(EquipmentProperty.NATIVE_THROW_SPEED, false);
        assertEquals(speed, launchSpeed(npc, round));
    }

    @ParameterizedTest
    @MethodSource("throwableDefaults")
    void importCopiesTheExactItemDefaultRatherThanTheOverrideOrRoundedNpcSpeed(Item item, float speed)
    {
        EntityNPCInterface npc = npc(new ItemStack(item));
        combat.setNativeThrowSpeed(3F);
        combat.setCustom(EquipmentProperty.NATIVE_THROW_SPEED, true);
        properties.importNow(npc, EquipmentProperty.NATIVE_THROW_SPEED);
        assertEquals(speed, combat.getNativeThrowSpeed());
        assertEquals(FloatTag.valueOf(speed), EquipmentValues.read(npc, EquipmentProperty.NATIVE_THROW_SPEED));
        assertTrue(combat.isCustom(EquipmentProperty.NATIVE_THROW_SPEED));
        CompoundTag saved = new CompoundTag();
        combat.save(saved);
        NpcCombatSettings restored = new NpcCombatSettings();
        restored.load(saved);
        assertEquals(speed, restored.getNativeThrowSpeed());
        assertTrue(restored.isCustom(EquipmentProperty.NATIVE_THROW_SPEED));
    }

    @ParameterizedTest
    @CsvSource({"0,0.01", "-5,0.01", "200,10", "2.25,2.25"})
    void decimalOverridesAreBoundedWithoutIntegerRounding(float input, float expected)
    {
        EntityNPCInterface npc = npc(new ItemStack(Items.SPLASH_POTION));
        EquipmentValues.write(npc, EquipmentProperty.NATIVE_THROW_SPEED, FloatTag.valueOf(input));
        assertEquals(expected, combat.getNativeThrowSpeed());
    }

    @Test
    void oldSavesAndInvalidValuesKeepTheCurrentItemsDefault()
    {
        EntityNPCInterface npc = npc(new ItemStack(Items.SPLASH_POTION));
        combat.setNativeThrowSpeed(3F);
        combat.load(new CompoundTag());
        assertFalse(combat.isCustom(EquipmentProperty.NATIVE_THROW_SPEED));
        assertEquals(0.75F, NpcThrowables.ownSpeed(npc));
        combat.setCustom(EquipmentProperty.NATIVE_THROW_SPEED, true);
        combat.setNativeThrowSpeed(Float.NaN);
        assertEquals(0.75F, NpcThrowables.ownSpeed(npc));
        combat.setNativeThrowSpeed(Float.POSITIVE_INFINITY);
        assertEquals(0.75F, NpcThrowables.ownSpeed(npc));
    }

    @Test
    void poweredFireChargesAndUnrecognizedItemsHaveNoInventedDefaultToImport()
    {
        EntityNPCInterface fireball = npc(new ItemStack(Items.FIRE_CHARGE));
        combat.setCustom(EquipmentProperty.NATIVE_THROW_SPEED, true);
        combat.setNativeThrowSpeed(3F);
        assertTrue(NpcThrowables.defaultSpeed(fireball).isEmpty());
        assertFalse(NpcEquipmentImport.apply(fireball, EquipmentProperty.NATIVE_THROW_SPEED));
        assertEquals(1F, launchSpeed(fireball, new ItemStack(Items.FIRE_CHARGE)));
        EntityNPCInterface snowball = npc(new ItemStack(Items.SNOWBALL));
        assertTrue(NpcThrowables.defaultSpeed(snowball).isEmpty());
        assertFalse(NpcEquipmentImport.apply(snowball, EquipmentProperty.NATIVE_THROW_SPEED));
    }

    @ParameterizedTest
    @CsvSource({"false,0.75", "true,2.25"})
    void aimingAndActualLaunchUseTheSameEffectiveSpeed(boolean custom, float expectedSpeed)
    {
        EntityNPCInterface npc = npc(new ItemStack(Items.SPLASH_POTION));
        combat.setNativeThrowSpeed(2.25F);
        combat.setCustom(EquipmentProperty.NATIVE_THROW_SPEED, custom);
        ServerLevel level = (ServerLevel) npc.level();
        MinecraftServer server = mock(MinecraftServer.class);
        when(level.getServer()).thenReturn(server);
        when(server.isSameThread()).thenReturn(true);
        when(level.addFreshEntity(any())).thenReturn(true);
        when(npc.getEyePosition()).thenReturn(Vec3.ZERO);
        LivingEntity target = mock(LivingEntity.class);
        when(target.level()).thenReturn(level);
        when(target.isAlive()).thenReturn(true);
        when(target.position()).thenReturn(new Vec3(10D, 0D, 0D));
        when(target.getBbHeight()).thenReturn(2F);
        Vec3 motion = new Vec3(0D, 0D, 0.2D);
        when(target.getDeltaMovement()).thenReturn(motion);
        NpcAPI npcApi = mock(NpcAPI.class);
        try (var projectiles = mockConstruction(ThrownPotion.class); var api = mockStatic(NpcAPI.class); var events = mockStatic(EventHooks.class))
        {
            api.when(NpcAPI::Instance).thenReturn(npcApi);
            assertTrue(NpcThrowables.fire(npc, target, false));
            Vec3 expectedAim = ProjectileAim.direction(new Vec3(10D, 1D, 0D), motion, expectedSpeed, 0.05D, 0.99D, false);
            assertEquals(1, projectiles.constructed().size());
            verify(projectiles.constructed().get(0)).shoot(expectedAim.x, expectedAim.y, expectedAim.z, expectedSpeed, 1F);
            events.verify(() -> EventHooks.onNPCRangedLaunched(eq(npc), any()));
        }
    }

    @Test
    void disablingNativeThrowablesPreservesOrdinaryProjectileHandlingAndTheStoredOverride()
    {
        EntityNPCInterface npc = npc(new ItemStack(Items.EGG));
        combat.setNativeThrowSpeed(2F);
        combat.setCustom(EquipmentProperty.NATIVE_THROW_SPEED, true);
        options.set(Feature.NATIVE_THROWABLES, false);
        assertFalse(EquipmentValues.inUse(npc, EquipmentProperty.NATIVE_THROW_SPEED));
        assertFalse(NpcThrowables.fire(npc, null, false));
        assertEquals(2F, combat.getNativeThrowSpeed());
    }

    private static float launchSpeed(EntityNPCInterface npc, ItemStack round)
    {
        NpcThrowables.Flight flight = NpcThrowables.flight(npc, round);
        assertNotNull(flight);
        return flight.speed();
    }

    private EntityNPCInterface npc(ItemStack ammunition)
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        DataStats stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
        when(((NpcWeaponSettings) stats).wolffsmodnpcsCombat()).thenReturn(combat);
        when(((NpcWeaponSettings) stats).wolffsmodnpcsWeaponOptions()).thenReturn(options);
        when(((NpcWeaponSettings) stats).wolffsmodnpcsTuning()).thenReturn(new NpcTuning());
        when(((NpcWeaponSettings) stats).wolffsmodnpcsTypeProperties()).thenReturn(properties);
        npc.stats = stats;
        npc.stats.ranged = mock(DataRanged.class);
        when(npc.stats.ranged.getSpeed()).thenReturn(99);
        when(npc.stats.ranged.getAccuracy()).thenReturn(100);
        when(npc.stats.ranged.getShotCount()).thenReturn(1);
        npc.inventory = mock(DataInventory.class);
        IItemStack slot = mock(IItemStack.class);
        when(slot.getMCItemStack()).thenReturn(ammunition);
        when(npc.inventory.getProjectile()).thenReturn(slot);
        ServerLevel level = mock(ServerLevel.class);
        when(npc.level()).thenReturn(level);
        return npc;
    }
}
