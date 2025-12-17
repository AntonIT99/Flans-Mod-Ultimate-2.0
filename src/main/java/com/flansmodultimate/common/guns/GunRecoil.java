package com.flansmodultimate.common.guns;

import java.util.Random;

public class GunRecoil
{
    private static final Random rand = new Random();

    private float vertical;
    private float horizontal;
    /** how fast the upwards motion stops after shooting. lower is better. 0-2, default 1 */
    private float recovery;
    /** modifies how much scoping modifies recovery (usually 20%). default 1%, lower is better, 0-2 */
    private float recoveryScope;
    /** how much the aim goes back down after shooting. lower is better. 0-2, default 1f */
    private float fall;
    /** how much spraying increases recoil over time. lower is better */
    private float increase;
    /** modifies how much sneaking modifies recovery (usually 10%). default 1%, lower is better, 0-2 */
    private float sneak;
    /** how much walking speed affects recoil. lower is bettr */
    private float speed;
    private float sprayLength;
    private float antiRecoil;

    public GunRecoil(float vertical, float horizontal, float recovery, float recoveryScope, float fall, float increase, float sneak, float speed)
    {
        this.vertical = vertical;
        this.horizontal = horizontal;
        this.recovery = recovery;
        this.recoveryScope = recoveryScope;
        this.fall = fall;
        this.increase = increase;
        this.sneak = sneak;
        this.speed = speed;
    }

    /** Empty recoil */
    public GunRecoil()
    {
        this(0, 0, 0, 0, 0, 0, 0, 0);
    }

    private GunRecoil(GunRecoil gunRecoil)
    {
        this(gunRecoil.vertical, gunRecoil.horizontal, gunRecoil.recovery, gunRecoil.recoveryScope, gunRecoil.fall, gunRecoil.increase, gunRecoil.sneak, gunRecoil.speed);
    }

    public GunRecoil read(String[] split)
    {
        vertical = read(split, 0, vertical);
        horizontal = read(split, 1, horizontal);
        recovery = read(split, 2, recovery);
        recoveryScope = read(split, 3, recoveryScope);
        fall = read(split, 4, fall);
        increase = read(split, 5, increase);
        sneak = read(split, 6, sneak);
        speed = read(split, 7, speed);

        if (split.length < 2)
            horizontal = vertical * 0.3F;
        return this;
    }

    private float read(String[] split, int i, float alt)
    {
        if (split.length > i)
        {
            String value = (split[i].indexOf('=') == -1) ? split[i] : split[i].substring(split[i].indexOf('=') + 1);
            return Float.parseFloat(value);
        }
        return alt;
    }

    public GunRecoil copy()
    {
        return new GunRecoil(this);
    }

    public void applyModifier(float recoilMultiplier)
    {
        vertical *= recoilMultiplier;
        horizontal *= recoilMultiplier;
    }

    public void addRecoil(GunRecoil recoil)
    {
        vertical += recoil.vertical;
        horizontal += 0.2f * (rand.nextBoolean() ? -1 : 1) * recoil.horizontal;
        recovery = recoil.recovery;
        recoveryScope = recoil.recoveryScope;
        increase = recoil.increase;
        sneak = recoil.sneak;
        fall = recoil.fall;
        speed = recoil.speed;
        sprayLength += 0.05F;
        vertical *= (1 + sprayLength * 2 * recoil.increase);
        horizontal *= (1 + sprayLength * 2 * recoil.increase);
        antiRecoil *= rand.nextFloat() * 0.1f;
    }

    public float update(boolean sneaking, boolean scoping, float playerSpeed)
    {
        float recov = 0.5F * recovery;

        if (sneaking)
            recov *= 0.9f * sneak;
        if (scoping)
            recov *= 0.8f * recoveryScope;

        if (vertical > 0)
            vertical *= recov;
        if (horizontal != 0)
            horizontal *= recov;

        sprayLength *= 0.95F;

        if (playerSpeed > 0.00F)
        {
            float speedMod = (1 + playerSpeed * speed);
            vertical *= speedMod;
            horizontal *= speedMod;
        }

        float anti = antiRecoil * 0.2F;

        antiRecoil *= 0.8F;
        antiRecoil += vertical * Math.max(0F, Math.min(1F, 1F - rand.nextFloat() * 0.2F - (fall - 1F)));

        return -vertical + anti;
    }
}
