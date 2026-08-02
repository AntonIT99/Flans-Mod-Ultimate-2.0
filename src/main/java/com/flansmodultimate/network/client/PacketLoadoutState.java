package com.flansmodultimate.network.client;

import com.flansmodultimate.client.teams.LoadoutClientState;
import com.flansmodultimate.common.teams.LoadoutSlot;
import com.flansmodultimate.common.teams.PlayerLoadout;
import com.flansmodultimate.common.teams.PlayerStats;
import com.flansmodultimate.common.teams.RewardBoxInstance;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.LoadoutPool;
import com.flansmodultimate.common.types.RewardBox;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.util.ModUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
public final class PacketLoadoutState implements IClientPacket
{
    public enum OpenScreen
    {
        NONE,
        HUB,
        CHOOSE,
        EDIT,
        REWARD_BOX,
        MISSION_RESULTS,
        CLOSE
    }

    public record Entry(LoadoutSlot slot, String typeId, String name, int unlockRank, ItemStack preview) {}
    public record BoxView(UUID id, String boxId, String name, boolean opened, String rewardKey, ItemStack preview) {}
    public record RewardView(String key, String typeId, String name, int rarity) {}

    private OpenScreen openScreen = OpenScreen.NONE;
    private String poolId = "";
    private String poolName = "";
    private int rank;
    private int experience;
    private int experienceForNextRank;
    private int selectedLoadout;
    private int editLoadout;
    private String revealedReward = "";
    private List<PlayerLoadout> loadouts = List.of();
    private List<Integer> loadoutUnlockRanks = List.of();
    private List<Entry> entries = List.of();
    private List<BoxView> boxes = List.of();
    private List<RewardView> rewards = List.of();

    public static PacketLoadoutState create(TeamsManager manager, ServerPlayer player, OpenScreen screen, int editLoadout, String revealedReward)
    {
        PacketLoadoutState packet = new PacketLoadoutState();
        packet.openScreen = screen;
        packet.editLoadout = Math.max(0, Math.min(LoadoutPool.LOADOUT_COUNT - 1, editLoadout));
        packet.revealedReward = revealedReward == null ? "" : revealedReward;
        LoadoutPool pool = manager.getCurrentLoadoutPool().orElse(null);
        if (pool == null) return packet;
        PlayerStats stats = manager.getStats(player);
        packet.poolId = pool.getOriginalShortName();
        packet.poolName = pool.getName();
        packet.rank = stats.getRank();
        packet.experience = stats.getExperience();
        packet.experienceForNextRank = pool.getExperienceForRank(stats.getRank());
        packet.selectedLoadout = stats.getSelectedLoadout();
        packet.loadouts = stats.getLoadouts(pool).stream().map(PlayerLoadout::copy).toList();
        packet.loadoutUnlockRanks = java.util.stream.IntStream.range(0, LoadoutPool.LOADOUT_COUNT).mapToObj(pool::getLoadoutUnlockLevel).toList();

        List<Entry> entries = new ArrayList<>();
        for (LoadoutSlot slot : LoadoutSlot.values())
            for (LoadoutPool.LoadoutEntry entry : pool.getEntries(slot))
            {
                InfoType type = InfoType.getInfoType(entry.typeId(), pool.getContentPack());
                if (type != null)
                    entries.add(new Entry(slot, type.getOriginalShortName(), type.getName(), entry.unlockRank(),
                        ModUtils.getItemStack(type).orElse(ItemStack.EMPTY)));
            }
        packet.entries = List.copyOf(entries);

        packet.boxes = stats.getRewardBoxes().stream().map(instance -> {
            RewardBox box = RewardBox.get(instance.boxId());
            return new BoxView(instance.id(), instance.boxId(), box == null ? instance.boxId() : box.getName(), instance.isOpened(), instance.rewardKey(),
                box == null ? ItemStack.EMPTY : ModUtils.getItemStack(box).orElse(ItemStack.EMPTY));
        }).toList();
        packet.rewards = stats.getRewardBoxes().stream().filter(RewardBoxInstance::isOpened).map(instance -> RewardBox.findReward(instance.rewardKey()))
            .filter(java.util.Objects::nonNull).distinct().map(reward ->
                new RewardView(reward.key(), reward.typeId(), reward.paintName(), reward.rarity().ordinal())).toList();
        return packet;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeByte(openScreen.ordinal());
        data.writeUtf(poolId); data.writeUtf(poolName);
        data.writeVarInt(rank); data.writeVarInt(experience); data.writeVarInt(experienceForNextRank);
        data.writeVarInt(selectedLoadout); data.writeVarInt(editLoadout); data.writeUtf(revealedReward);
        data.writeCollection(loadouts, (buf, loadout) -> loadout.write(buf));
        data.writeCollection(loadoutUnlockRanks, FriendlyByteBuf::writeVarInt);
        data.writeCollection(entries, (buf, entry) -> {
            buf.writeByte(entry.slot().ordinal()); buf.writeUtf(entry.typeId()); buf.writeUtf(entry.name());
            buf.writeVarInt(entry.unlockRank()); buf.writeItem(entry.preview());
        });
        data.writeCollection(boxes, (buf, box) -> {
            buf.writeUUID(box.id()); buf.writeUtf(box.boxId()); buf.writeUtf(box.name()); buf.writeBoolean(box.opened());
            buf.writeUtf(box.rewardKey()); buf.writeItem(box.preview());
        });
        data.writeCollection(rewards, (buf, reward) -> {
            buf.writeUtf(reward.key()); buf.writeUtf(reward.typeId()); buf.writeUtf(reward.name()); buf.writeVarInt(reward.rarity());
        });
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        int screen = data.readUnsignedByte();
        openScreen = screen < OpenScreen.values().length ? OpenScreen.values()[screen] : OpenScreen.NONE;
        poolId = data.readUtf(); poolName = data.readUtf();
        rank = data.readVarInt(); experience = data.readVarInt(); experienceForNextRank = data.readVarInt();
        selectedLoadout = data.readVarInt(); editLoadout = data.readVarInt(); revealedReward = data.readUtf();
        loadouts = data.readList(PlayerLoadout::read);
        loadoutUnlockRanks = data.readList(FriendlyByteBuf::readVarInt);
        entries = data.readList(buf -> {
            int slot = buf.readUnsignedByte();
            return new Entry(LoadoutSlot.values()[Math.min(slot, LoadoutSlot.values().length - 1)], buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readItem());
        });
        boxes = data.readList(buf -> new BoxView(buf.readUUID(), buf.readUtf(), buf.readUtf(), buf.readBoolean(), buf.readUtf(), buf.readItem()));
        rewards = data.readList(buf -> new RewardView(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readVarInt()));
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        LoadoutClientState.accept(this);
    }
}
