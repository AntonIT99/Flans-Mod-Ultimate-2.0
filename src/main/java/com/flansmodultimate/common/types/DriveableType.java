package com.flansmodultimate.common.types;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

public class DriveableType extends PaintableType
{
    //TODO: implement
    @Getter
    protected String lockedOnSound = StringUtils.EMPTY;
    @Getter
    protected int soundTime;
    @Getter
    protected int lockedOnSoundRange = 5;
}
