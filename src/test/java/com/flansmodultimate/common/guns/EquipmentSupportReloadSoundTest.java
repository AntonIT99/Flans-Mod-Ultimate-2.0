package com.flansmodultimate.common.guns;

import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.network.client.effects.PacketPlaySound;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class EquipmentSupportReloadSoundTest
{
    private LivingEntity holder;
    private ItemStack weapon;
    private GunType type;

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @BeforeEach
    void prepareGun()
    {
        holder = mock(LivingEntity.class);
        ServerLevel level = mock(ServerLevel.class);
        MinecraftServer server = mock(MinecraftServer.class);
        when(holder.level()).thenReturn(level);
        when(level.getServer()).thenReturn(server);
        when(server.isSameThread()).thenReturn(true);
        weapon = mock(ItemStack.class);
        GunItem gun = mock(GunItem.class);
        when(weapon.getItem()).thenReturn(gun);
        type = mock(GunType.class);
        when(gun.getConfigType()).thenReturn(type);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void missingOrBlankReloadSoundDoesNotCrashOrBroadcast(String sound)
    {
        when(type.getReloadSound(weapon)).thenReturn(sound);
        try (var sounds = mockStatic(PacketPlaySound.class))
        {
            assertDoesNotThrow(() -> FlansEquipment.playReloadSound(holder, weapon));
            sounds.verifyNoInteractions();
        }
    }

    @Test
    void configuredReloadSoundKeepsItsAuthoredRange()
    {
        when(type.getReloadSound(weapon)).thenReturn("reload");
        when(type.getReloadSoundRange()).thenReturn(24F);
        try (var sounds = mockStatic(PacketPlaySound.class))
        {
            FlansEquipment.playReloadSound(holder, weapon);
            sounds.verify(() -> PacketPlaySound.sendSoundPacket(holder, 24D, "reload", false));
            sounds.verifyNoMoreInteractions();
        }
    }
}
