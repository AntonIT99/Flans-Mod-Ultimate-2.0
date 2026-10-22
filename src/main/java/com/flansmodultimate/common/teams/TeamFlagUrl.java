package com.flansmodultimate.common.teams;

import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/** Web addresses the Teams editor accepts for a team flag, in place of a texture of a resource pack. */
public final class TeamFlagUrl
{
    public static final int MAX_LENGTH = 1024;

    private TeamFlagUrl()
    {}

    /** Whether {@code value} names a web address rather than a texture, valid or not. */
    @SuppressWarnings("HttpUrlsUsage") // Plain http is accepted on purpose, for servers hosting their own images.
    public static boolean isUrl(@Nullable String value)
    {
        if (value == null)
            return false;
        String lower = value.trim().toLowerCase(Locale.ROOT);
        return lower.startsWith("https://") || lower.startsWith("http://");
    }

    /** The address {@code value} names, or null when it is no plain http or https address with a host. */
    @Nullable
    public static URI parse(@Nullable String value)
    {
        if (!isUrl(value) || value.trim().length() > MAX_LENGTH)
            return null;
        try
        {
            URI uri = new URI(value.trim());
            boolean web = "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
            return web && uri.getHost() != null && !uri.getHost().isBlank() && uri.getRawUserInfo() == null ? uri : null;
        }
        catch (URISyntaxException exception)
        {
            return null;
        }
    }
}
