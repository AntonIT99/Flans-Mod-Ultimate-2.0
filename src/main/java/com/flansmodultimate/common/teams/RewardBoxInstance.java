package com.flansmodultimate.common.teams;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.UUIDUtil;

import java.util.UUID;

public record RewardBoxInstance(UUID id, String boxId, Origin origin, long awardedAt, String rewardKey)
{
    private static final String NBT_ID = "id";
    private static final String NBT_BOX = "box";
    private static final String NBT_ORIGIN = "origin";
    private static final String NBT_AWARDED_AT = "awarded_at";
    private static final String NBT_REWARD = "reward";

    public enum Origin
    {
        LEVEL_UP,
        COMMAND,
        MIGRATED
    }

    public RewardBoxInstance(String boxId, Origin origin)
    {
        this(UUID.randomUUID(), boxId, origin, System.currentTimeMillis(), "");
    }

    public boolean isOpened()
    {
        return !rewardKey.isBlank();
    }

    public RewardBoxInstance openedWith(String key)
    {
        return new RewardBoxInstance(id, boxId, origin, awardedAt, key);
    }

    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.store(NBT_ID, UUIDUtil.CODEC, id);
        tag.putString(NBT_BOX, boxId);
        tag.putString(NBT_ORIGIN, origin.name());
        tag.putLong(NBT_AWARDED_AT, awardedAt);
        tag.putString(NBT_REWARD, rewardKey);
        return tag;
    }

    public static RewardBoxInstance load(CompoundTag tag)
    {
        Origin origin;
        try
        {
            origin = Origin.valueOf(tag.getStringOr(NBT_ORIGIN, "MIGRATED"));
        }
        catch (IllegalArgumentException ignored)
        {
            origin = Origin.MIGRATED;
        }

        UUID id = tag.read(NBT_ID, UUIDUtil.LENIENT_CODEC).orElseGet(UUID::randomUUID);
        return new RewardBoxInstance(id, tag.getStringOr(NBT_BOX, ""), origin, tag.getLongOr(NBT_AWARDED_AT, 0L), tag.getStringOr(NBT_REWARD, ""));
    }
}
