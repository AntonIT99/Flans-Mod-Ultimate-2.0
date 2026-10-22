package com.flansmodultimate.common.item;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.guns.EnumFireDecision;
import com.flansmodultimate.common.guns.EnumFireMode;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class GunReloadDecisionTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void emptyLauncherCanReloadDuringShotRecoveryButCannotFireOrRestartAnActiveReload()
    {
        GunType type = mock(GunType.class);
        ItemStack stack = mock(ItemStack.class);
        when(type.getFireMode(stack)).thenReturn(EnumFireMode.SEMIAUTO);
        GunItem item = mock(GunItem.class);
        when(item.getConfigType()).thenReturn(type);
        GunItemHandler handler = spy(new GunItemHandler(item));
        doReturn(true).when(handler).hasEmptyAmmo(stack, null);
        PlayerData data = PlayerData.getInstance(UUID.randomUUID());
        InteractionHand hand = InteractionHand.MAIN_HAND;
        data.setShootKeyPressed(hand, true);
        data.setShootTime(hand, 299F);

        assertEquals(EnumFireDecision.RELOAD, handler.computeFireDecision(data, stack, hand, null));
        data.doGunReload(hand, 80F);
        assertEquals(EnumFireDecision.NO_ACTION, handler.computeFireDecision(data, stack, hand, null));
        data.setReloading(hand, false);
        doReturn(false).when(handler).hasEmptyAmmo(stack, null);
        assertEquals(EnumFireDecision.NO_ACTION, handler.computeFireDecision(data, stack, hand, null));
        data.setShootTime(hand, 0F);
        assertEquals(EnumFireDecision.SHOOT, handler.computeFireDecision(data, stack, hand, null));
    }
}
