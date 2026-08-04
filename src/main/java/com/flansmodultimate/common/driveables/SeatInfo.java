package com.flansmodultimate.common.driveables;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

/** Immutable-position, mutable-aim definition for a driver or passenger seat. */
@Getter
public final class SeatInfo
{
    private final int id;
    private final Vector3f position;
    private final EnumDriveablePart part;
    private final boolean driver;
    private final String gunTypeShortName;
    private final String gunName;
    @Nullable private final IContentProvider contentPack;

    @Setter private float minYaw = -360F;
    @Setter private float maxYaw = 360F;
    @Setter private float minPitch = -89F;
    @Setter private float maxPitch = 89F;
    @Setter private int gunnerID = -1;
    @Setter private Vector3f rotatedOffset = new Vector3f();
    @Setter private Vector3f gunOrigin;
    @Setter private Vector3f aimingSpeed = new Vector3f(2F, 2F, 0F);
    @Setter private boolean legacyAiming;
    @Setter private boolean yawBeforePitch;
    @Setter private boolean traverseSounds;
    @Setter private boolean latePitch = true;
    @Setter private String yawSound = StringUtils.EMPTY;
    @Setter private int yawSoundLength;
    @Setter private String pitchSound = StringUtils.EMPTY;
    @Setter private int pitchSoundLength;

    public SeatInfo(int id, Vector3f position, EnumDriveablePart part, boolean driver,
                    float minYaw, float maxYaw, float minPitch, float maxPitch,
                    @Nullable String gunTypeShortName, @Nullable String gunName)
    {
        this(id, position, part, driver, minYaw, maxYaw, minPitch, maxPitch, gunTypeShortName, gunName, null);
    }

    public SeatInfo(int id, Vector3f position, EnumDriveablePart part, boolean driver,
                    float minYaw, float maxYaw, float minPitch, float maxPitch,
                    @Nullable String gunTypeShortName, @Nullable String gunName,
                    @Nullable IContentProvider contentPack)
    {
        this.id = Math.max(0, id);
        this.position = new Vector3f(position.x, position.y, position.z);
        this.part = part == null ? EnumDriveablePart.CORE : part;
        this.driver = driver;
        this.minYaw = Math.min(minYaw, maxYaw);
        this.maxYaw = Math.max(minYaw, maxYaw);
        this.minPitch = Math.max(-89.9F, Math.min(minPitch, maxPitch));
        this.maxPitch = Math.min(89.9F, Math.max(minPitch, maxPitch));
        this.gunTypeShortName = StringUtils.defaultString(gunTypeShortName);
        this.gunName = StringUtils.defaultString(gunName);
        this.contentPack = contentPack;
        this.gunOrigin = new Vector3f(position.x, position.y, position.z);
    }

    public int getX() { return Math.round(position.x * 16F); }
    public int getY() { return Math.round(position.y * 16F); }
    public int getZ() { return Math.round(position.z * 16F); }

    @Nullable
    public GunType getGunType()
    {
        InfoType resolved = InfoType.getInfoType(gunTypeShortName, contentPack);
        return resolved instanceof GunType gunType ? gunType : null;
    }
}
