package com.flansmodultimate.common.types;

import com.flansmodultimate.common.guns.GunRecoil;
import com.flansmodultimate.config.CommonConfigSnapshot;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.minecraft.world.item.ItemStack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class GunTypeGlobalRecoilTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @ParameterizedTest
    @ValueSource(floats = {0F, 0.5F, 1F, 2F})
    void globalModifierScalesFancyRecoilWithoutMutatingTheWeapon(float modifier)
    {
        GunType type = new GunType();
        type.recoil.read(new String[]{"4", "2", "1", "1", "1", "0", "1", "0"});
        CommonConfigSnapshot snapshot = mock(CommonConfigSnapshot.class);
        when(snapshot.gunRecoilModifier()).thenReturn(modifier);

        try (var config = mockStatic(ModCommonConfig.class))
        {
            config.when(ModCommonConfig::get).thenReturn(snapshot);
            for (int shot = 0; shot < 2; shot++)
            {
                GunRecoil recoil = type.getRecoil(ItemStack.EMPTY);
                assertEquals(2F * modifier, recoil.getHorizontal(), 1E-6F);
                assertEquals(-2F * modifier, recoil.update(false, false, 0F), 1E-6F);
            }
        }
    }
}
