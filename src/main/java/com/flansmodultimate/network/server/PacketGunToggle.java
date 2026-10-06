package com.flansmodultimate.network.server;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketCancelSound;
import com.flansmodultimate.network.client.PacketGunToggleClient;
import com.flansmodultimate.network.client.PacketPlaySound;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Switches a gun and its toggleable attachments (flashlights, lasers, blades) on or off */
@NoArgsConstructor
public class PacketGunToggle implements IServerPacket
{
    private InteractionHand hand;

    public PacketGunToggle(InteractionHand hand)
    {
        this.hand = hand;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeEnum(hand);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        hand = data.readEnum(InteractionHand.class);
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !(stack.getItem() instanceof GunItem gunItem))
            return;

        GunType gunType = gunItem.getConfigType();
        PlayerData data = PlayerData.getInstance(player);
        if (data.getShootTime(hand) > 0F || !gunType.canToggle(stack))
            return;

        boolean on = !gunType.isToggledOn(stack);
        gunType.setToggledOn(stack, on);
        if (gunType.isToggleable())
        {
            if (!on)
                PacketHandler.sendToDimension(level.dimension(), new PacketCancelSound(data.getIdleSoundId(hand)));
            data.setIdleSoundDelay(hand, 0);
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        PacketHandler.sendTo(new PacketGunToggleClient(hand, on), player);

        String sound = gunType.getToggleSound(stack, on);
        if (StringUtils.isNotBlank(sound))
            PacketPlaySound.sendSoundPacket(player, gunType.getReloadSoundRange(), sound, true);
    }
}
