package com.flansmodultimate.common.teams;

import net.minecraft.nbt.CompoundTag;

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
        tag.putUUID(NBT_ID, id);
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
            origin = Origin.valueOf(tag.getString(NBT_ORIGIN));
        }
        catch (IllegalArgumentException ignored)
        {
            origin = Origin.MIGRATED;
        }

        UUID id = tag.hasUUID(NBT_ID) ? tag.getUUID(NBT_ID) : UUID.randomUUID();
        return new RewardBoxInstance(id, tag.getString(NBT_BOX), origin, tag.getLong(NBT_AWARDED_AT), tag.getString(NBT_REWARD));
    }
}
