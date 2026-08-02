package com.flansmodultimate.network.server;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.guns.EnumAttachmentType;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.teams.LoadoutSlot;
import com.flansmodultimate.common.teams.PlayerLoadout;
import com.flansmodultimate.common.teams.PlayerStats;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.common.types.AttachmentType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.LoadoutPool;
import com.flansmodultimate.common.types.PaintableType;
import com.flansmodultimate.common.types.RewardBox;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.network.client.PacketLoadoutState;
import com.flansmodultimate.network.client.PacketTeamsState;
import com.flansmodultimate.util.ModUtils;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

@NoArgsConstructor
public final class PacketLoadoutAction implements IServerPacket
{
    public enum Action
    {
        OPEN_HUB,
        OPEN_CHOOSE,
        EDIT,
        SELECT,
        SET_ENTRY,
        SET_PAINT,
        OPEN_BOX,
        PLAY
    }

    private Action action = Action.OPEN_HUB;
    private int index;
    private LoadoutSlot slot = LoadoutSlot.PRIMARY;
    private String value = "";

    private PacketLoadoutAction(Action action, int index, LoadoutSlot slot, String value)
    {
        this.action = action; this.index = index; this.slot = slot; this.value = value == null ? "" : value;
    }

    public static PacketLoadoutAction openHub()
    {
        return new PacketLoadoutAction(Action.OPEN_HUB, 0, LoadoutSlot.PRIMARY, "");
    }

    public static PacketLoadoutAction openChoose()
    {
        return new PacketLoadoutAction(Action.OPEN_CHOOSE, 0, LoadoutSlot.PRIMARY, "");
    }

    public static PacketLoadoutAction edit(int index)
    {
        return new PacketLoadoutAction(Action.EDIT, index, LoadoutSlot.PRIMARY, "");
    }

    public static PacketLoadoutAction select(int index)
    {
        return new PacketLoadoutAction(Action.SELECT, index, LoadoutSlot.PRIMARY, "");
    }

    public static PacketLoadoutAction setEntry(int index, LoadoutSlot slot, String id)
    {
        return new PacketLoadoutAction(Action.SET_ENTRY, index, slot, id);
    }

    public static PacketLoadoutAction setPaint(int index, LoadoutSlot slot, String key)
    {
        return new PacketLoadoutAction(Action.SET_PAINT, index, slot, key);
    }

    public static PacketLoadoutAction openBox(UUID id)
    {
        return new PacketLoadoutAction(Action.OPEN_BOX, 0, LoadoutSlot.PRIMARY, id.toString());
    }

    public static PacketLoadoutAction play()
    {
        return new PacketLoadoutAction(Action.PLAY, 0, LoadoutSlot.PRIMARY, "");
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeByte(action.ordinal());
        data.writeVarInt(index);
        data.writeByte(slot.ordinal());
        data.writeUtf(value, 256);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        int actionId = data.readUnsignedByte(); action = actionId < Action.values().length ? Action.values()[actionId] : Action.OPEN_HUB;
        index = data.readVarInt(); int slotId = data.readUnsignedByte(); slot = LoadoutSlot.values()[Math.min(slotId, LoadoutSlot.values().length - 1)]; value = data.readUtf(256);
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        TeamsManager manager = TeamsManager.getInstance();
        LoadoutPool pool = manager.getCurrentLoadoutPool().orElse(null);
        if (pool == null)
        {
            player.sendSystemMessage(Component.literal("No loadout pool is active"));
            manager.syncPlayer(player, PacketTeamsState.OpenScreen.TEAM_SELECT);
            return;
        }
        switch (action)
        {
            case OPEN_HUB -> manager.syncLoadouts(player, PacketLoadoutState.OpenScreen.HUB, 0, "");
            case OPEN_CHOOSE -> manager.syncLoadouts(player, PacketLoadoutState.OpenScreen.CHOOSE, 0, "");
            case EDIT -> manager.syncLoadouts(player, PacketLoadoutState.OpenScreen.EDIT, index, "");
            case PLAY -> manager.syncPlayer(player, PacketTeamsState.OpenScreen.TEAM_SELECT);
            case SELECT -> select(manager, player, pool);
            case SET_ENTRY -> setEntry(manager, player, pool);
            case SET_PAINT -> setPaint(manager, player, pool);
            case OPEN_BOX -> openBox(manager, player);
        }
    }

    private void select(TeamsManager manager, ServerPlayer player, LoadoutPool pool)
    {
        PlayerStats stats = manager.getStats(player);
        if (index < 0 || index >= LoadoutPool.LOADOUT_COUNT || stats.getRank() < pool.getLoadoutUnlockLevel(index)
            || !pool.validate(stats.getLoadouts(pool).get(index), stats.getRank(), stats::ownsReward)) return;
        stats.setSelectedLoadout(index);
        Team selected = PlayerData.getInstance(player).getNewTeam();
        if (selected != null && selected != Team.SPECTATORS) manager.respawnPlayer(player, true);
        manager.markPlayerDataDirty();
        manager.syncLoadouts(player, PacketLoadoutState.OpenScreen.CLOSE, index, "");
    }

    private void setEntry(TeamsManager manager, ServerPlayer player, LoadoutPool pool)
    {
        PlayerStats stats = manager.getStats(player);
        if (index < 0 || index >= LoadoutPool.LOADOUT_COUNT) return;
        PlayerLoadout edited = stats.getLoadouts(pool).get(index).copy();
        if ("$clear".equals(value)) edited.set(slot, ItemStack.EMPTY);
        else
        {
            InfoType type = InfoType.getInfoType(value, pool.getContentPack());
            if (type == null || !pool.isEntryUnlocked(slot, type, stats.getRank())) return;
            if (type instanceof AttachmentType attachment)
            {
                ItemStack gun = edited.get(slot);
                if (!(gun.getItem() instanceof GunItem gunItem) || !accepts(gunItem.getConfigType(), attachment)) return;
                CompoundTag attachments = gun.getOrCreateTag().getCompound(GunItem.NBT_ATTACHMENTS);
                String attachmentSlot = attachmentSlot(attachment.getEnumAttachmentType());
                ItemStack attachmentStack = ModUtils.getItemStack(attachment).orElse(ItemStack.EMPTY);
                if (attachmentStack.isEmpty()) return;
                attachments.put(attachmentSlot, attachmentStack.save(new CompoundTag()));
                gun.getOrCreateTag().put(GunItem.NBT_ATTACHMENTS, attachments);
                edited.set(slot, gun);
            }
            else
            {
                ItemStack stack = pool.createEntryStack(type);
                if (stack.isEmpty()) return;
                edited.set(slot, stack);
            }
        }
        if (stats.replaceLoadout(pool, index, edited))
        {
            manager.markPlayerDataDirty();
            manager.syncLoadouts(player, PacketLoadoutState.OpenScreen.EDIT, index, "");
        }
    }

    private void setPaint(TeamsManager manager, ServerPlayer player, LoadoutPool pool)
    {
        PlayerStats stats = manager.getStats(player);
        if (index < 0 || index >= LoadoutPool.LOADOUT_COUNT) return;
        RewardBox.Reward reward = "$default".equals(value) ? null : RewardBox.findReward(value);
        if (reward == null && !"$default".equals(value)) return;
        if (reward != null && !stats.ownsReward(value)) return;
        PlayerLoadout edited = stats.getLoadouts(pool).get(index).copy();
        ItemStack stack = edited.get(slot);
        if (!(stack.getItem() instanceof com.flansmodultimate.common.item.IFlanItem<?> item) || !(item.getConfigType() instanceof PaintableType paintable)
            || (reward != null && !paintable.getOriginalShortName().equalsIgnoreCase(reward.typeId()))) return;
        var paintjob = reward == null ? paintable.getDefaultPaintjob() : RewardBox.resolve(reward);
        if (paintjob == null) return;
        paintable.applyPaintjobToStack(stack, paintjob);
        edited.set(slot, stack);
        if (stats.replaceLoadout(pool, index, edited))
        {
            manager.markPlayerDataDirty();
            manager.syncLoadouts(player, PacketLoadoutState.OpenScreen.EDIT, index, "");
        }
    }

    private void openBox(TeamsManager manager, ServerPlayer player)
    {
        UUID id;
        try { id = UUID.fromString(value); } catch (IllegalArgumentException ignored) { return; }
        RewardBox.Reward reward = manager.openRewardBox(player, id);
        manager.syncLoadouts(player, PacketLoadoutState.OpenScreen.REWARD_BOX, 0, reward == null ? "" : reward.key());
    }

    private static boolean accepts(com.flansmodultimate.common.types.GunType gun, AttachmentType attachment)
    {
        if (!gun.isAllowAllAttachments() && !gun.getAllowedAttachments().contains(attachment)) return false;
        return switch (attachment.getEnumAttachmentType())
        {
            case BARREL -> gun.isAllowBarrelAttachments();
            case SIGHTS -> gun.isAllowScopeAttachments();
            case STOCK -> gun.isAllowStockAttachments();
            case GRIP -> gun.isAllowGripAttachments();
            case GADGET -> gun.isAllowGadgetAttachments();
            case SLIDE -> gun.isAllowSlideAttachments();
            case PUMP -> gun.isAllowPumpAttachments();
            case ACCESSORY -> gun.isAllowAccessoryAttachments();
            case GENERIC -> gun.getNumGenericAttachmentSlots() > 0;
        };
    }

    private static String attachmentSlot(EnumAttachmentType type)
    {
        return switch (type)
        {
            case BARREL -> GunItem.NBT_BARREL; case SIGHTS -> GunItem.NBT_SCOPE; case STOCK -> GunItem.NBT_STOCK;
            case GRIP -> GunItem.NBT_GRIP; case GADGET -> GunItem.NBT_GADGET; case SLIDE -> GunItem.NBT_SLIDE;
            case PUMP -> GunItem.NBT_PUMP; case ACCESSORY -> GunItem.NBT_ACCESSORY; case GENERIC -> GunItem.NBT_GENERIC + "0";
        };
    }
}
