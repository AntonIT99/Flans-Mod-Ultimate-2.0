package com.flansmodultimate.client.digitalammo;


import java.util.Arrays;

public final class LocalBulletManager
{
    private static final int DEFAULT_NUM_TYPES = 7;
    private static final int DEFAULT_AMOUNT = 100;

    private static double[] bullets;
    private static boolean initialized = false;

    private LocalBulletManager() {}

    public static void init()
    {
        if (initialized) return;
        bullets = new double[DEFAULT_NUM_TYPES];
        Arrays.fill(bullets, DEFAULT_AMOUNT);
        initialized = true;
    }

    public static double getBullets(int typeId)
    {
        if (!initialized) init();
        if (typeId < 1 || typeId > bullets.length)
            return 0.0;
        return bullets[typeId - 1];
    }

    public static void setBullets(int typeId, double amount)
    {
        if (!initialized) init();
        if (typeId < 1 || typeId > bullets.length)
            return;
        bullets[typeId - 1] = amount;
    }

    public static void setAllBullets(double[] values)
    {
        if (!initialized) init();
        if (values == null)
            return;
        bullets = Arrays.copyOf(values, values.length);
    }

    public static int getNumTypes()
    {
        if (!initialized) init();
        return bullets.length;
    }

    public static double[] getAllBullets()
    {
        if (!initialized) init();
        return Arrays.copyOf(bullets, bullets.length);
    }

    public static void reset()
    {
        if (bullets != null)
        {
            Arrays.fill(bullets, DEFAULT_AMOUNT);
        }
    }
}
