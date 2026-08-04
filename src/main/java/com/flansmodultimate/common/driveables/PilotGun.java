package com.flansmodultimate.common.driveables;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

/** Pilot-controlled gun mount. The gun reference is resolved lazily after all packs load. */
@Getter
public final class PilotGun extends DriveablePosition
{
    private final String gunTypeShortName;
    @Nullable private final IContentProvider contentPack;

    public PilotGun(Vector3f position, EnumDriveablePart part, String gunTypeShortName)
    {
        this(position, part, gunTypeShortName, null);
    }

    public PilotGun(Vector3f position, EnumDriveablePart part, String gunTypeShortName, @Nullable IContentProvider contentPack)
    {
        super(position, part);
        this.gunTypeShortName = StringUtils.defaultString(gunTypeShortName);
        this.contentPack = contentPack;
    }

    @Nullable
    public GunType getType()
    {
        InfoType resolved = InfoType.getInfoType(gunTypeShortName, contentPack);
        return resolved instanceof GunType gunType ? gunType : null;
    }
}
