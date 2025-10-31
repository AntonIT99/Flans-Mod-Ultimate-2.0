package com.wolffsarmormod.event;

import com.wolffsarmormod.ArmorMod;
import com.wolffsarmormod.common.PlayerData;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.world.entity.player.Player;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(modid = ArmorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CommonEventHandler
{
    @Getter
    private static long ticker;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
        {
            ticker++;
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e)
    {
        // avoid running twice per tick
        if (e.phase != TickEvent.Phase.END)
            return;
        Player p = e.player;

        if (!p.level().isClientSide)
        {
            PlayerData.getInstance(p).serverTick(p);
        }
        else
        {
            PlayerData.getInstance(p).clientTick(p);
        }
    }
}
