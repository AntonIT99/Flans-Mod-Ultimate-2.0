package com.wolffsmod.npcs.client;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.EquipmentProperty;
import com.wolffsmod.npcs.combat.NpcCombatSettings;
import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.sim.NpcTuning;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.client.gui.SubGuiNpcProjectiles;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataInventory;
import noppes.npcs.entity.data.DataRanged;
import noppes.npcs.entity.data.DataStats;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcNativeThrowSpeedControlsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @ParameterizedTest
    @CsvSource({"false,7.5", "true,22.5"})
    void standardSpeedFieldShowsTheEffectiveSpeedWithoutOverwritingTheStoredNpcDefault(boolean custom, double expected)
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        DataStats stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
        NpcCombatSettings combat = new NpcCombatSettings();
        combat.setNativeThrowSpeed(2.25F);
        combat.setCustom(EquipmentProperty.NATIVE_THROW_SPEED, custom);
        NpcWeaponOptions options = new NpcWeaponOptions();
        when(((NpcWeaponSettings) stats).wolffsmodnpcsCombat()).thenReturn(combat);
        when(((NpcWeaponSettings) stats).wolffsmodnpcsWeaponOptions()).thenReturn(options);
        when(((NpcWeaponSettings) stats).wolffsmodnpcsTuning()).thenReturn(new NpcTuning());
        npc.stats = stats;
        npc.stats.ranged = mock(DataRanged.class);
        when(npc.getMainHandItem()).thenReturn(ItemStack.EMPTY);
        when(npc.getOffhandItem()).thenReturn(ItemStack.EMPTY);
        npc.inventory = new DataInventory(npc);
        IItemStack ammunition = mock(IItemStack.class);
        when(ammunition.getMCItemStack()).thenReturn(new ItemStack(Items.SPLASH_POTION));
        npc.inventory.weapons.put(1, ammunition);
        SubGuiNpcProjectiles gui = mock(SubGuiNpcProjectiles.class);
        GuiTextFieldNop field = mock(GuiTextFieldNop.class);
        when(gui.getTextField(4)).thenReturn(field);
        try (var labels = mockConstruction(GuiLabel.class))
        {
            RangedControlLocks locks = NpcEquipmentControls.apply(gui, npc);
            assertEquals(RangedControlLocks.Reason.NATIVE_THROW_SPEED, locks.textFields().get(4));
            assertEquals(RangedControlLocks.Reason.NATIVE_THROW_SPEED, locks.labels().get(4));
            ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);
            verify(field).setValue(value.capture());
            assertEquals(expected, Double.parseDouble(value.getValue()), 1.0E-6D);
            verify(field).setEditable(false);
            verify(npc.stats.ranged, never()).setSpeed(anyInt());
            assertEquals(1, labels.constructed().size());
        }
    }
}
