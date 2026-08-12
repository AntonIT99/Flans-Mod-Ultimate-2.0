package com.flansmodultimate.config;

/** Units available for the client-side driveable speed readout. */
public enum EnumSpeedUnit
{
    KMH(3.6D, "km/h"),
    METERS_PER_SECOND(1D, "m/s"),
    MPH(2.2369362920544D, "mph");

    private final double blocksPerSecondMultiplier;
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

    public String getSymbol()
    {
        return symbol;
    }
}
