package com.flansmodultimate.network.server;

import com.flansmodultimate.FlansModItems;
import com.flansmodultimate.common.inventory.GunWorkbenchMenu;
import com.flansmodultimate.common.inventory.PaintjobTableMenu;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.paintjob.Paintjob;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.flansmodultimate.platform.network.PacketBuffer;
import io.netty.buffer.Unpooled;
import net.minecraftforge.registries.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PacketSelectPaintjobTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
        initializePaintcan();
    }

    @SuppressWarnings({"java:S3011", "UnstableApiUsage", "deprecation", "unchecked"}) // Plain JUnit initializes the item and its supplier through Forge's normal registry hook.
    private static void initializePaintcan() throws ReflectiveOperationException
    {
        RegistryObject<?> entry = (RegistryObject<?>) FlansModItems.rainbowPaintcan;
        if (entry.isPresent())
            return;

        ForgeRegistry<Item> registry = (ForgeRegistry<Item>) ForgeRegistries.ITEMS;
        registry.unfreeze();
        ((MappedRegistry<Item>) BuiltInRegistries.ITEM).unfreeze();
        try
        {
            registry.register(entry.getId(), new Item(new Item.Properties()));
            var update = RegistryObject.class.getDeclaredMethod("updateReference", IForgeRegistry.class);
            update.setAccessible(true);
            update.invoke(entry, registry);
        }
        finally
        {
            BuiltInRegistries.ITEM.freeze();
            registry.freeze();
        }
    }

    @Test
    void duplicateDyesArePaidInFullForBothWorkbenchAndPaintTable()
    {
        for (boolean table : new boolean[]{false, true})
        {
            ServerPlayer player = mock(ServerPlayer.class);
            Inventory inventory = new Inventory(player);
            when(player.getInventory()).thenReturn(inventory);
            when(player.getAbilities()).thenReturn(new Abilities());
            GunType type = mock(GunType.class);
            GunItem item = mock(GunItem.class);
            when(item.getConfigType()).thenReturn(type);
            when(item.getPaintableType()).thenReturn(type);
            ItemStack gun = mock(ItemStack.class);
            when(gun.getItem()).thenReturn(item);
            Paintjob paint = mock(Paintjob.class);
            when(paint.getId()).thenReturn(1);
            when(paint.isAddToTables()).thenReturn(true);
            when(paint.getDyesNeeded(false)).thenReturn(List.of(new ItemStack(Items.RED_DYE), new ItemStack(Items.RED_DYE)));
            when(type.isAddAnyPaintjobToTables()).thenReturn(true);
            when(type.getPaintjobs()).thenReturn(Map.of(1, paint));
            when(type.getApplicablePaintjobs()).thenReturn(List.of(paint));
            ItemStack cans = new ItemStack(Items.RED_DYE);
            if (table)
            {
                PaintjobTableMenu menu = mock(PaintjobTableMenu.class);
                when(menu.getPaintableStack()).thenReturn(gun);
                when(menu.getPaintCanStack()).thenReturn(cans);
                player.containerMenu = menu;
            }
            else
            {
                GunWorkbenchMenu menu = mock(GunWorkbenchMenu.class);
                when(menu.getGunStack()).thenReturn(gun);
                player.containerMenu = menu;
                inventory.setItem(0, new ItemStack(Items.RED_DYE));
            }
            PacketSelectPaintjob request = new PacketSelectPaintjob(1);
            ServerLevel level = mock(ServerLevel.class);
            request.handleServerSide(player, level);
            verify(type, never()).applyPaintjobToStack(gun, paint);
            if (table)
            {
                assertEquals(1, cans.getCount());
                inventory.setItem(0, new ItemStack(Items.RED_DYE));
            }
            else
            {
                assertEquals(1, inventory.getItem(0).getCount());
                inventory.getItem(0).grow(1);
            }
            request.handleServerSide(player, level);
            verify(type).applyPaintjobToStack(gun, paint);
            assertTrue(inventory.getItem(0).isEmpty());
            if (table)
                assertTrue(cans.isEmpty());
        }
    }

    @Test
    void rainbowPaymentRequiresExplicitChoiceAndLeavesOrdinaryDyesUntouched()
    {
        for (boolean table : new boolean[]{false, true})
        {
            PaymentFixture fixture = new PaymentFixture(table, List.of(new ItemStack(Items.RED_DYE, 8), new ItemStack(Items.BLUE_DYE, 4)));
            fixture.inventory.setItem(0, rainbowCan());
            fixture.inventory.setItem(1, new ItemStack(Items.RED_DYE, 3));

            fixture.select(false);
            verify(fixture.type, never()).applyPaintjobToStack(fixture.gun, fixture.paint);
            assertEquals(1, fixture.inventory.getItem(0).getCount());
            assertEquals(3, fixture.inventory.getItem(1).getCount());

            fixture.select(true);
            verify(fixture.type).applyPaintjobToStack(fixture.gun, fixture.paint);
            assertTrue(fixture.inventory.getItem(0).isEmpty());
            assertEquals(3, fixture.inventory.getItem(1).getCount());
        }
    }

    @Test
    void ordinaryClickPaysDyesEvenWhenRainbowPaintIsAvailable()
    {
        for (boolean table : new boolean[]{false, true})
        {
            PaymentFixture fixture = new PaymentFixture(table, List.of(new ItemStack(Items.GREEN_DYE, 8)));
            fixture.inventory.setItem(0, rainbowCan());
            fixture.inventory.setItem(1, new ItemStack(Items.GREEN_DYE, 8));
            fixture.select(false);
            verify(fixture.type).applyPaintjobToStack(fixture.gun, fixture.paint);
            assertEquals(1, fixture.inventory.getItem(0).getCount());
            assertTrue(fixture.inventory.getItem(1).isEmpty());
        }
    }

    @Test
    void mandatoryRainbowCansRemainAdditionalAndPaymentIsAtomicAcrossBothSources()
    {
        for (boolean table : new boolean[]{false, true})
        {
            PaymentFixture fixture = new PaymentFixture(table, List.of(new ItemStack(Items.YELLOW_DYE, 10), rainbowCan()));
            if (table)
                fixture.workspace = rainbowCan();
            else
                fixture.inventory.setItem(0, rainbowCan());

            fixture.select(true);
            verify(fixture.type, never()).applyPaintjobToStack(fixture.gun, fixture.paint);
            assertEquals(1, table ? fixture.workspace.getCount() : fixture.inventory.getItem(0).getCount());

            fixture.inventory.setItem(1, rainbowCan());
            fixture.select(true);
            verify(fixture.type).applyPaintjobToStack(fixture.gun, fixture.paint);
            assertTrue(fixture.inventory.getItem(0).isEmpty());
            assertTrue(fixture.inventory.getItem(1).isEmpty());
            assertTrue(fixture.workspace.isEmpty());
        }
    }

    @Test
    void rainbowChoicePreservesOtherIngredientsAndFreePaintjobsStayFree()
    {
        PaymentFixture paid = new PaymentFixture(false, List.of(new ItemStack(Items.RED_DYE, 8), new ItemStack(Items.IRON_INGOT, 2)));
        paid.inventory.setItem(0, rainbowCan());
        paid.select(true);
        verify(paid.type, never()).applyPaintjobToStack(paid.gun, paid.paint);
        assertEquals(1, paid.inventory.getItem(0).getCount());
        paid.inventory.setItem(1, new ItemStack(Items.IRON_INGOT, 2));
        paid.select(true);
        verify(paid.type).applyPaintjobToStack(paid.gun, paid.paint);
        assertTrue(paid.inventory.getItem(0).isEmpty());
        assertTrue(paid.inventory.getItem(1).isEmpty());

        PaymentFixture free = new PaymentFixture(false, List.of());
        free.inventory.setItem(0, rainbowCan());
        free.select(true);
        verify(free.type).applyPaintjobToStack(free.gun, free.paint);
        assertEquals(1, free.inventory.getItem(0).getCount());
        assertFalse(free.paint.hasDyeCost());
    }

    @Test
    void paymentChoiceSurvivesPacketRoundTrip()
    {
        PaymentFixture fixture = new PaymentFixture(false, List.of(new ItemStack(Items.RED_DYE, 8)));
        fixture.inventory.setItem(0, rainbowCan());
        PacketBuffer buffer = new PacketBuffer(new FriendlyByteBuf(Unpooled.buffer()));
        try
        {
            new PacketSelectPaintjob(1, true).encodeInto(buffer);
            PacketSelectPaintjob decoded = new PacketSelectPaintjob();
            decoded.decodeInto(buffer);
            assertEquals(0, buffer.readableBytes());
            decoded.handleServerSide(fixture.player, mock(ServerLevel.class));
            verify(fixture.type).applyPaintjobToStack(fixture.gun, fixture.paint);
            assertTrue(fixture.inventory.getItem(0).isEmpty());
        }
        finally
        {
            buffer.release();
        }
    }

    private static ItemStack rainbowCan()
    {
        return new ItemStack(FlansModItems.rainbowPaintcan.get());
    }

    private static final class PaymentFixture
    {
        private final ServerPlayer player = mock(ServerPlayer.class);
        private final Inventory inventory = new Inventory(player);
        private final GunType type = mock(GunType.class);
        private final ItemStack gun = mock(ItemStack.class);
        private final Paintjob paint = mock(Paintjob.class, CALLS_REAL_METHODS);
        private ItemStack workspace = ItemStack.EMPTY;

        private PaymentFixture(boolean table, List<ItemStack> required)
        {
            when(player.getInventory()).thenReturn(inventory);
            when(player.getAbilities()).thenReturn(new Abilities());
            GunItem item = mock(GunItem.class);
            when(item.getConfigType()).thenReturn(type);
            when(item.getPaintableType()).thenReturn(type);
            when(gun.getItem()).thenReturn(item);
            doReturn(1).when(paint).getId();
            doReturn(true).when(paint).isAddToTables();
            doReturn(required).when(paint).getDyesNeeded();
            when(type.isAddAnyPaintjobToTables()).thenReturn(true);
            when(type.getPaintjobs()).thenReturn(Map.of(1, paint));
            when(type.getApplicablePaintjobs()).thenReturn(List.of(paint));
            if (table)
            {
                PaintjobTableMenu menu = mock(PaintjobTableMenu.class);
                when(menu.getPaintableStack()).thenReturn(gun);
                when(menu.getPaintCanStack()).thenAnswer(invocation -> workspace);
                player.containerMenu = menu;
            }
            else
            {
                GunWorkbenchMenu menu = mock(GunWorkbenchMenu.class);
                when(menu.getGunStack()).thenReturn(gun);
                player.containerMenu = menu;
            }
        }

        private void select(boolean useRainbowPaint)
        {
            new PacketSelectPaintjob(1, useRainbowPaint).handleServerSide(player, mock(ServerLevel.class));
        }
    }
}
