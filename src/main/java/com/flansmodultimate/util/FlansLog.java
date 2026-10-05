package com.flansmodultimate.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared logging without initializing the mod's registries or client classes. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlansLog
{
    public static final String LOGGER_NAME = "com.flansmodultimate";
    public static final Logger log = LoggerFactory.getLogger(LOGGER_NAME);
}
