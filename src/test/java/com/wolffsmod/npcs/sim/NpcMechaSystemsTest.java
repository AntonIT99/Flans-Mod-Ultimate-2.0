package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.driveables.DriveableKind;
import com.flansmodultimate.api.driveables.MechaGait;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.api.equipment.MechaUpgrades;
import com.flansmodultimate.common.item.MechaAddonItem;
import com.flansmodultimate.common.types.MechaItemType;
import com.flansmodultimate.common.types.MechaType;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.model.FlanModelEntity;
import noppes.npcs.ModelData;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.data.DataInventory;
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcMechaSystemsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void passiveAddonsCombineWithoutMultiplyingDuplicatesAndClampLight()
    {
        ItemStack first = addon(true, false, 2F, 8, false);
        ItemStack second = addon(false, true, 5F, 30, true);
        assertEquals(new MechaUpgrades(true, true, 5F, 15, true), FlansEquipment.getMechaUpgrades(List.of(first, second, second, ItemStack.EMPTY)));
        assertEquals(MechaUpgrades.NONE, FlansEquipment.getMechaUpgrades(List.of()));
    }

    @Test
    void carriedUpgradesHealOnlyActiveLivingMechaModelsAndClearWhenInheritanceIsDisabled()
    {
        EntityCustomNpc npc = mock(EntityCustomNpc.class);
        npc.modelData = mock(ModelData.class);
        npc.inventory = new DataInventory(npc);
        npc.stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
        NpcWeaponOptions options = new NpcWeaponOptions();
        when(((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions()).thenReturn(options);
        MechaType type = mock(MechaType.class);
        when(type.getDriveableKind()).thenReturn(DriveableKind.MECHA);
        FlanModelEntity model = mock(FlanModelEntity.class);
        when(model.getInfoType()).thenReturn(type);
        when(npc.modelData.getEntity(npc)).thenReturn(model);
        when(npc.getMainHandItem()).thenReturn(ItemStack.EMPTY);
        when(npc.getOffhandItem()).thenReturn(ItemStack.EMPTY);
        when(npc.getDeltaMovement()).thenReturn(Vec3.ZERO);
        when(npc.isAlive()).thenReturn(true);
        ItemStack upgrade = addon(true, false, 3F, 12, false);
        IItemStack wrapped = mock(IItemStack.class);
        when(wrapped.getMCItemStack()).thenReturn(upgrade);
        npc.inventory.drops.put(5, wrapped);
        NpcMechaSystems systems = new NpcMechaSystems();
        systems.refresh(npc);
        assertTrue(systems.getUpgrades().stopFallDamage());
        assertEquals(12, systems.getUpgrades().lightLevel());
        npc.tickCount = 20;
        systems.tick(npc);
        verify(npc).heal(3F);
        options.set(Feature.TYPE_PROPERTIES, false);
        systems.refresh(npc);
        assertEquals(MechaUpgrades.NONE, systems.getUpgrades());
        systems.tick(npc);
        verify(npc, times(1)).heal(anyFloat());
    }

    @Test
    void landingUsesTheLastAirborneVelocity()
    {
        EntityCustomNpc npc = mock(EntityCustomNpc.class);
        npc.modelData = mock(ModelData.class);
        npc.stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
        when(((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions()).thenReturn(new NpcWeaponOptions());
        MechaType type = mock(MechaType.class);
        when(type.getDriveableKind()).thenReturn(DriveableKind.MECHA);
        FlanModelEntity model = mock(FlanModelEntity.class);
        when(model.getInfoType()).thenReturn(type);
        when(npc.modelData.getEntity(npc)).thenReturn(model);
        when(npc.isAlive()).thenReturn(true);
        when(npc.onGround()).thenReturn(false, false, false, false, false, true, true, true);
        when(npc.getDeltaMovement()).thenReturn(new Vec3(0D, -0.9D, 0D), new Vec3(0D, -0.2D, 0D));
        when(npc.position()).thenReturn(Vec3.ZERO);
        NpcMechaSystems systems = new NpcMechaSystems();
        try (MockedStatic<FlansEntityTypes> types = mockStatic(FlansEntityTypes.class))
        {
            systems.tick(npc);
            systems.tick(npc);
            systems.tick(npc);
            types.verify(() -> FlansEntityTypes.applyMechaLanding(npc, type, false, -0.2D));
        }
    }

    @Test
    void authoredFootfallWindowHandlesCycleWrap()
    {
        MechaGait gait = new MechaGait("stomp", 5, 0.12F, 0.2F, 0.3F);
        assertTrue(gait.crosses(0.15F, 0.27F));
        assertFalse(gait.crosses(0.4F, 0.52F));
        MechaGait atStart = new MechaGait("stomp", 5, 0.12F, 0F, 0.1F);
        assertTrue(atStart.crosses(0.95F, 0.07F));
    }

    private static ItemStack addon(boolean fall, boolean breaking, float repair, int light, boolean dark)
    {
        MechaItemType type = mock(MechaItemType.class);
        when(type.isStopMechaFallDamage()).thenReturn(fall);
        when(type.isForceBlockFallDamage()).thenReturn(breaking);
        when(type.isAutoRepair()).thenReturn(repair > 0F);
        when(type.getAutoRepairAmount()).thenReturn(repair);
        when(type.getLightLevel()).thenReturn(light);
        when(type.isForceDark()).thenReturn(dark);
        MechaAddonItem item = mock(MechaAddonItem.class);
        when(item.getConfigType()).thenReturn(type);
        ItemStack stack = mock(ItemStack.class);
        when(stack.getItem()).thenReturn(item);
        return stack;
    }
}
