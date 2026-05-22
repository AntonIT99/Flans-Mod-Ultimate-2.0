package com.flansmodultimate.common.digitalammo;

import com.flansmodultimate.config.ModCommonConfig;
import lombok.Getter;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class PlayerBulletStorage
{
    private static final ConcurrentHashMap<UUID, PlayerBulletData> PLAYER_BULLET_DATA = new ConcurrentHashMap<>();
    private static final int DEFAULT_NUM_TYPES = 7;
    private static final int DEFAULT_AMOUNT = 100;
    private static final int DEFAULT_MAX_AMOUNT = 1000;

    private PlayerBulletStorage() {}

    public static PlayerBulletData getBulletDataByPlayer(UUID playerUUID)
    {
        return PLAYER_BULLET_DATA.computeIfAbsent(playerUUID, PlayerBulletData::new);
    }

    public static void processPlayerBulletData(UUID playerUUID, Consumer<PlayerBulletData> consumer)
    {
        consumer.accept(PLAYER_BULLET_DATA.computeIfAbsent(playerUUID, PlayerBulletData::new));
    }

    public static double getBulletsTypeById(PlayerBulletData data, int typeId)
    {
        return data.getBullets(typeId);
    }

    public static void setBulletsById(PlayerBulletData data, int typeId, double amount)
    {
        data.setBullets(typeId, amount);
    }

    public static boolean hasEnoughBullets(PlayerBulletData data, int typeId, double amount)
    {
        return getBulletsTypeById(data, typeId) >= amount;
    }

    public static void takeBulletsById(PlayerBulletData data, int typeId, double amount)
    {
        setBulletsById(data, typeId, getBulletsTypeById(data, typeId) - amount);
    }

    public static void addBulletsById(PlayerBulletData data, int typeId, double amount)
    {
        setBulletsById(data, typeId, getBulletsTypeById(data, typeId) + amount);
    }

    public static void clearPlayerData(UUID playerUUID)
    {
        PLAYER_BULLET_DATA.remove(playerUUID);
    }

    public static final class PlayerBulletData
    {
        @Getter
        private final UUID player;
        private final double[] bullets;
        @Getter
        private final int maxAmount;

        public PlayerBulletData(UUID playerUUID)
        {
            this.player = playerUUID;
            int numTypes = getConfigNumTypes();
            this.bullets = new double[numTypes];
            int defaultAmount = getConfigDefaultAmount();
            this.maxAmount = getConfigMaxAmount();
            for (int i = 0; i < numTypes; i++)
            {
                this.bullets[i] = defaultAmount;
            }
        }

        private static int getConfigNumTypes()
        {
            try
            {
                return ModCommonConfig.get().digitalAmmoNumTypes();
            }
            catch (Exception e)
            {
                return DEFAULT_NUM_TYPES;
            }
        }

        private static int getConfigDefaultAmount()
        {
            try
            {
                return ModCommonConfig.get().digitalAmmoDefaultAmount();
            }
            catch (Exception e)
            {
                return DEFAULT_AMOUNT;
            }
        }

        private static int getConfigMaxAmount()
        {
            try
            {
                return ModCommonConfig.get().digitalAmmoMaxAmount();
            }
            catch (Exception e)
            {
                return DEFAULT_MAX_AMOUNT;
            }
        }

        public double getBullets(int typeId)
        {
            if (typeId < 1 || typeId > bullets.length)
                return 0.0;
            return bullets[typeId - 1];
        }

        public synchronized void setBullets(int typeId, double amount)
        {
            if (typeId < 1 || typeId > bullets.length)
                return;
            bullets[typeId - 1] = Math.max(0, Math.min(amount, maxAmount));
        }

        public synchronized void takeBullets(int typeId, double amount)
        {
            setBullets(typeId, getBullets(typeId) - amount);
        }

        public int getNumTypes()
        {
            return bullets.length;
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder("PlayerBulletData{player=");
            sb.append(player);
            for (int i = 0; i < bullets.length; i++)
            {
                sb.append(", bullet").append(i + 1).append("=").append(bullets[i]);
            }
            sb.append("}");
            return sb.toString();
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj) return true;
            if (!(obj instanceof PlayerBulletData other)) return false;
            return player != null && player.equals(other.player);
        }

        @Override
        public int hashCode()
        {
            return player != null ? player.hashCode() : 0;
        }
    }
}
