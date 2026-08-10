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
import com.flansmodultimate.network.PacketIO;
import com.flansmodultimate.util.ModUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.RegistryFriendlyByteBuf;
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
    public void encodeInto(RegistryFriendlyByteBuf data)
    {
        data.writeByte(openScreen.ordinal());
        data.writeUtf(poolId); data.writeUtf(poolName);
        data.writeVarInt(rank); data.writeVarInt(experience); data.writeVarInt(experienceForNextRank);
        data.writeVarInt(selectedLoadout); data.writeVarInt(editLoadout); data.writeUtf(revealedReward);
        data.writeVarInt(loadouts.size());
        for (PlayerLoadout loadout : loadouts) loadout.write(data);
        data.writeVarInt(loadoutUnlockRanks.size());
        for (int unlockRank : loadoutUnlockRanks) data.writeVarInt(unlockRank);
        data.writeVarInt(entries.size());
        for (Entry entry : entries)
        {
            data.writeByte(entry.slot().ordinal()); data.writeUtf(entry.typeId()); data.writeUtf(entry.name());
            data.writeVarInt(entry.unlockRank()); PacketIO.writeItem(data, entry.preview());
        }
        data.writeVarInt(boxes.size());
        for (BoxView box : boxes)
        {
            data.writeUUID(box.id()); data.writeUtf(box.boxId()); data.writeUtf(box.name()); data.writeBoolean(box.opened());
            data.writeUtf(box.rewardKey()); PacketIO.writeItem(data, box.preview());
        }
        data.writeCollection(rewards, (buf, reward) -> {
            buf.writeUtf(reward.key()); buf.writeUtf(reward.typeId()); buf.writeUtf(reward.name()); buf.writeVarInt(reward.rarity());
        });
    }

    @Override
    public void decodeInto(RegistryFriendlyByteBuf data)
    {
        int screen = data.readUnsignedByte();
        openScreen = screen < OpenScreen.values().length ? OpenScreen.values()[screen] : OpenScreen.NONE;
        poolId = data.readUtf(); poolName = data.readUtf();
        rank = data.readVarInt(); experience = data.readVarInt(); experienceForNextRank = data.readVarInt();
        selectedLoadout = data.readVarInt(); editLoadout = data.readVarInt(); revealedReward = data.readUtf();
        int loadoutCount = data.readVarInt();
        loadouts = new ArrayList<>(loadoutCount);
        for (int i = 0; i < loadoutCount; i++) loadouts.add(PlayerLoadout.read(data));
        int unlockCount = data.readVarInt();
        loadoutUnlockRanks = new ArrayList<>(unlockCount);
        for (int i = 0; i < unlockCount; i++) loadoutUnlockRanks.add(data.readVarInt());
        int entryCount = data.readVarInt();
        entries = new ArrayList<>(entryCount);
        for (int i = 0; i < entryCount; i++)
        {
            int slot = data.readUnsignedByte();
            entries.add(new Entry(LoadoutSlot.values()[Math.min(slot, LoadoutSlot.values().length - 1)],
                data.readUtf(), data.readUtf(), data.readVarInt(), PacketIO.readItem(data)));
        }
        int boxCount = data.readVarInt();
        boxes = new ArrayList<>(boxCount);
        for (int i = 0; i < boxCount; i++)
            boxes.add(new BoxView(data.readUUID(), data.readUtf(), data.readUtf(), data.readBoolean(), data.readUtf(), PacketIO.readItem(data)));
        rewards = data.readList(buf -> new RewardView(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readVarInt()));
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        LoadoutClientState.accept(this);
    }
}
