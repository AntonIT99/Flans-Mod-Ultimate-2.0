package com.flansmodultimate.config;

import lombok.Getter;

/** Units available for the client-side driveable speed readout. */
public enum EnumSpeedUnit
{
    KMH(3.6D, "km/h"),
    METERS_PER_SECOND(1D, "m/s"),
    MPH(2.2369362920544D, "mph");

    private final double blocksPerSecondMultiplier;
    @Getter
    private final String symbol;

    EnumSpeedUnit(double blocksPerSecondMultiplier, String symbol)
    {
        this.blocksPerSecondMultiplier = blocksPerSecondMultiplier;
        this.symbol = symbol;
    }

    public double convert(double blocksPerSecond)
    {
        return blocksPerSecond * blocksPerSecondMultiplier;
    }
}
