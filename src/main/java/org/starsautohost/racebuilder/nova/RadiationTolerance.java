package org.starsautohost.racebuilder.nova;

/*
 * Modified from Stars Nova project
 */
public class RadiationTolerance extends EnvironmentTolerance{

    public RadiationTolerance() 
    { 
    }

    // Calculate the minimum and maximum values of the tolerance ranges
    // expressed as a percentage of the total range. 
    // Radiation is in the range 0 to 100.
    @Override
    protected int makeInternalValue(double value)
    {
        return (int)value;
    }

    @Override
    protected String format(int value)
    {
        return value + "mR";
    }
}

